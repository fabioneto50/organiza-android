package com.organiza.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.organiza.app.model.ShiftTemplate
import com.organiza.app.model.ShiftType
import java.time.LocalDate
import java.time.LocalTime

private val shiftPalette = listOf(
    "#5147FF", "#28004F", "#FF1DAE", "#31D5B4", "#6B6B6B",
    "#FF3434", "#E79BAA", "#F59E0B", "#0EA5E9", "#16A34A"
)

@Composable
fun QuickAssignShiftDialog(
    date: LocalDate,
    templates: List<ShiftTemplate>,
    onDismiss: () -> Unit,
    onAssign: (String) -> Unit,
    onRemove: () -> Unit,
    onCreateTemplate: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Turno · ${date.dayOfMonth}/${date.monthValue}") },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 430.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text(
                        "Escolhe um tipo guardado. O turno desse dia é atualizado imediatamente.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                items(templates, key = { it.id }) { template ->
                    Button(
                        onClick = { onAssign(template.id); onDismiss() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = shiftColor(template.colorHex),
                            contentColor = Color.White
                        )
                    ) {
                        Text(template.code, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(10.dp))
                        Text(template.name, modifier = Modifier.weight(1f))
                        if (template.type !in listOf(ShiftType.FOLGA, ShiftType.FERIAS)) {
                            Text("${template.startTime}–${template.endTime}", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                item {
                    OutlinedButton(onClick = onCreateTemplate, modifier = Modifier.fillMaxWidth()) {
                        Text("+ Criar tipo de turno")
                    }
                }
                item {
                    TextButton(onClick = { onRemove(); onDismiss() }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Rounded.DeleteOutline, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Remover turno deste dia")
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Fechar") } }
    )
}

@Composable
fun CreateShiftTemplateDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, ShiftType, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(ShiftType.OUTRO) }
    var start by remember { mutableStateOf("08:00") }
    var end by remember { mutableStateOf("16:00") }
    var colorHex by remember { mutableStateOf(shiftPalette.first()) }

    fun applyPreset(newType: ShiftType) {
        type = newType
        when (newType) {
            ShiftType.DIA -> { start = "08:00"; end = "16:00" }
            ShiftType.TARDE -> { start = "16:00"; end = "23:59" }
            ShiftType.NOITE -> { start = "20:00"; end = "08:00" }
            ShiftType.LONGO -> { start = "08:00"; end = "20:00" }
            ShiftType.FOLGA, ShiftType.FERIAS -> { start = "00:00"; end = "00:00" }
            ShiftType.OUTRO -> Unit
        }
    }

    val validTimes = type in listOf(ShiftType.FOLGA, ShiftType.FERIAS) || (validTime(start) && validTime(end))

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Novo tipo de turno") },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 520.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it.take(30) },
                        label = { Text("Nome · ex.: Dia 2") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it.take(10) },
                        label = { Text("Código no calendário · ex.: D2") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Text("Categoria", style = MaterialTheme.typography.labelLarge)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(ShiftType.entries) { item ->
                            FilterChip(
                                selected = type == item,
                                onClick = { applyPreset(item) },
                                label = { Text(item.label) }
                            )
                        }
                    }
                }
                if (type !in listOf(ShiftType.FOLGA, ShiftType.FERIAS)) {
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                start,
                                { start = normalizeShiftTime(it) },
                                label = { Text("Início") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                end,
                                { end = normalizeShiftTime(it) },
                                label = { Text("Fim") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                item {
                    Text("Cor", style = MaterialTheme.typography.labelLarge)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(shiftPalette) { hex ->
                            Surface(
                                modifier = Modifier.size(36.dp).clickable { colorHex = hex },
                                shape = CircleShape,
                                color = shiftColor(hex),
                                border = if (colorHex == hex) BorderStroke(3.dp, MaterialTheme.colorScheme.onSurface) else null
                            ) {}
                        }
                    }
                }
                item {
                    Surface(color = shiftColor(colorHex), contentColor = Color.White, shape = MaterialTheme.shapes.extraLarge) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(code.ifBlank { "D2" }, fontWeight = FontWeight.Bold)
                            Text(name.ifBlank { "Pré-visualização" })
                            if (type !in listOf(ShiftType.FOLGA, ShiftType.FERIAS)) Text("$start–$end", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = name.isNotBlank() && code.isNotBlank() && validTimes,
                onClick = {
                    onSave(name, code, type, start, end, colorHex)
                    onDismiss()
                }
            ) { Text("Guardar tipo") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

private fun normalizeShiftTime(raw: String): String = raw.filter { it.isDigit() || it == ':' }.take(5)
private fun validTime(value: String): Boolean = runCatching { LocalTime.parse(value) }.isSuccess
