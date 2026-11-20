package com.voxmorph.ai.ai

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import java.nio.ByteOrder
import kotlin.math.min

data class DecodedAudio(
    val samples: FloatArray,
    val sampleRate: Int,
    val durationSec: Float
)

/**
 * Decodes WAV, MP3, M4A, and AAC files locally using Android MediaExtractor + MediaCodec
 * into mono float PCM [-1.0, 1.0]. No audio leaves the device.
 */
object AudioDecoder {
    fun decodeUri(context: Context, uri: Uri, maxDurationSec: Float = 30f): DecodedAudio {
        val extractor = MediaExtractor()
        extractor.setDataSource(context, uri, null)

        var audioTrackIndex = -1
        var format: MediaFormat? = null
        for (i in 0 until extractor.trackCount) {
            val f = extractor.getTrackFormat(i)
            val mime = f.getString(MediaFormat.KEY_MIME) ?: continue
            if (mime.startsWith("audio/")) {
                audioTrackIndex = i
                format = f
                break
            }
        }
        require(audioTrackIndex >= 0 && format != null) { "File tidak memiliki track audio yang didukung." }

        extractor.selectTrack(audioTrackIndex)
        val mime = format.getString(MediaFormat.KEY_MIME)!!
        val sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        val channels = if (format.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
            format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        } else 1

        val codec = MediaCodec.createDecoderByType(mime)
        codec.configure(format, null, null, 0)
        codec.start()

        val maxSamples = (sampleRate * maxDurationSec).toInt()
        val pcmOut = ArrayList<Float>(min(maxSamples, sampleRate * 10))
        val bufferInfo = MediaCodec.BufferInfo()
        var inputDone = false
        var outputDone = false

        try {
            while (!outputDone && pcmOut.size < maxSamples) {
                if (!inputDone) {
                    val inIdx = codec.dequeueInputBuffer(10_000)
                    if (inIdx >= 0) {
                        val inBuf = codec.getInputBuffer(inIdx)!!
                        val sampleSize = extractor.readSampleData(inBuf, 0)
                        if (sampleSize < 0) {
                            codec.queueInputBuffer(inIdx, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            codec.queueInputBuffer(inIdx, 0, sampleSize, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }

                val outIdx = codec.dequeueOutputBuffer(bufferInfo, 10_000)
                if (outIdx >= 0) {
                    val outBuf = codec.getOutputBuffer(outIdx)
                    if (outBuf != null && bufferInfo.size > 0) {
                        outBuf.position(bufferInfo.offset)
                        outBuf.limit(bufferInfo.offset + bufferInfo.size)
                        val shortBuf = outBuf.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
                        val shorts = ShortArray(shortBuf.remaining())
                        shortBuf.get(shorts)

                        var idx = 0
                        while (idx + channels <= shorts.size && pcmOut.size < maxSamples) {
                            var monoSum = 0f
                            for (c in 0 until channels) {
                                monoSum += shorts[idx + c] / 32768f
                            }
                            pcmOut.add(monoSum / channels)
                            idx += channels
                        }
                    }
                    codec.releaseOutputBuffer(outIdx, false)
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        outputDone = true
                    }
                }
            }
        } finally {
            codec.stop()
            codec.release()
            extractor.release()
        }

        val floatArr = FloatArray(pcmOut.size) { pcmOut[it] }
        val duration = if (sampleRate > 0) floatArr.size.toFloat() / sampleRate.toFloat() else 0f
        return DecodedAudio(floatArr, sampleRate, duration)
    }
}
