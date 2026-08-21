package com.organiza.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.organiza.app.model.PlanBlock
import com.organiza.app.model.PlanBlockType
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun SectionTitle(title: String, action: String? = null) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        action?.let { Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary) }
    }
}

@Composable
fun EnergyIndicator(value: Int, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            (1..5).forEach { level ->
                Surface(
                    color = if (level <= value) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.extraLarge,
                    modifier = Modifier.size(width = 34.dp, height = 8.dp)
                ) {}
            }
        }
        Text(
            text = when (value) {
                1 -> "Energia muito baixa"
                2 -> "Energia baixa"
                3 -> "Energia moderada"
                4 -> "Energia boa"
                else -> "Energia alta"
            },
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
fun PlanBlockRow(block: PlanBlock, onCalendar: (() -> Unit)? = null) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
        Column(modifier = Modifier.width(54.dp)) {
            Text(block.startTime, style = MaterialTheme.typography.labelLarge)
            Text(block.endTime, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Surface(
            modifier = Modifier.weight(1f), tonalElevation = if (block.type in listOf(PlanBlockType.TURNO, PlanBlockType.COMPROMISSO)) 3.dp else 1.dp,
            shape = MaterialTheme.shapes.medium,
            color = when (block.type) {
                PlanBlockType.TURNO -> MaterialTheme.colorScheme.secondaryContainer
                PlanBlockType.COMPROMISSO -> MaterialTheme.colorScheme.errorContainer
                PlanBlockType.RECUPERACAO -> MaterialTheme.colorScheme.tertiaryContainer
                PlanBlockType.TAREFA -> MaterialTheme.colorScheme.primaryContainer
                PlanBlockType.HABITO -> MaterialTheme.colorScheme.surfaceVariant
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(block.title, style = MaterialTheme.typography.titleSmall)
                    if (block.subtitle.isNotBlank()) Text(block.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (onCalendar != null) IconButton(onClick = onCalendar) { Icon(Icons.Rounded.Event, contentDescription = "Adicionar ao calendário") }
            }
        }
    }
}

@Composable
fun MetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(value, style = MaterialTheme.typography.titleLarge)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

fun friendlyDate(date: LocalDate): String {
    val today = LocalDate.now()
    return when (date) {
        today -> "Hoje"
        today.plusDays(1) -> "Amanhã"
        else -> date.format(DateTimeFormatter.ofPattern("EEEE, d MMM", Locale("pt", "PT"))).replaceFirstChar { it.uppercase() }
    }
}

fun shortDay(date: LocalDate): String = date.format(DateTimeFormatter.ofPattern("EEE", Locale("pt", "PT"))).replace(".", "").replaceFirstChar { it.uppercase() }
