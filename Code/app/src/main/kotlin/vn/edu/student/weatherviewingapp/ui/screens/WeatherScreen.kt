package vn.edu.student.weatherviewingapp.ui.screens

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import vn.edu.student.weatherviewingapp.utils.getCurrentLocation
import vn.edu.student.weatherviewingapp.utils.hasLocationPermission
import vn.edu.student.weatherviewingapp.utils.checkLocationSettingsAndGetLocation
import vn.edu.student.weatherviewingapp.data.getDailyForecastSummaries
import kotlinx.coroutines.delay
import vn.edu.student.weatherviewingapp.alerts.WeatherAlertSettingsStore
import vn.edu.student.weatherviewingapp.ui.components.WeatherAlertSettingsDialog
import vn.edu.student.weatherviewingapp.ui.WeatherUiState
import vn.edu.student.weatherviewingapp.ui.components.FavoriteLocationsSheet
import vn.edu.student.weatherviewingapp.ui.components.StatItem
import vn.edu.student.weatherviewingapp.ui.components.GlassCard
import vn.edu.student.weatherviewingapp.ui.components.ForecastRow
import vn.edu.student.weatherviewingapp.viewmodel.WeatherViewModel
import vn.edu.student.weatherviewingapp.ui.components.CacheFreshnessIndicator
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
    var showFavoritesSheet by rememberSaveable { mutableStateOf(false) }
    val alertSettingsStore = remember(context) { WeatherAlertSettingsStore(context) }
    var alertSettings by remember { mutableStateOf(alertSettingsStore.load()) }
    val uiState by viewModel.uiState.collectAsState()
    val suggestions by viewModel.suggestions.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val focusRequester = remember { FocusRequester() }
    var show5DaysForecast by remember { mutableStateOf(false) }
    var showComparePage by remember { mutableStateOf(false) }

    if (showComparePage) {
        CompareScreen(onBack = { showComparePage = false })
        return
    }

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

                        if (uiState is WeatherUiState.Success) {
                            val success = uiState as WeatherUiState.Success
                            val isFav = viewModel.isFavorite(success.weather.coord.lat, success.weather.coord.lon)
                            IconButton(onClick = {
                                viewModel.toggleFavorite(
                                    vn.edu.student.weatherviewingapp.data.LocationResult(
                                        name = success.weather.cityName,
                                        lat = success.weather.coord.lat,
                                        lon = success.weather.coord.lon,
                                        country = success.weather.sys.country ?: "VN",
                                        localNames = mapOf("vi" to success.weather.cityName)
                                    )
                                )
                            }) {
                                Icon(
                                    if (isFav) Icons.Default.Star else Icons.Default.StarOutline,
                                    contentDescription = "Yêu thích",
                                    tint = if (isFav) Color.Yellow else Color.White
                                )
                            }
                        }

                        IconButton(onClick = { showFavoritesSheet = true }) {
                            Icon(
                                Icons.Default.Bookmarks,
                                contentDescription = "Danh sách yêu thích",
                                tint = Color.White
                            )
                        }

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
                        IconButton(onClick = { showComparePage = true }) {
                            Icon(
                                Icons.Default.Compare,
                                contentDescription = "So sánh",
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
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            )
        }

        // BottomSheet Danh sách địa điểm yêu thích
        if (showFavoritesSheet) {
            FavoriteLocationsSheet(
                favorites = favorites,
                onSelect = { loc ->
                    viewModel.fetchWeatherByCoords(loc.lat, loc.lon, loc.localNames?.get("vi") ?: loc.name)
                    showFavoritesSheet = false
                },
                onRemove = { loc -> viewModel.toggleFavorite(loc) },
                onDismiss = { showFavoritesSheet = false }
            )
        }
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

