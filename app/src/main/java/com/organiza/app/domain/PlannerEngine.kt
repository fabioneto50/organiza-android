package com.organiza.app.domain

import com.organiza.app.model.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.max

object PlannerEngine {
    private val tf = DateTimeFormatter.ofPattern("HH:mm")

    private data class Interval(val start: LocalTime, val end: LocalTime)
    private data class Candidate(
        val id: String,
        val title: String,
        val duration: Int,
        val energy: EnergyDemand,
        val preferredMoment: PreferredMoment,
        val score: Int,
        val subtitle: String,
        val type: PlanBlockType,
        val taskId: String? = null,
        val habitId: String? = null
    )

    fun generate(
        tasks: List<LifeTask>,
        shifts: List<Shift>,
        appointments: List<Appointment> = emptyList(),
        habits: List<Habit> = emptyList(),
        preferences: UserPreferences,
        effectiveEnergy: Int = preferences.currentEnergy,
        startDate: LocalDate = LocalDate.now(),
        days: Int = 7,
        now: LocalDateTime = LocalDateTime.now()
    ): List<PlanBlock> {
        val pendingTasks = tasks.filterNot { it.completed }.toMutableList()
        val output = mutableListOf<PlanBlock>()

        repeat(days.coerceIn(1, 21)) { index ->
            val date = startDate.plusDays(index.toLong())
            val todayShifts = shifts.filter { it.localDate() == date }.sortedBy { it.startTime }
            val todayAppointments = appointments.filter { it.localDate() == date }.sortedBy { it.startTime }
            val previousNight = shifts.any { it.localDate() == date.minusDays(1) && it.type == ShiftType.NOITE }
            val fixed = mutableListOf<Interval>()

            todayShifts.filter { it.type !in listOf(ShiftType.FOLGA, ShiftType.FERIAS) }.forEach { shift ->
                output += PlanBlock(
                    date = date.toString(), startTime = shift.startTime, endTime = shift.endTime,
                    title = "Turno ${shift.type.label.lowercase()}",
                    subtitle = shift.note.ifBlank { "Período de trabalho protegido" },
                    type = PlanBlockType.TURNO
                )
                fixed += sameDayInterval(shift.start(), shift.end(), preferences)
            }

            todayAppointments.forEach { appointment ->
                output += PlanBlock(
                    date = date.toString(), startTime = appointment.startTime, endTime = appointment.endTime,
                    title = appointment.title,
                    subtitle = buildString {
                        append("Compromisso")
                        if (appointment.location.isNotBlank()) append(" · ${appointment.location}")
                    },
                    type = PlanBlockType.COMPROMISSO,
                    sourceAppointmentId = appointment.id
                )
                fixed += sameDayInterval(appointment.start(), appointment.end(), preferences)
            }

            if (previousNight && preferences.protectSleepAfterNight) {
                val recoveryStart = LocalTime.of(8, 30)
                val recoveryEnd = recoveryStart.plusHours(preferences.recoveryHoursAfterNight.toLong())
                    .coerceBefore(LocalTime.of(preferences.dayEndHour, 0))
                if (recoveryStart.isBefore(recoveryEnd)) {
                    output += PlanBlock(
                        date = date.toString(), startTime = recoveryStart.format(tf), endTime = recoveryEnd.format(tf),
                        title = "Recuperação pós-noite", subtitle = "Sono e recuperação protegidos",
                        type = PlanBlockType.RECUPERACAO
                    )
                    fixed += Interval(recoveryStart, recoveryEnd)
                }
            }

            val windows = availabilityWindows(date, fixed, preferences, now)
            val dailyLimit = dailyPlanningLimit(todayShifts, previousNight, effectiveEnergy)
            var plannedMinutes = 0
            var plannedItems = 0

            val dueHabits = habits.filter { it.isDue(date) && !it.isCompleted(date) }
            val habitCandidates = dueHabits.map { habit ->
                Candidate(
                    id = habit.id,
                    title = habit.title,
                    duration = habit.durationMinutes,
                    energy = habit.energyDemand,
                    preferredMoment = habit.preferredMoment,
                    score = 155,
                    subtitle = "Hábito · ${habit.durationMinutes} min",
                    type = PlanBlockType.HABITO,
                    habitId = habit.id
                )
            }.toMutableList()

            for ((windowStart, windowEnd) in windows) {
                var cursor = windowStart
                while (plannedMinutes < dailyLimit && plannedItems < 7) {
                    val available = minutesBetween(cursor, windowEnd)
                    if (available < 10) break
                    val energyCap = if (previousNight) 1 else energyCap(effectiveEnergy)

                    val taskCandidates = pendingTasks.map { task ->
                        Candidate(
                            id = task.id,
                            title = task.title,
                            duration = task.durationMinutes,
                            energy = task.energyDemand,
                            preferredMoment = task.preferredMoment,
                            score = taskScore(task, date, available),
                            subtitle = "${task.category.label} · ${task.durationMinutes} min · energia ${task.energyDemand.label.lowercase()}",
                            type = PlanBlockType.TAREFA,
                            taskId = task.id
                        )
                    }

                    val selected = (habitCandidates + taskCandidates)
                        .asSequence()
                        .filter { it.duration <= available }
                        .filter { plannedMinutes + it.duration <= dailyLimit }
                        .filter { it.energy.value <= energyCap }
                        .filter { momentFits(it.preferredMoment, cursor) }
                        .maxByOrNull { it.score + fitBonus(it.duration, available) }
                        ?: (habitCandidates + taskCandidates)
                            .asSequence()
                            .filter { it.duration <= available && plannedMinutes + it.duration <= dailyLimit }
                            .filter { it.energy == EnergyDemand.BAIXA }
                            .maxByOrNull { it.score }
                        ?: break

                    val end = cursor.plusMinutes(selected.duration.toLong())
                    output += PlanBlock(
                        date = date.toString(), startTime = cursor.format(tf), endTime = end.format(tf),
                        title = selected.title, subtitle = selected.subtitle, type = selected.type,
                        sourceTaskId = selected.taskId, sourceHabitId = selected.habitId
                    )
                    selected.taskId?.let { taskId -> pendingTasks.removeAll { it.id == taskId } }
                    selected.habitId?.let { habitId -> habitCandidates.removeAll { it.id == habitId } }
                    plannedMinutes += selected.duration
                    plannedItems++
                    cursor = end.plusMinutes(15)
                }
            }

            val noWork = todayShifts.none { it.type !in listOf(ShiftType.FOLGA, ShiftType.FERIAS) }
            if (noWork && plannedMinutes <= 90 && !previousNight) {
                addPersonalBlockIfFree(output, date, fixed, LocalTime.of(18, 0), LocalTime.of(19, 0))
            }
        }

        return output.sortedWith(compareBy<PlanBlock> { it.date }.thenBy { it.startTime })
    }

    fun suggestForMinutes(tasks: List<LifeTask>, minutes: Int, currentEnergy: Int): TimeSuggestion {
        if (minutes <= 0) return TimeSuggestion(0, emptyList(), 0, "Indica um período disponível.")
        val energyCap = energyCap(currentEnergy)
        val candidates = tasks.filterNot { it.completed }
            .filter { it.durationMinutes <= minutes && it.energyDemand.value <= energyCap }
            .sortedByDescending { it.priority.weight * 120 - it.durationMinutes + (4 - it.energyDemand.value) * 12 }

        val selected = mutableListOf<LifeTask>()
        var remaining = minutes
        candidates.forEach { task ->
            if (task.durationMinutes <= remaining) {
                selected += task
                remaining -= task.durationMinutes
            }
        }
        val used = minutes - remaining
        val message = when {
            selected.isEmpty() -> "Não há uma tarefa adequada a este tempo e energia. Mantém este período como margem ou recuperação."
            selected.size == 1 -> "A melhor utilização deste período é concluir “${selected.first().title}”."
            else -> "Consegues concluir ${selected.size} tarefas sem ultrapassar os $minutes minutos disponíveis."
        }
        return TimeSuggestion(minutes, selected, used, message)
    }

    private fun availabilityWindows(
        date: LocalDate,
        fixedRaw: List<Interval>,
        preferences: UserPreferences,
        now: LocalDateTime
    ): List<Pair<LocalTime, LocalTime>> {
        var dayStart = LocalTime.of(preferences.dayStartHour, 0)
        val dayEnd = LocalTime.of(preferences.dayEndHour, 0)
        if (date == now.toLocalDate()) {
            val roundedNow = now.toLocalTime().plusMinutes(10).withSecond(0).withNano(0)
            if (roundedNow.isAfter(dayStart)) dayStart = roundedNow
        }
        if (!dayStart.isBefore(dayEnd)) return emptyList()

        val fixed = fixedRaw.mapNotNull { interval ->
            val start = if (interval.start.isBefore(dayStart)) dayStart else interval.start
            val end = if (interval.end.isAfter(dayEnd)) dayEnd else interval.end
            if (start.isBefore(end)) Interval(start, end) else null
        }.sortedBy { it.start }

        val merged = mutableListOf<Interval>()
        fixed.forEach { current ->
            val last = merged.lastOrNull()
            if (last == null || current.start.isAfter(last.end)) merged += current
            else merged[merged.lastIndex] = Interval(last.start, maxTime(last.end, current.end))
        }

        val windows = mutableListOf<Pair<LocalTime, LocalTime>>()
        var cursor = dayStart
        merged.forEach { block ->
            val safeEnd = block.start.minusMinutes(20)
            if (cursor.isBefore(safeEnd) && minutesBetween(cursor, safeEnd) >= 20) windows += cursor to safeEnd
            cursor = maxTime(cursor, block.end.plusMinutes(20))
        }
        if (cursor.isBefore(dayEnd) && minutesBetween(cursor, dayEnd) >= 20) windows += cursor to dayEnd
        return windows
    }

    private fun sameDayInterval(start: LocalTime, end: LocalTime, preferences: UserPreferences): Interval {
        val dayEnd = LocalTime.of(preferences.dayEndHour, 0)
        return if (end.isAfter(start)) Interval(start, end) else Interval(start, dayEnd)
    }

    private fun taskScore(task: LifeTask, date: LocalDate, availableMinutes: Int): Int {
        val due = task.dueLocalDate()
        val dueBoost = when {
            due == null -> 0
            due.isBefore(date) -> 220
            due == date -> 180
            due == date.plusDays(1) -> 100
            else -> 0
        }
        return task.priority.weight * 120 + dueBoost + fitBonus(task.durationMinutes, availableMinutes) - task.energyDemand.value * 8
    }

    private fun fitBonus(duration: Int, available: Int): Int = max(0, 40 - (available - duration).coerceAtLeast(0))

    private fun dailyPlanningLimit(shifts: List<Shift>, previousNight: Boolean, energy: Int): Int = when {
        previousNight -> 75
        shifts.any { it.type == ShiftType.LONGO } -> 45
        shifts.any { it.type in listOf(ShiftType.DIA, ShiftType.TARDE, ShiftType.NOITE) } -> if (energy <= 2) 60 else 120
        energy <= 1 -> 90
        energy == 2 -> 150
        energy == 3 -> 240
        else -> 330
    }

    private fun momentFits(moment: PreferredMoment, time: LocalTime): Boolean = when (moment) {
        PreferredMoment.QUALQUER -> true
        PreferredMoment.MANHA -> time.hour < 12
        PreferredMoment.TARDE -> time.hour in 12..17
        PreferredMoment.NOITE -> time.hour >= 18
    }

    private fun energyCap(currentEnergy: Int): Int = when (currentEnergy.coerceIn(1, 5)) {
        1, 2 -> 1
        3 -> 2
        else -> 3
    }

    private fun minutesBetween(start: LocalTime, end: LocalTime): Int =
        ((end.toSecondOfDay() - start.toSecondOfDay()) / 60).coerceAtLeast(0)

    private fun maxTime(a: LocalTime, b: LocalTime): LocalTime = if (a.isAfter(b)) a else b

    private fun LocalTime.coerceBefore(limit: LocalTime): LocalTime = if (isAfter(limit)) limit else this

    private fun addPersonalBlockIfFree(
        output: MutableList<PlanBlock>, date: LocalDate, fixed: List<Interval>, start: LocalTime, end: LocalTime
    ) {
        val overlaps = fixed.any { it.start < end && it.end > start }
        val planOverlap = output.any { it.date == date.toString() && runCatching {
            val s = LocalTime.parse(it.startTime); val e = LocalTime.parse(it.endTime)
            s < end && e > start
        }.getOrDefault(false) }
        if (!overlaps && !planOverlap) {
            output += PlanBlock(
                date = date.toString(), startTime = start.format(tf), endTime = end.format(tf),
                title = "Tempo pessoal", subtitle = "Margem protegida para lazer, pausa ou imprevistos",
                type = PlanBlockType.PESSOAL
            )
        }
    }
}
