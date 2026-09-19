package vn.edu.student.weatherviewingapp.data

import vn.edu.student.weatherviewingapp.utils.getDayNameVi

data class DailyForecastSummary(
    val dayLabel: String,
    val weatherMain: String,
    val icon: String,
    val tempMax: Int,
    val tempMin: Int
)

fun getDailyForecastSummaries(forecastList: List<ForecastItem>): List<DailyForecastSummary> {
    val dayGroups = forecastList
        .groupBy { it.dtTxt.take(10) }
        .entries
        .sortedBy { it.key }
        .take(5)

    return dayGroups.mapIndexed { index, (_, itemsInDay) ->
        val maxTemp = itemsInDay.maxOf { it.main.tempMax }.toInt()
        val minTemp = itemsInDay.minOf { it.main.tempMin }.toInt()

        val repItem = itemsInDay.firstOrNull { it.dtTxt.contains("12:00") }
            ?: itemsInDay.getOrNull(itemsInDay.size / 2)
            ?: itemsInDay.first()

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
