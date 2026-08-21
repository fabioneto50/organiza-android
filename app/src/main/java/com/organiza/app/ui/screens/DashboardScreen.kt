package com.organiza.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.organiza.app.OrganizaViewModel
import com.organiza.app.model.ShiftType
import com.organiza.app.ui.components.*
import java.time.LocalDate
import java.time.LocalTime

@Composable
fun DashboardScreen(
    viewModel: OrganizaViewModel,
    contentPadding: PaddingValues,
    openPlan: () -> Unit,
    openCalendar: () -> Unit,
    openTasks: () -> Unit
) {
    val data by viewModel.data.collectAsState()
    var showTime by remember { mutableStateOf(false) }
    var showTask by remember { mutableStateOf(false) }
    var showAppointment by remember { mutableStateOf(false) }
    val today = LocalDate.now()
    val planToday = data.plan.filter { it.date == today.toString() }
    val pending = data.tasks.count { !it.completed }
    val effectiveEnergy = viewModel.effectiveEnergy()
    val nextShift = data.shifts.filter { it.type !in listOf(ShiftType.FOLGA, ShiftType.FERIAS) && it.localDate() >= today }
        .sortedWith(compareBy({ it.localDate() }, { it.startTime })).firstOrNull()

    if (showTime) TimeAvailableDialog({ showTime = false }, viewModel::timeSuggestion)
    if (showTask) AddTaskDialog({ showTask = false }, viewModel::addTask)
    if (showAppointment) AddAppointmentDialog({ showAppointment = false }, viewModel::addAppointment)

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(contentPadding),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(greeting(), style = MaterialTheme.typography.headlineMedium)
                Text("A app organiza o que cabe no teu dia — e protege o que precisa de ficar livre.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        item {
            Card {
                Column(modifier = Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("Estado de hoje", style = MaterialTheme.typography.titleMedium)
                            Text("$pending tarefas pendentes", style = MaterialTheme.typography.bodySmall)
                        }
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null)
                    }
                    EnergyIndicator(effectiveEnergy)
                    if (data.preferences.useSleepFromHealthConnect && data.health.lastSleepHours != null) {
                        Text("Sono recente: ${"%.1f".format(data.health.lastSleepHours)} h · energia efetiva $effectiveEnergy/5", style = MaterialTheme.typography.bodySmall)
                    }
                    Button(onClick = { viewModel.organizeLife(); openPlan() }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Rounded.AutoAwesome, null); Spacer(Modifier.width(8.dp)); Text("ORGANIZA A MINHA VIDA")
                    }
                    OutlinedButton(onClick = { showTime = true }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Rounded.Timer, null); Spacer(Modifier.width(8.dp)); Text("Tenho X minutos")
                    }
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                MetricCard("Tarefas", pending.toString(), Modifier.weight(1f))
                MetricCard("Plano hoje", planToday.size.toString(), Modifier.weight(1f))
                MetricCard("Hábitos", data.habits.count { it.isDue(today) && !it.isCompleted(today) }.toString(), Modifier.weight(1f))
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = { showTask = true }, label = { Text("Nova tarefa") }, leadingIcon = { Icon(Icons.Rounded.AddTask, null) })
                AssistChip(onClick = { showAppointment = true }, label = { Text("Compromisso") }, leadingIcon = { Icon(Icons.Rounded.Event, null) })
                AssistChip(onClick = openCalendar, label = { Text("Agenda") }, leadingIcon = { Icon(Icons.Rounded.CalendarMonth, null) })
            }
        }

        item { SectionTitle("Próximo turno") }
        item {
            if (nextShift == null) Text("Sem turno futuro registado.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            else Card {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("${friendlyDate(nextShift.localDate())} · ${nextShift.type.label}", style = MaterialTheme.typography.titleSmall)
                        Text("${nextShift.startTime}–${nextShift.endTime}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Rounded.WorkHistory, null)
                }
            }
        }

        item { SectionTitle("Plano de hoje", if (planToday.isNotEmpty()) "${planToday.size} blocos" else null) }
        if (planToday.isEmpty()) {
            item {
                Card { Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Ainda não existe um plano para hoje.")
                    TextButton(onClick = { viewModel.organizeLife(); openPlan() }) { Text("Gerar plano") }
                } }
            }
        } else {
            items(planToday, key = { it.id }) { PlanBlockRow(it) }
        }

        item {
            OutlinedButton(onClick = openTasks, modifier = Modifier.fillMaxWidth()) { Text("Ver todas as tarefas") }
        }
    }
}

private fun greeting(): String = when (LocalTime.now().hour) {
    in 5..11 -> "Bom dia"
    in 12..19 -> "Boa tarde"
    else -> "Boa noite"
}
