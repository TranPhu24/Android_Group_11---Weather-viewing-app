package vn.edu.student.weatherviewingapp.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import vn.edu.student.weatherviewingapp.data.ForecastItem

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