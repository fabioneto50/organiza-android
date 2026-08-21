package com.organiza.app.data

import android.content.Context
import com.organiza.app.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class LocalRepository(context: Context) {
    private val file = File(context.filesDir, "organiza_data.json")
    private val _data = MutableStateFlow(load())
    val data: StateFlow<AppData> = _data.asStateFlow()

    fun addTask(task: LifeTask) = update(_data.value.copy(tasks = _data.value.tasks + task))
    fun toggleTask(id: String) = update(_data.value.copy(tasks = _data.value.tasks.map { if (it.id == id) it.copy(completed = !it.completed) else it }))
    fun deleteTask(id: String) = update(_data.value.copy(tasks = _data.value.tasks.filterNot { it.id == id }))

    fun addShift(shift: Shift) = update(_data.value.copy(shifts = _data.value.shifts + shift))
    fun setShiftForDate(shift: Shift) = update(_data.value.copy(shifts = _data.value.shifts.filterNot { it.date == shift.date } + shift))
    fun addShifts(shifts: List<Shift>) = update(_data.value.copy(shifts = (_data.value.shifts + shifts).distinctBy { "${it.date}-${it.type}-${it.startTime}-${it.endTime}" }))
    fun deleteShift(id: String) = update(_data.value.copy(shifts = _data.value.shifts.filterNot { it.id == id }))
    fun clearShiftsForDate(date: String) = update(_data.value.copy(shifts = _data.value.shifts.filterNot { it.date == date }))

    fun addShiftTemplate(template: ShiftTemplate) = update(_data.value.copy(shiftTemplates = _data.value.shiftTemplates + template))
    fun deleteShiftTemplate(id: String) = update(_data.value.copy(shiftTemplates = _data.value.shiftTemplates.filterNot { it.id == id && !it.system }))

    fun addAppointment(appointment: Appointment) = update(_data.value.copy(appointments = _data.value.appointments + appointment))
    fun mergeAppointments(appointments: List<Appointment>) {
        val existingExternal = _data.value.appointments.mapNotNull { it.externalId }.toSet()
        val fresh = appointments.filter { it.externalId == null || it.externalId !in existingExternal }
        if (fresh.isNotEmpty()) update(_data.value.copy(appointments = _data.value.appointments + fresh))
    }
    fun deleteAppointment(id: String) = update(_data.value.copy(appointments = _data.value.appointments.filterNot { it.id == id }))

    fun addGoal(goal: Goal) = update(_data.value.copy(goals = _data.value.goals + goal))
    fun setGoalProgress(id: String, progress: Int) = update(_data.value.copy(goals = _data.value.goals.map {
        if (it.id == id) it.copy(progressPercent = progress.coerceIn(0, 100)) else it
    }))
    fun deleteGoal(id: String) = update(_data.value.copy(goals = _data.value.goals.filterNot { it.id == id }))

    fun addHabit(habit: Habit) = update(_data.value.copy(habits = _data.value.habits + habit))
    fun toggleHabitForDate(id: String, date: String) = update(_data.value.copy(habits = _data.value.habits.map { habit ->
        if (habit.id != id) habit else {
            val dates = habit.completedDates.toMutableSet()
            if (!dates.add(date)) dates.remove(date)
            habit.copy(completedDates = dates)
        }
    }))
    fun deleteHabit(id: String) = update(_data.value.copy(habits = _data.value.habits.filterNot { it.id == id }))

    fun savePlan(plan: List<PlanBlock>) = update(_data.value.copy(plan = plan))
    fun updatePreferences(preferences: UserPreferences) = update(_data.value.copy(preferences = preferences))
    fun updateHealth(snapshot: HealthSnapshot) = update(_data.value.copy(health = snapshot))
    fun clearAll() = update(AppData())

    private fun update(newData: AppData) {
        _data.value = newData
        runCatching { file.writeText(toJson(newData).toString(2)) }
    }

    private fun load(): AppData = runCatching {
        if (!file.exists()) return@runCatching AppData()
        fromJson(JSONObject(file.readText()))
    }.getOrElse { AppData() }

    private fun toJson(data: AppData): JSONObject = JSONObject().apply {
        put("tasks", JSONArray().apply { data.tasks.forEach { task -> put(JSONObject().apply {
            put("id", task.id); put("title", task.title); put("durationMinutes", task.durationMinutes)
            put("category", task.category.name); put("priority", task.priority.name); put("energyDemand", task.energyDemand.name)
            put("preferredMoment", task.preferredMoment.name); put("dueDate", task.dueDate ?: JSONObject.NULL)
            put("completed", task.completed); put("createdAt", task.createdAt)
        }) } })
        put("shifts", JSONArray().apply { data.shifts.forEach { shift -> put(JSONObject().apply {
            put("id", shift.id); put("date", shift.date); put("type", shift.type.name); put("startTime", shift.startTime)
            put("endTime", shift.endTime); put("note", shift.note); put("templateId", shift.templateId ?: JSONObject.NULL)
        }) } })
        put("shiftTemplates", JSONArray().apply { data.shiftTemplates.forEach { t -> put(JSONObject().apply {
            put("id", t.id); put("name", t.name); put("code", t.code); put("type", t.type.name)
            put("startTime", t.startTime); put("endTime", t.endTime); put("colorHex", t.colorHex); put("system", t.system)
        }) } })
        put("appointments", JSONArray().apply { data.appointments.forEach { a -> put(JSONObject().apply {
            put("id", a.id); put("title", a.title); put("date", a.date); put("startTime", a.startTime); put("endTime", a.endTime)
            put("location", a.location); put("note", a.note); put("source", a.source.name); put("externalId", a.externalId ?: JSONObject.NULL)
        }) } })
        put("goals", JSONArray().apply { data.goals.forEach { g -> put(JSONObject().apply {
            put("id", g.id); put("title", g.title); put("category", g.category.name); put("targetDate", g.targetDate ?: JSONObject.NULL)
            put("progressPercent", g.progressPercent); put("note", g.note)
        }) } })
        put("habits", JSONArray().apply { data.habits.forEach { h -> put(JSONObject().apply {
            put("id", h.id); put("title", h.title); put("durationMinutes", h.durationMinutes); put("energyDemand", h.energyDemand.name)
            put("preferredMoment", h.preferredMoment.name); put("daysOfWeek", JSONArray(h.daysOfWeek.toList()))
            put("completedDates", JSONArray(h.completedDates.toList())); put("active", h.active)
        }) } })
        put("plan", JSONArray().apply { data.plan.forEach { block -> put(JSONObject().apply {
            put("id", block.id); put("date", block.date); put("startTime", block.startTime); put("endTime", block.endTime)
            put("title", block.title); put("subtitle", block.subtitle); put("type", block.type.name)
            put("sourceTaskId", block.sourceTaskId ?: JSONObject.NULL); put("sourceHabitId", block.sourceHabitId ?: JSONObject.NULL)
            put("sourceAppointmentId", block.sourceAppointmentId ?: JSONObject.NULL)
        }) } })
        put("preferences", JSONObject().apply {
            val p = data.preferences
            put("currentEnergy", p.currentEnergy); put("protectSleepAfterNight", p.protectSleepAfterNight)
            put("dayStartHour", p.dayStartHour); put("dayEndHour", p.dayEndHour); put("recoveryHoursAfterNight", p.recoveryHoursAfterNight)
            put("notificationsEnabled", p.notificationsEnabled); put("reminderMinutes", p.reminderMinutes)
            put("autoReplan", p.autoReplan); put("useSleepFromHealthConnect", p.useSleepFromHealthConnect)
        })
        put("health", JSONObject().apply {
            put("lastSleepHours", data.health.lastSleepHours ?: JSONObject.NULL)
            put("lastSleepEnd", data.health.lastSleepEnd ?: JSONObject.NULL)
            put("lastSync", data.health.lastSync ?: JSONObject.NULL)
        })
    }

    private fun fromJson(json: JSONObject): AppData {
        val tasks = json.optJSONArray("tasks").toObjects { o -> LifeTask(
            id = o.optString("id", java.util.UUID.randomUUID().toString()), title = o.optString("title", "Tarefa"),
            durationMinutes = o.optInt("durationMinutes", 30), category = enumOr(TaskCategory.PESSOAL, o.optString("category")),
            priority = enumOr(TaskPriority.MEDIA, o.optString("priority")), energyDemand = enumOr(EnergyDemand.MEDIA, o.optString("energyDemand")),
            preferredMoment = enumOr(PreferredMoment.QUALQUER, o.optString("preferredMoment")), dueDate = o.nullableString("dueDate"),
            completed = o.optBoolean("completed", false), createdAt = o.optString("createdAt", "")
        ) }
        val shifts = json.optJSONArray("shifts").toObjects { o -> Shift(
            id = o.optString("id", java.util.UUID.randomUUID().toString()), date = o.optString("date"),
            type = enumOr(ShiftType.OUTRO, o.optString("type")), startTime = o.optString("startTime", "09:00"),
            endTime = o.optString("endTime", "17:00"), note = o.optString("note"), templateId = o.nullableString("templateId")
        ) }
        val templatesFromJson = json.optJSONArray("shiftTemplates").toObjects { o -> ShiftTemplate(
            id = o.optString("id", java.util.UUID.randomUUID().toString()), name = o.optString("name", "Turno"),
            code = o.optString("code", "T").take(10), type = enumOr(ShiftType.OUTRO, o.optString("type")),
            startTime = o.optString("startTime", "09:00"), endTime = o.optString("endTime", "17:00"),
            colorHex = o.optString("colorHex", "#5147FF"), system = o.optBoolean("system", false)
        ) }
        val shiftTemplates = if (templatesFromJson.isEmpty()) defaultShiftTemplates() else {
            val systemIds = templatesFromJson.filter { it.system }.map { it.id }.toSet()
            defaultShiftTemplates().filterNot { it.id in systemIds } + templatesFromJson
        }
        val appointments = json.optJSONArray("appointments").toObjects { o -> Appointment(
            id = o.optString("id", java.util.UUID.randomUUID().toString()), title = o.optString("title", "Compromisso"),
            date = o.optString("date"), startTime = o.optString("startTime", "09:00"), endTime = o.optString("endTime", "10:00"),
            location = o.optString("location"), note = o.optString("note"), source = enumOr(AppointmentSource.MANUAL, o.optString("source")),
            externalId = o.nullableString("externalId")
        ) }
        val goals = json.optJSONArray("goals").toObjects { o -> Goal(
            id = o.optString("id", java.util.UUID.randomUUID().toString()), title = o.optString("title", "Objetivo"),
            category = enumOr(TaskCategory.PESSOAL, o.optString("category")), targetDate = o.nullableString("targetDate"),
            progressPercent = o.optInt("progressPercent", 0), note = o.optString("note")
        ) }
        val habits = json.optJSONArray("habits").toObjects { o -> Habit(
            id = o.optString("id", java.util.UUID.randomUUID().toString()), title = o.optString("title", "Hábito"),
            durationMinutes = o.optInt("durationMinutes", 20), energyDemand = enumOr(EnergyDemand.BAIXA, o.optString("energyDemand")),
            preferredMoment = enumOr(PreferredMoment.QUALQUER, o.optString("preferredMoment")),
            daysOfWeek = o.optJSONArray("daysOfWeek").toIntSet((1..7).toSet()), completedDates = o.optJSONArray("completedDates").toStringSet(),
            active = o.optBoolean("active", true)
        ) }
        val plan = json.optJSONArray("plan").toObjects { o -> PlanBlock(
            id = o.optString("id", java.util.UUID.randomUUID().toString()), date = o.optString("date"), startTime = o.optString("startTime"),
            endTime = o.optString("endTime"), title = o.optString("title"), subtitle = o.optString("subtitle"),
            type = enumOr(PlanBlockType.PESSOAL, o.optString("type")), sourceTaskId = o.nullableString("sourceTaskId"),
            sourceHabitId = o.nullableString("sourceHabitId"), sourceAppointmentId = o.nullableString("sourceAppointmentId")
        ) }
        val p = json.optJSONObject("preferences")
        val preferences = UserPreferences(
            currentEnergy = p?.optInt("currentEnergy", 3) ?: 3,
            protectSleepAfterNight = p?.optBoolean("protectSleepAfterNight", true) ?: true,
            dayStartHour = p?.optInt("dayStartHour", 8) ?: 8,
            dayEndHour = p?.optInt("dayEndHour", 22) ?: 22,
            recoveryHoursAfterNight = p?.optInt("recoveryHoursAfterNight", 7) ?: 7,
            notificationsEnabled = p?.optBoolean("notificationsEnabled", false) ?: false,
            reminderMinutes = p?.optInt("reminderMinutes", 20) ?: 20,
            autoReplan = p?.optBoolean("autoReplan", true) ?: true,
            useSleepFromHealthConnect = p?.optBoolean("useSleepFromHealthConnect", false) ?: false
        )
        val h = json.optJSONObject("health")
        val health = HealthSnapshot(
            lastSleepHours = h?.nullableDouble("lastSleepHours"), lastSleepEnd = h?.nullableString("lastSleepEnd"), lastSync = h?.nullableString("lastSync")
        )
        return AppData(
            tasks = tasks,
            shifts = shifts,
            shiftTemplates = shiftTemplates,
            appointments = appointments,
            goals = goals,
            habits = habits,
            plan = plan,
            preferences = preferences,
            health = health
        )
    }
}

private inline fun <reified T : Enum<T>> enumOr(fallback: T, raw: String): T = enumValues<T>().firstOrNull { it.name == raw } ?: fallback
private fun <T> JSONArray?.toObjects(transform: (JSONObject) -> T): List<T> = if (this == null) emptyList() else buildList {
    for (i in 0 until length()) runCatching { add(transform(getJSONObject(i))) }
}
private fun JSONArray?.toStringSet(): Set<String> = if (this == null) emptySet() else buildSet {
    for (i in 0 until length()) add(optString(i))
}
private fun JSONArray?.toIntSet(fallback: Set<Int>): Set<Int> = if (this == null) fallback else buildSet {
    for (i in 0 until length()) add(optInt(i))
}.ifEmpty { fallback }
private fun JSONObject.nullableString(key: String): String? = if (!has(key) || isNull(key)) null else optString(key).takeIf { it.isNotBlank() && it != "null" }
private fun JSONObject.nullableDouble(key: String): Double? = if (!has(key) || isNull(key)) null else optDouble(key).takeIf { !it.isNaN() }
