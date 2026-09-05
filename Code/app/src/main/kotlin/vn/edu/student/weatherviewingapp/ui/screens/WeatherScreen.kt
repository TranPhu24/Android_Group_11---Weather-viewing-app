package vn.edu.student.weatherviewingapp.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.delay
import vn.edu.student.weatherviewingapp.alerts.WeatherAlertSettingsStore
import vn.edu.student.weatherviewingapp.data.CacheFreshness
import vn.edu.student.weatherviewingapp.data.ForecastItem
import vn.edu.student.weatherviewingapp.data.WeatherCachePolicy
import vn.edu.student.weatherviewingapp.ui.WeatherUiState
import vn.edu.student.weatherviewingapp.viewmodel.WeatherViewModel
import java.text.SimpleDateFormat
import java.util.*
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherScreen(
    viewModel: WeatherViewModel = viewModel()
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var cityInput by rememberSaveable { mutableStateOf("") }
    var showSearch by remember { mutableStateOf(false) }
    var showAlertSettings by rememberSaveable { mutableStateOf(false) }
    val alertSettingsStore = remember(context) { WeatherAlertSettingsStore(context) }
    var alertSettings by remember { mutableStateOf(alertSettingsStore.load()) }
    val uiState by viewModel.uiState.collectAsState()
    val suggestions by viewModel.suggestions.collectAsState()
    val focusRequester = remember { FocusRequester() }
    var show5DaysForecast by remember { mutableStateOf(false) }

    LaunchedEffect(showSearch) {
        if (showSearch) {
            delay(150.milliseconds)
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    val settingResultRequest = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { activityResult ->
        if (activityResult.resultCode == Activity.RESULT_OK) {
            getCurrentLocation(context) { lat, lon ->
                viewModel.fetchWeatherByCoords(lat, lon)
            }
        } else {
            Toast.makeText(context, "Bạn cần bật GPS để sử dụng tính năng này.", Toast.LENGTH_SHORT).show()
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        ) {
            checkLocationSettingsAndGetLocation(context, settingResultRequest) { lat, lon ->
                viewModel.fetchWeatherByCoords(lat, lon)
            }
        } else {
            Toast.makeText(context, "Quyền vị trí bị từ chối.", Toast.LENGTH_SHORT).show()
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

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
            // Header
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
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                        TextField(
                            value = cityInput,
                            onValueChange = {
                                cityInput = it
                                viewModel.searchLocations(it)
                            },
                            placeholder = {
                                Text(
                                    "Tìm quận, huyện, tỉnh...",
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                            },
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
                                    viewModel.fetchWeatherByCoords(
                                        first.lat,
                                        first.lon,
                                        first.localNames?.get("vi") ?: first.name
                                    )
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
                                Icon(
                                    Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    tint = Color.White
                                )
                            }
                        }
                    } else {
                        IconButton(onClick = {
                            if (hasLocationPermission(context)) {
                                checkLocationSettingsAndGetLocation(
                                    context,
                                    settingResultRequest
                                ) { lat, lon ->
                                    viewModel.fetchWeatherByCoords(lat, lon)
                                }
                            } else {
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }
                        }) {
                            Icon(
                                Icons.Default.MyLocation,
                                contentDescription = "Vị trí của tôi",
                                tint = Color.White
                            )
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
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "Tìm kiếm",
                                tint = Color.White
                            )
                        }
                        IconButton(onClick = { showAlertSettings = true }) {
                            Icon(
                                Icons.Default.Notifications,
                                contentDescription = "Cảnh báo thời tiết",
                                tint = Color.White
                            )
                        }
                    }
                }
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                // Main Weather Content
                Column(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
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
                            WeatherContent(
                                state = state,
                                onOpen5DaysForecast = { show5DaysForecast = true }
                            )
                        }
                        is WeatherUiState.Error -> {
                            Spacer(modifier = Modifier.height(100.dp))
                            Text(
                                text = state.message,
                                color = Color.White,
                                modifier = Modifier
                                    .padding(16.dp)
                                    .background(Color.Red.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                    .padding(8.dp),
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
                                    val stateInfo = if (!loc.state.isNullOrEmpty() && loc.state != loc.name && loc.state != nameVi) {
                                        ", ${loc.state}"
                                    } else {
                                        ""
                                    }

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
                                        Text(
                                            text = "$nameVi$stateInfo",
                                            color = Color.White,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = "Việt Nam",
                                            color = Color.White.copy(alpha = 0.6f),
                                            fontSize = 14.sp
                                        )
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

        // Overlay Màn hình Dự báo 5 ngày (Full Screen)
        if (show5DaysForecast && uiState is WeatherUiState.Success) {
            val successState = uiState as WeatherUiState.Success
            Forecast5DaysScreen(
                forecastList = successState.forecast.list,
                currentWeather = successState.weather,
                cityName = successState.weather.cityName,
                onClose = { show5DaysForecast = false }
            )
        }

        // Dialog Cài đặt Cảnh báo thời tiết
        if (showAlertSettings) {
            WeatherAlertSettingsDialog(
                settings = alertSettings,
                onDismiss = { showAlertSettings = false },
                onSave = { updatedSettings ->
                    alertSettingsStore.save(updatedSettings)
                    alertSettings = updatedSettings
                    showAlertSettings = false
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                    ) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            )
        }
    }
}

private fun checkLocationSettingsAndGetLocation(
    context: Context,
    settingResultRequest: ActivityResultLauncher<IntentSenderRequest>,
    onLocationFound: (Double, Double) -> Unit
) {
    val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000).build()
    val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest).setAlwaysShow(true)
    val client = LocationServices.getSettingsClient(context)
    client.checkLocationSettings(builder.build()).addOnSuccessListener {
        getCurrentLocation(context, onLocationFound)
    }.addOnFailureListener { exception ->
        if (exception is ResolvableApiException) {
            try {
                val intentSenderRequest = IntentSenderRequest.Builder(exception.resolution).build()
                settingResultRequest.launch(intentSenderRequest)
            } catch (sendEx: Exception) {
                Toast.makeText(context, "Không thể mở cài đặt vị trí.", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Thiết bị không hỗ trợ dịch vụ vị trí.", Toast.LENGTH_SHORT).show()
        }
    }
}

private fun hasLocationPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
}

private fun getCurrentLocation(context: Context, onLocationFound: (Double, Double) -> Unit) {
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    Toast.makeText(context, "Đang lấy vị trí hiện tại...", Toast.LENGTH_SHORT).show()
    try {
        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            CancellationTokenSource().token
        ).addOnSuccessListener { location ->
            if (location != null) {
                onLocationFound(location.latitude, location.longitude)
            } else {
                Toast.makeText(context, "Không lấy được vị trí, thử lại sau.", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(context, "Lỗi kết nối dịch vụ định vị.", Toast.LENGTH_SHORT).show()
        }
    } catch (e: SecurityException) {
        Toast.makeText(context, "Chưa được cấp quyền truy cập Vị trí.", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun WeatherContent(
    state: WeatherUiState.Success,
    onOpen5DaysForecast: () -> Unit
) {
    val weather = state.weather
    val main = weather.main

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Khối giữa: Chỉ báo Cache, Nhiệt độ & AQI
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CacheFreshnessIndicator(state.refreshedAtMillis)

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "${main.temp.toInt()}°C",
                fontSize = 90.sp,
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
                    val aqiText = when (aqi) {
                        1 -> "Tốt"
                        2 -> "Khá"
                        3 -> "Trung bình"
                        4 -> "Kém"
                        5 -> "Rất kém"
                        else -> "Không rõ"
                    }
                    Icon(
                        Icons.Default.Cloud,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "AQI $aqi - $aqiText", color = Color.White, fontSize = 14.sp)
                }
            }
        }

        // Khối đáy: Box Dự báo 3 ngày & Chi tiết
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            GlassCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "Dự báo 5 ngày",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 14.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val dailySummaries = getDailyForecastSummaries(state.forecast.list).take(3)
                    dailySummaries.forEachIndexed { index, summary ->
                        if (index == 0) {
                            ForecastRow(
                                dayLabel = "Hôm nay",
                                weatherMain = state.weather.weather.firstOrNull()?.main ?: "",
                                icon = state.weather.weather.firstOrNull()?.icon ?: "01d",
                                tempMax = main.tempMax.toInt(),
                                tempMin = main.tempMin.toInt()
                            )
                        } else {
                            ForecastRow(
                                dayLabel = summary.dayLabel,
                                weatherMain = summary.weatherMain,
                                icon = summary.icon,
                                tempMax = summary.tempMax,
                                tempMin = summary.tempMin
                            )
                        }
                        if (index < dailySummaries.size - 1) Spacer(modifier = Modifier.height(12.dp))
                    }
                    HorizontalDivider(
                        color = Color.White.copy(alpha = 0.2f),
                        modifier = Modifier.padding(vertical = 10.dp)
                    )
                    Button(
                        onClick = { onOpen5DaysForecast() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Xem dự báo 5 ngày", color = Color.White, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                Icons.Default.ArrowForward,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            GlassCard {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        StatItem(Modifier.weight(1f), "Độ ẩm", "${main.humidity}%")
                        StatItem(Modifier.weight(1f), "Cảm giác", "${main.feelsLike.toInt()}°")
                    }
                    HorizontalDivider(
                        color = Color.White.copy(alpha = 0.2f),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    Row(modifier = Modifier.fillMaxWidth()) {
                        StatItem(Modifier.weight(1f), "Thấp nhất", "${main.tempMin.toInt()}°")
                        StatItem(Modifier.weight(1f), "Áp suất", "${main.pressure} mbar")
                    }
                    HorizontalDivider(
                        color = Color.White.copy(alpha = 0.2f),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    Row(modifier = Modifier.fillMaxWidth()) {
                        StatItem(Modifier.weight(1f), "Cao nhất", "${main.tempMax.toInt()}°")
                        StatItem(Modifier.weight(1f), "Tốc độ gió", "${weather.wind.speed.toInt()}km/h")
                    }
                }
            }
        }
    }
}

@Composable
private fun CacheFreshnessIndicator(refreshedAtMillis: Long) {
    var nowMillis by remember(refreshedAtMillis) { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(refreshedAtMillis) {
        while (true) {
            nowMillis = System.currentTimeMillis()
            delay(60_000)
        }
    }

    val cacheStatus = WeatherCachePolicy.getStatus(refreshedAtMillis, nowMillis)
    val isStale = cacheStatus.freshness == CacheFreshness.STALE
    val backgroundColor = if (isStale) Color(0xFFD84315).copy(alpha = 0.88f) else Color.White.copy(alpha = 0.22f)
    val message = if (isStale) {
        "Dữ liệu đã cũ • cập nhật ${formatCacheAge(cacheStatus.ageMillis)} trước"
    } else {
        "Dữ liệu mới • cập nhật ${formatCacheAge(cacheStatus.ageMillis)} trước"
    }

    Surface(color = backgroundColor, shape = RoundedCornerShape(16.dp)) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isStale) Icons.Default.Warning else Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = Color.White
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(message, color = Color.White, fontSize = 13.sp)
        }
    }
}

private fun formatCacheAge(ageMillis: Long): String {
    val minutes = ageMillis / 60_000
    return when {
        minutes < 1 -> "vừa xong"
        minutes < 60 -> "$minutes phút"
        else -> "${minutes / 60} giờ ${minutes % 60} phút"
    }
}

@Composable
fun ForecastRow(
    dayLabel: String,
    weatherMain: String,
    icon: String,
    tempMax: Int,
    tempMin: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = "https://openweathermap.org/img/wn/$icon@2x.png",
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
            text = weatherMain,
            modifier = Modifier.weight(1f),
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 16.sp,
            fontStyle = FontStyle.Italic
        )
        Text(
            text = "$tempMax° / $tempMin°",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End
        )
    }
}

@Composable
fun ForecastRow(item: ForecastItem, dayLabel: String) {
    ForecastRow(
        dayLabel = dayLabel,
        weatherMain = item.weather.firstOrNull()?.main ?: "",
        icon = item.weather.firstOrNull()?.icon ?: "01d",
        tempMax = item.main.tempMax.toInt(),
        tempMin = item.main.tempMin.toInt()
    )
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

data class DailyForecastSummary(
    val dayLabel: String,
    val weatherMain: String,
    val icon: String,
    val tempMax: Int,
    val tempMin: Int
)

fun getDailyForecastSummaries(forecastList: List<ForecastItem>): List<DailyForecastSummary> {
    val dayChunks = forecastList.chunked(8).take(5)

    return dayChunks.mapIndexed { index, itemsInDay ->
        val maxTemp = itemsInDay.maxOf { it.main.tempMax }.toInt()
        val minTemp = itemsInDay.minOf { it.main.tempMin }.toInt()
        val repItem = itemsInDay.getOrNull(4) ?: itemsInDay.first()

        val label = when (index) {
            0 -> "Hôm nay"
            1 -> "Ngày mai"
            else -> getDayNameVi(repItem.dt)
        }

        DailyForecastSummary(
            dayLabel = label,
            weatherMain = repItem.weather.firstOrNull()?.main ?: "",
            icon = repItem.weather.firstOrNull()?.icon ?: "01d",
            tempMax = maxTemp,
            tempMin = minTemp
        )
    }
}