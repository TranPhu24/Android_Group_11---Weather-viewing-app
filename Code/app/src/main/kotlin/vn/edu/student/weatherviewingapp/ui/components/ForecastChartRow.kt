package vn.edu.student.weatherviewingapp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import vn.edu.student.weatherviewingapp.data.DailyForecastSummary
import kotlin.math.roundToInt

@Composable
fun ForecastChartRow(
    fiveDaysList: List<DailyForecastSummary>,
    modifier: Modifier = Modifier
) {
    if (fiveDaysList.isEmpty()) return

    val overallMax = fiveDaysList.maxOf { it.tempMax }
    val overallMin = fiveDaysList.minOf { it.tempMin }
    val tempRange = (overallMax - overallMin).coerceAtLeast(1).toFloat()

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(360.dp)
            .background(Color(0x33000000), RoundedCornerShape(24.dp))
    ) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()
        
        val columnWidthPx = widthPx / fiveDaysList.size
        
        // Define chart area boundaries (Y-axis)
        val chartTopY = heightPx * 0.35f
        val chartBottomY = heightPx * 0.65f
        val chartHeight = chartBottomY - chartTopY
        val maxPadding = chartHeight * 0.1f
        val effectiveChartHeight = chartHeight - maxPadding * 2

        // We pre-calculate point Y coordinates
        val points = fiveDaysList.mapIndexed { index, item ->
            val x = columnWidthPx * (index + 0.5f)
            val maxNormalized = if (tempRange == 0f) 0.5f else (overallMax - item.tempMax) / tempRange
            val minNormalized = if (tempRange == 0f) 0.5f else (overallMax - item.tempMin) / tempRange

            val maxY = chartTopY + maxPadding + maxNormalized * effectiveChartHeight
            val minY = chartTopY + maxPadding + minNormalized * effectiveChartHeight

            Triple(Offset(x, maxY), Offset(x, minY), item)
        }

        // Draw Lines and Points
        Canvas(modifier = Modifier.fillMaxSize()) {
            val maxPath = Path().apply {
                points.forEachIndexed { index, (maxPt, _, _) ->
                    if (index == 0) moveTo(maxPt.x, maxPt.y)
                    else lineTo(maxPt.x, maxPt.y)
                }
            }
            drawPath(
                path = maxPath,
                color = Color.White.copy(alpha = 0.7f),
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            val minPath = Path().apply {
                points.forEachIndexed { index, (_, minPt, _) ->
                    if (index == 0) moveTo(minPt.x, minPt.y)
                    else lineTo(minPt.x, minPt.y)
                }
            }
            drawPath(
                path = minPath,
                color = Color.White.copy(alpha = 0.7f),
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            points.forEach { (maxPt, minPt, _) ->
                // Draw Max Point
                drawCircle(color = Color.White, radius = 4.dp.toPx(), center = maxPt)
                drawCircle(color = Color(0xFF42A5F5), radius = 2.dp.toPx(), center = maxPt)
                // Draw Min Point
                drawCircle(color = Color.White, radius = 4.dp.toPx(), center = minPt)
                drawCircle(color = Color(0xFF42A5F5), radius = 2.dp.toPx(), center = minPt)
            }
        }

        // Overlay Texts and Icons using Layout offsets
        points.forEachIndexed { index, (maxPt, minPt, item) ->
            // Highlight background for the first day (Hôm nay)
            val isFirst = index == 0
            if (isFirst) {
                Box(
                    modifier = Modifier
                        .offset { IntOffset((index * columnWidthPx).roundToInt(), 0) }
                        .width(maxWidth / 5)
                        .fillMaxHeight()
                        .background(Color(0x22FFFFFF), RoundedCornerShape(24.dp))
                )
            }

            // Top Info (Day, Date, Icon)
            Column(
                modifier = Modifier
                    .offset { IntOffset((index * columnWidthPx).roundToInt(), 16.dp.roundToPx()) }
                    .width(maxWidth / 5),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = item.dayLabel,
                    color = if (isFirst) Color.White else Color.White.copy(alpha = 0.8f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.dateString,
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                AsyncImage(
                    model = "https://openweathermap.org/img/wn/${item.icon}@2x.png",
                    contentDescription = null,
                    modifier = Modifier.size(36.dp)
                )
            }

            // Max Temp Text
            Box(
                modifier = Modifier
                    .offset { IntOffset((index * columnWidthPx).roundToInt(), (maxPt.y - 28.dp.toPx()).roundToInt()) }
                    .width(maxWidth / 5),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${item.tempMax}°",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Min Temp Text
            Box(
                modifier = Modifier
                    .offset { IntOffset((index * columnWidthPx).roundToInt(), (minPt.y + 12.dp.toPx()).roundToInt()) }
                    .width(maxWidth / 5),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${item.tempMin}°",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Bottom Info (Secondary icon, wind speed)
            Column(
                modifier = Modifier
                    .offset { IntOffset((index * columnWidthPx).roundToInt(), (heightPx - 80.dp.toPx()).roundToInt()) }
                    .width(maxWidth / 5),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AsyncImage(
                    model = "https://openweathermap.org/img/wn/${item.icon}.png", // small icon
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    alpha = 0.8f
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${String.format("%.1f", item.windSpeed)}km/h",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 10.sp
                )
            }
        }
    }
}
