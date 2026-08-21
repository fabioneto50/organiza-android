package com.organiza.app.integrations

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import com.organiza.app.model.Appointment
import com.organiza.app.model.AppointmentSource
import com.organiza.app.model.PlanBlock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class CalendarIntegration(private val context: Context) {
    fun importUpcoming(days: Int = 14): List<Appointment> {
        val zone = ZoneId.systemDefault()
        val start = LocalDate.now().atStartOfDay(zone).toInstant().toEpochMilli()
        val end = LocalDate.now().plusDays(days.toLong()).plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val uriBuilder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        ContentUris.appendId(uriBuilder, start)
        ContentUris.appendId(uriBuilder, end)

        val projection = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.EVENT_LOCATION,
            CalendarContract.Instances.ALL_DAY
        )

        val result = mutableListOf<Appointment>()
        context.contentResolver.query(uriBuilder.build(), projection, null, null, "${CalendarContract.Instances.BEGIN} ASC")?.use { cursor ->
            val eventIdIndex = cursor.getColumnIndexOrThrow(CalendarContract.Instances.EVENT_ID)
            val titleIndex = cursor.getColumnIndexOrThrow(CalendarContract.Instances.TITLE)
            val beginIndex = cursor.getColumnIndexOrThrow(CalendarContract.Instances.BEGIN)
            val endIndex = cursor.getColumnIndexOrThrow(CalendarContract.Instances.END)
            val locationIndex = cursor.getColumnIndexOrThrow(CalendarContract.Instances.EVENT_LOCATION)
            val allDayIndex = cursor.getColumnIndexOrThrow(CalendarContract.Instances.ALL_DAY)
            while (cursor.moveToNext()) {
                val eventId = cursor.getLong(eventIdIndex)
                val beginMs = cursor.getLong(beginIndex)
                val endMs = cursor.getLong(endIndex).takeIf { it > beginMs } ?: (beginMs + 60 * 60 * 1000L)
                val begin = Instant.ofEpochMilli(beginMs).atZone(zone)
                val finish = Instant.ofEpochMilli(endMs).atZone(zone)
                val allDay = cursor.getInt(allDayIndex) == 1
                result += Appointment(
                    title = cursor.getString(titleIndex)?.takeIf { it.isNotBlank() } ?: "Compromisso",
                    date = begin.toLocalDate().toString(),
                    startTime = if (allDay) "08:00" else begin.toLocalTime().withSecond(0).withNano(0).toString().take(5),
                    endTime = if (allDay) "20:00" else finish.toLocalTime().withSecond(0).withNano(0).toString().take(5),
                    location = cursor.getString(locationIndex).orEmpty(),
                    note = if (allDay) "Evento de dia inteiro importado do calendário" else "Importado do calendário do dispositivo",
                    source = AppointmentSource.DEVICE_CALENDAR,
                    externalId = "calendar:$eventId:$beginMs"
                )
            }
        }
        return result
    }

    fun appointmentIntent(appointment: Appointment): Intent {
        val date = LocalDate.parse(appointment.date)
        val start = LocalDateTime.of(date, LocalTime.parse(appointment.startTime))
        var end = LocalDateTime.of(date, LocalTime.parse(appointment.endTime))
        if (!end.isAfter(start)) end = end.plusDays(1)
        return eventInsertIntent(
            title = appointment.title,
            start = start,
            end = end,
            description = appointment.note,
            location = appointment.location
        )
    }

    fun planBlockIntent(block: PlanBlock): Intent {
        val date = LocalDate.parse(block.date)
        val start = LocalDateTime.of(date, LocalTime.parse(block.startTime))
        var end = LocalDateTime.of(date, LocalTime.parse(block.endTime))
        if (!end.isAfter(start)) end = end.plusDays(1)
        return eventInsertIntent(block.title, start, end, block.subtitle, "")
    }

    private fun eventInsertIntent(title: String, start: LocalDateTime, end: LocalDateTime, description: String, location: String): Intent {
        val zone = ZoneId.systemDefault()
        var safeEnd = end
        if (!safeEnd.isAfter(start)) safeEnd = start.plusHours(1)
        return Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, title)
            putExtra(CalendarContract.Events.DESCRIPTION, description)
            putExtra(CalendarContract.Events.EVENT_LOCATION, location)
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, start.atZone(zone).toInstant().toEpochMilli())
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, safeEnd.atZone(zone).toInstant().toEpochMilli())
        }
    }
}
