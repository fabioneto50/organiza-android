package com.organiza.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.organiza.app.OrganizaViewModel
import com.organiza.app.model.ShiftType
import com.organiza.app.ui.components.*
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HomeScreen(
    viewModel: OrganizaViewModel,
    contentPadding: PaddingValues,
    openToday: () -> Unit,
    openShifts: () -> Unit,
    openCalendar: () -> Unit,
    openPlan: () -> Unit,
    openTasks: () -> Unit
) {
    val data by viewModel.data.collectAsState()
    val today = LocalDate.now()
    val pending = data.tasks.count { !it.completed }
    val planToday = data.plan.filter { it.date == today.toString() }
    val todayShift = data.shifts.firstOrNull { it.date == today.toString() }
    val nextShift = data.shifts
        .filter { it.localDate() >= today && it.type !in listOf(ShiftType.FOLGA, ShiftType.FERIAS) }
        .sortedWith(compareBy({ it.localDate() }, { it.startTime }))
        .firstOrNull()
    var showTime by remember { mutableStateOf(false) }
    var showShift by remember { mutableStateOf(false) }

    if (showTime) TimeAvailableDialog({ showTime = false }, viewModel::timeSuggestion)
    if (showShift) {
        QuickAssignShiftDialog(
            date = today,
            templates = data.shiftTemplates,
            onDismiss = { showShift = false },
            onAssign = { viewModel.applyShiftTemplate(today, it) },
            onRemove = { viewModel.removeShiftsForDate(today) },
            onCreateTemplate = {
                showShift = false
                openShifts()
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(contentPadding),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(homeGreeting(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text(
                        today.format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", Locale("pt", "PT"))).replaceFirstChar { it.uppercase() },
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.primaryContainer) {
                    Icon(Icons.Rounded.AutoAwesome, null, modifier = Modifier.padding(12.dp))
                }
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("O teu dia num relance", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    if (todayShift != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Turno de hoje", style = MaterialTheme.typography.labelMedium)
                                if (todayShift.type !in listOf(ShiftType.FOLGA, ShiftType.FERIAS)) {
                                    Text("${todayShift.startTime}–${todayShift.endTime}", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            ShiftPill(todayShift, data.shiftTemplates, modifier = Modifier.widthIn(min = 88.dp, max = 140.dp))
                        }
                    } else {
                        Text("Ainda não definiste o turno de hoje.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    EnergyIndicator(viewModel.effectiveEnergy())
                    Button(onClick = { viewModel.organizeLife(); openPlan() }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Rounded.AutoAwesome, null)
                        Spacer(Modifier.width(8.dp))
                        Text("ORGANIZA A MINHA VIDA")
                    }
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                HomeActionCard(
                    title = "Minha escala",
                    subtitle = nextShift?.let { "Próximo: ${it.localDate().dayOfMonth}/${it.localDate().monthValue}" } ?: "Ver mês completo",
                    icon = Icons.Rounded.CalendarMonth,
                    modifier = Modifier.weight(1f),
                    onClick = openShifts
                )
                HomeActionCard(
                    title = "Adicionar turno",
                    subtitle = "Hoje em 2 toques",
                    icon = Icons.Rounded.AddCircle,
                    modifier = Modifier.weight(1f),
                    onClick = { showShift = true }
                )
            }
        }

        item {
            WeekShiftStrip(
                shifts = data.shifts,
                templates = data.shiftTemplates,
                onOpen = openShifts
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                MetricCard("Tarefas", pending.toString(), Modifier.weight(1f))
                MetricCard("Plano hoje", planToday.size.toString(), Modifier.weight(1f))
                MetricCard("Energia", "${viewModel.effectiveEnergy()}/5", Modifier.weight(1f))
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { showTime = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.Timer, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Tenho X minutos")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = openCalendar, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Rounded.Event, null)
                        Spacer(Modifier.width(6.dp))
                        Text("Agenda")
                    }
                    OutlinedButton(onClick = openTasks, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Rounded.CheckCircle, null)
                        Spacer(Modifier.width(6.dp))
                        Text("Tarefas")
                    }
                }
            }
        }

        item {
            Button(onClick = openToday, modifier = Modifier.fillMaxWidth()) {
                Text("Entrar nos menus")
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Rounded.ArrowForward, null)
            }
        }
    }
}

@Composable
private fun HomeActionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(modifier = modifier.clickable(onClick = onClick)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, null)
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun WeekShiftStrip(
    shifts: List<com.organiza.app.model.Shift>,
    templates: List<com.organiza.app.model.ShiftTemplate>,
    onOpen: () -> Unit
) {
    val today = LocalDate.now()
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Próximos 7 dias", style = MaterialTheme.typography.titleMedium)
                Text("Ver mês", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                (0L..6L).forEach { offset ->
                    val date = today.plusDays(offset)
                    val shift = shifts.firstOrNull { it.date == date.toString() }
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(
                            date.format(DateTimeFormatter.ofPattern("EEE", Locale("pt", "PT"))).replace(".", ""),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(date.dayOfMonth.toString(), style = MaterialTheme.typography.bodyMedium, fontWeight = if (offset == 0L) FontWeight.Bold else FontWeight.Normal)
                        if (shift != null) ShiftPill(shift, templates, compact = true, modifier = Modifier.fillMaxWidth())
                        else Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.height(22.dp).fillMaxWidth()) {}
                    }
                }
            }
        }
    }
}

private fun homeGreeting(): String = when (LocalTime.now().hour) {
    in 5..11 -> "Bom dia"
    in 12..19 -> "Boa tarde"
    else -> "Boa noite"
}
