package vn.edu.student.weatherviewingapp.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import vn.edu.student.weatherviewingapp.ui.CompareUiState
import vn.edu.student.weatherviewingapp.ui.LocationWeatherComparison
import vn.edu.student.weatherviewingapp.viewmodel.CompareViewModel

import androidx.compose.ui.tooling.preview.Preview
import vn.edu.student.weatherviewingapp.data.Coord
import vn.edu.student.weatherviewingapp.data.Main
import vn.edu.student.weatherviewingapp.data.Weather
import vn.edu.student.weatherviewingapp.data.WeatherResponse
import vn.edu.student.weatherviewingapp.data.Wind
import vn.edu.student.weatherviewingapp.data.Sys
import vn.edu.student.weatherviewingapp.data.AirPollutionResponse
import vn.edu.student.weatherviewingapp.data.AirPollutionItem
import vn.edu.student.weatherviewingapp.data.AirPollutionMain

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompareScreen(
    onBack: () -> Unit,
    viewModel: CompareViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val selectedLocations by viewModel.selectedLocations.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadFavorites()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("So sánh thời tiết", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF2196F3), Color(0xFF64B5F6), Color(0xFFBBDEFB))
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                                Text(
                    text = "Chọn địa điểm so sánh (${selectedLocations.size})",
                    modifier = Modifier.padding(16.dp),
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 160.dp)
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White.copy(alpha = 0.2f)
                ) {
                    LazyColumn(modifier = Modifier.padding(8.dp)) {
                        items(favorites) { location ->
                            val isSelected = selectedLocations.contains(location)
                            val name = location.localNames?.get("vi") ?: location.name
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.toggleSelection(location) }
                                    .padding(vertical = 8.dp, horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                    contentDescription = null,
                                    tint = if (isSelected) Color.White else Color.White.copy(alpha = 0.5f),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = name,
                                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f),
                                    fontSize = 16.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                                Box(modifier = Modifier.weight(1f)) {
                    when (val state = uiState) {
                        is CompareUiState.Empty -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Compare, contentDescription = null, tint = Color.White.copy(alpha = 0.3f), modifier = Modifier.size(64.dp))
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text("Chọn ít nhất 2 địa điểm\nđể xem biểu đồ đối chiếu", color = Color.White.copy(alpha = 0.5f), textAlign = TextAlign.Center)
                                }
                            }
                        }
                        is CompareUiState.Loading -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = Color.White)
                            }
                        }
                        is CompareUiState.Success -> {
                            ComparisonContent(state.comparisons)
                        }
                        is CompareUiState.Error -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(state.message, color = Color(0xFFFF8A80), textAlign = TextAlign.Center)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ComparisonContent(comparisons: List<LocationWeatherComparison>) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(bottom = 24.dp)
    ) {
                Text(
            text = "Biểu đồ Nhiệt độ Trực quan",
            modifier = Modifier.padding(start = 24.dp, top = 8.dp, bottom = 16.dp),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
        
        TemperatureBarChart(comparisons)

        Spacer(modifier = Modifier.height(32.dp))

                Text(
            text = "Chi tiết thông số",
            modifier = Modifier.padding(start = 24.dp, bottom = 16.dp),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(28.dp),
            color = Color.White.copy(alpha = 0.05f),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
        ) {
            val tableScrollState = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(tableScrollState)
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.width(110.dp)) {
                    Box(modifier = Modifier.height(30.dp))

                    StatLabelRow("Cảm giác", Icons.Default.Thermostat)
                    StatLabelRow("Độ ẩm", Icons.Default.WaterDrop)
                    StatLabelRow("Gió", Icons.Default.Air)
                    StatLabelRow("AQI", Icons.Default.Cloud)
                    StatLabelRow("Áp suất", Icons.Default.Compress)
                }

                comparisons.forEach { item ->
                    ComparisonCard(item)
                }
            }
        }
    }
}

@Composable
fun TemperatureBarChart(comparisons: List<LocationWeatherComparison>) {
    val maxTemp = comparisons.maxOfOrNull { it.weather.main.temp } ?: 40.0
    val displayMax = maxOf(maxTemp + 5, 30.0)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(28.dp),
        color = Color.White.copy(alpha = 0.2f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(start = 20.dp, end = 20.dp, top = 40.dp, bottom = 40.dp)) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val barWidth = 45.dp.toPx()
            val spacing = (canvasWidth - (barWidth * comparisons.size)) / (comparisons.size + 1)

                        val gridLines = 4
            for (i in 0..gridLines) {
                val yGrid = (canvasHeight / gridLines) * i
                drawLine(
                    color = Color.White.copy(alpha = 0.05f),
                    start = Offset(0f, yGrid),
                    end = Offset(canvasWidth, yGrid),
                    strokeWidth = 1.dp.toPx()
                )
            }

            comparisons.forEachIndexed { index, item ->
                val temp = item.weather.main.temp
                val barHeight = (temp / displayMax) * canvasHeight
                val x = spacing + index * (barWidth + spacing)
                val y = canvasHeight - barHeight.toFloat()

                                val barGradient = Brush.verticalGradient(
                    colors = if (temp > 30) listOf(Color(0xFFFF8A65), Color(0xFFE64A19))
                            else if (temp > 20) listOf(Color(0xFFFFF176), Color(0xFFFFD54F))
                            else listOf(Color(0xFFE0F7FA), Color(0xFF80DEEA))
                )

                                drawRoundRect(
                    brush = barGradient,
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeight.toFloat()),
                    cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
                )

                                drawRoundRect(
                    color = Color.White.copy(alpha = 0.2f),
                    topLeft = Offset(x + 5.dp.toPx(), y + 5.dp.toPx()),
                    size = Size(barWidth / 4, barHeight.toFloat() - 10.dp.toPx()),
                    cornerRadius = CornerRadius(4.dp.toPx())
                )

                                drawContext.canvas.nativeCanvas.drawText(
                    "${temp.toInt()}°",
                    x + barWidth / 2,
                    y - 12.dp.toPx(),
                    android.graphics.Paint().apply {
                        color = android.graphics.Color.WHITE
                        textSize = 16.sp.toPx()
                        textAlign = android.graphics.Paint.Align.CENTER
                        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                    }
                )

                                val displayName = if (item.name.length > 8) item.name.take(7) + ".." else item.name
                drawContext.canvas.nativeCanvas.drawText(
                    displayName,
                    x + barWidth / 2,
                    canvasHeight + 25.dp.toPx(),
                    android.graphics.Paint().apply {
                        color = android.graphics.Color.WHITE
                        alpha = (0.7f * 255).toInt()
                        textSize = 11.sp.toPx()
                        textAlign = android.graphics.Paint.Align.CENTER
                    }
                )
            }
        }
    }
}

@Composable
fun StatLabelRow(label: String, icon: ImageVector) {
    Row(
        modifier = Modifier
            .height(60.dp)             .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Text(text = label, color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun ComparisonCard(item: LocationWeatherComparison) {
    Column(
        modifier = Modifier
            .width(130.dp)
            .padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
                Box(modifier = Modifier.height(30.dp), contentAlignment = Alignment.Center) {
            Text(
                text = item.name,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp,
                maxLines = 1,
                textAlign = TextAlign.Center
            )
        }

                StatValueBox("${item.weather.main.feelsLike.toInt()}°")
        StatValueBox("${item.weather.main.humidity}%")
        StatValueBox("${item.weather.wind.speed.toInt()} km/h")
        
        val aqi = item.airPollution.list.firstOrNull()?.main?.aqi ?: 0
        val aqiColor = when(aqi) {
            1 -> Color(0xFF66BB6A)
            2 -> Color(0xFFFFEE58)
            3 -> Color(0xFFFFA726)
            4 -> Color(0xFFFF7043)
            5 -> Color(0xFFEF5350)
            else -> Color.White
        }
        StatValueBox("AQI $aqi", aqiColor)
        
        StatValueBox("${item.weather.main.pressure}")
    }
}

@Composable
fun StatValueBox(value: String, textColor: Color = Color.White) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .padding(vertical = 4.dp),
        color = Color.White.copy(alpha = 0.2f),
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text = value, color = textColor, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun CompareScreenPreview() {
    val mockWeather = WeatherResponse(
        main = Main(28.5, 30.0, 27.0, 29.0, 75, 1010),
        weather = listOf(Weather("Clouds", "nhiều mây", "04d")),
        cityName = "Hà Nội",
        timestamp = 1631456000L,
        coord = Coord(21.0, 105.8),
        wind = Wind(5.5),
        sys = Sys("VN")
    )
    
    val mockAir = AirPollutionResponse(
        list = listOf(AirPollutionItem(AirPollutionMain(2), emptyMap()))
    )

    val comparisons = listOf(
        LocationWeatherComparison("Hà Nội", mockWeather, mockAir),
        LocationWeatherComparison("TP.HCM", mockWeather.copy(cityName = "TP.HCM", main = mockWeather.main.copy(temp = 33.0, feelsLike = 36.0)), mockAir.copy(list = listOf(AirPollutionItem(AirPollutionMain(4), emptyMap())))),
        LocationWeatherComparison("Đà Lạt", mockWeather.copy(cityName = "Đà Lạt", main = mockWeather.main.copy(temp = 19.0, feelsLike = 18.0)), mockAir.copy(list = listOf(AirPollutionItem(AirPollutionMain(1), emptyMap()))))
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF2196F3), Color(0xFF64B5F6), Color(0xFFBBDEFB))
                )
            )
    ) {
        ComparisonContent(comparisons = comparisons)
    }
}
