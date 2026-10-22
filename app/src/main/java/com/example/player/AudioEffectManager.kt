package com.example.player

import android.content.Context
import android.media.audiofx.Equalizer
import android.util.Log
import com.example.util.FileLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class EqualizerBand(
    val index: Short,
    val centerFreqHz: Int,
    val minLevelMb: Short,
    val maxLevelMb: Short,
    var currentLevelMb: Short
) {
    val formattedFreq: String
        get() = if (centerFreqHz >= 1000) "${centerFreqHz / 1000} kHz" else "$centerFreqHz Hz"
}

enum class EqPreset(val titleFa: String, val titleEn: String) {
    FLAT("عادی (خطی)", "Flat"),
    BASS_BOOST("بیس عمیق", "Deep Bass"),
    VOCAL("وضوح صدا (وکال)", "Vocal Boost"),
    ROCK("راک", "Rock"),
    POP("پاپ", "Pop"),
    JAZZ("جاز", "Jazz"),
    ELECTRONIC("الکترونیک", "Electronic"),
    CLASSICAL("کلاسیک", "Classical")
}

/**
 * Hardware equalizer only.
 * Extra DSP effects were removed in v1.1.6 because their HAL
 * implementations are unreliable across many Android devices.
 */
class AudioEffectManager(private val context: Context) {

    private var equalizer: Equalizer? = null
    private var currentSessionId: Int = 0

    private val _isEnabled = MutableStateFlow(true)
    val isEnabled: StateFlow<Boolean> = _isEnabled.asStateFlow()

    private val _currentPreset = MutableStateFlow(EqPreset.FLAT)
    val currentPreset: StateFlow<EqPreset> = _currentPreset.asStateFlow()

    private val _bands = MutableStateFlow<List<EqualizerBand>>(emptyList())
    val bands: StateFlow<List<EqualizerBand>> = _bands.asStateFlow()

    fun attachToAudioSession(audioSessionId: Int) {
        FileLogger.info("AudioEffectManager", "attachToAudioSession id=$audioSessionId current=$currentSessionId")
        if (audioSessionId <= 0 || audioSessionId == currentSessionId) return
        currentSessionId = audioSessionId
        release()

        try {
            val eq = runCatching { Equalizer(0, audioSessionId) }.getOrNull() ?: run {
                Log.w("AudioEffectManager", "Equalizer not supported")
                setupSimulatedBands(); return
            }
            eq.enabled = false
            equalizer = eq

            val list = mutableListOf<EqualizerBand>()
            for (i in 0 until eq.numberOfBands) {
                val bandIndex = i.toShort()
                val centerFreq = eq.getCenterFreq(bandIndex) / 1000
                val level = eq.getBandLevel(bandIndex)
                list.add(EqualizerBand(bandIndex, centerFreq, eq.bandLevelRange[0], eq.bandLevelRange[1], level))
            }
            _bands.value = list
        } catch (e: Exception) {
            FileLogger.error("AudioEffectManager", "Equalizer attach failed", e)
            setupSimulatedBands()
        }
    }

    private fun setupSimulatedBands() {
        if (_bands.value.isNotEmpty()) return
        val freqs = listOf(60, 230, 910, 3600, 14000)
        _bands.value = freqs.mapIndexed { idx, f ->
            EqualizerBand(idx.toShort(), f, -1500, 1500, 0)
        }
    }

    fun setEnabled(enabled: Boolean) {
        _isEnabled.value = enabled
        try { equalizer?.enabled = enabled } catch (e: Exception) {}
    }

    fun setBandLevel(bandIndex: Short, levelMb: Short) {
        try { equalizer?.setBandLevel(bandIndex, levelMb) } catch (e: Exception) {}
        _bands.value = _bands.value.map { if (it.index == bandIndex) it.copy(currentLevelMb = levelMb) else it }
    }

    fun applyPreset(preset: EqPreset) {
        _currentPreset.value = preset
        val levels = when (preset) {
            EqPreset.FLAT -> listOf(0, 0, 0, 0, 0)
            EqPreset.BASS_BOOST -> listOf(1400, 1000, 400, 0, 0)
            EqPreset.VOCAL -> listOf(-300, 100, 800, 600, 200)
            EqPreset.ROCK -> listOf(600, 400, -200, 300, 700)
            EqPreset.POP -> listOf(-200, 300, 600, 400, -100)
            EqPreset.JAZZ -> listOf(400, 200, 100, 300, 500)
            EqPreset.ELECTRONIC -> listOf(900, 600, 0, 400, 900)
            EqPreset.CLASSICAL -> listOf(500, 300, -100, 300, 400)
        }
        _bands.value.forEachIndexed { i, band ->
            setBandLevel(band.index, (levels.getOrNull(i) ?: 0).toShort())
        }
    }

    fun applyUserPreferences() { setEnabled(_isEnabled.value) }

    fun release() {
        try { equalizer?.release() } catch (e: Exception) {}
        equalizer = null
    }
}
