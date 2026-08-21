package com.organiza.app.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

enum class TaskCategory(val label: String) {
    CASA("Casa"), SAUDE("Saúde"), EXERCICIO("Exercício"), ESTUDO("Estudo"),
    TRABALHO("Trabalho"), RECADOS("Recados"), PESSOAL("Pessoal")
}

enum class TaskPriority(val label: String, val weight: Int) {
    BAIXA("Baixa", 1), MEDIA("Média", 2), ALTA("Alta", 3), URGENTE("Urgente", 4)
}

enum class EnergyDemand(val label: String, val value: Int) {
    BAIXA("Baixa", 1), MEDIA("Média", 2), ALTA("Alta", 3)
}

enum class PreferredMoment(val label: String) {
    QUALQUER("Qualquer"), MANHA("Manhã"), TARDE("Tarde"), NOITE("Noite")
}

enum class ShiftType(val label: String) {
    DIA("Diurno"), TARDE("Tarde"), NOITE("Noturno"), LONGO("Prolongado"),
    FOLGA("Folga"), FERIAS("Férias"), OUTRO("Outro")
}

data class ShiftTemplate(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val code: String,
    val type: ShiftType,
    val startTime: String,
    val endTime: String,
    val colorHex: String,
    val system: Boolean = false
) {
    fun start(): LocalTime = LocalTime.parse(startTime)
    fun end(): LocalTime = LocalTime.parse(endTime)
}

fun defaultShiftTemplates(): List<ShiftTemplate> = listOf(
    ShiftTemplate("builtin-morning", "Manhã", "M", ShiftType.DIA, "08:00", "16:00", "#5147FF", true),
    ShiftTemplate("builtin-afternoon", "Tarde", "T", ShiftType.TARDE, "16:00", "23:59", "#7C3AED", true),
    ShiftTemplate("builtin-night", "Noite", "N", ShiftType.NOITE, "20:00", "08:00", "#28004F", true),
    ShiftTemplate("builtin-long", "12 horas", "12H", ShiftType.LONGO, "08:00", "20:00", "#6B6B6B", true),
    ShiftTemplate("builtin-off", "Folga", "Folga", ShiftType.FOLGA, "00:00", "00:00", "#FF3434", true),
    ShiftTemplate("builtin-vacation", "Férias", "Férias", ShiftType.FERIAS, "00:00", "00:00", "#E79BAA", true)
)

enum class AppointmentSource { MANUAL, DEVICE_CALENDAR }

data class LifeTask(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val durationMinutes: Int,
    val category: TaskCategory = TaskCategory.PESSOAL,
    val priority: TaskPriority = TaskPriority.MEDIA,
    val energyDemand: EnergyDemand = EnergyDemand.MEDIA,
    val preferredMoment: PreferredMoment = PreferredMoment.QUALQUER,
    val dueDate: String? = null,
    val completed: Boolean = false,
    val createdAt: String = LocalDateTime.now().toString()
) {
    fun dueLocalDate(): LocalDate? = dueDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
}

data class Shift(
    val id: String = UUID.randomUUID().toString(),
    val date: String,
    val type: ShiftType,
    val startTime: String,
    val endTime: String,
    val note: String = "",
    val templateId: String? = null
) {
    fun localDate(): LocalDate = LocalDate.parse(date)
    fun start(): LocalTime = LocalTime.parse(startTime)
    fun end(): LocalTime = LocalTime.parse(endTime)
}

data class Appointment(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val date: String,
    val startTime: String,
    val endTime: String,
    val location: String = "",
    val note: String = "",
    val source: AppointmentSource = AppointmentSource.MANUAL,
    val externalId: String? = null
) {
    fun localDate(): LocalDate = LocalDate.parse(date)
    fun start(): LocalTime = LocalTime.parse(startTime)
    fun end(): LocalTime = LocalTime.parse(endTime)
}

data class Goal(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val category: TaskCategory = TaskCategory.PESSOAL,
    val targetDate: String? = null,
    val progressPercent: Int = 0,
    val note: String = ""
)

data class Habit(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val durationMinutes: Int = 20,
    val energyDemand: EnergyDemand = EnergyDemand.BAIXA,
    val preferredMoment: PreferredMoment = PreferredMoment.QUALQUER,
    val daysOfWeek: Set<Int> = DayOfWeek.entries.map { it.value }.toSet(),
    val completedDates: Set<String> = emptySet(),
    val active: Boolean = true
) {
    fun isDue(date: LocalDate): Boolean = active && daysOfWeek.contains(date.dayOfWeek.value)
    fun isCompleted(date: LocalDate): Boolean = completedDates.contains(date.toString())
}

enum class PlanBlockType { TURNO, COMPROMISSO, TAREFA, HABITO, RECUPERACAO, PESSOAL, REFEICAO }

data class PlanBlock(
    val id: String = UUID.randomUUID().toString(),
    val date: String,
    val startTime: String,
    val endTime: String,
    val title: String,
    val subtitle: String = "",
    val type: PlanBlockType,
    val sourceTaskId: String? = null,
    val sourceHabitId: String? = null,
    val sourceAppointmentId: String? = null
)

data class HealthSnapshot(
    val lastSleepHours: Double? = null,
    val lastSleepEnd: String? = null,
    val lastSync: String? = null
)

data class UserPreferences(
    val currentEnergy: Int = 3,
    val protectSleepAfterNight: Boolean = true,
    val dayStartHour: Int = 8,
    val dayEndHour: Int = 22,
    val recoveryHoursAfterNight: Int = 7,
    val notificationsEnabled: Boolean = false,
    val reminderMinutes: Int = 20,
    val autoReplan: Boolean = true,
    val useSleepFromHealthConnect: Boolean = false
)

data class AppData(
    val tasks: List<LifeTask> = emptyList(),
    val shifts: List<Shift> = emptyList(),
    val shiftTemplates: List<ShiftTemplate> = defaultShiftTemplates(),
    val appointments: List<Appointment> = emptyList(),
    val goals: List<Goal> = emptyList(),
    val habits: List<Habit> = emptyList(),
    val plan: List<PlanBlock> = emptyList(),
    val preferences: UserPreferences = UserPreferences(),
    val health: HealthSnapshot = HealthSnapshot()
)

data class TimeSuggestion(
    val minutesAvailable: Int,
    val selectedTasks: List<LifeTask>,
    val totalMinutes: Int,
    val message: String
)
