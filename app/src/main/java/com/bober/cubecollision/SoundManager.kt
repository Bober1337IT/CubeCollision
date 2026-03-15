package com.bober.cubecollision

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool

class SoundManager(context: Context, resId: Int) {

    private val soundPool: SoundPool
    private val soundId: Int

    init {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setAudioAttributes(audioAttributes)
            .setMaxStreams(10) // allow overlapping sounds
            .build()

        // Load the sound once
        soundId = soundPool.load(context, resId, 1)
    }

    fun play(volume: Float = 1f) {
        soundPool.play(
            soundId,
            volume,
            volume,
            1,
            0,
            1f
        )
    }
}