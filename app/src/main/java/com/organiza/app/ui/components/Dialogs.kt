package com.organiza.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.organiza.app.model.*
import java.time.LocalDate
import java.time.LocalTime

@Composable
fun AddTaskDialog(
    onDismiss: () -> Unit,
    onSave: (String, Int, TaskCategory, TaskPriority, EnergyDemand, PreferredMoment, LocalDate?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var minutesText by remember { mutableStateOf("30") }
    var category by remember { mutableStateOf(TaskCategory.PESSOAL) }
    var priority by remember { mutableStateOf(TaskPriority.MEDIA) }
    var energy by remember { mutableStateOf(EnergyDemand.MEDIA) }
    var moment by remember { mutableStateOf(PreferredMoment.QUALQUER) }
    var dueOffset by remember { mutableStateOf<Int?>(null) }
    val minutes = minutesText.toIntOrNull() ?: 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nova tarefa") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("O que tens para fazer?") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    minutesText, { minutesText = it.filter(Char::isDigit).take(3) }, label = { Text("Duração estimada (min)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                ChoiceRow("Categoria", TaskCategory.entries, category, { it.label }) { category = it }
                ChoiceRow("Prioridade", TaskPriority.entries, priority, { it.label }) { priority = it }
                ChoiceRow("Energia necessária", EnergyDemand.entries, energy, { it.label }) { energy = it }
                ChoiceRow("Preferência", PreferredMoment.entries, moment, { it.label }) { moment = it }
                Text("Prazo", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { FilterChip(selected = dueOffset == null, onClick = { dueOffset = null }, label = { Text("Sem prazo") }) }
                    items(listOf(0, 1, 3, 7, 14)) { offset ->
                        FilterChip(selected = dueOffset == offset, onClick = { dueOffset = offset }, label = { Text(if (offset == 0) "Hoje" else "+$offset d") })
                    }
                }
            }
        },
        confirmButton = {
            Button(enabled = title.isNotBlank() && minutes in 5..480, onClick = {
                onSave(title, minutes, category, priority, energy, moment, dueOffset?.let { LocalDate.now().plusDays(it.toLong()) })
                onDismiss()
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
fun AddAppointmentDialog(
    onDismiss: () -> Unit,
    onSave: (String, LocalDate, String, String, String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var offset by remember { mutableIntStateOf(0) }
    var start by remember { mutableStateOf("10:00") }
    var end by remember { mutableStateOf("11:00") }
    var location by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Novo compromisso") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("Título") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Text("Dia", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items((0..14).toList()) { d -> FilterChip(selected = offset == d, onClick = { offset = d }, label = { Text(when (d) { 0 -> "Hoje"; 1 -> "Amanhã"; else -> "+$d d" }) }) }
                }
                TimeFields(start, { start = normalizeTimeInput(it) }, end, { end = normalizeTimeInput(it) })
                OutlinedTextField(location, { location = it }, label = { Text("Local (opcional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(note, { note = it }, label = { Text("Nota (opcional)") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(enabled = title.isNotBlank() && isValidTime(start) && isValidTime(end), onClick = {
                onSave(title, LocalDate.now().plusDays(offset.toLong()), start, end, location, note); onDismiss()
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
fun AddShiftDialog(onDismiss: () -> Unit, onSave: (LocalDate, ShiftType, String, String, String) -> Unit) {
    var offset by remember { mutableIntStateOf(0) }
    var type by remember { mutableStateOf(ShiftType.DIA) }
    var start by remember { mutableStateOf("08:00") }
    var end by remember { mutableStateOf("16:00") }
    var note by remember { mutableStateOf("") }

    fun applyPreset(newType: ShiftType) {
        type = newType
        when (newType) {
            ShiftType.DIA -> { start = "08:00"; end = "16:00" }
            ShiftType.TARDE -> { start = "16:00"; end = "23:59" }
            ShiftType.NOITE -> { start = "20:00"; end = "08:00" }
            ShiftType.LONGO -> { start = "08:00"; end = "20:00" }
            ShiftType.FOLGA, ShiftType.FERIAS -> { start = "00:00"; end = "00:00" }
            ShiftType.OUTRO -> { start = "09:00"; end = "17:00" }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Adicionar turno") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Dia", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items((0..14).toList()) { d -> FilterChip(selected = offset == d, onClick = { offset = d }, label = { Text(if (d == 0) "Hoje" else "+$d d") }) }
                }
                ChoiceRow("Tipo", ShiftType.entries, type, { it.label }) { applyPreset(it) }
                if (type !in listOf(ShiftType.FOLGA, ShiftType.FERIAS)) TimeFields(start, { start = normalizeTimeInput(it) }, end, { end = normalizeTimeInput(it) })
                OutlinedTextField(note, { note = it }, label = { Text("Nota (opcional)") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            val valid = type in listOf(ShiftType.FOLGA, ShiftType.FERIAS) || (isValidTime(start) && isValidTime(end))
            Button(enabled = valid, onClick = { onSave(LocalDate.now().plusDays(offset.toLong()), type, start, end, note); onDismiss() }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
fun ImportShiftPatternDialog(onDismiss: () -> Unit, onImport: (LocalDate, String) -> Int) {
    var offset by remember { mutableIntStateOf(0) }
    var pattern by remember { mutableStateOf("M M N N F F") }
    var imported by remember { mutableStateOf<Int?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Importar padrão de turnos") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Códigos: M/D 08–16 · T 16–24 · N 20–08 · L/12 08–20 · F folga · V férias.")
                ChoiceRow("Começar", listOf(0, 1, 2, 3, 7), offset, { if (it == 0) "Hoje" else "+$it d" }) { offset = it }
                OutlinedTextField(pattern, { pattern = it }, label = { Text("Ex.: M M N N F F") }, modifier = Modifier.fillMaxWidth())
                imported?.let { Text("$it dias importados.", color = MaterialTheme.colorScheme.primary) }
            }
        },
        confirmButton = {
            Button(enabled = pattern.isNotBlank(), onClick = { imported = onImport(LocalDate.now().plusDays(offset.toLong()), pattern) }) { Text("Importar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Fechar") } }
    )
}

@Composable
fun AddGoalDialog(onDismiss: () -> Unit, onSave: (String, TaskCategory, LocalDate?, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(TaskCategory.PESSOAL) }
    var targetOffset by remember { mutableStateOf<Int?>(30) }
    var note by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss, title = { Text("Novo objetivo") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(title, { title = it }, label = { Text("Objetivo") }, modifier = Modifier.fillMaxWidth())
            ChoiceRow("Categoria", TaskCategory.entries, category, { it.label }) { category = it }
            Text("Prazo", style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { FilterChip(selected = targetOffset == null, onClick = { targetOffset = null }, label = { Text("Sem prazo") }) }
                items(listOf(7, 30, 60, 90, 180)) { d -> FilterChip(selected = targetOffset == d, onClick = { targetOffset = d }, label = { Text("$d d") }) }
            }
            OutlinedTextField(note, { note = it }, label = { Text("Nota (opcional)") }, modifier = Modifier.fillMaxWidth())
        } },
        confirmButton = { Button(enabled = title.isNotBlank(), onClick = {
            onSave(title, category, targetOffset?.let { LocalDate.now().plusDays(it.toLong()) }, note); onDismiss()
        }) { Text("Guardar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
fun AddHabitDialog(onDismiss: () -> Unit, onSave: (String, Int, EnergyDemand, PreferredMoment, Set<Int>) -> Unit) {
    var title by remember { mutableStateOf("") }
    var minutesText by remember { mutableStateOf("20") }
    var energy by remember { mutableStateOf(EnergyDemand.BAIXA) }
    var moment by remember { mutableStateOf(PreferredMoment.QUALQUER) }
    var days by remember { mutableStateOf((1..7).toSet()) }
    val minutes = minutesText.toIntOrNull() ?: 0
    val dayLabels = listOf("Seg", "Ter", "Qua", "Qui", "Sex", "Sáb", "Dom")
    AlertDialog(
        onDismissRequest = onDismiss, title = { Text("Novo hábito") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(title, { title = it }, label = { Text("Hábito") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(minutesText, { minutesText = it.filter(Char::isDigit).take(3) }, label = { Text("Duração (min)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            ChoiceRow("Energia", EnergyDemand.entries, energy, { it.label }) { energy = it }
            ChoiceRow("Momento", PreferredMoment.entries, moment, { it.label }) { moment = it }
            Text("Dias", style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items((1..7).toList()) { d ->
                    FilterChip(selected = d in days, onClick = { days = if (d in days) days - d else days + d }, label = { Text(dayLabels[d - 1]) })
                }
            }
        } },
        confirmButton = { Button(enabled = title.isNotBlank() && minutes in 5..240 && days.isNotEmpty(), onClick = { onSave(title, minutes, energy, moment, days); onDismiss() }) { Text("Guardar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
fun TimeAvailableDialog(onDismiss: () -> Unit, onSuggest: (Int) -> TimeSuggestion) {
    var minutesText by remember { mutableStateOf("20") }
    var suggestion by remember { mutableStateOf<TimeSuggestion?>(null) }
    val minutes = minutesText.toIntOrNull() ?: 0
    AlertDialog(
        onDismissRequest = onDismiss, title = { Text("Tenho X minutos") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("A app cruza o tempo disponível com a prioridade e o nível de energia atual.")
            OutlinedTextField(minutesText, { minutesText = it.filter(Char::isDigit).take(3) }, label = { Text("Minutos disponíveis") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(listOf(10, 15, 20, 30, 45, 60)) { p -> AssistChip(onClick = { minutesText = p.toString() }, label = { Text("$p min") }) } }
            suggestion?.let { result ->
                HorizontalDivider(); Text(result.message)
                result.selectedTasks.forEach { task -> Text("• ${task.title} · ${task.durationMinutes} min") }
                if (result.selectedTasks.isNotEmpty()) Text("Total: ${result.totalMinutes}/${result.minutesAvailable} min", style = MaterialTheme.typography.labelMedium)
            }
        } },
        confirmButton = { Button(enabled = minutes in 5..480, onClick = { suggestion = onSuggest(minutes) }) { Text(if (suggestion == null) "Sugerir" else "Recalcular") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Fechar") } }
    )
}

@Composable
private fun TimeFields(start: String, onStart: (String) -> Unit, end: String, onEnd: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(start, onStart, label = { Text("Início") }, singleLine = true, modifier = Modifier.weight(1f))
        OutlinedTextField(end, onEnd, label = { Text("Fim") }, singleLine = true, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun <T> ChoiceRow(title: String, values: List<T>, selected: T, label: (T) -> String, onSelected: (T) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(values) { value -> FilterChip(selected = selected == value, onClick = { onSelected(value) }, label = { Text(label(value)) }) }
        }
    }
}

private fun normalizeTimeInput(raw: String): String = raw.filter { it.isDigit() || it == ':' }.take(5)
private fun isValidTime(value: String): Boolean = runCatching { LocalTime.parse(value) }.isSuccess
