package com.superloan.app.ui.sim

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * 来电铃声与震动。
 *
 * 权限说明：
 *  - 放系统铃声**不需要任何权限**；
 *  - 震动需要 `android.permission.VIBRATE`（普通权限，安装即授予，无运行时弹窗），
 *    已在本应用的 Manifest 中显式声明。
 *
 * 手机处于静音 / 震动模式时自动不响铃、只震动 —— 夜深人静时打开不至于把自己吓一跳。
 */
class SimRingtone(context: Context) {

    private val appContext = context.applicationContext
    private var player: MediaPlayer? = null

    /** 开始响铃 + 震动（重复播放，直到 [stop]） */
    fun start() {
        startRingtone()
        startVibration()
    }

    /** 停止响铃与震动 */
    fun stop() {
        stopRingtone()
        stopVibration()
    }

    // ------------------------------------------------------------------
    // 铃声
    // ------------------------------------------------------------------

    private fun startRingtone() {
        if (player != null || !canRing()) return
        runCatching {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: return
            player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(appContext, uri)
                // 循环播放，营造「一直在打」的感觉
                isLooping = true
                prepare()
                start()
            }
        }.onFailure {
            // 铃声资源缺失或被系统限制时静默降级，只留震动
            player = null
        }
    }

    private fun stopRingtone() {
        runCatching {
            player?.let {
                if (it.isPlaying) it.stop()
                it.release()
            }
        }
        player = null
    }

    /** 系统处于静音 / 震动模式时不外放 */
    private fun canRing(): Boolean {
        val audioManager = appContext.getSystemService(AudioManager::class.java) ?: return true
        return audioManager.ringerMode == AudioManager.RINGER_MODE_NORMAL
    }

    // ------------------------------------------------------------------
    // 震动
    // ------------------------------------------------------------------

    private fun startVibration() {
        val vibrator = obtainVibrator() ?: return
        if (!vibrator.hasVibrator()) return
        runCatching {
            // 长-短-长-短-长，第 0 位重复播放 => 循环震动
            val pattern = longArrayOf(0L, 800L, 500L, 800L, 500L, 1200L)
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0))
        }
    }

    private fun stopVibration() {
        runCatching { obtainVibrator()?.cancel() }
    }

    private fun obtainVibrator(): Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }
}
