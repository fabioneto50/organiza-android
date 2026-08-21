package com.organiza.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.EditCalendar
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.organiza.app.OrganizaViewModel
import com.organiza.app.model.Shift
import com.organiza.app.model.ShiftTemplate
import com.organiza.app.model.ShiftType
import com.organiza.app.ui.components.*
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun ShiftsScreen(viewModel: OrganizaViewModel, contentPadding: PaddingValues) {
    val data by viewModel.data.collectAsState()
    var month by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var showQuickAssign by remember { mutableStateOf(false) }
    var showCreateTemplate by remember { mutableStateOf(false) }
    var showImport by remember { mutableStateOf(false) }

    if (showQuickAssign) {
        QuickAssignShiftDialog(
            date = selectedDate,
            templates = data.shiftTemplates,
            onDismiss = { showQuickAssign = false },
            onAssign = { viewModel.applyShiftTemplate(selectedDate, it) },
            onRemove = { viewModel.removeShiftsForDate(selectedDate) },
            onCreateTemplate = {
                showQuickAssign = false
                showCreateTemplate = true
            }
        )
    }
    if (showCreateTemplate) {
        CreateShiftTemplateDialog(
            onDismiss = { showCreateTemplate = false },
            onSave = viewModel::createShiftTemplate
        )
    }
    if (showImport) ImportShiftPatternDialog({ showImport = false }, viewModel::importShiftPattern)

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(contentPadding),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("A minha escala", style = MaterialTheme.typography.headlineMedium)
                    Text("Toca num dia para aplicar um turno.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                FilledIconButton(onClick = { showCreateTemplate = true }) { Icon(Icons.Rounded.Add, "Novo tipo") }
            }
        }

        item {
            Card {
                Column(Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    MonthHeader(
                        month = month,
                        onPrevious = {
                            month = month.minusMonths(1)
                            selectedDate = month.atDay(1)
                        },
                        onNext = {
                            month = month.plusMonths(1)
                            selectedDate = month.atDay(1)
                        },
                        onToday = {
                            month = YearMonth.now()
                            selectedDate = LocalDate.now()
                        }
                    )
                    WeekdayHeader()
                    MonthGrid(
                        month = month,
                        selectedDate = selectedDate,
                        shifts = data.shifts,
                        templates = data.shiftTemplates,
                        onSelect = { date ->
                            selectedDate = date
                            if (YearMonth.from(date) != month) month = YearMonth.from(date)
                            showQuickAssign = true
                        }
                    )
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Adicionar rapidamente", style = MaterialTheme.typography.titleMedium)
                        Text(
                            selectedDate.format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", Locale("pt", "PT"))).replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(onClick = { showQuickAssign = true }) { Text("Ver todos") }
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(data.shiftTemplates, key = { it.id }) { template ->
                        Button(
                            onClick = { viewModel.applyShiftTemplate(selectedDate, template.id) },
                            colors = ButtonDefaults.buttonColors(containerColor = shiftColor(template.colorHex), contentColor = Color.White),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(template.code, fontWeight = FontWeight.Bold)
                        }
                    }
                    item {
                        OutlinedButton(onClick = { showCreateTemplate = true }) { Text("+ Novo") }
                    }
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = { showImport = true }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.EditCalendar, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Padrão")
                }
                Button(onClick = { showQuickAssign = true }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.Add, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Adicionar turno")
                }
            }
        }

        item {
            val selectedShift = data.shifts.firstOrNull { it.date == selectedDate.toString() }
            SelectedDayCard(selectedDate, selectedShift, data.shiftTemplates) {
                viewModel.removeShiftsForDate(selectedDate)
            }
        }

        item { SectionTitle("Tipos de turno", "${data.shiftTemplates.size} guardados") }
        items(data.shiftTemplates, key = { it.id }) { template ->
            TemplateRow(template, onDelete = { viewModel.deleteShiftTemplate(template.id) })
        }
    }
}

@Composable
private fun MonthHeader(month: YearMonth, onPrevious: () -> Unit, onNext: () -> Unit, onToday: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            month.format(DateTimeFormatter.ofPattern("yyyy.MM")),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onToday) { Text("Hoje") }
        IconButton(onClick = onPrevious) { Icon(Icons.Rounded.ChevronLeft, "Mês anterior") }
        IconButton(onClick = onNext) { Icon(Icons.Rounded.ChevronRight, "Mês seguinte") }
    }
}

@Composable
private fun WeekdayHeader() {
    val labels = listOf("seg.", "ter.", "qua.", "qui.", "sex.", "sáb.", "dom.")
    Row(Modifier.fillMaxWidth()) {
        labels.forEachIndexed { index, label ->
            Text(
                label,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelMedium,
                color = if (index == 6) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MonthGrid(
    month: YearMonth,
    selectedDate: LocalDate,
    shifts: List<Shift>,
    templates: List<ShiftTemplate>,
    onSelect: (LocalDate) -> Unit
) {
    val first = month.atDay(1)
    val start = first.minusDays((first.dayOfWeek.value - 1).toLong())
    val dates = (0 until 42).map { start.plusDays(it.toLong()) }
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        dates.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                week.forEach { date ->
                    val shift = shifts.firstOrNull { it.date == date.toString() }
                    val isCurrentMonth = YearMonth.from(date) == month
                    val isToday = date == LocalDate.now()
                    val isSelected = date == selectedDate
                    val sunday = date.dayOfWeek.value == 7
                    Surface(
                        modifier = Modifier.weight(1f).padding(1.dp).height(76.dp).clickable { onSelect(date) },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                        border = if (isToday) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                date.dayOfMonth.toString(),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                color = when {
                                    !isCurrentMonth -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f)
                                    sunday -> MaterialTheme.colorScheme.error
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                            )
                            shift?.let {
                                ShiftPill(
                                    shift = it,
                                    templates = templates,
                                    compact = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectedDayCard(date: LocalDate, shift: Shift?, templates: List<ShiftTemplate>, onRemove: () -> Unit) {
    Card {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("Dia ${date.dayOfMonth}", style = MaterialTheme.typography.titleMedium)
                if (shift == null) {
                    Text("Sem turno definido", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    ShiftPill(shift, templates, modifier = Modifier.widthIn(max = 150.dp))
                    if (shift.type !in listOf(ShiftType.FOLGA, ShiftType.FERIAS)) {
                        Text("${shift.startTime}–${shift.endTime}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            if (shift != null) IconButton(onClick = onRemove) { Icon(Icons.Rounded.DeleteOutline, "Remover turno") }
        }
    }
}

@Composable
private fun TemplateRow(template: ShiftTemplate, onDelete: () -> Unit) {
    Card {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                color = shiftColor(template.colorHex),
                contentColor = Color.White,
                shape = RoundedCornerShape(50),
                modifier = Modifier.widthIn(min = 72.dp)
            ) {
                Text(template.code, modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp), fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(template.name, style = MaterialTheme.typography.titleSmall)
                Text(
                    if (template.type in listOf(ShiftType.FOLGA, ShiftType.FERIAS)) template.type.label else "${template.startTime}–${template.endTime} · ${template.type.label}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!template.system) IconButton(onClick = onDelete) { Icon(Icons.Rounded.DeleteOutline, "Apagar tipo") }
        }
    }
}
