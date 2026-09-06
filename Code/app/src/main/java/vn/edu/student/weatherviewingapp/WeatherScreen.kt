package vn.edu.student.weatherviewingapp

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
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
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
import vn.edu.student.weatherviewingapp.data.ForecastItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherScreen(
    viewModel: WeatherViewModel = viewModel()
) {
    val context = LocalContext.current

    // Lưu lựa chọn đơn vị nhiệt độ của người dùng.
    val prefs = remember {
        context.getSharedPreferences(
            "unit_settings",
            Context.MODE_PRIVATE
        )
    }

    // Mặc định sử dụng độ C.
    var temperatureUnit by rememberSaveable {
        mutableStateOf(
            prefs.getString(
                "temperature_unit",
                "C"
            ) ?: "C"
        )
    }

    val keyboardController =
        LocalSoftwareKeyboardController.current

    var cityInput by rememberSaveable {
        mutableStateOf("")
    }

    var showSearch by remember {
        mutableStateOf(false)
    }

    val uiState by viewModel.uiState.collectAsState()

    val suggestions by
    viewModel.suggestions.collectAsState()

    val focusRequester = remember {
        FocusRequester()
    }

    LaunchedEffect(showSearch) {
        if (showSearch) {
            delay(150)

            focusRequester.requestFocus()

            keyboardController?.show()
        }
    }

    val locationPermissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->

            val fineLocationGranted =
                permissions[
                    Manifest.permission.ACCESS_FINE_LOCATION
                ] == true

            val coarseLocationGranted =
                permissions[
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ] == true

            if (
                fineLocationGranted ||
                coarseLocationGranted
            ) {
                getCurrentLocation(
                    context = context
                ) { lat, lon ->

                    viewModel.fetchWeatherByCoords(
                        lat = lat,
                        lon = lon
                    )
                }
            }
        }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF2196F3),
                        Color(0xFF64B5F6),
                        Color(0xFFBBDEFB)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {

            // ==========================
            // HEADER
            // ==========================

            Surface(
                color = if (showSearch) {
                    Color.Black.copy(alpha = 0.7f)
                } else {
                    Color.Transparent
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .padding(horizontal = 8.dp),
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    if (showSearch) {

                        // Nút quay lại
                        IconButton(
                            onClick = {
                                showSearch = false
                                cityInput = ""

                                viewModel
                                    .clearSuggestions()

                                keyboardController
                                    ?.hide()
                            }
                        ) {
                            Icon(
                                imageVector =
                                    Icons
                                        .AutoMirrored
                                        .Filled
                                        .ArrowBack,
                                contentDescription =
                                    "Quay lại",
                                tint = Color.White
                            )
                        }

                        // Ô tìm kiếm
                        TextField(
                            value = cityInput,
                            onValueChange = {
                                cityInput = it

                                viewModel
                                    .searchLocations(it)
                            },
                            placeholder = {
                                Text(
                                    text =
                                        "Tìm quận, huyện, tỉnh...",
                                    color =
                                        Color.White
                                            .copy(
                                                alpha = 0.6f
                                            )
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(
                                    focusRequester
                                ),
                            colors =
                                TextFieldDefaults.colors(
                                    focusedContainerColor =
                                        Color.Transparent,
                                    unfocusedContainerColor =
                                        Color.Transparent,
                                    focusedIndicatorColor =
                                        Color.Transparent,
                                    unfocusedIndicatorColor =
                                        Color.Transparent,
                                    cursorColor =
                                        Color.White,
                                    focusedTextColor =
                                        Color.White
                                ),
                            keyboardOptions =
                                KeyboardOptions(
                                    imeAction =
                                        ImeAction.Search
                                ),
                            keyboardActions =
                                KeyboardActions(
                                    onSearch = {

                                        if (
                                            suggestions
                                                .isNotEmpty()
                                        ) {
                                            val first =
                                                suggestions.first()

                                            val displayName =
                                                first.localNames
                                                    ?.get("vi")
                                                    ?: first.name

                                            viewModel
                                                .fetchWeatherByCoords(
                                                    lat =
                                                        first.lat,
                                                    lon =
                                                        first.lon,
                                                    name =
                                                        displayName
                                                )

                                            showSearch = false
                                            cityInput = ""

                                            viewModel
                                                .clearSuggestions()

                                            keyboardController
                                                ?.hide()

                                        } else if (
                                            cityInput.isNotBlank()
                                        ) {

                                            viewModel
                                                .fetchWeather(
                                                    cityInput
                                                )

                                            showSearch = false
                                            cityInput = ""

                                            keyboardController
                                                ?.hide()
                                        }
                                    }
                                ),
                            singleLine = true
                        )

                        if (cityInput.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    cityInput = ""

                                    viewModel
                                        .clearSuggestions()
                                }
                            ) {
                                Icon(
                                    imageVector =
                                        Icons.Default.Clear,
                                    contentDescription =
                                        "Xóa",
                                    tint =
                                        Color.White
                                )
                            }
                        }

                    } else {

                        // Nút GPS
                        IconButton(
                            onClick = {

                                if (
                                    hasLocationPermission(
                                        context
                                    )
                                ) {

                                    getCurrentLocation(
                                        context
                                    ) { lat, lon ->

                                        viewModel
                                            .fetchWeatherByCoords(
                                                lat,
                                                lon
                                            )
                                    }

                                } else {

                                    locationPermissionLauncher
                                        .launch(
                                            arrayOf(
                                                Manifest.permission
                                                    .ACCESS_FINE_LOCATION,
                                                Manifest.permission
                                                    .ACCESS_COARSE_LOCATION
                                            )
                                        )
                                }
                            }
                        ) {
                            Icon(
                                imageVector =
                                    Icons.Default.MyLocation,
                                contentDescription =
                                    "Vị trí của tôi",
                                tint = Color.White
                            )
                        }

                        // Tên địa điểm
                        val title =
                            when (
                                val state = uiState
                            ) {
                                is WeatherUiState.Success ->
                                    state.weather.cityName

                                else ->
                                    "Thời tiết"
                            }

                        Text(
                            text = title,
                            color = Color.White,
                            fontWeight =
                                FontWeight.Bold,
                            fontSize = 20.sp,
                            modifier =
                                Modifier.weight(1f),
                            textAlign =
                                TextAlign.Center
                        )

                        // Nút tìm kiếm
                        IconButton(
                            onClick = {
                                showSearch = true
                            }
                        ) {
                            Icon(
                                imageVector =
                                    Icons.Default.Search,
                                contentDescription =
                                    "Tìm kiếm",
                                tint = Color.White
                            )
                        }
                    }
                }
            }

            // ==========================
            // NỘI DUNG CHÍNH
            // ==========================

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {

                val scrollState =
                    rememberScrollState()

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            horizontal = 20.dp
                        )
                        .verticalScroll(
                            scrollState
                        ),
                    horizontalAlignment =
                        Alignment.CenterHorizontally
                ) {

                    when (
                        val state = uiState
                    ) {

                        is WeatherUiState.Initial -> {

                            Spacer(
                                modifier =
                                    Modifier.height(
                                        100.dp
                                    )
                            )

                            Text(
                                text =
                                    "Tìm kiếm quận/huyện hoặc dùng GPS.",
                                color =
                                    Color.White,
                                fontSize =
                                    18.sp
                            )
                        }

                        is WeatherUiState.Loading -> {

                            Spacer(
                                modifier =
                                    Modifier.height(
                                        100.dp
                                    )
                            )

                            CircularProgressIndicator(
                                color =
                                    Color.White
                            )
                        }

                        is WeatherUiState.Success -> {

                            WeatherContent(
                                state = state,
                                temperatureUnit =
                                    temperatureUnit,
                                onUnitChange = {
                                        unit ->

                                    temperatureUnit =
                                        unit

                                    // Ghi nhớ lựa chọn.
                                    prefs.edit()
                                        .putString(
                                            "temperature_unit",
                                            unit
                                        )
                                        .apply()
                                }
                            )
                        }

                        is WeatherUiState.Error -> {

                            Spacer(
                                modifier =
                                    Modifier.height(
                                        100.dp
                                    )
                            )

                            Text(
                                text =
                                    state.message,
                                color =
                                    Color.White,
                                modifier =
                                    Modifier
                                        .padding(
                                            16.dp
                                        )
                                        .background(
                                            Color.Red
                                                .copy(
                                                    alpha =
                                                        0.5f
                                                ),
                                            RoundedCornerShape(
                                                8.dp
                                            )
                                        )
                                        .padding(
                                            8.dp
                                        ),
                                textAlign =
                                    TextAlign.Center
                            )
                        }
                    }
                }

                // ==========================
                // GỢI Ý TÌM KIẾM
                // ==========================

                if (showSearch) {

                    Surface(
                        color =
                            Color.Black
                                .copy(
                                    alpha =
                                        0.85f
                                ),
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .zIndex(10f)
                    ) {

                        if (
                            suggestions
                                .isNotEmpty()
                        ) {

                            LazyColumn(
                                modifier =
                                    Modifier.padding(
                                        horizontal =
                                            20.dp,
                                        vertical =
                                            10.dp
                                    )
                            ) {

                                items(
                                    suggestions
                                ) { loc ->

                                    val nameVi =
                                        loc.localNames
                                            ?.get("vi")
                                            ?: loc.name

                                    val stateInfo =
                                        if (
                                            loc.state !=
                                            null
                                        ) {
                                            ", ${loc.state}"
                                        } else {
                                            ""
                                        }

                                    Column(
                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .clickable {

                                                    viewModel
                                                        .fetchWeatherByCoords(
                                                            lat =
                                                                loc.lat,
                                                            lon =
                                                                loc.lon,
                                                            name =
                                                                nameVi
                                                        )

                                                    showSearch =
                                                        false

                                                    cityInput =
                                                        ""

                                                    viewModel
                                                        .clearSuggestions()

                                                    keyboardController
                                                        ?.hide()
                                                }
                                                .padding(
                                                    16.dp
                                                )
                                    ) {

                                        Text(
                                            text =
                                                "$nameVi$stateInfo",
                                            color =
                                                Color.White,
                                            fontSize =
                                                18.sp,
                                            fontWeight =
                                                FontWeight.Medium
                                        )

                                        Text(
                                            text =
                                                "Việt Nam",
                                            color =
                                                Color.White
                                                    .copy(
                                                        alpha =
                                                            0.6f
                                                    ),
                                            fontSize =
                                                14.sp
                                        )
                                    }

                                    HorizontalDivider(
                                        color =
                                            Color.White
                                                .copy(
                                                    alpha =
                                                        0.1f
                                                )
                                    )
                                }
                            }

                        } else if (
                            cityInput.length >= 2
                        ) {

                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxSize()
                                        .padding(
                                            top =
                                                40.dp
                                        ),
                                contentAlignment =
                                    Alignment.TopCenter
                            ) {

                                Text(
                                    text =
                                        "Đang tìm kiếm gợi ý cho '$cityInput'...",
                                    color =
                                        Color.White
                                            .copy(
                                                alpha =
                                                    0.7f
                                            )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// =====================================================
// LOCATION
// =====================================================

private fun hasLocationPermission(
    context: Context
): Boolean {

    val coarseGranted =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission
                .ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    val fineGranted =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission
                .ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    return coarseGranted || fineGranted
}

private fun getCurrentLocation(
    context: Context,
    onLocationFound: (
        Double,
        Double
    ) -> Unit
) {

    val fusedLocationClient =
        LocationServices
            .getFusedLocationProviderClient(
                context
            )

    try {

        fusedLocationClient
            .lastLocation
            .addOnSuccessListener {
                    location ->

                if (location != null) {

                    onLocationFound(
                        location.latitude,
                        location.longitude
                    )
                }
            }

    } catch (
        e: SecurityException
    ) {
        // Quyền vị trí chưa được cấp.
    }
}

// =====================================================
// UNIT SETTINGS
// =====================================================

/**
 * Chuyển đổi nhiệt độ từ Celsius sang đơn vị
 * mà người dùng lựa chọn.
 *
 * Fahrenheit = Celsius * 9/5 + 32
 */
private fun formatTemperature(
    celsius: Double,
    unit: String
): String {

    val convertedValue =
        if (unit == "F") {

            celsius * 9.0 / 5.0 + 32.0

        } else {

            celsius
        }

    return "${convertedValue.roundToInt()}°$unit"
}

// =====================================================
// WEATHER CONTENT
// =====================================================

@Composable
fun WeatherContent(
    state: WeatherUiState.Success,
    temperatureUnit: String,
    onUnitChange: (String) -> Unit
) {

    val weather =
        state.weather

    val main =
        weather.main

    Spacer(
        modifier =
            Modifier.height(20.dp)
    )

    // Nhiệt độ hiện tại
    Text(
        text =
            formatTemperature(
                main.temp,
                temperatureUnit
            ),
        fontSize =
            110.sp,
        fontWeight =
            FontWeight.Bold,
        color =
            Color.White
    )

    // ==========================
    // CHỌN °C / °F
    // ==========================

    Row(
        horizontalArrangement =
            Arrangement.spacedBy(
                8.dp
            ),
        verticalAlignment =
            Alignment.CenterVertically
    ) {

        FilterChip(
            selected =
                temperatureUnit == "C",
            onClick = {
                onUnitChange("C")
            },
            label = {
                Text("°C")
            },
            colors =
                FilterChipDefaults
                    .filterChipColors(
                        containerColor =
                            Color.White
                                .copy(
                                    alpha =
                                        0.15f
                                ),
                        labelColor =
                            Color.White,
                        selectedContainerColor =
                            Color.White
                                .copy(
                                    alpha =
                                        0.35f
                                ),
                        selectedLabelColor =
                            Color.White
                    )
        )

        FilterChip(
            selected =
                temperatureUnit == "F",
            onClick = {
                onUnitChange("F")
            },
            label = {
                Text("°F")
            },
            colors =
                FilterChipDefaults
                    .filterChipColors(
                        containerColor =
                            Color.White
                                .copy(
                                    alpha =
                                        0.15f
                                ),
                        labelColor =
                            Color.White,
                        selectedContainerColor =
                            Color.White
                                .copy(
                                    alpha =
                                        0.35f
                                ),
                        selectedLabelColor =
                            Color.White
                    )
        )
    }

    Spacer(
        modifier =
            Modifier.height(6.dp)
    )

    // Mô tả và nhiệt độ cao/thấp
    Text(
        text =
            "${weather.weather.firstOrNull()?.main.orEmpty()} " +
                    "${
                        formatTemperature(
                            main.tempMax,
                            temperatureUnit
                        )
                    } / ${
                        formatTemperature(
                            main.tempMin,
                            temperatureUnit
                        )
                    }",
        fontSize =
            20.sp,
        color =
            Color.White,
        fontWeight =
            FontWeight.Medium
    )

    Spacer(
        modifier =
            Modifier.height(10.dp)
    )

    // ==========================
    // AQI
    // ==========================

    Surface(
        color =
            Color.White
                .copy(
                    alpha =
                        0.3f
                ),
        shape =
            RoundedCornerShape(
                20.dp
            )
    ) {

        Row(
            modifier =
                Modifier.padding(
                    horizontal =
                        12.dp,
                    vertical =
                        6.dp
                ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            val aqi =
                state.airPollution
                    .list
                    .firstOrNull()
                    ?.main
                    ?.aqi
                    ?: 0

            val aqiText =
                when (aqi) {
                    1 -> "Tốt"
                    2 -> "Khá"
                    3 -> "Trung bình"
                    4 -> "Kém"
                    5 -> "Rất kém"
                    else -> "Không rõ"
                }

            Icon(
                imageVector =
                    Icons.Default.Cloud,
                contentDescription =
                    null,
                modifier =
                    Modifier.size(
                        16.dp
                    ),
                tint =
                    Color.White
            )

            Spacer(
                modifier =
                    Modifier.width(
                        6.dp
                    )
            )

            Text(
                text =
                    "AQI $aqi - $aqiText",
                color =
                    Color.White,
                fontSize =
                    14.sp
            )
        }
    }

    Spacer(
        modifier =
            Modifier.height(30.dp)
    )

    // ==========================
    // DỰ BÁO 3 NGÀY
    // ==========================

    GlassCard {

        Column(
            modifier =
                Modifier.padding(
                    16.dp
                )
        ) {

            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.SpaceBetween,
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Row(
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Icon(
                        imageVector =
                            Icons.Default.CalendarToday,
                        contentDescription =
                            null,
                        tint =
                            Color.White
                                .copy(
                                    alpha =
                                        0.7f
                                ),
                        modifier =
                            Modifier.size(
                                16.dp
                            )
                    )

                    Spacer(
                        modifier =
                            Modifier.width(
                                4.dp
                            )
                    )

                    Text(
                        text =
                            "Dự báo 3 ngày",
                        color =
                            Color.White
                                .copy(
                                    alpha =
                                        0.7f
                                ),
                        fontSize =
                            14.sp
                    )
                }

                Text(
                    text =
                        "Chi tiết ▶",
                    color =
                        Color.White
                            .copy(
                                alpha =
                                    0.7f
                            ),
                    fontSize =
                        12.sp
                )
            }

            Spacer(
                modifier =
                    Modifier.height(
                        16.dp
                    )
            )

            /*
             * OpenWeather trả dự báo theo khoảng
             * 3 giờ/lần.
             * 8 phần tử tương ứng khoảng 24 giờ.
             */
            val dailyForecast =
                state.forecast
                    .list
                    .filterIndexed {
                            index, _ ->

                        index % 8 == 0
                    }
                    .take(3)

            dailyForecast
                .forEachIndexed {
                        index,
                        item ->

                    val dayLabel =
                        when (index) {

                            0 ->
                                "Hôm nay"

                            1 ->
                                "Ngày mai"

                            else ->
                                getDayNameVi(
                                    item.dt
                                )
                        }

                    ForecastRow(
                        item = item,
                        dayLabel =
                            dayLabel,
                        temperatureUnit =
                            temperatureUnit
                    )

                    if (
                        index <
                        dailyForecast.size - 1
                    ) {

                        Spacer(
                            modifier =
                                Modifier.height(
                                    20.dp
                                )
                        )
                    }
                }
        }
    }

    Spacer(
        modifier =
            Modifier.height(20.dp)
    )

    // ==========================
    // THÔNG SỐ CHI TIẾT
    // ==========================

    GlassCard {

        Column(
            modifier =
                Modifier.padding(
                    16.dp
                )
        ) {

            Row(
                modifier =
                    Modifier.fillMaxWidth()
            ) {

                StatItem(
                    modifier =
                        Modifier.weight(
                            1f
                        ),
                    label =
                        "Độ ẩm",
                    value =
                        "${main.humidity}%"
                )

                StatItem(
                    modifier =
                        Modifier.weight(
                            1f
                        ),
                    label =
                        "Cảm giác",
                    value =
                        formatTemperature(
                            main.feelsLike,
                            temperatureUnit
                        )
                )
            }

            HorizontalDivider(
                color =
                    Color.White
                        .copy(
                            alpha =
                                0.2f
                        ),
                modifier =
                    Modifier.padding(
                        vertical =
                            12.dp
                    )
            )

            Row(
                modifier =
                    Modifier.fillMaxWidth()
            ) {

                StatItem(
                    modifier =
                        Modifier.weight(
                            1f
                        ),
                    label =
                        "Thấp nhất",
                    value =
                        formatTemperature(
                            main.tempMin,
                            temperatureUnit
                        )
                )

                StatItem(
                    modifier =
                        Modifier.weight(
                            1f
                        ),
                    label =
                        "Áp suất",
                    value =
                        "${main.pressure} mbar"
                )
            }

            HorizontalDivider(
                color =
                    Color.White
                        .copy(
                            alpha =
                                0.2f
                        ),
                modifier =
                    Modifier.padding(
                        vertical =
                            12.dp
                    )
            )

            Row(
                modifier =
                    Modifier.fillMaxWidth()
            ) {

                StatItem(
                    modifier =
                        Modifier.weight(
                            1f
                        ),
                    label =
                        "Cao nhất",
                    value =
                        formatTemperature(
                            main.tempMax,
                            temperatureUnit
                        )
                )

                /*
                 * OpenWeather với units=metric
                 * trả tốc độ gió theo m/s.
                 * Chuyển sang km/h bằng cách × 3.6.
                 */
                StatItem(
                    modifier =
                        Modifier.weight(
                            1f
                        ),
                    label =
                        "Tốc độ gió",
                    value =
                        "${(
                                weather.wind.speed *
                                        3.6
                                ).roundToInt()} km/h"
                )
            }
        }
    }

    Spacer(
        modifier =
            Modifier.height(40.dp)
    )
}

// =====================================================
// FORECAST ROW
// =====================================================

@Composable
fun ForecastRow(
    item: ForecastItem,
    dayLabel: String,
    temperatureUnit: String
) {

    Row(
        modifier =
            Modifier.fillMaxWidth(),
        verticalAlignment =
            Alignment.CenterVertically
    ) {

        AsyncImage(
            model =
                "https://openweathermap.org/img/wn/" +
                        "${item.weather.firstOrNull()?.icon}@2x.png",
            contentDescription =
                "Biểu tượng thời tiết",
            modifier =
                Modifier.size(
                    32.dp
                )
        )

        Spacer(
            modifier =
                Modifier.width(
                    12.dp
                )
        )

        Text(
            text =
                dayLabel,
            modifier =
                Modifier.weight(
                    1.2f
                ),
            color =
                Color.White,
            fontWeight =
                FontWeight.Bold,
            fontSize =
                18.sp
        )

        Text(
            text =
                item.weather
                    .firstOrNull()
                    ?.main
                    .orEmpty(),
            modifier =
                Modifier.weight(
                    1f
                ),
            color =
                Color.White
                    .copy(
                        alpha =
                            0.8f
                    ),
            fontSize =
                16.sp,
            fontStyle =
                FontStyle.Italic
        )

        Text(
            text =
                "${
                    formatTemperature(
                        item.main.tempMax,
                        temperatureUnit
                    )
                } / ${
                    formatTemperature(
                        item.main.tempMin,
                        temperatureUnit
                    )
                }",
            color =
                Color.White,
            fontWeight =
                FontWeight.Bold,
            fontSize =
                18.sp,
            modifier =
                Modifier.weight(
                    1f
                ),
            textAlign =
                TextAlign.End
        )
    }
}

// =====================================================
// STAT ITEM
// =====================================================

@Composable
fun StatItem(
    modifier: Modifier,
    label: String,
    value: String
) {

    Row(
        modifier =
            modifier.padding(
                end = 8.dp
            ),
        horizontalArrangement =
            Arrangement.SpaceBetween,
        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Text(
            text =
                label,
            color =
                Color.White
                    .copy(
                        alpha =
                            0.7f
                    ),
            fontSize =
                14.sp
        )

        Text(
            text =
                value,
            color =
                Color.White,
            fontSize =
                16.sp,
            fontWeight =
                FontWeight.Bold
        )
    }
}

// =====================================================
// GLASS CARD
// =====================================================

@Composable
fun GlassCard(
    content: @Composable () -> Unit
) {

    Surface(
        color =
            Color.White
                .copy(
                    alpha =
                        0.15f
                ),
        shape =
            RoundedCornerShape(
                24.dp
            ),
        modifier =
            Modifier.fillMaxWidth()
    ) {

        content()
    }
}

// =====================================================
// DATE FORMAT
// =====================================================

fun getDayNameVi(
    timestamp: Long
): String {

    val date =
        Date(
            timestamp * 1000
        )

    val localeVi =
        Locale.forLanguageTag(
            "vi-VN"
        )

    val sdf =
        SimpleDateFormat(
            "EEEE",
            localeVi
        )

    return sdf
        .format(date)
        .replaceFirstChar {
            it.uppercase()
        }
}