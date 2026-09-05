package vn.edu.student.weatherviewingapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import vn.edu.student.weatherviewingapp.data.ForecastItem
import vn.edu.student.weatherviewingapp.data.WeatherResponse

@Composable
fun Forecast5DaysScreen(
    forecastList: List<ForecastItem>,
    currentWeather: WeatherResponse,
    cityName: String,
    onClose: () -> Unit
) {
    // Lọc lấy 5 ngày dự báo (mỗi ngày 1 mốc đại diện)
    val fiveDaysList = getDailyForecastSummaries(forecastList).take(5)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF1E88E5), Color(0xFF42A5F5), Color(0xFF90CAF9))
                )
            )
            .statusBarsPadding()
            .zIndex(50f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(20.dp)
        ) {
            // Thanh Header có nút Quay lại
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { onClose() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại", tint = Color.White)
                }
                Text(
                    text = "Dự báo 5 ngày",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.width(48.dp))
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = cityName,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 16.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            GlassCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    fiveDaysList.forEachIndexed { index, item ->
                        if (index == 0) {
                            ForecastRow(
                                dayLabel = "Hôm nay",
                                weatherMain = currentWeather.weather.firstOrNull()?.main ?: "",
                                icon = currentWeather.weather.firstOrNull()?.icon ?: "01d",
                                tempMax = currentWeather.main.tempMax.toInt(),
                                tempMin = currentWeather.main.tempMin.toInt()
                            )
                        } else {
                            ForecastRow(item.dayLabel,
                                weatherMain = item.weatherMain,
                                icon = item.icon,
                                tempMax = item.tempMax,
                                tempMin = item.tempMin
                            )
                        }

                        if (index < fiveDaysList.size - 1) {
                            HorizontalDivider(
                                color = Color.White.copy(alpha = 0.15f),
                                modifier = Modifier.padding(vertical = 14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

