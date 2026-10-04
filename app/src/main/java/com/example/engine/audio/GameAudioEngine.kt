package com.example.engine.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * Real-time procedural audio synthesizer for dynamic engine roar, nitro hiss,
 * tire screech, crash effects, and sirens using Android AudioTrack.
 */
class GameAudioEngine {

    private val sampleRate = 22050
    private var audioTrack: AudioTrack? = null
    private var synthJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    // Dynamic audio parameters updated from physics
    @Volatile var isMuted: Boolean = false
    @Volatile var rpmNorm: Float = 0.15f // 0.0 to 1.0
    @Volatile var throttle: Float = 0.0f
    @Volatile var isNitroActive: Boolean = false
    @Volatile var tireSlip: Float = 0.0f // 0.0 to 1.0 for screech
    @Volatile var triggerCrash: Boolean = false
    @Volatile var isSirenActive: Boolean = false
    @Volatile var isEngineRunning: Boolean = false
    @Volatile var baseEnginePitch: Float = 1.0f

    private var crashDecay: Float = 0f
    private var sirenPhase: Float = 0f

    fun start() {
        if (audioTrack != null) return

        try {
            val minBufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val bufferSize = minBufferSize.coerceAtLeast(2048)

            audioTrack = AudioTrack.Builder()
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
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioTrack?.play()

            synthJob = scope.launch {
                val chunkSize = 1024
                val buffer = ShortArray(chunkSize)
                var enginePhase1 = 0f
                var enginePhase2 = 0f
                var nitroNoisePhase = 0f

                while (isActive) {
                    if (isMuted || (!isEngineRunning && !triggerCrash && !isSirenActive)) {
                        buffer.fill(0)
                        audioTrack?.write(buffer, 0, chunkSize)
                        kotlinx.coroutines.delay(20)
                        continue
                    }

                    if (triggerCrash) {
                        triggerCrash = false
                        crashDecay = 1.0f
                    }

                    // Calculate instantaneous frequency for engine cylinders
                    val targetFreq = (65f + rpmNorm * 260f) * baseEnginePitch
                    val freqDelta = (2f * PI.toFloat() * targetFreq) / sampleRate

                    val screechVolume = (tireSlip - 0.25f).coerceIn(0f, 0.7f) * 0.8f
                    val nitroVolume = if (isNitroActive) 0.6f else 0f
                    val throttleGain = 0.5f + throttle * 0.5f

                    for (i in 0 until chunkSize) {
                        var sample = 0f

                        // 1. Engine synthesis (Dual saw/sine harmonic cylinder pulses)
                        if (isEngineRunning) {
                            enginePhase1 = (enginePhase1 + freqDelta) % (2f * PI.toFloat())
                            enginePhase2 = (enginePhase2 + freqDelta * 2.02f) % (2f * PI.toFloat())
                            val fundamental = sin(enginePhase1)
                            val subHarmonic = sin(enginePhase1 * 0.5f) * 0.4f
                            val overHarmonic = sin(enginePhase2) * 0.35f
                            val roughness = (Random.nextFloat() - 0.5f) * 0.15f * throttleGain
                            sample += (fundamental + subHarmonic + overHarmonic + roughness) * 0.35f * throttleGain
                        }

                        // 2. Nitro hiss (High-pass filtered white noise)
                        if (nitroVolume > 0f) {
                            val whiteNoise = (Random.nextFloat() * 2f - 1f)
                            nitroNoisePhase = nitroNoisePhase * 0.85f + whiteNoise * 0.15f
                            sample += (whiteNoise - nitroNoisePhase) * nitroVolume
                        }

                        // 3. Tire screech (Narrow band high pitch noise)
                        if (screechVolume > 0f) {
                            val squeal = sin(i * 0.32f + sin(i * 0.05f) * 2f) * (Random.nextFloat() * 0.5f + 0.5f)
                            sample += squeal * screechVolume
                        }

                        // 4. Police siren (dual frequency wail)
                        if (isSirenActive) {
                            sirenPhase = (sirenPhase + 0.0004f) % 1.0f
                            val sirenFreq = if (sirenPhase < 0.5f) 720f else 960f
                            val sPhase = (i * 2f * PI.toFloat() * sirenFreq / sampleRate) % (2f * PI.toFloat())
                            sample += sin(sPhase) * 0.3f
                        }

                        // 5. Crash impact transient
                        if (crashDecay > 0.001f) {
                            sample += (Random.nextFloat() * 2f - 1f) * crashDecay * 0.9f
                            crashDecay *= 0.9995f
                        }

                        // Clamp to 16-bit PCM range
                        val clamped = sample.coerceIn(-1.0f, 1.0f)
                        buffer[i] = (clamped * 28000f).toInt().toShort()
                    }

                    audioTrack?.write(buffer, 0, chunkSize)
                }
            }
        } catch (e: Exception) {
            // Audio initialization fallback
        }
    }

    fun stop() {
        synthJob?.cancel()
        synthJob = null
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (_: Exception) {}
        audioTrack = null
    }
}
