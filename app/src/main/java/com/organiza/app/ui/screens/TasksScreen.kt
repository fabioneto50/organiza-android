package com.organiza.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.organiza.app.OrganizaViewModel
import com.organiza.app.ui.components.AddTaskDialog

@Composable
fun TasksScreen(viewModel: OrganizaViewModel, contentPadding: PaddingValues) {
    val data by viewModel.data.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    var showDone by remember { mutableStateOf(false) }
    if (showAdd) AddTaskDialog({ showAdd = false }, viewModel::addTask)
    val tasks = data.tasks.filter { showDone || !it.completed }.sortedWith(compareBy({ it.completed }, { -it.priority.weight }))

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(contentPadding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column { Text("Tarefas", style = MaterialTheme.typography.headlineMedium); Text("Duração, prazo, energia e prioridade alimentam o planeamento.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                FilledIconButton(onClick = { showAdd = true }) { Icon(Icons.Rounded.Add, "Nova tarefa") }
            }
        }
        item { Row(verticalAlignment = Alignment.CenterVertically) { Switch(showDone, { showDone = it }); Spacer(Modifier.width(8.dp)); Text("Mostrar concluídas") } }
        if (tasks.isEmpty()) item { Card { Text("Sem tarefas nesta vista.", modifier = Modifier.padding(18.dp)) } }
        items(tasks, key = { it.id }) { task ->
            Card {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = task.completed, onCheckedChange = { viewModel.toggleTask(task.id) })
                    Column(Modifier.weight(1f)) {
                        Text(task.title, style = MaterialTheme.typography.titleSmall)
                        Text("${task.durationMinutes} min · ${task.priority.label} · energia ${task.energyDemand.label.lowercase()}${task.dueDate?.let { " · prazo $it" } ?: ""}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { viewModel.deleteTask(task.id) }) { Text("×") }
                }
            }
        }
    }
}
