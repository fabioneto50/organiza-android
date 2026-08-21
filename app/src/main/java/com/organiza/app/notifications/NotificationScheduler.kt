package com.organiza.app.notifications

import android.content.Context
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.organiza.app.model.PlanBlock
import com.organiza.app.model.PlanBlockType
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

class NotificationScheduler(private val context: Context) {
    fun reschedule(plan: List<PlanBlock>, reminderMinutes: Int, enabled: Boolean) {
        val workManager = WorkManager.getInstance(context)
        workManager.cancelAllWorkByTag(TAG)
        if (!enabled) return
        val now = LocalDateTime.now()
        plan.asSequence()
            .filter { it.type in setOf(PlanBlockType.COMPROMISSO, PlanBlockType.TAREFA, PlanBlockType.HABITO, PlanBlockType.TURNO) }
            .take(40)
            .forEach { block ->
                val start = runCatching { LocalDateTime.of(LocalDate.parse(block.date), LocalTime.parse(block.startTime)) }.getOrNull() ?: return@forEach
                val reminderAt = start.minusMinutes(reminderMinutes.toLong())
                if (!reminderAt.isAfter(now)) return@forEach
                val delay = Duration.between(now, reminderAt).toMillis()
                val data = Data.Builder()
                    .putString(NotificationWorker.KEY_TITLE, block.title)
                    .putString(NotificationWorker.KEY_TEXT, "Começa às ${block.startTime}. ${block.subtitle}".trim())
                    .putInt(NotificationWorker.KEY_ID, block.id.hashCode())
                    .build()
                val request = OneTimeWorkRequestBuilder<NotificationWorker>()
                    .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                    .setInputData(data)
                    .addTag(TAG)
                    .build()
                workManager.enqueue(request)
            }
    }

    companion object { private const val TAG = "organiza_reminders" }
}
