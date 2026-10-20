package com.example.player

import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.audio.BaseAudioProcessor
import java.nio.ByteBuffer

/**
 * Applies ReplayGain (loudness normalization) to PCM audio samples.
 *
 * The gain is read from the MediaItem's MediaMetadata extras and applied
 * on a per-track basis. Supports 16-bit and float PCM encodings.
 *
 * Reference: ReplayGain 1.0 specification (target -14 dBFS = 89 dB SPL)
 * https://wiki.hydrogenaud.io/index.php?title=ReplayGain_1.0_specification
 */
@UnstableApi
class ReplayGainAudioProcessor : BaseAudioProcessor() {

    @Volatile
    private var gainFactor: Float = 1.0f // linear multiplier (1.0 = no change)

    /** Set gain in dB (e.g. -6.5f). Internally converted to linear. */
    fun setGainDb(gainDb: Float) {
        gainFactor = Math.pow(10.0, (gainDb / 20.0)).toFloat()
    }

    fun reset() {
        gainFactor = 1.0f
    }

    override fun onConfigure(inputAudioFormat: androidx.media3.common.audio.AudioProcessor.AudioFormat): androidx.media3.common.audio.AudioProcessor.AudioFormat {
        return when (inputAudioFormat.encoding) {
            C.ENCODING_PCM_16BIT,
            C.ENCODING_PCM_FLOAT -> inputAudioFormat
            else -> androidx.media3.common.audio.AudioProcessor.AudioFormat(
                inputAudioFormat.sampleRate,
                inputAudioFormat.channelCount,
                C.ENCODING_PCM_16BIT
            )
        }
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return

        val output = replaceOutputBuffer(remaining)
        val encoding = inputAudioFormat.encoding

        when (encoding) {
            C.ENCODING_PCM_16BIT -> {
                while (inputBuffer.remaining() >= 2) {
                    val sample = inputBuffer.short
                    val scaled = (sample * gainFactor).toInt()
                        .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                    output.putShort(scaled.toShort())
                }
            }
            C.ENCODING_PCM_FLOAT -> {
                while (inputBuffer.remaining() >= 4) {
                    val sample = inputBuffer.float
                    val scaled = (sample * gainFactor).coerceIn(-1.0f, 1.0f)
                    output.putFloat(scaled)
                }
            }
        }

        output.flip()
    }
}
