package com.organiza.app.ui.components

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.organiza.app.model.Shift
import com.organiza.app.model.ShiftTemplate
import com.organiza.app.model.ShiftType

fun shiftColor(hex: String): Color = runCatching { Color(AndroidColor.parseColor(hex)) }
    .getOrDefault(Color(0xFF5147FF))

fun templateForShift(shift: Shift, templates: List<ShiftTemplate>): ShiftTemplate? {
    return shift.templateId?.let { id -> templates.firstOrNull { it.id == id } }
        ?: templates.firstOrNull {
            it.type == shift.type && it.startTime == shift.startTime && it.endTime == shift.endTime
        }
}

fun shiftDisplayCode(shift: Shift, templates: List<ShiftTemplate>): String {
    return templateForShift(shift, templates)?.code ?: when (shift.type) {
        ShiftType.DIA -> "M"
        ShiftType.TARDE -> "T"
        ShiftType.NOITE -> "N"
        ShiftType.LONGO -> "12H"
        ShiftType.FOLGA -> "Folga"
        ShiftType.FERIAS -> "Férias"
        ShiftType.OUTRO -> "Turno"
    }
}

@Composable
fun ShiftPill(
    shift: Shift,
    templates: List<ShiftTemplate>,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val template = templateForShift(shift, templates)
    val color = template?.let { shiftColor(it.colorHex) } ?: MaterialTheme.colorScheme.primary
    Surface(
        modifier = modifier,
        color = color,
        contentColor = Color.White,
        shape = RoundedCornerShape(50)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(horizontal = if (compact) 5.dp else 9.dp, vertical = if (compact) 3.dp else 5.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                shiftDisplayCode(shift, templates),
                style = if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
