package com.example.player

import android.content.Context
import android.os.Handler
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink.DefaultAudioProcessorChain
import androidx.media3.common.audio.AudioProcessor

/**
 * Custom RenderersFactory that injects ReplayGainAudioProcessor into the
 * default audio pipeline (before the platform AudioTrack).
 *
 * Reference: Media3 ExoPlayer custom render factory pattern
 * https://developer.android.com/media/media3/exoplayer/customization
 */
@UnstableApi
class ReplayGainRenderersFactory(
    context: Context,
    private val replayGainProcessor: ReplayGainAudioProcessor
) : DefaultRenderersFactory(context) {

    override fun buildAudioSink(
        context: Context,
        enableFloatOutput: Boolean,
        enableAudioTrackPlaybackParams: Boolean
    ): AudioSink? {
        return DefaultAudioSink.Builder(context)
            .setEnableFloatOutput(enableFloatOutput)
            .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
            .setAudioProcessorChain(
                DefaultAudioProcessorChain(
                    replayGainProcessor // inject our processor
                )
            )
            .build()
    }
}
