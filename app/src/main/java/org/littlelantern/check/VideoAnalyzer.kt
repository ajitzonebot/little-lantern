package org.littlelantern.check

import android.graphics.Bitmap
import android.graphics.Color
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import java.io.File
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.roundToInt

/** Measured on the device from the downloaded files. Nothing is uploaded. */
data class MediaMeasurements(
    val cutsPerMinute: Double?,
    val flashesPerMinute: Double?,
    val meanSaturation: Double?,
    val loudSpikesPerMinute: Double?,
)

object VideoAnalyzer {

    /** Analyse at most the first 10 minutes. */
    private const val MAX_ANALYSED_MS = 10 * 60_000L
    private const val SAMPLES_PER_SECOND = 4.0
    private const val CUT_THRESHOLD = 0.45
    private const val FLASH_THRESHOLD = 0.30
    private const val BATCH = 16

    fun analyze(videoFile: File, audioFile: File?, onProgress: (Float) -> Unit): MediaMeasurements {
        val visual = runCatching { analyzeFrames(videoFile) { onProgress(it * 0.8f) } }.getOrNull()
        val loud = runCatching { audioFile?.let { loudSpikesPerMinute(it) } }.getOrNull()
        onProgress(1f)
        return MediaMeasurements(visual?.cuts, visual?.flashes, visual?.saturation, loud)
    }

    private class Visual(val cuts: Double, val flashes: Double, val saturation: Double)

    private fun analyzeFrames(file: File, onProgress: (Float) -> Unit): Visual? {
        val r = MediaMetadataRetriever()
        try {
            r.setDataSource(file.path)
            val durationMs = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: return null
            val frameCount = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_FRAME_COUNT)?.toIntOrNull() ?: return null
            if (durationMs <= 0 || frameCount <= 0) return null
            val fps = frameCount / (durationMs / 1000.0)
            val step = max(1, (fps / SAMPLES_PER_SECOND).roundToInt())
            val lastFrame = minOf(frameCount, (fps * MAX_ANALYSED_MS / 1000.0).toInt())

            var prevHist: IntArray? = null
            var prevLuma = -1.0
            var cuts = 0
            var flashes = 0
            var satSum = 0.0
            var samples = 0
            var start = 0
            while (start < lastFrame) {
                val n = minOf(BATCH, lastFrame - start)
                val frames = r.getFramesAtIndex(start, n)
                frames.forEachIndexed { i, frame ->
                    if ((start + i) % step == 0) {
                        val s = sample(frame)
                        prevHist?.let { if (histogramDistance(it, s.hist) > CUT_THRESHOLD) cuts++ }
                        if (prevLuma >= 0 && abs(s.luma - prevLuma) > FLASH_THRESHOLD) flashes++
                        prevHist = s.hist
                        prevLuma = s.luma
                        satSum += s.saturation
                        samples++
                    }
                    frame.recycle()
                }
                start += n
                onProgress(start.toFloat() / lastFrame)
            }
            if (samples < 2) return null
            val minutes = (lastFrame / fps) / 60.0
            return Visual(cuts / minutes, flashes / minutes, satSum / samples)
        } finally {
            r.release()
        }
    }

    private class Sample(val hist: IntArray, val luma: Double, val saturation: Double)

    private fun sample(frame: Bitmap): Sample {
        val small = Bitmap.createScaledBitmap(frame, 32, 18, true)
        val pixels = IntArray(32 * 18)
        small.getPixels(pixels, 0, 32, 0, 0, 32, 18)
        if (small !== frame) small.recycle()
        val hist = IntArray(64)
        val hsv = FloatArray(3)
        var luma = 0.0
        var sat = 0.0
        for (p in pixels) {
            val red = Color.red(p)
            val green = Color.green(p)
            val blue = Color.blue(p)
            hist[(red shr 6) * 16 + (green shr 6) * 4 + (blue shr 6)]++
            luma += (0.299 * red + 0.587 * green + 0.114 * blue) / 255.0
            Color.RGBToHSV(red, green, blue, hsv)
            sat += hsv[1]
        }
        return Sample(hist, luma / pixels.size, sat / pixels.size)
    }

    /** 0 = identical colour distribution, 1 = no overlap. */
    private fun histogramDistance(a: IntArray, b: IntArray): Double {
        var diff = 0
        var total = 0
        for (i in a.indices) {
            diff += abs(a[i] - b[i])
            total += a[i]
        }
        return diff / (2.0 * total)
    }

    /** Counts sudden jumps of 10 dB or more above the typical level, per minute. */
    private fun loudSpikesPerMinute(file: File): Double? {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        try {
            extractor.setDataSource(file.path)
            val track = (0 until extractor.trackCount).firstOrNull {
                extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            } ?: return null
            extractor.selectTrack(track)
            val format = extractor.getTrackFormat(track)
            val decoder = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME)!!)
            codec = decoder
            decoder.configure(format, null, null, 0)
            decoder.start()

            var windowSize = format.getInteger(MediaFormat.KEY_SAMPLE_RATE) * format.getInteger(MediaFormat.KEY_CHANNEL_COUNT) / 10
            val windowsDb = ArrayList<Double>()
            var acc = 0.0
            var count = 0
            val info = MediaCodec.BufferInfo()
            var inputDone = false
            var outputDone = false
            while (!outputDone && windowsDb.size < MAX_ANALYSED_MS / 100) {
                if (!inputDone) {
                    val inIndex = decoder.dequeueInputBuffer(10_000)
                    if (inIndex >= 0) {
                        val buffer = decoder.getInputBuffer(inIndex)!!
                        val size = extractor.readSampleData(buffer, 0)
                        if (size < 0) {
                            decoder.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            decoder.queueInputBuffer(inIndex, 0, size, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }
                val outIndex = decoder.dequeueOutputBuffer(info, 10_000)
                if (outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    val out = decoder.outputFormat
                    windowSize = out.getInteger(MediaFormat.KEY_SAMPLE_RATE) * out.getInteger(MediaFormat.KEY_CHANNEL_COUNT) / 10
                } else if (outIndex >= 0) {
                    val out = decoder.getOutputBuffer(outIndex)!!
                    out.position(info.offset)
                    out.limit(info.offset + info.size)
                    val shorts = out.order(ByteOrder.nativeOrder()).asShortBuffer()
                    while (shorts.hasRemaining()) {
                        val s = shorts.get() / 32768.0
                        acc += s * s
                        count++
                        if (count >= windowSize) {
                            windowsDb.add(10 * log10(acc / count + 1e-12))
                            acc = 0.0
                            count = 0
                        }
                    }
                    decoder.releaseOutputBuffer(outIndex, false)
                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) outputDone = true
                }
            }
            if (windowsDb.size < 50) return null
            val median = windowsDb.sorted()[windowsDb.size / 2]
            var spikes = 0
            var inSpike = false
            for (db in windowsDb) {
                val loud = db > median + 10
                if (loud && !inSpike) spikes++
                inSpike = loud
            }
            val minutes = windowsDb.size / 600.0
            return spikes / minutes
        } finally {
            codec?.let {
                runCatching { it.stop() }
                it.release()
            }
            extractor.release()
        }
    }
}
