package com.organiza.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.organiza.app.OrganizaViewModel
import com.organiza.app.ui.components.AddShiftDialog
import com.organiza.app.ui.components.ImportShiftPatternDialog
import com.organiza.app.ui.components.friendlyDate
import java.time.LocalDate

@Composable
fun ShiftsScreen(viewModel: OrganizaViewModel, contentPadding: PaddingValues) {
    val data by viewModel.data.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    var showImport by remember { mutableStateOf(false) }
    if (showAdd) AddShiftDialog({ showAdd = false }, viewModel::addShift)
    if (showImport) ImportShiftPatternDialog({ showImport = false }, viewModel::importShiftPattern)
    val shifts = data.shifts.filter { it.localDate() >= LocalDate.now().minusDays(1) }.sortedWith(compareBy({ it.localDate() }, { it.startTime }))

    LazyColumn(modifier = Modifier.fillMaxSize().padding(contentPadding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column { Text("Turnos", style = MaterialTheme.typography.headlineMedium); Text("Os turnos condicionam carga, janelas livres e recuperação.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                FilledIconButton(onClick = { showAdd = true }) { Icon(Icons.Rounded.Add, "Adicionar") }
            }
        }
        item {
            OutlinedButton(onClick = { showImport = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Rounded.FileDownload, null); Spacer(Modifier.width(8.dp)); Text("Importar padrão de escala")
            }
        }
        if (shifts.isEmpty()) item { Card { Text("Ainda não tens turnos registados.", modifier = Modifier.padding(18.dp)) } }
        items(shifts, key = { it.id }) { shift ->
            Card {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("${friendlyDate(shift.localDate())} · ${shift.type.label}", style = MaterialTheme.typography.titleSmall)
                        Text("${shift.startTime}–${shift.endTime}${shift.note.takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""}", style = MaterialTheme.typography.bodySmall)
                    }
                    IconButton(onClick = { viewModel.deleteShift(shift.id) }) { Text("×") }
                }
            }
        }
    }
}
