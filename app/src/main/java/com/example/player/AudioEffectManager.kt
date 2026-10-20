package com.example.player

import android.content.Context
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
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

class AudioEffectManager(private val context: Context) {


    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null

    private val _isEnabled = MutableStateFlow(true)
    val isEnabled: StateFlow<Boolean> = _isEnabled.asStateFlow()

    private val _currentPreset = MutableStateFlow(EqPreset.FLAT)
    val currentPreset: StateFlow<EqPreset> = _currentPreset.asStateFlow()

    private val _bands = MutableStateFlow<List<EqualizerBand>>(emptyList())
    val bands: StateFlow<List<EqualizerBand>> = _bands.asStateFlow()

    private val _bassBoostStrength = MutableStateFlow(0) // 0 - 1000
    val bassBoostStrength: StateFlow<Int> = _bassBoostStrength.asStateFlow()

    private val _virtualizerStrength = MutableStateFlow(0) // 0 - 1000
    val virtualizerStrength: StateFlow<Int> = _virtualizerStrength.asStateFlow()

    private var currentSessionId: Int = 0

    fun attachToAudioSession(audioSessionId: Int) {
        FileLogger.info("AudioEffectManager", "attachToAudioSession id=$audioSessionId current=$currentSessionId")
        if (audioSessionId <= 0 || audioSessionId == currentSessionId) return
        currentSessionId = audioSessionId

        // SAFETY: release old effects before creating new ones
        release()

        try {
            val eq = runCatching { Equalizer(0, audioSessionId) }.getOrNull() ?: run {
                Log.w("AudioEffectManager", "Equalizer not supported on this device")
                setupSimulatedBands()
                return
            }
            eq.enabled = false
            equalizer = eq

            val numBands = eq.numberOfBands
            val minLevel = eq.bandLevelRange[0]
            val maxLevel = eq.bandLevelRange[1]
            val list = mutableListOf<EqualizerBand>()

            for (i in 0 until numBands) {
                val bandIndex = i.toShort()
                val centerFreq = eq.getCenterFreq(bandIndex) / 1000
                val level = eq.getBandLevel(bandIndex)
                list.add(EqualizerBand(bandIndex, centerFreq, minLevel, maxLevel, level))
            }
            _bands.value = list

            // Bass Boost
            try {
                val bb = runCatching { BassBoost(0, audioSessionId) }.getOrNull()
                if (bb != null) {
                    bb.enabled = false
                    if (bb.strengthSupported) {
                        bb.setStrength(_bassBoostStrength.value.toShort())
                    }
                    bassBoost = bb
                } else {
                    Log.d("AudioEffectManager", "BassBoost not available")
                }
            } catch (e: Exception) {
                Log.d("AudioEffectManager", "BassBoost unavailable on this device/emulator: ${e.message}")
            }

            // Virtualizer
            try {
                val virt = runCatching { Virtualizer(0, audioSessionId) }.getOrNull()
                if (virt != null) {
                    virt.enabled = false
                    if (virt.strengthSupported) {
                        virt.setStrength(_virtualizerStrength.value.toShort())
                    }
                    virtualizer = virt
                } else {
                    Log.d("AudioEffectManager", "Virtualizer not available")
                }
            } catch (e: Exception) {
                Log.d("AudioEffectManager", "Virtualizer unavailable on this device/emulator: ${e.message}")
            }

        } catch (e: Exception) {
            FileLogger.error("AudioEffectManager", "Equalizer attach failed", e)
            Log.w("AudioEffectManager", "Equalizer hardware attach fallback: ${e.message}")
            // Provide simulated bands for UI controls even if hardware audiofx is in emulator sandbox
            setupSimulatedBands()
        }
        setEnabled(_isEnabled.value)
    }

    private fun setupSimulatedBands() {
        if (_bands.value.isNotEmpty()) return
        val defaultFreqs = listOf(60, 230, 910, 3600, 14000)
        _bands.value = defaultFreqs.mapIndexed { idx, freq ->
            EqualizerBand(
                index = idx.toShort(),
                centerFreqHz = freq,
                minLevelMb = -1500,
                maxLevelMb = 1500,
                currentLevelMb = 0
            )
        }
    }

    fun setEnabled(enabled: Boolean) {
        _isEnabled.value = enabled
        try {
            equalizer?.enabled = enabled
            bassBoost?.enabled = enabled
            virtualizer?.enabled = enabled
        } catch (e: Exception) {}
    }

    fun setBandLevel(bandIndex: Short, levelMb: Short) {
        try {
            equalizer?.setBandLevel(bandIndex, levelMb)
        } catch (e: Exception) {}

        _bands.value = _bands.value.map { band ->
            if (band.index == bandIndex) band.copy(currentLevelMb = levelMb) else band
        }
    }

    fun setBassBoost(strength: Int) {
        val safe = strength.coerceIn(0, 1000)
        _bassBoostStrength.value = safe
        try {
            bassBoost?.let {
                if (it.strengthSupported) it.setStrength(safe.toShort())
            }
        } catch (e: Exception) {}
    }

    fun setVirtualizer(strength: Int) {
        val safe = strength.coerceIn(0, 1000)
        _virtualizerStrength.value = safe
        try {
            virtualizer?.let {
                if (it.strengthSupported) it.setStrength(safe.toShort())
            }
        } catch (e: Exception) {}
    }

    fun applyPreset(preset: EqPreset) {
        _currentPreset.value = preset
        val levels = when (preset) {
            EqPreset.FLAT -> listOf(0, 0, 0, 0, 0)
            EqPreset.BASS_BOOST -> listOf(900, 600, 200, 0, 0)
            EqPreset.VOCAL -> listOf(-300, 100, 800, 600, 200)
            EqPreset.ROCK -> listOf(600, 400, -200, 300, 700)
            EqPreset.POP -> listOf(-200, 300, 600, 400, -100)
            EqPreset.JAZZ -> listOf(400, 200, 100, 300, 500)
            EqPreset.ELECTRONIC -> listOf(800, 500, 0, 400, 900)
            EqPreset.CLASSICAL -> listOf(500, 300, -100, 300, 400)
        }

        val currentList = _bands.value
        currentList.forEachIndexed { i, band ->
            val targetLevel = (levels.getOrNull(i) ?: 0).toShort()
            setBandLevel(band.index, targetLevel)
        }

        if (preset == EqPreset.BASS_BOOST) {
            setBassBoost(750)
        } else if (preset == EqPreset.ELECTRONIC) {
            setBassBoost(600)
            setVirtualizer(500)
        }
    }

    fun release() {
        try {
            equalizer?.release()
            bassBoost?.release()
            virtualizer?.release()
        } catch (e: Exception) {}
        equalizer = null
        bassBoost = null
        virtualizer = null
    }
}
