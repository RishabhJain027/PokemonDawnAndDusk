package com.dawnanddusk.core

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.sin

class AudioService(private val context: Context) {

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    private val scope = CoroutineScope(Dispatchers.Default)

    /**
     * Plays an synthesized retro chime for encounter start.
     */
    fun playEncounterSound() {
        scope.launch {
            vibrate(50)
            playToneSequence(listOf(440 to 100, 554 to 100, 659 to 150, 880 to 250))
        }
    }

    /**
     * Plays a throw whoosh tone.
     */
    fun playThrowSound() {
        scope.launch {
            vibrate(30)
            playToneSequence(listOf(300 to 50, 450 to 50, 600 to 70))
        }
    }

    /**
     * Plays Pokéball shake click.
     */
    fun playBallShakeSound() {
        scope.launch {
            vibrate(40)
            playToneSequence(listOf(523 to 60, 440 to 60))
        }
    }

    /**
     * Plays victory jingle upon successful capture.
     */
    fun playCaptureSuccessSound() {
        scope.launch {
            vibrate(100)
            playToneSequence(listOf(523 to 120, 659 to 120, 783 to 150, 1046 to 350))
        }
    }

    /**
     * Plays escape tone.
     */
    fun playEscapeSound() {
        scope.launch {
            vibrate(80)
            playToneSequence(listOf(400 to 100, 300 to 120, 200 to 200))
        }
    }

    /**
     * Synthesizes audio tones smoothly without external asset dependencies.
     */
    private fun playToneSequence(notes: List<Pair<Int, Int>>) {
        try {
            val sampleRate = 44100
            val minBufSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )

            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(maxOf(minBufSize, 8192))
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioTrack.play()

            for ((freq, durationMs) in notes) {
                val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
                val samples = ShortArray(numSamples)
                for (i in 0 until numSamples) {
                    val angle = 2.0 * Math.PI * i / (sampleRate / freq.toDouble())
                    // Envelope attack and release to avoid clicks
                    val envelope = when {
                        i < 200 -> i / 200.0
                        i > numSamples - 200 -> (numSamples - i) / 200.0
                        else -> 1.0
                    }
                    samples[i] = (sin(angle) * 16000 * envelope).toInt().toShort()
                }
                audioTrack.write(samples, 0, numSamples)
            }

            audioTrack.stop()
            audioTrack.release()
        } catch (_: Exception) {
            // Audio fallback gracefully ignores platform exceptions
        }
    }

    private fun vibrate(durationMs: Long) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(durationMs)
            }
        } catch (_: Exception) {
            // Haptic fallback
        }
    }
}
