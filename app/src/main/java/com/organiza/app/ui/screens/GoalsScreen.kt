package com.organiza.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.organiza.app.OrganizaViewModel
import com.organiza.app.ui.components.AddGoalDialog
import com.organiza.app.ui.components.AddHabitDialog
import java.time.LocalDate

@Composable
fun GoalsScreen(viewModel: OrganizaViewModel, contentPadding: PaddingValues) {
    val data by viewModel.data.collectAsState()
    var addGoal by remember { mutableStateOf(false) }
    var addHabit by remember { mutableStateOf(false) }
    if (addGoal) AddGoalDialog({ addGoal = false }, viewModel::addGoal)
    if (addHabit) AddHabitDialog({ addHabit = false }, viewModel::addHabit)
    val today = LocalDate.now()

    LazyColumn(modifier = Modifier.fillMaxSize().padding(contentPadding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Objetivos e hábitos", style = MaterialTheme.typography.headlineMedium) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { addGoal = true }, modifier = Modifier.weight(1f)) { Text("Novo objetivo") }
                OutlinedButton(onClick = { addHabit = true }, modifier = Modifier.weight(1f)) { Text("Novo hábito") }
            }
        }
        item { Text("Objetivos", style = MaterialTheme.typography.titleMedium) }
        if (data.goals.isEmpty()) item { Text("Sem objetivos registados.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        items(data.goals, key = { it.id }) { goal ->
            Card { Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Text(goal.title, style = MaterialTheme.typography.titleSmall); Text("${goal.category.label}${goal.targetDate?.let { " · até $it" } ?: ""}", style = MaterialTheme.typography.bodySmall) }
                    IconButton(onClick = { viewModel.deleteGoal(goal.id) }) { Text("×") }
                }
                LinearProgressIndicator(progress = { goal.progressPercent / 100f }, modifier = Modifier.fillMaxWidth())
                Slider(value = goal.progressPercent.toFloat(), onValueChange = { viewModel.setGoalProgress(goal.id, it.toInt()) }, valueRange = 0f..100f, steps = 9)
                Text("${goal.progressPercent}%", style = MaterialTheme.typography.labelMedium)
            } }
        }
        item { Text("Hábitos", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 6.dp)) }
        if (data.habits.isEmpty()) item { Text("Sem hábitos registados.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        items(data.habits, key = { it.id }) { habit ->
            val due = habit.isDue(today)
            val done = habit.isCompleted(today)
            Card { Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = done, onCheckedChange = { viewModel.toggleHabitToday(habit.id) }, enabled = due)
                Column(Modifier.weight(1f)) {
                    Text(habit.title, style = MaterialTheme.typography.titleSmall)
                    Text("${habit.durationMinutes} min · ${habit.preferredMoment.label}${if (!due) " · não previsto hoje" else ""}", style = MaterialTheme.typography.bodySmall)
                }
                IconButton(onClick = { viewModel.deleteHabit(habit.id) }) { Text("×") }
            } }
        }
    }
}
