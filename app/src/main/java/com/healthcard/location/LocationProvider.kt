package com.healthcard.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/** 简单经纬度坐标。 */
data class GeoPoint(
    val latitude: Double,
    val longitude: Double,
)

/**
 * 基于系统 [LocationManager] 的一次性定位封装，不依赖 Google Play 服务。
 * 调用方需自行确保已获得定位权限（见 [RouteScreen] 中的运行时申请）。
 */
class LocationProvider(context: Context) {

    private val locationManager =
        context.applicationContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    /**
     * 获取当前坐标：优先请求一次实时更新，超时则回退到最近已知位置。
     * 无权限、无可用 provider 或始终拿不到坐标时返回 null。
     */
    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    suspend fun getCurrentLocation(timeoutMillis: Long = 10_000): GeoPoint? {
        val fresh = withTimeoutOrNull(timeoutMillis) { requestSingleUpdate() }
        if (fresh != null) return fresh
        return lastKnownLocation()
    }

    @SuppressLint("MissingPermission")
    private suspend fun requestSingleUpdate(): GeoPoint? = suspendCancellableCoroutine { cont ->
        val provider = when {
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            else -> null
        }
        if (provider == null) {
            cont.resume(null)
            return@suspendCancellableCoroutine
        }

        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                locationManager.removeUpdates(this)
                if (cont.isActive) cont.resume(location.toGeoPoint())
            }

            // 兼容旧 API 的空实现
            override fun onProviderDisabled(provider: String) {}
            override fun onProviderEnabled(provider: String) {}
            @Deprecated("Deprecated in API 29")
            override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) {}
        }

        try {
            locationManager.requestLocationUpdates(provider, 0L, 0f, listener, Looper.getMainLooper())
        } catch (e: SecurityException) {
            if (cont.isActive) cont.resume(null)
            return@suspendCancellableCoroutine
        }

        cont.invokeOnCancellation { locationManager.removeUpdates(listener) }
    }

    @SuppressLint("MissingPermission")
    private fun lastKnownLocation(): GeoPoint? {
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        return providers
            .mapNotNull { runCatching { locationManager.getLastKnownLocation(it) }.getOrNull() }
            .maxByOrNull { it.time }
            ?.toGeoPoint()
    }

    private fun Location.toGeoPoint() = GeoPoint(latitude, longitude)
}
