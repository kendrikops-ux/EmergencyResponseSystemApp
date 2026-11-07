package com.example.emergencyresponsesystem.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

object SoundUtils {
    private var mediaPlayer: MediaPlayer? = null
    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null

    fun playEmergencyAlert(context: Context) {
        stopEmergencyAlert() // Stop any ongoing alert sound/vibration first
        try {
            // Force Alarm stream volume to maximum so mute/low volume won't affect it
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val maxAlarmVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxAlarmVolume, 0)

            // Play high-priority alarm ringtone using USAGE_ALARM
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            val mp = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(context, alarmUri)
                isLooping = true // Loop alarm continuously until acknowledged/en route
                prepare()
                start()
            }
            mediaPlayer = mp

            // Vibrate heavily until stopped
            val vib = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            vibrator = vib

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vib.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 800, 200, 800, 200, 800), 0))
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(longArrayOf(0, 800, 200, 800, 200, 800), 0)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            try {
                val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                val rt = RingtoneManager.getRingtone(context, ringtoneUri)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    rt.audioAttributes = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                }
                rt.play()
                ringtone = rt
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    fun stopEmergencyAlert() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
            mediaPlayer = null
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            ringtone?.let {
                if (it.isPlaying) {
                    it.stop()
                }
            }
            ringtone = null
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            vibrator?.cancel()
            vibrator = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
