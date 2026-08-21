package com.organiza.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.organiza.app.OrganizaViewModel
import com.organiza.app.model.PlanBlockType
import com.organiza.app.ui.components.*
import java.time.LocalDate

@Composable
fun CalendarScreen(viewModel: OrganizaViewModel, contentPadding: PaddingValues) {
    val data by viewModel.data.collectAsState()
    val context = LocalContext.current
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var showAdd by remember { mutableStateOf(false) }
    val calendarPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) viewModel.importDeviceCalendar()
    }

    if (showAdd) AddAppointmentDialog({ showAdd = false }, viewModel::addAppointment)

    val appointments = data.appointments.filter { it.date == selectedDate.toString() }.sortedBy { it.startTime }
    val shifts = data.shifts.filter { it.date == selectedDate.toString() }.sortedBy { it.startTime }
    val flexiblePlan = data.plan.filter { it.date == selectedDate.toString() && it.type !in setOf(PlanBlockType.COMPROMISSO, PlanBlockType.TURNO) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(contentPadding),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column { Text("Agenda", style = MaterialTheme.typography.headlineMedium); Text("Compromissos, turnos e plano no mesmo sítio.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                FilledIconButton(onClick = { showAdd = true }) { Icon(Icons.Rounded.Add, "Adicionar compromisso") }
            }
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items((0..13).map { LocalDate.now().plusDays(it.toLong()) }) { date ->
                    FilterChip(
                        selected = selectedDate == date,
                        onClick = { selectedDate = date },
                        label = { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(shortDay(date)); Text(date.dayOfMonth.toString()) } }
                    )
                }
            }
        }
        item {
            OutlinedButton(onClick = {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED) viewModel.importDeviceCalendar()
                else calendarPermission.launch(Manifest.permission.READ_CALENDAR)
            }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Rounded.Download, null); Spacer(Modifier.width(8.dp)); Text("Importar próximos 14 dias do calendário")
            }
        }

        item { SectionTitle(friendlyDate(selectedDate)) }

        if (appointments.isNotEmpty()) {
            item { Text("Compromissos", style = MaterialTheme.typography.titleSmall) }
            items(appointments, key = { it.id }) { a ->
                Card {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("${a.startTime}–${a.endTime} · ${a.title}", style = MaterialTheme.typography.titleSmall)
                            if (a.location.isNotBlank()) Text(a.location, style = MaterialTheme.typography.bodySmall)
                            Text(if (a.source.name == "DEVICE_CALENDAR") "Importado do calendário" else "Criado na Organiza", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        TextButton(onClick = { context.startActivity(viewModel.appointmentCalendarIntent(a)) }) { Text("Calendário") }
                        IconButton(onClick = { viewModel.deleteAppointment(a.id) }) { Text("×") }
                    }
                }
            }
        }

        if (shifts.isNotEmpty()) {
            item { Text("Turnos", style = MaterialTheme.typography.titleSmall) }
            items(shifts, key = { it.id }) { shift ->
                Card { Column(Modifier.fillMaxWidth().padding(14.dp)) { Text(shift.type.label, style = MaterialTheme.typography.titleSmall); Text("${shift.startTime}–${shift.endTime}") } }
            }
        }

        if (flexiblePlan.isNotEmpty()) {
            item { Text("Plano inteligente", style = MaterialTheme.typography.titleSmall) }
            items(flexiblePlan, key = { it.id }) { block -> PlanBlockRow(block) { context.startActivity(viewModel.planCalendarIntent(block)) } }
        }

        if (appointments.isEmpty() && shifts.isEmpty() && flexiblePlan.isEmpty()) {
            item { Card { Text("Este dia está livre. A app pode usar parte desta margem quando gerar o plano.", modifier = Modifier.padding(18.dp)) } }
        }
    }
}
