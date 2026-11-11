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
 * 地图路线界面的 ViewModel。
 *
 * 用高德步行路径规划能力**优化路径**：先两两规划出各点间真实步行距离，再求
 * 「起点 → 各途经点 → 回起点」总距离最短的访问顺序（小规模精确求解），
 * 最后按最优顺序拼接完整环线折线，避免折返、路径更合理。
 */
class MapRouteViewModel(application: Application) : AndroidViewModel(application) {

    private val web = AmapWebService()

    val isLoading = mutableStateOf(false)
    val error = mutableStateOf<String?>(null)
    val route = mutableStateOf<WalkRoute?>(null)
    val origin = mutableStateOf<RoutePoint?>(null)
    val waypoints = mutableStateOf<List<RoutePoint>>(emptyList()) // 优化后的顺序，供标记
    val routeName = mutableStateOf("")

    fun planRoute(originWgs: GeoPoint?, waypointsGcj: List<RoutePoint>, name: String) {
        routeName.value = name
        val ctx = getApplication<Application>()

        viewModelScope.launch {
            isLoading.value = true
            error.value = null
            route.value = null
            waypoints.value = emptyList()
            try {
                val start = originWgs?.let { AmapCoord.wgsToGcj(ctx, it) }
                origin.value = start
                if (start == null) {
                    error.value = "无法获取起点定位"
                    return@launch
                }
                // 最多 5 个途经点，控制两两规划的调用量
                val wps = waypointsGcj.take(5)
                if (wps.isEmpty()) {
                    error.value = "该路线没有途经点"
                    return@launch
                }

                val nodes = listOf(start) + wps          // index 0 = 起点
                val n = nodes.size

                // 1) 两两步行规划（真实道路距离与折线）
                val seg = HashMap<Int, WalkRoute>()      // key = i*10 + j (i<j)
                val dist = Array(n) { DoubleArray(n) }
                for (i in 0 until n) for (j in i + 1 until n) {
                    val wr = web.walkingRoute(nodes[i], nodes[j])
                    seg[i * 10 + j] = wr
                    dist[i][j] = wr.distanceMeters.toDouble()
                    dist[j][i] = dist[i][j]
                }

                // 2) 求最优访问顺序（起点固定为 0，环线回到 0）
                val wpIdx = (1 until n).toList()
                val bestOrder = permutations(wpIdx).minByOrNull { order ->
                    var d = dist[0][order.first()]
                    for (k in 0 until order.size - 1) d += dist[order[k]][order[k + 1]]
                    d + dist[order.last()][0]
                } ?: wpIdx

                waypoints.value = bestOrder.map { nodes[it] }

                // 3) 按最优顺序拼接完整折线
                val fullOrder = listOf(0) + bestOrder + listOf(0)
                val allPoints = mutableListOf<RoutePoint>()
                var totalDist = 0
                var totalDur = 0
                for (k in 0 until fullOrder.size - 1) {
                    val i = fullOrder[k]
                    val j = fullOrder[k + 1]
                    val wr = seg[minOf(i, j) * 10 + maxOf(i, j)]!!
                    val pts = if (i <= j) wr.points else wr.points.reversed()
                    allPoints.addAll(pts)
                    totalDist += wr.distanceMeters
                    totalDur += wr.durationSeconds
                }

                route.value = WalkRoute(allPoints, totalDist, totalDur)
            } catch (e: Exception) {
                error.value = e.message ?: "路线规划失败"
            } finally {
                isLoading.value = false
            }
        }
    }

    /** 生成全排列（规模 ≤5，开销可忽略）。 */
    private fun permutations(items: List<Int>): List<List<Int>> {
        if (items.size <= 1) return listOf(items)
        val result = mutableListOf<List<Int>>()
        for (i in items.indices) {
            val rest = items.toMutableList().apply { removeAt(i) }
            for (p in permutations(rest)) result.add(listOf(items[i]) + p)
        }
        return result
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
