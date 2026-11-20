package org.littlelantern.video

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.nio.ByteOrder
import kotlin.coroutines.coroutineContext
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Checks pace, flashes, colour and loudness by streaming the lowest-quality version of a video
 * and decoding it in memory. Frames are measured and thrown away; nothing is written to storage.
 */
object StreamCheck {

    data class Visual(val cutsPerMinute: Double?, val flashesMax: Int, val redFlashesMax: Int, val saturation: Double)

    private val headers = mapOf("User-Agent" to Net.USER_AGENT)
    private const val TIMEOUT_US = 10_000L
    private const val GRID_W = 32
    private const val GRID_H = 18

    suspend fun visual(url: String, durationHintSeconds: Long, onProgress: (Float) -> Unit): Visual =
        withContext(Dispatchers.Default) {
            val extractor = MediaExtractor()
            var codec: MediaCodec? = null
            try {
                extractor.setDataSource(url, headers)
                val track = (0 until extractor.trackCount).firstOrNull {
                    extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("video/") == true
                } ?: error("No video track")
                extractor.selectTrack(track)
                val format = extractor.getTrackFormat(track)
                val mime = format.getString(MediaFormat.KEY_MIME)!!
                val durationS = if (format.containsKey(MediaFormat.KEY_DURATION)) {
                    format.getLong(MediaFormat.KEY_DURATION) / 1e6
                } else {
                    durationHintSeconds.toDouble()
                }
                format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)
                val decoder = MediaCodec.createDecoderByType(mime)
                codec = decoder
                decoder.configure(format, null, null, 0)
                decoder.start()

                val cuts = CutCounter()
                val flashes = FlashCounter()
                val redFlashes = FlashCounter(minChange = 0.25, darkBelow = 1.01)
                var satSum = 0.0
                var frames = 0
                val info = MediaCodec.BufferInfo()
                var inputDone = false

                while (true) {
                    coroutineContext.ensureActive()
                    if (!inputDone) {
                        val inIndex = decoder.dequeueInputBuffer(TIMEOUT_US)
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
                    val outIndex = decoder.dequeueOutputBuffer(info, TIMEOUT_US)
                    if (outIndex >= 0) {
                        val t = info.presentationTimeUs / 1e6
                        decoder.getOutputImage(outIndex)?.use { image ->
                            val m = measure(image)
                            cuts.onFrame(m.histogram, t)
                            flashes.onFrame(m.luminance, t)
                            redFlashes.onFrame(m.redShare, t)
                            satSum += m.saturation
                            frames++
                        }
                        decoder.releaseOutputBuffer(outIndex, false)
                        if (durationS > 0) onProgress((t / durationS).toFloat().coerceIn(0f, 1f))
                        if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) break
                    }
                }
                Visual(
                    cutsPerMinute = cuts.perMinute(durationS),
                    flashesMax = flashes.maxFlashesPerSecond,
                    redFlashesMax = redFlashes.maxFlashesPerSecond,
                    saturation = if (frames == 0) 0.0 else satSum / frames,
                )
            } finally {
                runCatching { codec?.stop() }
                runCatching { codec?.release() }
                extractor.release()
            }
        }

    private class FrameMeasure(val histogram: DoubleArray, val luminance: Double, val saturation: Double, val redShare: Double)

    /** Samples a 32 x 18 grid of the frame: luma histogram, mean relative luminance, chroma strength, share of saturated red. */
    private fun measure(image: android.media.Image): FrameMeasure {
        val y = image.planes[0]
        val u = image.planes[1]
        val v = image.planes[2]
        val w = image.width
        val h = image.height
        val hist = DoubleArray(16)
        var lumSum = 0.0
        var satSum = 0.0
        var red = 0
        val n = GRID_W * GRID_H
        for (gy in 0 until GRID_H) {
            val py = (gy * h) / GRID_H + h / (2 * GRID_H)
            for (gx in 0 until GRID_W) {
                val px = (gx * w) / GRID_W + w / (2 * GRID_W)
                val yv = y.buffer.get(py * y.rowStride + px * y.pixelStride).toInt() and 0xFF
                val cx = px / 2
                val cy = py / 2
                val uv = (u.buffer.get(cy * u.rowStride + cx * u.pixelStride).toInt() and 0xFF) - 128
                val vv = (v.buffer.get(cy * v.rowStride + cx * v.pixelStride).toInt() and 0xFF) - 128
                hist[yv shr 4] += 1.0
                lumSum += (yv / 255.0).pow(2.2)
                satSum += sqrt((uv * uv + vv * vv).toDouble()) / 128.0
                if (vv > 45 && uv < 0 && yv in 40..200) red++
            }
        }
        for (i in hist.indices) hist[i] /= n.toDouble()
        return FrameMeasure(hist, lumSum / n, satSum / n, red.toDouble() / n)
    }

    /** Decodes the lowest-bitrate audio stream and counts sudden loudness jumps. */
    suspend fun loudnessJumps(url: String): Int = withContext(Dispatchers.Default) {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        try {
            extractor.setDataSource(url, headers)
            val track = (0 until extractor.trackCount).firstOrNull {
                extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            } ?: error("No audio track")
            extractor.selectTrack(track)
            val format = extractor.getTrackFormat(track)
            val decoder = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME)!!)
            codec = decoder
            decoder.configure(format, null, null, 0)
            decoder.start()

            val sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            val channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            val windowSamples = sampleRate / 10 * channels
            val counter = LoudnessJumpCounter()
            var sumSquares = 0.0
            var count = 0
            val info = MediaCodec.BufferInfo()
            var inputDone = false

            while (true) {
                coroutineContext.ensureActive()
                if (!inputDone) {
                    val inIndex = decoder.dequeueInputBuffer(TIMEOUT_US)
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
                val outIndex = decoder.dequeueOutputBuffer(info, TIMEOUT_US)
                if (outIndex >= 0) {
                    val out = decoder.getOutputBuffer(outIndex)!!.order(ByteOrder.nativeOrder()).asShortBuffer()
                    while (out.hasRemaining()) {
                        val s = out.get() / 32768.0
                        sumSquares += s * s
                        count++
                        if (count >= windowSamples) {
                            val rms = sqrt(sumSquares / count)
                            counter.onWindow(if (rms <= 1e-6) -120.0 else 20 * log10(rms))
                            sumSquares = 0.0
                            count = 0
                        }
                    }
                    decoder.releaseOutputBuffer(outIndex, false)
                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) break
                }
            }
            counter.jumps
        } finally {
            runCatching { codec?.stop() }
            runCatching { codec?.release() }
            extractor.release()
        }
    }
}
