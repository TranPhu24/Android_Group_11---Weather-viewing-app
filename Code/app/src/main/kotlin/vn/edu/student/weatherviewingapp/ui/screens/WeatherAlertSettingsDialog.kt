package vn.edu.student.weatherviewingapp.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import vn.edu.student.weatherviewingapp.alerts.WeatherAlertSettings

@Composable
fun WeatherAlertSettingsDialog(
    settings: WeatherAlertSettings,
    onDismiss: () -> Unit,
    onSave: (WeatherAlertSettings) -> Unit
) {
    var temperatureEnabled by remember(settings) { mutableStateOf(settings.temperatureEnabled) }
    var temperatureThreshold by remember(settings) { mutableStateOf(settings.temperatureThreshold.toString()) }
    var windEnabled by remember(settings) { mutableStateOf(settings.windEnabled) }
    var windThreshold by remember(settings) { mutableStateOf(settings.windThresholdKmh.toString()) }
    var conditionEnabled by remember(settings) { mutableStateOf(settings.conditionEnabled) }
    var alertRain by remember(settings) { mutableStateOf(settings.alertRain) }
    var alertThunderstorm by remember(settings) { mutableStateOf(settings.alertThunderstorm) }
    var alertSnow by remember(settings) { mutableStateOf(settings.alertSnow) }
    var alertExtreme by remember(settings) { mutableStateOf(settings.alertExtreme) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cảnh báo thời tiết") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                AlertSwitch("Nhiệt độ cao", temperatureEnabled) { temperatureEnabled = it }
                if (temperatureEnabled) {
                    OutlinedTextField(
                        value = temperatureThreshold,
                        onValueChange = { temperatureThreshold = it },
                        label = { Text("Ngưỡng nhiệt độ (°C)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                AlertSwitch("Gió mạnh", windEnabled) { windEnabled = it }
                if (windEnabled) {
                    OutlinedTextField(
                        value = windThreshold,
                        onValueChange = { windThreshold = it },
                        label = { Text("Ngưỡng tốc độ gió (km/h)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                AlertSwitch("Thời tiết xấu", conditionEnabled) { conditionEnabled = it }
                if (conditionEnabled) {
                    AlertCheckbox("Mưa / mưa phùn", alertRain) { alertRain = it }
                    AlertCheckbox("Giông bão", alertThunderstorm) { alertThunderstorm = it }
                    AlertCheckbox("Tuyết", alertSnow) { alertSnow = it }
                    AlertCheckbox("Thời tiết cực đoan", alertExtreme) { alertExtreme = it }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(
                    WeatherAlertSettings(
                        temperatureEnabled = temperatureEnabled,
                        temperatureThreshold = temperatureThreshold.toDoubleOrNull()?.coerceAtLeast(-100.0) ?: settings.temperatureThreshold,
                        windEnabled = windEnabled,
                        windThresholdKmh = windThreshold.toDoubleOrNull()?.coerceAtLeast(0.0) ?: settings.windThresholdKmh,
                        conditionEnabled = conditionEnabled,
                        alertRain = alertRain,
                        alertThunderstorm = alertThunderstorm,
                        alertSnow = alertSnow,
                        alertExtreme = alertExtreme
                    )
                )
            }) { Text("Lưu") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } }
    )
}

@Composable
private fun AlertSwitch(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun AlertCheckbox(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(label)
    }
}
