package com.organiza.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import com.organiza.app.OrganizaViewModel
import com.organiza.app.ui.components.EnergyIndicator

@Composable
fun MoreScreen(
    viewModel: OrganizaViewModel,
    contentPadding: PaddingValues,
    openShifts: () -> Unit,
    openGoals: () -> Unit
) {
    val data by viewModel.data.collectAsState()
    val context = LocalContext.current
    var confirmClear by remember { mutableStateOf(false) }

    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        viewModel.setNotifications(granted)
    }
    val healthPermissionsLauncher = rememberLauncherForActivityResult(PermissionController.createRequestPermissionResultContract()) { granted ->
        if (granted.containsAll(viewModel.healthPermissions())) {
            viewModel.setUseSleepFromHealthConnect(true)
            viewModel.syncHealthConnect()
        } else {
            viewModel.setUseSleepFromHealthConnect(false)
        }
    }

    if (confirmClear) AlertDialog(
        onDismissRequest = { confirmClear = false },
        title = { Text("Apagar todos os dados?") },
        text = { Text("Remove tarefas, turnos, compromissos, objetivos, hábitos e o plano guardado neste dispositivo.") },
        confirmButton = { Button(onClick = { viewModel.clearAll(); confirmClear = false }) { Text("Apagar") } },
        dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancelar") } }
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(contentPadding),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { Text("Mais", style = MaterialTheme.typography.headlineMedium) }

        item {
            Card {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Áreas", style = MaterialTheme.typography.titleMedium)
                    OutlinedButton(onClick = openShifts, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Rounded.WorkHistory, null); Spacer(Modifier.width(8.dp)); Text("Turnos e escala") }
                    OutlinedButton(onClick = openGoals, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Rounded.TrackChanges, null); Spacer(Modifier.width(8.dp)); Text("Objetivos e hábitos") }
                }
            }
        }

        item {
            Card {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Energia e recuperação", style = MaterialTheme.typography.titleMedium)
                    EnergyIndicator(data.preferences.currentEnergy)
                    Text("Nível manual", style = MaterialTheme.typography.labelMedium)
                    Slider(value = data.preferences.currentEnergy.toFloat(), onValueChange = { viewModel.setEnergy(it.toInt()) }, valueRange = 1f..5f, steps = 3)
                    SettingSwitch("Proteger sono após turno noturno", data.preferences.protectSleepAfterNight, viewModel::setProtectSleep)
                }
            }
        }

        item {
            Card {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Planeamento", style = MaterialTheme.typography.titleMedium)
                    SettingSwitch("Reorganização automática", data.preferences.autoReplan, viewModel::setAutoReplan)
                    Text("Quando está ativa, alterações a tarefas, turnos, compromissos e hábitos voltam a calcular os próximos dias.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedButton(onClick = viewModel::reorganizeNow, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Rounded.Refresh, null); Spacer(Modifier.width(8.dp)); Text("Reorganizar agora") }
                }
            }
        }

        item {
            Card {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Notificações", style = MaterialTheme.typography.titleMedium)
                    SettingSwitch("Lembretes do plano", data.preferences.notificationsEnabled) { enabled ->
                        if (!enabled) viewModel.setNotifications(false)
                        else if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else viewModel.setNotifications(true)
                    }
                    Text("Antecedência", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(10, 20, 30, 60).forEach { value ->
                            FilterChip(selected = data.preferences.reminderMinutes == value, onClick = { viewModel.setReminderMinutes(value) }, label = { Text("$value min") })
                        }
                    }
                    Text("Os lembretes usam o WorkManager do Android; podem sofrer pequenos atrasos por otimizações de bateria.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        item {
            Card {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Health Connect", style = MaterialTheme.typography.titleMedium)
                    val availability = viewModel.healthAvailability()
                    val status = when (availability) {
                        HealthConnectClient.SDK_AVAILABLE -> "Disponível"
                        HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> "Necessita de instalação/atualização"
                        else -> "Não disponível neste dispositivo"
                    }
                    Text(status, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    data.health.lastSleepHours?.let { Text("Última sessão de sono: ${"%.1f".format(it)} h") }
                    SettingSwitch("Usar sono para ajustar energia", data.preferences.useSleepFromHealthConnect) { enabled ->
                        if (!enabled) viewModel.setUseSleepFromHealthConnect(false)
                        else if (availability == HealthConnectClient.SDK_AVAILABLE) healthPermissionsLauncher.launch(viewModel.healthPermissions())
                    }
                    if (availability == HealthConnectClient.SDK_AVAILABLE) {
                        OutlinedButton(onClick = viewModel::syncHealthConnect, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Rounded.Sync, null); Spacer(Modifier.width(8.dp)); Text("Atualizar dados de sono") }
                    }
                    Text("Nesta versão é lida apenas a duração da sessão de sono. O valor fica guardado localmente e serve para limitar a carga planeada.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        item {
            Card {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Integrações", style = MaterialTheme.typography.titleMedium)
                    Text("Google Calendar: a app importa eventos sincronizados no calendário Android e pode abrir eventos já preenchidos para guardar no calendário.")
                    Text("IA: a reorganização inteligente funciona localmente. A arquitetura deixa a camada de planeamento separada para, numa versão futura, ligar um modelo cloud sem substituir a interface ou os dados.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        item {
            OutlinedButton(onClick = { confirmClear = true }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                Icon(Icons.Rounded.DeleteForever, null); Spacer(Modifier.width(8.dp)); Text("Apagar dados locais")
            }
        }
        item { Spacer(Modifier.height(12.dp)) }
    }
}

@Composable
private fun SettingSwitch(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}
