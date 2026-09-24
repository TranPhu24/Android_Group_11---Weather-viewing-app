package vn.edu.student.weatherviewingapp.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import androidx.compose.ui.text.style.TextOverflow
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
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherScreen(
    viewModel: WeatherViewModel = viewModel()
) {
    val context = LocalContext.current

    // Mục 10 - Theo dõi trạng thái kết nối mạng.
    val isOffline = rememberIsOffline()

    // Mục 8 - Lưu và khôi phục đơn vị nhiệt độ người dùng đã chọn.
    val unitPreferences = remember(context) {
        context.getSharedPreferences(
            "unit_settings",
            Context.MODE_PRIVATE
        )
    }

    var temperatureUnit by rememberSaveable {
        mutableStateOf(
            unitPreferences.getString(
                "temperature_unit",
                "C"
            ) ?: "C"
        )
    }

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

    var showPermissionDialog by remember { mutableStateOf(false) }

    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text("Yêu cầu quyền vị trí") },
            text = { Text("Ứng dụng cần quyền vị trí để tải thời tiết ngay tại nơi bạn đang đứng. Vui lòng cấp quyền trong phần Cài đặt của máy.") },
            confirmButton = {
                TextButton(onClick = {
                    showPermissionDialog = false
                    vn.edu.student.weatherviewingapp.utils.openAppSettings(context)
                }) {
                    Text("Mở Cài đặt")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text("Hủy")
                }
            }
        )
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
            showPermissionDialog = true
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
                        Box(
                            modifier = Modifier.fillMaxWidth().height(64.dp)
                        ) {
                            // Bên trái
                            IconButton(
                                onClick = { showSearch = true },
                                modifier = Modifier.align(Alignment.CenterStart)
                            ) {
                                Icon(Icons.Default.Search, contentDescription = "Tìm kiếm", tint = Color.White)
                            }

                            // Ở giữa - Căn giữa tuyệt đối
                            val title = when (val state = uiState) {
                                is WeatherUiState.Success -> state.weather.cityName
                                else -> "Thời tiết"
                            }
                            Text(
                                text = title,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                modifier = Modifier.align(Alignment.Center).padding(horizontal = 48.dp),
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            // Bên phải
                            Row(
                                modifier = Modifier.align(Alignment.CenterEnd),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (uiState is WeatherUiState.Success) {
                                    val success = uiState as WeatherUiState.Success
                                    val isFav = favorites.any { it.lat == success.weather.coord.lat && it.lon == success.weather.coord.lon }
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

                                var menuExpanded by remember { mutableStateOf(false) }
                                Box {
                                    IconButton(onClick = { menuExpanded = true }) {
                                        Icon(Icons.Default.MoreVert, contentDescription = "Thêm", tint = Color.White)
                                    }
                                    DropdownMenu(
                                        expanded = menuExpanded,
                                        onDismissRequest = { menuExpanded = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Địa điểm đã lưu") },
                                            onClick = { showFavoritesSheet = true; menuExpanded = false },
                                            leadingIcon = { Icon(Icons.Default.Bookmarks, contentDescription = null) }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("So sánh thời tiết") },
                                            onClick = { showComparePage = true; menuExpanded = false },
                                            leadingIcon = { Icon(Icons.Default.Compare, contentDescription = null) }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Cảnh báo thời tiết") },
                                            onClick = { showAlertSettings = true; menuExpanded = false },
                                            leadingIcon = { Icon(Icons.Default.Notifications, contentDescription = null) }
                                        )
                                        HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 4.dp))
                                        DropdownMenuItem(
                                            text = { Text("Đổi đơn vị: ${if (temperatureUnit == "C") "°F" else "°C"}") },
                                            onClick = {
                                                val newUnit = if (temperatureUnit == "C") "F" else "C"
                                                temperatureUnit = newUnit
                                                unitPreferences.edit().putString("temperature_unit", newUnit).apply()
                                                menuExpanded = false
                                            },
                                            leadingIcon = { Icon(Icons.Default.Thermostat, contentDescription = null) }
                                        )
                                    }
                                }
                            }
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
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.5f),
                                modifier = Modifier.size(80.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Tìm kiếm thành phố\nhoặc nhấn biểu tượng Vị trí",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 18.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                        is WeatherUiState.Loading -> {
                            Spacer(modifier = Modifier.height(150.dp))
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(50.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Đang tải dữ liệu...",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 16.sp
                            )
                        }
                        is WeatherUiState.Success -> {
                            WeatherContent(
                                state = state,
                                temperatureUnit = temperatureUnit,
                                isOffline = isOffline,
                                onUnitChange = { unit ->
                                    temperatureUnit = unit
                                    unitPreferences.edit()
                                        .putString("temperature_unit", unit)
                                        .apply()
                                },
                                onOpen5DaysForecast = { show5DaysForecast = true }
                            )
                        }
                        is WeatherUiState.Error -> {
                            Spacer(modifier = Modifier.height(100.dp))
                            Icon(
                                Icons.Default.ErrorOutline,
                                contentDescription = "Lỗi",
                                tint = Color(0xFFFF5252),
                                modifier = Modifier.size(80.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Úi, có lỗi xảy ra!",
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = state.message,
                                color = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.padding(horizontal = 24.dp),
                                textAlign = TextAlign.Center,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = { viewModel.retry() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Text("Thử lại", color = Color(0xFF1E88E5), fontWeight = FontWeight.Bold)
                            }
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
                            Box(modifier = Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) {
                                Text("Không tìm thấy kết quả phù hợp cho '$cityInput'", color = Color.White.copy(alpha = 0.7f), fontSize = 16.sp)
                            }
                        }
                    }
                }
            }
        }

        // GPS Floating Action Button
        FloatingActionButton(
            onClick = {
                if (hasLocationPermission(context)) {
                    checkLocationSettingsAndGetLocation(context, settingResultRequest) { lat, lon ->
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
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 40.dp, end = 24.dp),
            containerColor = Color.White.copy(alpha = 0.95f),
            contentColor = Color(0xFF1E88E5),
            shape = androidx.compose.foundation.shape.CircleShape
        ) {
            Icon(Icons.Default.MyLocation, contentDescription = "Vị trí của tôi")
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
    temperatureUnit: String,
    isOffline: Boolean,
    onUnitChange: (String) -> Unit,
    onOpen5DaysForecast: () -> Unit
) {
    val weather = state.weather
    val main = weather.main

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isOffline) {
                Surface(
                    color = Color(0xFFFFF3CD).copy(alpha = 0.9f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.WifiOff,
                            contentDescription = null,
                            tint = Color(0xFF664D03),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Đang ngoại tuyến (Dữ liệu cũ)",
                            color = Color(0xFF664D03),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            CacheFreshnessIndicator(state.refreshedAtMillis)
        }

        // Khối giữa: Chỉ báo Cache, Nhiệt độ & AQI
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = formatTemperature(
                    main.temp,
                    temperatureUnit
                ),
                fontSize = 110.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "${weather.weather.firstOrNull()?.main} " +
                        "${formatTemperature(main.tempMax, temperatureUnit)} / " +
                        formatTemperature(main.tempMin, temperatureUnit),
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
                                tempMax = convertTemperature(main.tempMax, temperatureUnit).roundToInt(),
                                tempMin = convertTemperature(main.tempMin, temperatureUnit).roundToInt()
                            )
                        } else {
                            ForecastRow(
                                dayLabel = summary.dayLabel,
                                weatherMain = summary.weatherMain,
                                icon = summary.icon,
                                tempMax = convertTemperature(summary.tempMax.toDouble(), temperatureUnit).roundToInt(),
                                tempMin = convertTemperature(summary.tempMin.toDouble(), temperatureUnit).roundToInt()
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
                                Icons.AutoMirrored.Filled.ArrowForward,
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
                        StatItem(Modifier.weight(1f), "Cảm giác", formatTemperature(main.feelsLike, temperatureUnit))
                    }
                    HorizontalDivider(
                        color = Color.White.copy(alpha = 0.2f),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    Row(modifier = Modifier.fillMaxWidth()) {
                        StatItem(Modifier.weight(1f), "Thấp nhất", formatTemperature(main.tempMin, temperatureUnit))
                        StatItem(Modifier.weight(1f), "Áp suất", "${main.pressure} mbar")
                    }
                    HorizontalDivider(
                        color = Color.White.copy(alpha = 0.2f),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    Row(modifier = Modifier.fillMaxWidth()) {
                        StatItem(Modifier.weight(1f), "Cao nhất", formatTemperature(main.tempMax, temperatureUnit))
                        StatItem(Modifier.weight(1f), "Tốc độ gió", "${weather.wind.speed.toInt()}km/h")
                    }
                }
            }
        }
    }
}

// =====================================================
// MỤC 8 - UNIT SETTINGS
// =====================================================

private fun convertTemperature(
    celsius: Double,
    unit: String
): Double {
    return if (unit == "F") {
        celsius * 9.0 / 5.0 + 32.0
    } else {
        celsius
    }
}

private fun formatTemperature(
    celsius: Double,
    unit: String
): String {
    return "${convertTemperature(celsius, unit).roundToInt()}°$unit"
}

// =====================================================
// MỤC 10 - OFFLINE STATUS
// =====================================================

private fun hasInternet(
    manager: ConnectivityManager
): Boolean {
    val network = manager.activeNetwork ?: return false
    val capabilities =
        manager.getNetworkCapabilities(network) ?: return false

    return capabilities.hasCapability(
        NetworkCapabilities.NET_CAPABILITY_INTERNET
    ) && capabilities.hasCapability(
        NetworkCapabilities.NET_CAPABILITY_VALIDATED
    )
}

@Composable
private fun rememberIsOffline(): Boolean {
    val context = LocalContext.current

    val manager = remember(context) {
        context.getSystemService(
            Context.CONNECTIVITY_SERVICE
        ) as ConnectivityManager
    }

    var isOffline by remember {
        mutableStateOf(!hasInternet(manager))
    }

    DisposableEffect(manager) {
        val executor = ContextCompat.getMainExecutor(context)

        val callback =
            object : ConnectivityManager.NetworkCallback() {

                private fun updateStatus() {
                    executor.execute {
                        isOffline = !hasInternet(manager)
                    }
                }

                override fun onAvailable(network: Network) {
                    updateStatus()
                }

                override fun onLost(network: Network) {
                    updateStatus()
                }

                override fun onCapabilitiesChanged(
                    network: Network,
                    networkCapabilities: NetworkCapabilities
                ) {
                    updateStatus()
                }
            }

        manager.registerDefaultNetworkCallback(callback)
        isOffline = !hasInternet(manager)

        onDispose {
            runCatching {
                manager.unregisterNetworkCallback(callback)
            }
        }
    }

    return isOffline
}
