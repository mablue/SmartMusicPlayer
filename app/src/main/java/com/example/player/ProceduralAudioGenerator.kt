package com.example.player

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sin

/**
 * Generates lightweight, melodic WAV audio files locally in cacheDir.
 * Guarantees that every song has playable, real acoustic sound even offline
 * without relying on external network connectivity or local mp3 uploads.
 */
object ProceduralAudioGenerator {

    private const val TAG = "ProceduralAudio"
    private const val SAMPLE_RATE = 22050

    fun getOrCreateSongAudioFile(
        context: Context,
        songId: Long,
        baseFrequency: Double = 440.0,
        durationSeconds: Int = 45
    ): String {
        val safeDuration = durationSeconds.coerceIn(45, 600)
        val minBytes = safeDuration.toLong() * SAMPLE_RATE * 2
        val fileName = "smart_song_${songId}_${safeDuration}s.wav"
        val file = File(context.cacheDir, fileName)
        if (file.exists() && file.length() >= minBytes) {
            return file.absolutePath
        }

        try {
            val numSamples = SAMPLE_RATE * safeDuration
            val audioData = ShortArray(numSamples)

            // Musical melody scale notes based on songId
            val chordProgressions = listOf(
                listOf(261.63, 329.63, 392.00, 523.25), // C Major
                listOf(220.00, 261.63, 329.63, 440.00), // A Minor
                listOf(174.61, 220.00, 261.63, 349.23), // F Major
                listOf(196.00, 246.94, 293.66, 392.00)  // G Major
            )
            val selectedChord = chordProgressions[(songId % chordProgressions.size).toInt()]

            var phase = 0.0
            val beatSamples = SAMPLE_RATE / 2 // half second per arpeggio note

            for (i in 0 until numSamples) {
                val noteIdx = ((i / beatSamples) % selectedChord.size)
                val targetFreq = selectedChord[noteIdx]
                val envelope = 0.5 + 0.5 * sin(2.0 * Math.PI * (i % beatSamples) / beatSamples)
                val sampleValue = (sin(phase) * 16000.0 * envelope).toInt().toShort()

                audioData[i] = sampleValue
                phase += 2.0 * Math.PI * targetFreq / SAMPLE_RATE
                if (phase > 2.0 * Math.PI) {
                    phase -= 2.0 * Math.PI
                }
            }

            writeWavFile(file, audioData, SAMPLE_RATE)
            return file.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Error generating procedural audio", e)
            return ""
        }
    }

    private fun writeWavFile(file: File, pcmData: ShortArray, sampleRate: Int) {
        val byteData = ByteArray(pcmData.size * 2)
        val byteBuffer = ByteBuffer.wrap(byteData).order(ByteOrder.LITTLE_ENDIAN)
        for (sample in pcmData) {
            byteBuffer.putShort(sample)
        }

        FileOutputStream(file).use { out ->
            val totalDataLen = byteData.size + 36
            val byteRate = sampleRate * 2

            // RIFF header
            out.write("RIFF".toByteArray())
            out.write(intToByteArray(totalDataLen))
            out.write("WAVE".toByteArray())

            // fmt chunk
            out.write("fmt ".toByteArray())
            out.write(intToByteArray(16)) // subchunk1 size
            out.write(shortToByteArray(1)) // audio format 1 = PCM
            out.write(shortToByteArray(1)) // num channels = 1 (mono)
            out.write(intToByteArray(sampleRate))
            out.write(intToByteArray(byteRate))
            out.write(shortToByteArray(2)) // block align
            out.write(shortToByteArray(16)) // bits per sample

            // data chunk
            out.write("data".toByteArray())
            out.write(intToByteArray(byteData.size))
            out.write(byteData)
        }
    }

    private fun intToByteArray(value: Int): ByteArray =
        ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array()

    private fun shortToByteArray(value: Short): ByteArray =
        ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(value).array()
}
