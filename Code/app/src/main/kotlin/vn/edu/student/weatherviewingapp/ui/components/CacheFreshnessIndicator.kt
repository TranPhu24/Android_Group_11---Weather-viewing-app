package vn.edu.student.weatherviewingapp.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import vn.edu.student.weatherviewingapp.data.CacheFreshness
import vn.edu.student.weatherviewingapp.data.WeatherCachePolicy
import vn.edu.student.weatherviewingapp.utils.formatCacheAge

@Composable
fun CacheFreshnessIndicator(refreshedAtMillis: Long) {
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