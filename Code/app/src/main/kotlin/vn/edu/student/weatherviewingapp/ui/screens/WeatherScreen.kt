package vn.edu.student.weatherviewingapp.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.delay
import vn.edu.student.weatherviewingapp.ui.WeatherUiState
import vn.edu.student.weatherviewingapp.viewmodel.WeatherViewModel
import vn.edu.student.weatherviewingapp.data.ForecastItem
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherScreen(
    viewModel: WeatherViewModel = viewModel()
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var cityInput by rememberSaveable { mutableStateOf("") }
    var showSearch by remember { mutableStateOf(false) }
    val uiState by viewModel.uiState.collectAsState()
    val suggestions by viewModel.suggestions.collectAsState()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(showSearch) {
        if (showSearch) {
            delay(150)
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        ) {
            getCurrentLocation(context) { lat, lon ->
                viewModel.fetchWeatherByCoords(lat, lon)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF2196F3), Color(0xFF64B5F6), Color(0xFFBBDEFB))
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Custom Header
            Surface(
                color = if (showSearch) Color.Black.copy(alpha = 0.7f) else Color.Transparent,
                modifier = Modifier.fillMaxWidth().statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (showSearch) {
                        IconButton(onClick = { 
                            showSearch = false
                            cityInput = ""
                            viewModel.clearSuggestions()
                            keyboardController?.hide()
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                        TextField(
                            value = cityInput,
                            onValueChange = { 
                                cityInput = it
                                viewModel.searchLocations(it)
                            },
                            placeholder = { Text("Tìm quận, huyện, tỉnh...", color = Color.White.copy(alpha = 0.6f)) },
                            modifier = Modifier.weight(1f).focusRequester(focusRequester),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                cursorColor = Color.White,
                                focusedTextColor = Color.White
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = {
                                if (suggestions.isNotEmpty()) {
                                    val first = suggestions.first()
                                    viewModel.fetchWeatherByCoords(first.lat, first.lon, first.localNames?.get("vi") ?: first.name)
                                    showSearch = false
                                    viewModel.clearSuggestions()
                                    cityInput = ""
                                    keyboardController?.hide()
                                } else if (cityInput.isNotBlank()) {
                                    viewModel.fetchWeather(cityInput)
                                    showSearch = false
                                    cityInput = ""
                                    keyboardController?.hide()
                                }
                            }),
                            singleLine = true
                        )
                        if (cityInput.isNotEmpty()) {
                            IconButton(onClick = { 
                                cityInput = ""
                                viewModel.clearSuggestions()
                            }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.White)
                            }
                        }
                    } else {
                        IconButton(onClick = { 
                            if (hasLocationPermission(context)) {
                                getCurrentLocation(context) { lat, lon ->
                                    viewModel.fetchWeatherByCoords(lat, lon)
                                }
                            } else {
                                locationPermissionLauncher.launch(
                                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                                )
                            }
                        }) {
                            Icon(Icons.Default.MyLocation, contentDescription = "Vị trí của tôi", tint = Color.White)
                        }
                        
                        val title = when (val state = uiState) {
                            is WeatherUiState.Success -> state.weather.cityName
                            else -> "Thời tiết"
                        }
                        Text(
                            text = title,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )

                        IconButton(onClick = { showSearch = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Tìm kiếm", tint = Color.White)
                        }
                    }
                }
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                // Main Weather Content
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp).verticalScroll(scrollState),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when (val state = uiState) {
                        is WeatherUiState.Initial -> {
                            Spacer(modifier = Modifier.height(100.dp))
                            Text("Tìm kiếm quận/huyện hoặc dùng GPS.", color = Color.White, fontSize = 18.sp)
                        }
                        is WeatherUiState.Loading -> {
                            Spacer(modifier = Modifier.height(100.dp))
                            CircularProgressIndicator(color = Color.White)
                        }
                        is WeatherUiState.Success -> {
                            WeatherContent(state)
                        }
                        is WeatherUiState.Error -> {
                            Spacer(modifier = Modifier.height(100.dp))
                            Text(
                                text = state.message,
                                color = Color.White,
                                modifier = Modifier.padding(16.dp).background(Color.Red.copy(alpha = 0.5f), RoundedCornerShape(8.dp)).padding(8.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Suggestions Overlay
                if (showSearch) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.85f),
                        modifier = Modifier.fillMaxSize().zIndex(10f)
                    ) {
                        if (suggestions.isNotEmpty()) {
                            LazyColumn(modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
                                items(suggestions) { loc ->
                                    val nameVi = loc.localNames?.get("vi") ?: loc.name
                                    val stateInfo = if (loc.state != null) ", ${loc.state}" else ""
                                    
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                viewModel.fetchWeatherByCoords(loc.lat, loc.lon, nameVi)
                                                showSearch = false
                                                viewModel.clearSuggestions()
                                                cityInput = ""
                                                keyboardController?.hide()
                                            }
                                            .padding(16.dp)
                                    ) {
                                        Text(text = "$nameVi$stateInfo", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                                        Text(text = "Việt Nam", color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp)
                                    }
                                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                                }
                            }
                        } else if (cityInput.length >= 2) {
                            Box(modifier = Modifier.fillMaxSize().padding(top = 40.dp), contentAlignment = Alignment.TopCenter) {
                                Text("Đang tìm kiếm gợi ý cho '$cityInput'...", color = Color.White.copy(alpha = 0.7f))
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun hasLocationPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
           ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
}

private fun getCurrentLocation(context: Context, onLocationFound: (Double, Double) -> Unit) {
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    try {
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                onLocationFound(location.latitude, location.longitude)
            }
        }
    } catch (e: SecurityException) { }
}

@Composable
fun WeatherContent(state: WeatherUiState.Success) {
    val weather = state.weather
    val main = weather.main
    
    Spacer(modifier = Modifier.height(20.dp))

    Text(
        text = "${main.temp.toInt()}°",
        fontSize = 120.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White
    )

    Text(
        text = "${weather.weather.firstOrNull()?.main} ${main.tempMax.toInt()}° / ${main.tempMin.toInt()}°",
        fontSize = 20.sp,
        color = Color.White,
        fontWeight = FontWeight.Medium
    )

    Spacer(modifier = Modifier.height(10.dp))

    Surface(
        color = Color.White.copy(alpha = 0.3f),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val aqi = state.airPollution.list.firstOrNull()?.main?.aqi ?: 0
            val aqiText = when(aqi) {
                1 -> "Tốt"
                2 -> "Khá"
                3 -> "Trung bình"
                4 -> "Kém"
                5 -> "Rất kém"
                else -> "Không rõ"
            }
            Icon(Icons.Default.Cloud, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = "AQI $aqi - $aqiText", color = Color.White, fontSize = 14.sp)
        }
    }

    Spacer(modifier = Modifier.height(30.dp))

    GlassCard {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Dự báo 3 ngày", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
                }
                Text("Chi tiết \u25B6", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            val dailyForecast = state.forecast.list.filterIndexed { index, _ -> index % 8 == 0 }.take(3)
            dailyForecast.forEachIndexed { index, item ->
                ForecastRow(item, if(index == 0) "Hôm nay" else if(index == 1) "Ngày mai" else getDayNameVi(item.dt))
                if (index < dailyForecast.size - 1) Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    Spacer(modifier = Modifier.height(20.dp))

    GlassCard {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                StatItem(Modifier.weight(1f), "Độ ẩm", "${main.humidity}%")
                StatItem(Modifier.weight(1f), "Cảm giác", "${main.feelsLike.toInt()}°")
            }
            HorizontalDivider(color = Color.White.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 12.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                StatItem(Modifier.weight(1f), "Thấp nhất", "${main.tempMin.toInt()}°")
                StatItem(Modifier.weight(1f), "Áp suất", "${main.pressure} mbar")
            }
            HorizontalDivider(color = Color.White.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 12.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                StatItem(Modifier.weight(1f), "Cao nhất", "${main.tempMax.toInt()}°")
                StatItem(Modifier.weight(1f), "Tốc độ gió", "${weather.wind.speed.toInt()}km/h")
            }
        }
    }
    
    Spacer(modifier = Modifier.height(40.dp))
}

@Composable
fun ForecastRow(item: ForecastItem, dayLabel: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = "https://openweathermap.org/img/wn/${item.weather.firstOrNull()?.icon}@2x.png",
            contentDescription = null,
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = dayLabel,
            modifier = Modifier.weight(1.2f),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
        Text(
            text = item.weather.firstOrNull()?.main ?: "",
            modifier = Modifier.weight(1f),
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 16.sp,
            fontStyle = FontStyle.Italic
        )
        Text(
            text = "${item.main.tempMax.toInt()}° / ${item.main.tempMin.toInt()}°",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End
        )
    }
}

@Composable
fun StatItem(modifier: Modifier, label: String, value: String) {
    Row(
        modifier = modifier.padding(end = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
        Text(text = value, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun GlassCard(content: @Composable () -> Unit) {
    Surface(
        color = Color.White.copy(alpha = 0.15f),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        content()
    }
}

fun getDayNameVi(timestamp: Long): String {
    val date = Date(timestamp * 1000)
    val localeVi = Locale.forLanguageTag("vi-VN")
    val sdf = SimpleDateFormat("EEEE", localeVi)
    return sdf.format(date).replaceFirstChar { it.uppercase() }
}
