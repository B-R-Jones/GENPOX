package com.example.genpox.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.SoundPool
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Random
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin

class PoxSynthManager {
    var isMuted = false
    private var sampleRate = 48000
    private var soundPool: SoundPool? = null
    private var keepWarmTrack: AudioTrack? = null
    private val soundMap = ConcurrentHashMap<String, Int>()
    private val synthScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Volatile
    private var isInitialized = false

    fun initialize(context: Context) {
        if (isInitialized) return
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            val nativeSampleRateStr = audioManager?.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)
            sampleRate = nativeSampleRateStr?.toIntOrNull() ?: 48000

            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            soundPool = SoundPool.Builder()
                .setMaxStreams(12)
                .setAudioAttributes(attributes)
                .build()

            // Pre-synthesize and load all sound effects into SoundPool for <10ms hardware latency
            soundMap["beep_default"] = loadSound(context, "beep_default", generateBeepSamples(440f, 0.05f, "sine"))
            soundMap["beep_high"] = loadSound(context, "beep_high", generateBeepSamples(600f, 0.05f, "sine"))
            soundMap["beep_low"] = loadSound(context, "beep_low", generateBeepSamples(400f, 0.08f, "sine"))
            soundMap["combinator_tick"] = loadSound(context, "combinator_tick", generateBeepSamples(440f, 0.03f, "sawtooth"))
            soundMap["reject"] = loadSound(context, "reject", generateBeepSamples(220f, 0.25f, "sawtooth"))
            soundMap["success"] = loadSound(context, "success", generateSynthesisSuccessSamples())
            soundMap["alert"] = loadSound(context, "alert", generateAlertBuzzerSamples())
            soundMap["trade"] = loadSound(context, "trade", generateTradeSuccessSamples())
            soundMap["geiger"] = loadSound(context, "geiger", generateGeigerClickSamples(1.0f))

            // DNA sequence notes
            soundMap["note_A"] = loadSound(context, "note_A", generateBeepSamples(440.0f, 0.08f, "sine"))
            soundMap["note_T"] = loadSound(context, "note_T", generateBeepSamples(554.37f, 0.08f, "sine"))
            soundMap["note_G"] = loadSound(context, "note_G", generateBeepSamples(659.25f, 0.08f, "sine"))
            soundMap["note_C"] = loadSound(context, "note_C", generateBeepSamples(830.61f, 0.08f, "sine"))

            // Keep DSP hardware active continuously so SoundPool never encounters HAL standby delay
            startKeepWarmAnchor()

            isInitialized = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startKeepWarmAnchor() {
        try {
            val silentSamples = ShortArray(480) // 10ms of pure digital 0-amplitude silence
            val minBufSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
            )
            val bufferSizeBytes = maxOf(minBufSize, silentSamples.size * 2)

            val builder = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setFlags(AudioAttributes.FLAG_LOW_LATENCY)
                        .build(),
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build(),
                )
                .setBufferSizeInBytes(bufferSizeBytes)
                .setTransferMode(AudioTrack.MODE_STATIC)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                builder.setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
            }

            val track = builder.build()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try {
                    track.setBufferSizeInFrames(silentSamples.size)
                } catch (e: Exception) {
                    // Ignore
                }
            }

            track.write(silentSamples, 0, silentSamples.size)
            track.setLoopPoints(0, silentSamples.size, -1)
            track.play()
            keepWarmTrack = track
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadSound(context: Context, name: String, samples: ShortArray): Int = try {
        val wavBytes = pcmToWav(samples, sampleRate)
        val file = File(context.cacheDir, "pox_synth_$name.wav")
        file.writeBytes(wavBytes)
        soundPool?.load(file.absolutePath, 1) ?: 0
    } catch (e: Exception) {
        0
    }

    private fun pcmToWav(pcmSamples: ShortArray, sampleRate: Int): ByteArray {
        val dataSize = pcmSamples.size * 2
        val totalSize = 36 + dataSize
        val buffer = ByteBuffer.allocate(44 + dataSize).order(ByteOrder.LITTLE_ENDIAN)

        buffer.put("RIFF".toByteArray())
        buffer.putInt(totalSize)
        buffer.put("WAVE".toByteArray())

        buffer.put("fmt ".toByteArray())
        buffer.putInt(16)
        buffer.putShort(1.toShort()) // PCM
        buffer.putShort(1.toShort()) // Mono
        buffer.putInt(sampleRate)
        buffer.putInt(sampleRate * 2)
        buffer.putShort(2.toShort())
        buffer.putShort(16.toShort())

        buffer.put("data".toByteArray())
        buffer.putInt(dataSize)

        for (sample in pcmSamples) {
            buffer.putShort(sample)
        }

        return buffer.array()
    }

    private fun playSoundKey(key: String, volume: Float = 1.0f) {
        if (isMuted || !isInitialized) return
        val soundId = soundMap[key] ?: return
        if (soundId != 0) {
            soundPool?.play(soundId, volume, volume, 1, 0, 1.0f)
        }
    }

    fun pause() {
        try {
            soundPool?.autoPause()
            keepWarmTrack?.stop()
            keepWarmTrack?.release()
            keepWarmTrack = null
        } catch (e: Exception) {
            // Ignore
        }
    }

    fun resume() {
        try {
            soundPool?.autoResume()
            startKeepWarmAnchor()

            // Prime SoundPool's native fast-path stream allocator on app resume
            val defaultId = soundMap["beep_default"] ?: 0
            if (defaultId != 0) {
                soundPool?.play(defaultId, 0.0f, 0.0f, 1, 0, 1.0f)
            }
        } catch (e: Exception) {
            // Ignore
        }
    }

    fun stopAll() {
        try {
            soundPool?.autoPause()
        } catch (e: Exception) {
            // Ignore
        }
    }

    fun release() {
        isInitialized = false
        try {
            soundPool?.release()
            soundPool = null
            keepWarmTrack?.stop()
            keepWarmTrack?.release()
            keepWarmTrack = null
        } catch (e: Exception) {
            // Ignore
        }
    }

    fun playBeep(freq: Float, durationSec: Float, type: String = "square") {
        if (isMuted || !isInitialized) return
        when {
            freq in 420f..460f && type == "sine" -> playSoundKey("beep_default")
            freq in 580f..620f -> playSoundKey("beep_high")
            freq in 380f..410f -> playSoundKey("beep_low")
            else -> playSoundKey("beep_default")
        }
    }

    fun playCombinatorTick() {
        playSoundKey("combinator_tick")
    }

    fun playSynthesisSuccess() {
        playSoundKey("success")
    }

    fun playAlertBuzzer() {
        playSoundKey("alert")
    }

    fun playReject() {
        playSoundKey("reject")
    }

    fun playTradeSuccess() {
        playSoundKey("trade")
    }

    fun playGeigerClick(volumeMultiplier: Float = 1.0f) {
        playSoundKey("geiger", volumeMultiplier.coerceIn(0.1f, 1.0f))
    }

    fun playCreatureSequenceAudio(sequence: String) {
        if (isMuted || !isInitialized || sequence.isEmpty()) return
        synthScope.launch {
            for (char in sequence) {
                val soundKey = when (char) {
                    'A' -> "note_A"
                    'T' -> "note_T"
                    'G' -> "note_G"
                    'C' -> "note_C"
                    else -> "note_A"
                }
                playSoundKey(soundKey, 0.8f)
                delay(60L)
            }
        }
    }

    private fun generateBeepSamples(freq: Float, durationSec: Float, type: String): ShortArray {
        val numSamples = (sampleRate * durationSec).toInt()
        val samples = ShortArray(numSamples)
        val angleStep = 2.0 * Math.PI * freq / sampleRate
        var currentAngle = 0.0

        for (i in 0 until numSamples) {
            val t = i.toDouble() / numSamples
            val rawVal = when (type) {
                "sine" -> sin(currentAngle)
                "triangle" -> {
                    val phase = (currentAngle % (2 * Math.PI)) / (2 * Math.PI)
                    if (phase < 0.5) -1.0 + 4.0 * phase else 3.0 - 4.0 * phase
                }
                "sawtooth" -> {
                    val phase = (currentAngle % (2 * Math.PI)) / (2 * Math.PI)
                    -1.0 + 2.0 * phase
                }
                else -> if (sin(currentAngle) >= 0) 1.0 else -1.0
            }
            currentAngle += angleStep
            val envelope = 1.0 - t
            val amplitude = (rawVal * Short.MAX_VALUE * 0.15 * envelope).toInt()
            samples[i] = amplitude.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return samples
    }

    private fun generateSynthesisSuccessSamples(): ShortArray {
        val notes = floatArrayOf(523.25f, 587.33f, 659.25f, 698.46f, 783.99f, 880.00f)
        val noteDuration = 0.12f
        val noteDelay = 0.06f
        val totalSec = noteDelay * (notes.size - 1) + noteDuration
        val totalSamplesCount = (sampleRate * totalSec).toInt()
        val samples = ShortArray(totalSamplesCount)

        for (idx in notes.indices) {
            val freq = notes[idx]
            val startSample = (sampleRate * idx * noteDelay).toInt()
            val noteSamples = (sampleRate * noteDuration).toInt()
            val angleStep = 2.0 * Math.PI * freq / sampleRate
            var currentAngle = 0.0

            for (i in 0 until noteSamples) {
                val outIdx = startSample + i
                if (outIdx >= totalSamplesCount) break
                val t = i.toDouble() / noteSamples
                val valSine = sin(currentAngle)
                currentAngle += angleStep
                val envelope = 1.0 - t
                val amp = (valSine * Short.MAX_VALUE * 0.15 * envelope).toInt()
                val combined = samples[outIdx] + amp
                samples[outIdx] = combined.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
        }
        return samples
    }

    private fun generateAlertBuzzerSamples(): ShortArray {
        val durationSec = 0.35f
        val numSamples = (sampleRate * durationSec).toInt()
        val samples = ShortArray(numSamples)

        val freq1 = 150.0
        val freq2 = 155.0
        val angleStep1 = 2.0 * Math.PI * freq1 / sampleRate
        val angleStep2 = 2.0 * Math.PI * freq2 / sampleRate
        var currentAngle1 = 0.0
        var currentAngle2 = 0.0

        for (i in 0 until numSamples) {
            val t = i.toDouble() / numSamples
            val phase1 = (currentAngle1 % (2 * Math.PI)) / (2 * Math.PI)
            val phase2 = (currentAngle2 % (2 * Math.PI)) / (2 * Math.PI)
            val val1 = -1.0 + 2.0 * phase1
            val val2 = -1.0 + 2.0 * phase2
            currentAngle1 += angleStep1
            currentAngle2 += angleStep2

            val envelope = 1.0 - t
            val amp = ((val1 + val2) * 0.5 * Short.MAX_VALUE * 0.15 * envelope).toInt()
            samples[i] = amp.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return samples
    }

    private fun generateTradeSuccessSamples(): ShortArray {
        val durationSec = 0.4f
        val numSamples = (sampleRate * durationSec).toInt()
        val samples = ShortArray(numSamples)

        val freqStart = 300.0
        val freqEnd = 1200.0
        var currentAngle = 0.0

        for (i in 0 until numSamples) {
            val t = i.toDouble() / numSamples
            val freq = freqStart * freqEnd.div(freqStart).pow(t)
            val angleStep = 2.0 * Math.PI * freq / sampleRate

            val phase = (currentAngle % (2 * Math.PI)) / (2 * Math.PI)
            val rawVal = if (phase < 0.5) -1.0 + 4.0 * phase else 3.0 - 4.0 * phase
            currentAngle += angleStep

            val envelope = 1.0 - t
            val amp = (rawVal * Short.MAX_VALUE * 0.15 * envelope).toInt()
            samples[i] = amp.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return samples
    }

    private fun generateGeigerClickSamples(volumeMultiplier: Float = 1.0f): ShortArray {
        val durationSec = 0.005f
        val numSamples = (sampleRate * durationSec).toInt()
        val samples = ShortArray(numSamples)
        val random = Random()
        for (i in 0 until numSamples) {
            val noise = -1.0 + 2.0 * random.nextDouble()
            val envelope = exp(-6.0 * i / numSamples)
            val amp = (noise * Short.MAX_VALUE * 0.20 * envelope * volumeMultiplier).toInt()
            samples[i] = amp.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return samples
    }
}
