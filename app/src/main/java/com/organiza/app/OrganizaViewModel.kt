package com.organiza.app

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.organiza.app.data.LocalRepository
import com.organiza.app.domain.PlannerEngine
import com.organiza.app.integrations.CalendarIntegration
import com.organiza.app.integrations.HealthConnectManager
import com.organiza.app.model.*
import com.organiza.app.notifications.NotificationScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.math.min

class OrganizaViewModel(
    private val repository: LocalRepository,
    private val notifications: NotificationScheduler,
    private val calendar: CalendarIntegration,
    private val healthConnect: HealthConnectManager
) : ViewModel() {
    val data: StateFlow<AppData> = repository.data

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun consumeMessage() { _message.value = null }

    fun addTask(
        title: String,
        minutes: Int,
        category: TaskCategory,
        priority: TaskPriority,
        energy: EnergyDemand,
        preferredMoment: PreferredMoment,
        dueDate: LocalDate?
    ) {
        if (title.isBlank() || minutes <= 0) return
        repository.addTask(
            LifeTask(
                title = title.trim(), durationMinutes = minutes, category = category,
                priority = priority, energyDemand = energy, preferredMoment = preferredMoment,
                dueDate = dueDate?.toString()
            )
        )
        maybeReplan()
    }

    fun toggleTask(id: String) { repository.toggleTask(id); maybeReplan() }
    fun deleteTask(id: String) { repository.deleteTask(id); maybeReplan() }

    fun addShift(date: LocalDate, type: ShiftType, start: String, end: String, note: String) {
        repository.addShift(Shift(date = date.toString(), type = type, startTime = start, endTime = end, note = note.trim()))
        maybeReplan()
    }

    fun applyShiftTemplate(date: LocalDate, templateId: String) {
        val template = data.value.shiftTemplates.firstOrNull { it.id == templateId } ?: return
        repository.setShiftForDate(
            Shift(
                date = date.toString(),
                type = template.type,
                startTime = template.startTime,
                endTime = template.endTime,
                note = template.name,
                templateId = template.id
            )
        )
        maybeReplan()
    }

    fun removeShiftsForDate(date: LocalDate) {
        repository.clearShiftsForDate(date.toString())
        maybeReplan()
    }

    fun createShiftTemplate(
        name: String,
        code: String,
        type: ShiftType,
        start: String,
        end: String,
        colorHex: String
    ) {
        if (name.isBlank() || code.isBlank()) return
        repository.addShiftTemplate(
            ShiftTemplate(
                name = name.trim(),
                code = code.trim().take(10),
                type = type,
                startTime = if (type in listOf(ShiftType.FOLGA, ShiftType.FERIAS)) "00:00" else start,
                endTime = if (type in listOf(ShiftType.FOLGA, ShiftType.FERIAS)) "00:00" else end,
                colorHex = colorHex,
                system = false
            )
        )
        _message.value = "Tipo de turno guardado."
    }

    fun deleteShiftTemplate(id: String) {
        val template = data.value.shiftTemplates.firstOrNull { it.id == id } ?: return
        if (template.system) return
        repository.deleteShiftTemplate(id)
    }

    fun deleteShift(id: String) { repository.deleteShift(id); maybeReplan() }

    fun importShiftPattern(startDate: LocalDate, pattern: String): Int {
        val codes = pattern.uppercase().split(Regex("[\\s,;|/]+" )).filter { it.isNotBlank() }
        var offset = 0L
        val shifts = mutableListOf<Shift>()
        codes.forEach { code ->
            val date = startDate.plusDays(offset)
            val shift = when (code) {
                "M", "D" -> Shift(date = date.toString(), type = ShiftType.DIA, startTime = "08:00", endTime = "16:00", note = "Importado: $code", templateId = "builtin-morning")
                "T" -> Shift(date = date.toString(), type = ShiftType.TARDE, startTime = "16:00", endTime = "23:59", note = "Importado: T", templateId = "builtin-afternoon")
                "N" -> Shift(date = date.toString(), type = ShiftType.NOITE, startTime = "20:00", endTime = "08:00", note = "Importado: N", templateId = "builtin-night")
                "L", "12" -> Shift(date = date.toString(), type = ShiftType.LONGO, startTime = "08:00", endTime = "20:00", note = "Importado: $code", templateId = "builtin-long")
                "F" -> Shift(date = date.toString(), type = ShiftType.FOLGA, startTime = "00:00", endTime = "00:00", note = "Importado: F", templateId = "builtin-off")
                "V" -> Shift(date = date.toString(), type = ShiftType.FERIAS, startTime = "00:00", endTime = "00:00", note = "Importado: V", templateId = "builtin-vacation")
                else -> null
            }
            if (shift != null) { shifts += shift; offset++ }
        }
        repository.addShifts(shifts)
        if (shifts.isNotEmpty()) maybeReplan()
        return shifts.size
    }

    fun addAppointment(title: String, date: LocalDate, start: String, end: String, location: String, note: String) {
        if (title.isBlank()) return
        repository.addAppointment(
            Appointment(title = title.trim(), date = date.toString(), startTime = start, endTime = end, location = location.trim(), note = note.trim())
        )
        maybeReplan()
    }

    fun deleteAppointment(id: String) { repository.deleteAppointment(id); maybeReplan() }

    fun importDeviceCalendar(days: Int = 14) {
        viewModelScope.launch {
            runCatching {
                val imported = withContext(Dispatchers.IO) { calendar.importUpcoming(days) }
                repository.mergeAppointments(imported)
                maybeReplan()
                imported.size
            }.onSuccess { count ->
                _message.value = if (count == 0) "Não encontrei eventos novos no calendário." else "Importados $count eventos do calendário."
            }.onFailure { error ->
                _message.value = "Não foi possível importar o calendário: ${error.message ?: "erro de acesso"}."
            }
        }
    }

    fun appointmentCalendarIntent(appointment: Appointment): Intent = calendar.appointmentIntent(appointment)
    fun planCalendarIntent(block: PlanBlock): Intent = calendar.planBlockIntent(block)

    fun addGoal(title: String, category: TaskCategory, targetDate: LocalDate?, note: String) {
        if (title.isBlank()) return
        repository.addGoal(Goal(title = title.trim(), category = category, targetDate = targetDate?.toString(), note = note.trim()))
    }

    fun setGoalProgress(id: String, progress: Int) = repository.setGoalProgress(id, progress)
    fun deleteGoal(id: String) = repository.deleteGoal(id)

    fun addHabit(
        title: String,
        minutes: Int,
        energy: EnergyDemand,
        preferredMoment: PreferredMoment,
        daysOfWeek: Set<Int>
    ) {
        if (title.isBlank() || minutes <= 0 || daysOfWeek.isEmpty()) return
        repository.addHabit(Habit(title = title.trim(), durationMinutes = minutes, energyDemand = energy, preferredMoment = preferredMoment, daysOfWeek = daysOfWeek))
        maybeReplan()
    }

    fun toggleHabitToday(id: String) { repository.toggleHabitForDate(id, LocalDate.now().toString()); maybeReplan() }
    fun deleteHabit(id: String) { repository.deleteHabit(id); maybeReplan() }

    fun organizeLife(days: Int = 7) {
        val current = data.value
        val plan = PlannerEngine.generate(
            tasks = current.tasks,
            shifts = current.shifts,
            appointments = current.appointments,
            habits = current.habits,
            preferences = current.preferences,
            effectiveEnergy = effectiveEnergy(current),
            days = days,
            now = LocalDateTime.now()
        )
        repository.savePlan(plan)
        notifications.reschedule(plan, current.preferences.reminderMinutes, current.preferences.notificationsEnabled)
    }

    fun reorganizeNow() {
        organizeLife(7)
        _message.value = "Plano reorganizado a partir de agora."
    }

    fun timeSuggestion(minutes: Int): TimeSuggestion = PlannerEngine.suggestForMinutes(
        tasks = data.value.tasks, minutes = minutes, currentEnergy = effectiveEnergy(data.value)
    )

    fun effectiveEnergy(): Int = effectiveEnergy(data.value)

    private fun effectiveEnergy(current: AppData): Int {
        val manual = current.preferences.currentEnergy.coerceIn(1, 5)
        if (!current.preferences.useSleepFromHealthConnect) return manual
        val hours = current.health.lastSleepHours ?: return manual
        val sleepCap = when {
            hours < 5.0 -> 1
            hours < 6.0 -> 2
            hours < 7.0 -> 3
            hours < 8.0 -> 4
            else -> 5
        }
        return min(manual, sleepCap)
    }

    fun setEnergy(value: Int) {
        repository.updatePreferences(data.value.preferences.copy(currentEnergy = value.coerceIn(1, 5)))
        maybeReplan()
    }

    fun setProtectSleep(enabled: Boolean) { repository.updatePreferences(data.value.preferences.copy(protectSleepAfterNight = enabled)); maybeReplan() }
    fun setAutoReplan(enabled: Boolean) = repository.updatePreferences(data.value.preferences.copy(autoReplan = enabled))
    fun setNotifications(enabled: Boolean) {
        repository.updatePreferences(data.value.preferences.copy(notificationsEnabled = enabled))
        notifications.reschedule(data.value.plan, data.value.preferences.reminderMinutes, enabled)
    }
    fun setReminderMinutes(minutes: Int) {
        val value = minutes.coerceIn(5, 120)
        repository.updatePreferences(data.value.preferences.copy(reminderMinutes = value))
        notifications.reschedule(data.value.plan, value, data.value.preferences.notificationsEnabled)
    }
    fun setUseSleepFromHealthConnect(enabled: Boolean) {
        repository.updatePreferences(data.value.preferences.copy(useSleepFromHealthConnect = enabled))
        maybeReplan()
    }

    fun healthPermissions(): Set<String> = healthConnect.permissions
    fun healthAvailability(): Int = healthConnect.availability()

    fun syncHealthConnect() {
        viewModelScope.launch {
            runCatching { healthConnect.readLatestSleep() }
                .onSuccess { summary ->
                    if (summary == null) {
                        _message.value = "Sem sessão de sono disponível ou sem permissão."
                    } else {
                        repository.updateHealth(
                            HealthSnapshot(
                                lastSleepHours = summary.hours,
                                lastSleepEnd = summary.endTime,
                                lastSync = LocalDateTime.now().toString()
                            )
                        )
                        _message.value = "Sono atualizado: ${"%.1f".format(summary.hours)} h."
                        maybeReplan()
                    }
                }
                .onFailure { _message.value = "Não foi possível ler o Health Connect: ${it.message ?: "erro"}." }
        }
    }

    fun clearAll() {
        repository.clearAll()
        notifications.reschedule(emptyList(), 20, false)
    }

    private fun maybeReplan() {
        if (data.value.preferences.autoReplan) organizeLife(7)
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val appContext = context.applicationContext
                return OrganizaViewModel(
                    LocalRepository(appContext),
                    NotificationScheduler(appContext),
                    CalendarIntegration(appContext),
                    HealthConnectManager(appContext)
                ) as T
            }
        }
    }
}
