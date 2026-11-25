package com.voidlauncher

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.voidlauncher.data.VersionRepository
import com.voidlauncher.engine.EngineRepository
import com.voidlauncher.util.Net
import com.voidlauncher.util.VoidPaths
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Downloads continue when VOID is closed. The service owns the work; the UI only observes
 * the shared repository StateFlows, so returning to the app always shows live progress.
 */
class DownloadService : Service() {

    companion object {
        private const val CHANNEL = "void_downloads"
        private const val NOTIFICATION_ID = 41
        const val ACTION_VERSION = "void.action.install_version"
        const val ACTION_JAVA = "void.action.download_java"
        const val EXTRA_ID = "void.extra.id"
        const val EXTRA_MAJOR = "void.extra.major"
        const val EXTRA_COMPONENT = "void.extra.component"

        fun startVersion(context: Context, id: String) {
            val i = Intent(context, DownloadService::class.java).apply {
                action = ACTION_VERSION
                putExtra(EXTRA_ID, id)
            }
            context.startForegroundService(i)
        }

        fun startJava(context: Context, component: String, major: Int) {
            context.startForegroundService(Intent(context, DownloadService::class.java).apply {
                action = ACTION_JAVA
                putExtra(EXTRA_COMPONENT, component)
                putExtra(EXTRA_MAJOR, major)
            })
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null
    private lateinit var notificationManager: NotificationManager

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(
                NotificationChannel(CHANNEL, "Downloads", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "VOID game file downloads"
                },
            )
        }
        startForeground(NOTIFICATION_ID, notification("Starting download", 0f, 0L, 0L))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        job?.cancel()
        when (intent?.action) {
            ACTION_VERSION -> {
                val id = intent.getStringExtra(EXTRA_ID) ?: return STOP_RESULT
                val repo = (application as VoidApplication).versions
                job = scope.launch { repo.install(id).join() }
                observe(repo.download)
            }
            ACTION_JAVA -> {
                val major = intent.getIntExtra(EXTRA_MAJOR, 17)
                val component = intent.getStringExtra(EXTRA_COMPONENT) ?: "jre-legacy"
                val repo = (application as VoidApplication).javaRepo
                job = scope.launch {
                    repo.downloadRuntime(component, major)
                    updateNotification("Runtime ready", 1f, 0L, 0L)
                }
                observe(repo.download)
            }
        }
        return START_NOT_STICKY
    }

    private fun observe(flow: kotlinx.coroutines.flow.StateFlow<com.voidlauncher.model.DownloadState>) {
        job = scope.launch {
            flow.collect { state ->
                if (state.active) {
                    updateNotification(
                        state.stage.ifBlank { "Downloading" }, state.percent,
                        state.bytesDone, state.bytesTotal,
                    )
                } else if (state.success || state.error != null) {
                    updateNotification(
                        if (state.success) "Download complete" else "Download failed: ${state.error}",
                        if (state.success) 1f else 0f, state.bytesDone, state.bytesTotal,
                    )
                }
            }
        }
    }

    private fun percentage(done: Long, total: Long): Float =
        if (total > 0) (done.toFloat() / total).coerceIn(0f, 1f) else 0f

    private fun updateNotification(text: String, progress: Float, done: Long, total: Long) {
        notificationManager.notify(NOTIFICATION_ID, notification(text, progress, done, total))
    }

    private fun notification(text: String, progress: Float, done: Long, total: Long): Notification {
        val intent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val detail = if (total > 0) "${Net.formatBytes(done)} / ${Net.formatBytes(total)}" else ""
        return NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_download)
            .setContentTitle("VOID Launcher")
            .setContentText(if (detail.isBlank()) text else "$text · $detail")
            .setContentIntent(intent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(1000, (progress * 1000).toInt(), total <= 0)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}

private const val STOP_RESULT = Service.START_NOT_STICKY
