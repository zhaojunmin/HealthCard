package com.healthcard.viewmodel

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.amap.api.maps.CoordinateConverter
import com.amap.api.maps.model.LatLng
import com.healthcard.amap.AmapPrivacy
import com.healthcard.amap.AmapWebService
import com.healthcard.amap.RoutePoint
import com.healthcard.amap.WalkRoute
import com.healthcard.location.GeoPoint
import kotlinx.coroutines.launch

/**
 * 地图路线界面的 ViewModel：把起点(GPS)与终点交给高德 Web 服务做步行路径规划，
 * 结果（GCJ-02 折线）交给 [com.healthcard.ui.MapRouteScreen] 在高德地图上绘制。
 */
class MapRouteViewModel(application: Application) : AndroidViewModel(application) {

    private val web = AmapWebService()

    val isLoading = mutableStateOf(false)
    val error = mutableStateOf<String?>(null)
    val route = mutableStateOf<WalkRoute?>(null)
    val origin = mutableStateOf<RoutePoint?>(null)        // GCJ-02
    val destination = mutableStateOf<RoutePoint?>(null)   // GCJ-02
    val destName = mutableStateOf("")

    /**
     * @param originWgs 起点（系统 GPS，WGS-84），会转换为高德 GCJ-02。
     * @param destLat/destLon 终点（视为 GCJ-02，由大模型给出）。
     */
    fun plan(originWgs: GeoPoint?, destLat: Double, destLon: Double, name: String) {
        AmapPrivacy.ensure(getApplication())
        destName.value = name
        val dest = RoutePoint(destLat, destLon)
        destination.value = dest

        viewModelScope.launch {
            isLoading.value = true
            error.value = null
            route.value = null
            try {
                val start = originWgs?.let { wgsToGcj(it) }
                origin.value = start
                if (start == null) {
                    error.value = "无法获取起点定位"
                    return@launch
                }
                route.value = web.walkingRoute(start, dest)
            } catch (e: Exception) {
                error.value = e.message ?: "路线规划失败"
            } finally {
                isLoading.value = false
            }
        }
    }

    /** WGS-84 → GCJ-02（高德）。失败时原样返回。 */
    private fun wgsToGcj(p: GeoPoint): RoutePoint = try {
        val converted = CoordinateConverter(getApplication())
            .from(CoordinateConverter.CoordType.GPS)
            .coord(LatLng(p.latitude, p.longitude))
            .convert()
        RoutePoint(converted.latitude, converted.longitude)
    } catch (e: Exception) {
        RoutePoint(p.latitude, p.longitude)
    }

    companion object {
        fun Factory(application: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    MapRouteViewModel(application) as T
            }
    }
}
