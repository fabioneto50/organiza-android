package com.organiza.app.domain

import com.organiza.app.model.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class PlannerEngineTest {
    @Test
    fun postNightCreatesRecoveryBlock() {
        val today = LocalDate.of(2026, 8, 21)
        val previousNight = Shift(
            date = today.minusDays(1).toString(),
            type = ShiftType.NOITE,
            startTime = "20:00",
            endTime = "08:00"
        )

        val plan = PlannerEngine.generate(
            tasks = emptyList(),
            shifts = listOf(previousNight),
            preferences = UserPreferences(protectSleepAfterNight = true),
            startDate = today,
            days = 1,
            now = LocalDateTime.of(today, LocalTime.of(7, 0))
        )

        assertTrue(plan.any { it.type == PlanBlockType.RECUPERACAO })
    }

    @Test
    fun fixedAppointmentIsProtectedFromFlexibleTasks() {
        val day = LocalDate.of(2026, 8, 22)
        val appointment = Appointment(
            title = "Consulta",
            date = day.toString(),
            startTime = "11:00",
            endTime = "12:00"
        )
        val task = LifeTask(
            title = "Estudar",
            durationMinutes = 90,
            priority = TaskPriority.ALTA,
            energyDemand = EnergyDemand.MEDIA
        )

        val plan = PlannerEngine.generate(
            tasks = listOf(task),
            shifts = emptyList(),
            appointments = listOf(appointment),
            preferences = UserPreferences(currentEnergy = 4),
            startDate = day,
            days = 1,
            now = LocalDateTime.of(day, LocalTime.of(7, 0))
        )

        val flexible = plan.filter { it.type == PlanBlockType.TAREFA }
        val appointmentStart = LocalTime.of(11, 0)
        val appointmentEnd = LocalTime.of(12, 0)
        assertTrue(plan.any { it.type == PlanBlockType.COMPROMISSO && it.title == "Consulta" })
        assertFalse(flexible.any {
            val start = LocalTime.parse(it.startTime)
            val end = LocalTime.parse(it.endTime)
            start < appointmentEnd && end > appointmentStart
        })
    }

    @Test
    fun lowEnergyDoesNotScheduleHighEnergyTask() {
        val day = LocalDate.of(2026, 8, 22)
        val task = LifeTask(title = "Treino exigente", durationMinutes = 45, energyDemand = EnergyDemand.ALTA)
        val plan = PlannerEngine.generate(
            tasks = listOf(task),
            shifts = emptyList(),
            preferences = UserPreferences(currentEnergy = 1),
            effectiveEnergy = 1,
            startDate = day,
            days = 1,
            now = LocalDateTime.of(day, LocalTime.of(7, 0))
        )
        assertFalse(plan.any { it.sourceTaskId == task.id })
    }

    @Test
    fun dueHabitCanBePlanned() {
        val day = LocalDate.of(2026, 8, 24) // Monday
        val habit = Habit(
            title = "Alongamentos",
            durationMinutes = 15,
            energyDemand = EnergyDemand.BAIXA,
            daysOfWeek = setOf(day.dayOfWeek.value)
        )
        val plan = PlannerEngine.generate(
            tasks = emptyList(),
            shifts = emptyList(),
            habits = listOf(habit),
            preferences = UserPreferences(currentEnergy = 3),
            startDate = day,
            days = 1,
            now = LocalDateTime.of(day, LocalTime.of(7, 0))
        )
        assertTrue(plan.any { it.type == PlanBlockType.HABITO && it.sourceHabitId == habit.id })
    }

    @Test
    fun timeSuggestionDoesNotExceedAvailableMinutes() {
        val tasks = listOf(
            LifeTask(title = "A", durationMinutes = 10, energyDemand = EnergyDemand.BAIXA),
            LifeTask(title = "B", durationMinutes = 15, energyDemand = EnergyDemand.BAIXA),
            LifeTask(title = "C", durationMinutes = 30, energyDemand = EnergyDemand.BAIXA)
        )

        val result = PlannerEngine.suggestForMinutes(tasks, 20, 3)

        assertTrue(result.totalMinutes <= 20)
        assertEquals(result.selectedTasks.sumOf { it.durationMinutes }, result.totalMinutes)
    }
}
