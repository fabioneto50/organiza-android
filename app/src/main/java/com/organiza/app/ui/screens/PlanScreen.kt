package com.organiza.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.organiza.app.OrganizaViewModel
import com.organiza.app.ui.components.PlanBlockRow
import com.organiza.app.ui.components.friendlyDate
import java.time.LocalDate

@Composable
fun PlanScreen(viewModel: OrganizaViewModel, contentPadding: PaddingValues) {
    val data by viewModel.data.collectAsState()
    val context = LocalContext.current
    val dates = (0..6).map { LocalDate.now().plusDays(it.toLong()) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(contentPadding),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Plano inteligente", style = MaterialTheme.typography.headlineMedium)
            Text("Compromissos e turnos ficam fixos; tarefas e hábitos são reorganizados à volta deles.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { viewModel.organizeLife() }, modifier = Modifier.weight(1f)) { Icon(Icons.Rounded.AutoAwesome, null); Spacer(Modifier.width(6.dp)); Text("Gerar 7 dias") }
                OutlinedButton(onClick = viewModel::reorganizeNow, modifier = Modifier.weight(1f)) { Icon(Icons.Rounded.Refresh, null); Spacer(Modifier.width(6.dp)); Text("Reorganizar") }
            }
        }
        dates.forEach { date ->
            val blocks = data.plan.filter { it.date == date.toString() }
            item(key = "h-$date") { Text(friendlyDate(date), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp)) }
            if (blocks.isEmpty()) item(key = "e-$date") { Text("Sem blocos planeados.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            else items(blocks, key = { it.id }) { block -> PlanBlockRow(block) { context.startActivity(viewModel.planCalendarIntent(block)) } }
        }
    }
}
