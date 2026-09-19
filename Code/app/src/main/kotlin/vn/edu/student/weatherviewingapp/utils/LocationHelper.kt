package vn.edu.student.weatherviewingapp.utils

import android.content.Context
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource

fun checkLocationSettingsAndGetLocation(
    context: Context,
    settingResultRequest: ActivityResultLauncher<IntentSenderRequest>,
    onLocationFound: (Double, Double) -> Unit
) {
    val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000).build()
    val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest).setAlwaysShow(true)
    val client = LocationServices.getSettingsClient(context)
    client.checkLocationSettings(builder.build()).addOnSuccessListener {
        getCurrentLocation(context, onLocationFound)
    }.addOnFailureListener { exception ->
        if (exception is ResolvableApiException) {
            try {
                val intentSenderRequest = IntentSenderRequest.Builder(exception.resolution).build()
                settingResultRequest.launch(intentSenderRequest)
            } catch (sendEx: Exception) {
                Toast.makeText(context, "Không thể mở cài đặt vị trí.", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Thiết bị không hỗ trợ dịch vụ vị trí.", Toast.LENGTH_SHORT).show()
        }
    }
}

fun getCurrentLocation(context: Context, onLocationFound: (Double, Double) -> Unit) {
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    Toast.makeText(context, "Đang lấy vị trí hiện tại...", Toast.LENGTH_SHORT).show()
    try {
        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            CancellationTokenSource().token
        ).addOnSuccessListener { location ->
            if (location != null) {
                onLocationFound(location.latitude, location.longitude)
            } else {
                Toast.makeText(context, "Không lấy được vị trí, thử lại sau.", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(context, "Lỗi kết nối dịch vụ định vị.", Toast.LENGTH_SHORT).show()
        }
    } catch (e: SecurityException) {
        Toast.makeText(context, "Chưa được cấp quyền truy cập Vị trí.", Toast.LENGTH_SHORT).show()
    }
}

fun hasLocationPermission(context: Context): Boolean {
    return androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
            androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
}