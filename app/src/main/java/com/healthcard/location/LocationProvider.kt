package com.healthcard.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Looper
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
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

    private val appContext = context.applicationContext
    private val locationManager =
        appContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager

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

    /**
     * 逆地理编码：把经纬度转成可读的地点描述（如「北京市海淀区中关村大街」）。
     * 使用系统 [Geocoder]，无需额外依赖或密钥；不可用或失败时返回 null。
     */
    suspend fun reverseGeocode(point: GeoPoint): String? {
        if (!Geocoder.isPresent()) return null
        val geocoder = Geocoder(appContext, Locale.getDefault())

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            suspendCancellableCoroutine { cont ->
                geocoder.getFromLocation(point.latitude, point.longitude, 1,
                    object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<Address>) {
                            if (cont.isActive) cont.resume(addresses.firstOrNull()?.toReadable())
                        }

                        override fun onError(errorMessage: String?) {
                            if (cont.isActive) cont.resume(null)
                        }
                    })
            }
        } else {
            withContext(Dispatchers.IO) {
                runCatching {
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocation(point.latitude, point.longitude, 1)
                        ?.firstOrNull()
                        ?.toReadable()
                }.getOrNull()
            }
        }
    }

    /** 把 [Address] 组装成一行可读地址。 */
    private fun Address.toReadable(): String? {
        getAddressLine(0)?.takeIf { it.isNotBlank() }?.let { return it }
        val parts = listOfNotNull(
            adminArea,        // 省/直辖市
            locality,         // 市
            subLocality,      // 区
            thoroughfare,     // 街道
            featureName,      // 门牌/地点名
        ).distinct()
        return parts.joinToString("").ifBlank { null }
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
