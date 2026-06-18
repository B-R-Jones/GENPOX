package com.example.genpox.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.sin

class PoxSynthManager {
    var isMuted = false
    private val sampleRate = 44100
    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    private var appContext: Context? = null
    private var audioManager: AudioManager? = null
    private var focusRequest: Any? = null

    private val activeTracks = java.util.Collections.synchronizedSet(mutableSetOf<AudioTrack>())

    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                stopAll()
            }
        }
    }

    fun initialize(context: Context) {
        val app = context.applicationContext
        appContext = app
        audioManager = app.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    private fun requestFocus(): Boolean {
        val am = audioManager ?: return true
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val playbackAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                val request = android.media.AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                    .setAudioAttributes(playbackAttributes)
                    .setAcceptsDelayedFocusGain(false)
                    .setOnAudioFocusChangeListener(audioFocusChangeListener)
                    .build()
                focusRequest = request
                am.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            } else {
                @Suppress("DEPRECATION")
                am.requestAudioFocus(
                    audioFocusChangeListener,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
                ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            }
        } catch (e: Exception) {
            true
        }
    }

    private fun abandonFocus() {
        val am = audioManager ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                (focusRequest as? android.media.AudioFocusRequest)?.let {
                    am.abandonAudioFocusRequest(it)
                }
            } else {
                @Suppress("DEPRECATION")
                am.abandonAudioFocus(audioFocusChangeListener)
            }
        } catch (e: Exception) {
            // Ignore
        }
    }

    fun stopAll() {
        val tracksToStop = synchronized(activeTracks) {
            val list = activeTracks.toList()
            activeTracks.clear()
            list
        }
        coroutineScope.launch {
            tracksToStop.forEach { track ->
                try {
                    track.stop()
                    track.release()
                } catch (e: Exception) {
                    // Ignore
                }
            }
            abandonFocus()
        }
    }

    fun playBeep(freq: Float, durationSec: Float, type: String = "square") {
        if (isMuted) return
        coroutineScope.launch {
            if (!requestFocus()) return@launch
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
                        if (phase < 0.5) {
                            -1.0 + 4.0 * phase
                        } else {
                            3.0 - 4.0 * phase
                        }
                    }
                    "sawtooth" -> {
                        val phase = (currentAngle % (2 * Math.PI)) / (2 * Math.PI)
                        -1.0 + 2.0 * phase
                    }
                    else -> { // square
                        if (sin(currentAngle) >= 0) 1.0 else -1.0
                    }
                }
                currentAngle += angleStep
                // Decay envelope
                val envelope = 1.0 - t
                val amplitude = (rawVal * Short.MAX_VALUE * 0.04 * envelope).toInt()
                samples[i] = amplitude.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
            writeAndPlay(samples)
        }
    }

    fun playCombinatorTick() {
        playBeep(440f, 0.03f, "sawtooth")
    }

    fun playSynthesisSuccess() {
        if (isMuted) return
        coroutineScope.launch {
            if (!requestFocus()) return@launch
            val notes = floatArrayOf(523.25f, 587.33f, 659.25f, 698.46f, 783.99f, 880.00f)
            val noteDuration = 0.15f
            val noteDelay = 0.08f
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
                    val amp = (valSine * Short.MAX_VALUE * 0.05 * envelope).toInt()
                    // Add note to buffer (mixing if overlapping)
                    val combined = samples[outIdx] + amp
                    samples[outIdx] = combined.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }
            }
            writeAndPlay(samples)
        }
    }

    fun playAlertBuzzer() {
        if (isMuted) return
        coroutineScope.launch {
            if (!requestFocus()) return@launch
            val durationSec = 0.4f
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
                // Sawtooth generator
                val phase1 = (currentAngle1 % (2 * Math.PI)) / (2 * Math.PI)
                val phase2 = (currentAngle2 % (2 * Math.PI)) / (2 * Math.PI)
                val val1 = -1.0 + 2.0 * phase1
                val val2 = -1.0 + 2.0 * phase2
                currentAngle1 += angleStep1
                currentAngle2 += angleStep2

                val envelope = 1.0 - t
                val amp = ((val1 + val2) * 0.5 * Short.MAX_VALUE * 0.06 * envelope).toInt()
                samples[i] = amp.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
            writeAndPlay(samples)
        }
    }

    fun playReject() {
        playBeep(220f, 0.25f, "sawtooth")
    }

    fun playCreatureSequenceAudio(sequence: String) {
        if (isMuted) return
        coroutineScope.launch {
            if (!requestFocus()) return@launch
            val chars = sequence.uppercase().replace(Regex("[^AGTC]"), "")
            if (chars.isEmpty()) return@launch

            val numNotes = (chars.length / 8).coerceIn(4, 8)
            var acount = 0
            var gcount = 0
            var tcount = 0
            var ccount = 0
            for (c in chars) {
                when (c) {
                    'A' -> acount++
                    'G' -> gcount++
                    'T' -> tcount++
                    'C' -> ccount++
                }
            }

            val maxCount = maxOf(acount, gcount, tcount, ccount)
            val oscType = when (maxCount) {
                acount -> "sine"
                gcount -> "triangle"
                tcount -> "sawtooth"
                else -> "square"
            }

            val noteDelay = 0.08f + (acount % 3) * 0.02f
            val noteDuration = 0.06f + (gcount % 4) * 0.03f

            val totalSec = noteDelay * (numNotes - 1) + noteDuration
            val totalSamplesCount = (sampleRate * totalSec).toInt()
            val samples = ShortArray(totalSamplesCount)

            for (i in 0 until numNotes) {
                val blockLength = chars.length / numNotes
                val startIdx = i * blockLength
                val endIdx = ((i + 1) * blockLength).coerceAtMost(chars.length)
                if (startIdx >= chars.length) break
                val charBlock = chars.substring(startIdx, endIdx)
                var baseValue = 0
                for (c in charBlock) {
                    when (c) {
                        'A' -> baseValue += 1
                        'G' -> baseValue += 2
                        'T' -> baseValue += 3
                        'C' -> baseValue += 4
                    }
                }

                val freq = 180f + (baseValue * 30f) + (i * 40f)
                val startSample = (sampleRate * i * noteDelay).toInt()
                val noteSamples = (sampleRate * noteDuration).toInt()
                val angleStep = 2.0 * Math.PI * freq / sampleRate
                var currentAngle = 0.0

                for (j in 0 until noteSamples) {
                    val outIdx = startSample + j
                    if (outIdx >= totalSamplesCount) break
                    val t = j.toDouble() / noteSamples

                    val rawVal = when (oscType) {
                        "sine" -> sin(currentAngle)
                        "triangle" -> {
                            val phase = (currentAngle % (2 * Math.PI)) / (2 * Math.PI)
                            if (phase < 0.5) -1.0 + 4.0 * phase else 3.0 - 4.0 * phase
                        }
                        "sawtooth" -> {
                            val phase = (currentAngle % (2 * Math.PI)) / (2 * Math.PI)
                            -1.0 + 2.0 * phase
                        }
                        else -> {
                            if (sin(currentAngle) >= 0) 1.0 else -1.0
                        }
                    }
                    currentAngle += angleStep
                    val envelope = 1.0 - t
                    val amp = (rawVal * Short.MAX_VALUE * 0.04 * envelope).toInt()
                    val combined = samples[outIdx] + amp
                    samples[outIdx] = combined.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }
            }
            writeAndPlay(samples)
        }
    }

    fun playTradeSuccess() {
        if (isMuted) return
        coroutineScope.launch {
            if (!requestFocus()) return@launch
            val durationSec = 0.5f
            val numSamples = (sampleRate * durationSec).toInt()
            val samples = ShortArray(numSamples)

            val freqStart = 300.0
            val freqEnd = 1200.0
            var currentAngle = 0.0

            for (i in 0 until numSamples) {
                val t = i.toDouble() / numSamples
                val freq = freqStart * Math.pow(freqEnd / freqStart, t)
                val angleStep = 2.0 * Math.PI * freq / sampleRate

                val phase = (currentAngle % (2 * Math.PI)) / (2 * Math.PI)
                val rawVal = if (phase < 0.5) -1.0 + 4.0 * phase else 3.0 - 4.0 * phase
                currentAngle += angleStep

                val envelope = 1.0 - t
                val amp = (rawVal * Short.MAX_VALUE * 0.05 * envelope).toInt()
                samples[i] = amp.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
            writeAndPlay(samples)
        }
    }

    private fun writeAndPlay(samples: ShortArray) {
        var track: AudioTrack? = null
        try {
            track = AudioTrack.Builder()
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
                .setBufferSizeInBytes(samples.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            activeTracks.add(track)
            track.write(samples, 0, samples.size)
            track.play()

            val playDurationMs = (samples.size.toDouble() / sampleRate * 1000).toLong() + 100
            coroutineScope.launch {
                kotlinx.coroutines.delay(playDurationMs)
                val shouldRelease = synchronized(activeTracks) {
                    activeTracks.remove(track)
                }
                if (shouldRelease) {
                    try {
                        track.stop()
                        track.release()
                    } catch (e: Exception) {
                        // Ignore release errors
                    }
                    val isEmpty = synchronized(activeTracks) { activeTracks.isEmpty() }
                    if (isEmpty) {
                        abandonFocus()
                    }
                }
            }
        } catch (e: Exception) {
            if (track != null) {
                synchronized(activeTracks) {
                    activeTracks.remove(track)
                }
            }
            val isEmpty = synchronized(activeTracks) { activeTracks.isEmpty() }
            if (isEmpty) {
                abandonFocus()
            }
        }
    }
}
