package com.amon.timer

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

object FocusSoundManager {

    private const val PREFS_NAME = "amon_sound_prefs"
    private const val KEY_SOUND_URI = "selected_sound_uri"
    private const val KEY_SOUND_ENABLED = "sound_enabled"
    private const val KEY_HAPTIC_ENABLED = "haptic_enabled"

    private var mediaPlayer: MediaPlayer? = null

    // 📳 1. हेप्टिक वाइब्रेशन (क्लिक vs कम्पलीशन बज़)
    fun triggerHaptic(context: Context, isCompletion: Boolean = false) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_HAPTIC_ENABLED, true)) return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                val vibrator = vibratorManager?.defaultVibrator
                val effect = if (isCompletion) {
                    VibrationEffect.createWaveform(longArrayOf(0, 180, 100, 240), intArrayOf(0, 255, 0, 255), -1)
                } else {
                    VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
                }
                vibrator?.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (isCompletion) {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(longArrayOf(0, 180, 100, 240), -1)
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator?.vibrate(VibrationEffect.createOneShot(35, VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator?.vibrate(35)
                    }
                }
            }
        } catch (_: Exception) {}
    }

    // 🔔 2. समाप्ति साउंड बजाना (अब यह सीधे res/raw/chime.mp3 बजाएगा)
    fun playCompletionSound(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_SOUND_ENABLED, true)) return

        try {
            stopSound()

            // 🎵 MediaPlayer के जरिए res/raw/chime को लोड करके बजाना
            mediaPlayer = MediaPlayer.create(context, R.raw.chime).apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                }
                setOnCompletionListener { 
                    it.release()
                    mediaPlayer = null
                }
                start()
            }
        } catch (e: Exception) {
            Log.e("FocusSound", "Error playing chime sound: ${e.localizedMessage}")
        }
    }

    fun stopSound() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
            mediaPlayer = null
        } catch (_: Exception) {}
    }

    fun saveSoundUri(context: Context, uri: Uri?) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_SOUND_URI, uri?.toString() ?: "").apply()
    }

    fun getSelectedSoundUri(context: Context): Uri {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_SOUND_URI, null)
        return if (!saved.isNullOrBlank()) Uri.parse(saved) else android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
    }

    fun isSoundEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean(KEY_SOUND_ENABLED, true)
    }

    fun setSoundEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putBoolean(KEY_SOUND_ENABLED, enabled).apply()
    }
}
