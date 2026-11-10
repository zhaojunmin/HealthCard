package com.healthcard.viewmodel

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.healthcard.amap.AmapCoord
import com.healthcard.amap.AmapWebService
import com.healthcard.amap.RoutePoint
import com.healthcard.amap.WalkRoute
import com.healthcard.location.GeoPoint
import kotlinx.coroutines.launch

/**
 * 地图路线界面的 ViewModel：把「起点 → 各真实途经点 → 回起点」逐段用高德步行 API 规划并拼接，
 * 得到完整环线折线与真实总距离，交给 [com.healthcard.ui.MapRouteScreen] 绘制。
 */
class MapRouteViewModel(application: Application) : AndroidViewModel(application) {

    private val web = AmapWebService()

    val isLoading = mutableStateOf(false)
    val error = mutableStateOf<String?>(null)
    val route = mutableStateOf<WalkRoute?>(null)
    val origin = mutableStateOf<RoutePoint?>(null)             // GCJ-02 起点
    val waypoints = mutableStateOf<List<RoutePoint>>(emptyList()) // GCJ-02 途经点
    val routeName = mutableStateOf("")

    /**
     * @param originWgs 起点（系统 GPS，WGS-84），内部转为 GCJ-02。
     * @param waypointsGcj 真实途经点（GCJ-02）。
     */
    fun planRoute(originWgs: GeoPoint?, waypointsGcj: List<RoutePoint>, name: String) {
        routeName.value = name
        waypoints.value = waypointsGcj
        val ctx = getApplication<Application>()

        viewModelScope.launch {
            isLoading.value = true
            error.value = null
            route.value = null
            try {
                val start = originWgs?.let { AmapCoord.wgsToGcj(ctx, it) }
                origin.value = start
                if (start == null) {
                    error.value = "无法获取起点定位"
                    return@launch
                }
                if (waypointsGcj.isEmpty()) {
                    error.value = "该路线没有途经点"
                    return@launch
                }

                // 起点 → 各途经点 → 回起点
                val sequence = buildList {
                    add(start)
                    addAll(waypointsGcj)
                    add(start)
                }

                val allPoints = mutableListOf<RoutePoint>()
                var totalDist = 0
                var totalDur = 0
                for (i in 0 until sequence.size - 1) {
                    val seg = web.walkingRoute(sequence[i], sequence[i + 1])
                    allPoints.addAll(seg.points)
                    totalDist += seg.distanceMeters
                    totalDur += seg.durationSeconds
                }

                route.value = WalkRoute(allPoints, totalDist, totalDur)
            } catch (e: Exception) {
                error.value = e.message ?: "路线规划失败"
            } finally {
                isLoading.value = false
            }
        }
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
