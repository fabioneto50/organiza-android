package com.organiza.app.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.organiza.app.R

class NotificationWorker(appContext: Context, workerParams: WorkerParameters) : Worker(appContext, workerParams) {
    override fun doWork(): Result {
        createChannel(applicationContext)
        if (Build.VERSION.SDK_INT >= 33 && applicationContext.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return Result.success()
        }
        val title = inputData.getString(KEY_TITLE) ?: "Organiza"
        val text = inputData.getString(KEY_TEXT) ?: "Tens um bloco planeado a começar."
        val id = inputData.getInt(KEY_ID, title.hashCode())
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(applicationContext).notify(id, notification)
        return Result.success()
    }

    companion object {
        const val CHANNEL_ID = "organiza_plano"
        const val KEY_TITLE = "title"
        const val KEY_TEXT = "text"
        const val KEY_ID = "id"

        fun createChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(CHANNEL_ID, "Plano e compromissos", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Lembretes do plano inteligente e compromissos"
                }
                context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
            }
        }
    }
}
