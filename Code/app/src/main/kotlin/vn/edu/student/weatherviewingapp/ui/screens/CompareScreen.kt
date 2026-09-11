package vn.edu.student.weatherviewingapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import vn.edu.student.weatherviewingapp.ui.CompareUiState
import vn.edu.student.weatherviewingapp.ui.LocationWeatherComparison
import vn.edu.student.weatherviewingapp.viewmodel.CompareViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompareScreen(
    onBack: () -> Unit,
    viewModel: CompareViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val selectedLocations by viewModel.selectedLocations.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("So sánh địa điểm", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF2196F3))
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF2196F3), Color(0xFFBBDEFB))
                    )
                )
        ) {
            // Location Selection
            Text(
                text = "Chọn ít nhất 2 địa điểm để so sánh:",
                modifier = Modifier.padding(16.dp),
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 200.dp)
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color.White.copy(alpha = 0.2f)
            ) {
                LazyColumn {
                    items(favorites) { location ->
                        val isSelected = selectedLocations.contains(location)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.toggleSelection(location) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = if (isSelected) Color.White else Color.White.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = location.localNames?.get("vi") ?: location.name,
                                color = Color.White,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Comparison Results
            Box(modifier = Modifier.fillMaxSize()) {
                when (val state = uiState) {
                    is CompareUiState.Empty -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Hãy chọn thêm địa điểm", color = Color.White)
                        }
                    }
                    is CompareUiState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Color.White)
                        }
                    }
                    is CompareUiState.Success -> {
                        ComparisonTable(state.comparisons)
                    }
                    is CompareUiState.Error -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(state.message, color = Color.Red, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ComparisonTable(comparisons: List<LocationWeatherComparison>) {
    val scrollState = rememberScrollState()
    
    Row(
        modifier = Modifier
            .fillMaxSize()
            .horizontalScroll(scrollState)
            .padding(16.dp)
    ) {
        // Categories column
        Column(modifier = Modifier.width(100.dp)) {
            ComparisonHeader("")
            ComparisonLabel("Nhiệt độ")
            ComparisonLabel("Cảm giác")
            ComparisonLabel("Độ ẩm")
            ComparisonLabel("Gió")
            ComparisonLabel("AQI")
        }

        // Data columns
        comparisons.forEach { item ->
            Column(modifier = Modifier.width(120.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                ComparisonHeader(item.name)
                ComparisonValue("${item.weather.main.temp.toInt()}°C")
                ComparisonValue("${item.weather.main.feelsLike.toInt()}°C")
                ComparisonValue("${item.weather.main.humidity}%")
                ComparisonValue("${item.weather.wind.speed.toInt()}km/h")
                ComparisonValue(item.airPollution.list.firstOrNull()?.main?.aqi?.toString() ?: "-")
            }
        }
    }
}

@Composable
fun ComparisonHeader(text: String) {
    Text(
        text = text,
        modifier = Modifier
            .height(50.dp)
            .padding(8.dp),
        fontWeight = FontWeight.Bold,
        color = Color.White,
        textAlign = TextAlign.Center
    )
}

@Composable
fun ComparisonLabel(text: String) {
    Text(
        text = text,
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .padding(8.dp),
        color = Color.White.copy(alpha = 0.7f),
        fontSize = 14.sp
    )
}

@Composable
fun ComparisonValue(text: String) {
    Surface(
        modifier = Modifier
            .width(100.dp)
            .height(40.dp)
            .padding(4.dp),
        color = Color.White.copy(alpha = 0.15f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text = text, color = Color.White, fontWeight = FontWeight.Medium)
        }
    }
}
