package org.littlelantern.check

import android.graphics.Color
import android.media.Image
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.log10

/** Measured on the phone from short streamed samples. Nothing is saved. */
data class MediaMeasurements(
    val cutsPerMinute: Double?,
    val flashesPerMinute: Double?,
    val meanSaturation: Double?,
    val loudSpikesPerMinute: Double?,
)

/**
 * Streams [Sampling.SAMPLES] short stretches spread across the whole video and measures them in memory.
 * Frames and sound are decoded, measured and dropped; nothing touches storage.
 */
object VideoAnalyzer {

    private const val FRAME_STEP_US = 250_000L // 4 frames per second
    private const val CUT_THRESHOLD = 0.45
    private const val FLASH_THRESHOLD = 0.30
    private const val TIMEOUT_US = 10_000L
    private const val MAX_IDLE_LOOPS = 3_000

    fun analyze(
        videoUrl: String,
        audioUrl: String?,
        durationMs: Long,
        headers: Map<String, String>,
        onProgress: (Float) -> Unit,
    ): MediaMeasurements {
        val segments = Sampling.segments(durationMs)
        if (segments.isEmpty()) return MediaMeasurements(null, null, null, null)
        val visual = runCatching {
            analyzeVideo(videoUrl, headers, segments) { onProgress(it * 0.7f) }
        }.getOrNull()
        val loud = runCatching {
            analyzeAudio(audioUrl ?: videoUrl, headers, segments) { onProgress(0.7f + it * 0.3f) }
        }.getOrNull()
        onProgress(1f)
        return MediaMeasurements(visual?.cuts, visual?.flashes, visual?.saturation, loud)
    }

    private class Visual(val cuts: Double, val flashes: Double, val saturation: Double)

    private fun analyzeVideo(url: String, headers: Map<String, String>, segments: List<Segment>, onProgress: (Float) -> Unit): Visual? {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        try {
            extractor.setDataSource(url, headers)
            val track = findTrack(extractor, "video/") ?: return null
            extractor.selectTrack(track)
            val format = extractor.getTrackFormat(track)
            val decoder = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME)!!)
            codec = decoder
            decoder.configure(format, null, null, 0)
            decoder.start()

            var cuts = 0
            var flashes = 0
            var satSum = 0.0
            var samples = 0
            var measuredMs = 0L
            val info = MediaCodec.BufferInfo()

            segments.forEachIndexed { index, seg ->
                val startUs = seg.startMs * 1000
                val endUs = seg.endMs * 1000
                extractor.seekTo(startUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
                decoder.flush()
                var prevHist: IntArray? = null
                var prevLuma = -1.0
                var lastSampleUs = Long.MIN_VALUE
                var inputDone = false
                var idle = 0
                var reachedEnd = false
                while (!reachedEnd && idle < MAX_IDLE_LOOPS) {
                    if (!inputDone) {
                        val inIndex = decoder.dequeueInputBuffer(TIMEOUT_US)
                        if (inIndex >= 0) {
                            val size = extractor.readSampleData(decoder.getInputBuffer(inIndex)!!, 0)
                            if (size < 0 || extractor.sampleTime > endUs + 1_000_000) {
                                decoder.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                inputDone = true
                            } else {
                                decoder.queueInputBuffer(inIndex, 0, size, extractor.sampleTime, 0)
                                extractor.advance()
                            }
                        }
                    }
                    val outIndex = decoder.dequeueOutputBuffer(info, TIMEOUT_US)
                    if (outIndex >= 0) {
                        idle = 0
                        val pts = info.presentationTimeUs
                        if (pts >= startUs && pts - lastSampleUs >= FRAME_STEP_US) {
                            decoder.getOutputImage(outIndex)?.use { image ->
                                val s = sample(image)
                                prevHist?.let { if (histogramDistance(it, s.hist) > CUT_THRESHOLD) cuts++ }
                                if (prevLuma >= 0 && abs(s.luma - prevLuma) > FLASH_THRESHOLD) flashes++
                                prevHist = s.hist
                                prevLuma = s.luma
                                satSum += s.saturation
                                samples++
                            }
                            lastSampleUs = pts
                        }
                        decoder.releaseOutputBuffer(outIndex, false)
                        if (pts >= endUs || info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) reachedEnd = true
                    } else {
                        idle++
                    }
                }
                measuredMs += seg.lengthMs
                onProgress((index + 1).toFloat() / segments.size)
            }
            if (samples < 2) return null
            val minutes = measuredMs / 60_000.0
            return Visual(cuts / minutes, flashes / minutes, satSum / samples)
        } finally {
            codec?.let {
                runCatching { it.stop() }
                it.release()
            }
            extractor.release()
        }
    }

    private class FrameSample(val hist: IntArray, val luma: Double, val saturation: Double)

    /** Reads a 32x18 grid of pixels from a YUV_420_888 frame, converts to RGB, and measures it. */
    private fun sample(image: Image): FrameSample {
        val yPlane = image.planes[0]
        val uPlane = image.planes[1]
        val vPlane = image.planes[2]
        val w = image.width
        val h = image.height
        val hist = IntArray(64)
        val hsv = FloatArray(3)
        var luma = 0.0
        var sat = 0.0
        var n = 0
        for (gy in 0 until 18) {
            val y = ((gy + 0.5) * h / 18).toInt().coerceIn(0, h - 1)
            for (gx in 0 until 32) {
                val x = ((gx + 0.5) * w / 32).toInt().coerceIn(0, w - 1)
                val yy = yPlane.buffer.get(y * yPlane.rowStride + x * yPlane.pixelStride).toInt() and 0xFF
                val u = (uPlane.buffer.get((y / 2) * uPlane.rowStride + (x / 2) * uPlane.pixelStride).toInt() and 0xFF) - 128
                val v = (vPlane.buffer.get((y / 2) * vPlane.rowStride + (x / 2) * vPlane.pixelStride).toInt() and 0xFF) - 128
                val r = (yy + 1.402 * v).toInt().coerceIn(0, 255)
                val g = (yy - 0.344 * u - 0.714 * v).toInt().coerceIn(0, 255)
                val b = (yy + 1.772 * u).toInt().coerceIn(0, 255)
                hist[(r shr 6) * 16 + (g shr 6) * 4 + (b shr 6)]++
                luma += yy / 255.0
                Color.RGBToHSV(r, g, b, hsv)
                sat += hsv[1]
                n++
            }
        }
        return FrameSample(hist, luma / n, sat / n)
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

    /** Sudden jumps of 10 dB or more above the typical level, per minute of sampled sound. */
    private fun analyzeAudio(url: String, headers: Map<String, String>, segments: List<Segment>, onProgress: (Float) -> Unit): Double? {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        try {
            extractor.setDataSource(url, headers)
            val track = findTrack(extractor, "audio/") ?: return null
            extractor.selectTrack(track)
            val format = extractor.getTrackFormat(track)
            val decoder = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME)!!)
            codec = decoder
            decoder.configure(format, null, null, 0)
            decoder.start()

            var windowSize = format.getInteger(MediaFormat.KEY_SAMPLE_RATE) * format.getInteger(MediaFormat.KEY_CHANNEL_COUNT) / 10
            val perSegmentDb = ArrayList<List<Double>>()
            val info = MediaCodec.BufferInfo()

            segments.forEachIndexed { index, seg ->
                val startUs = seg.startMs * 1000
                val endUs = seg.endMs * 1000
                extractor.seekTo(startUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
                decoder.flush()
                val windows = ArrayList<Double>()
                var acc = 0.0
                var count = 0
                var inputDone = false
                var idle = 0
                var reachedEnd = false
                while (!reachedEnd && idle < MAX_IDLE_LOOPS) {
                    if (!inputDone) {
                        val inIndex = decoder.dequeueInputBuffer(TIMEOUT_US)
                        if (inIndex >= 0) {
                            val size = extractor.readSampleData(decoder.getInputBuffer(inIndex)!!, 0)
                            if (size < 0 || extractor.sampleTime > endUs) {
                                decoder.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                inputDone = true
                            } else {
                                decoder.queueInputBuffer(inIndex, 0, size, extractor.sampleTime, 0)
                                extractor.advance()
                            }
                        }
                    }
                    val outIndex = decoder.dequeueOutputBuffer(info, TIMEOUT_US)
                    when {
                        outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            val out = decoder.outputFormat
                            windowSize = out.getInteger(MediaFormat.KEY_SAMPLE_RATE) * out.getInteger(MediaFormat.KEY_CHANNEL_COUNT) / 10
                        }
                        outIndex >= 0 -> {
                            idle = 0
                            if (info.presentationTimeUs >= startUs) {
                                val out = decoder.getOutputBuffer(outIndex)!!
                                out.position(info.offset)
                                out.limit(info.offset + info.size)
                                val shorts = out.order(ByteOrder.nativeOrder()).asShortBuffer()
                                while (shorts.hasRemaining()) {
                                    val s = shorts.get() / 32768.0
                                    acc += s * s
                                    count++
                                    if (count >= windowSize) {
                                        windows.add(10 * log10(acc / count + 1e-12))
                                        acc = 0.0
                                        count = 0
                                    }
                                }
                            }
                            decoder.releaseOutputBuffer(outIndex, false)
                            if (info.presentationTimeUs >= endUs || info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                                reachedEnd = true
                            }
                        }
                        else -> idle++
                    }
                }
                perSegmentDb += windows
                onProgress((index + 1).toFloat() / segments.size)
            }
            val all = perSegmentDb.flatten()
            if (all.size < 50) return null
            val median = all.sorted()[all.size / 2]
            var spikes = 0
            for (windows in perSegmentDb) {
                var inSpike = false
                for (db in windows) {
                    val loud = db > median + 10
                    if (loud && !inSpike) spikes++
                    inSpike = loud
                }
            }
            val minutes = all.size / 600.0
            return spikes / minutes
        } finally {
            codec?.let {
                runCatching { it.stop() }
                it.release()
            }
            extractor.release()
        }
    }

    private fun findTrack(extractor: MediaExtractor, prefix: String): Int? =
        (0 until extractor.trackCount).firstOrNull {
            extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith(prefix) == true
        }
}
