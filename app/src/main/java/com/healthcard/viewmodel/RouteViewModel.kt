package com.healthcard.viewmodel

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.healthcard.a2ui.A2uiMessage
import com.healthcard.amap.AmapCoord
import com.healthcard.amap.AmapWebService
import com.healthcard.amap.RunningRoute
import com.healthcard.config.ConfigRepository
import com.healthcard.location.GeoPoint
import com.healthcard.location.LocationProvider
import com.healthcard.model.ModelException
import com.healthcard.model.ModelProviderFactory
import com.healthcard.route.RoutePlanResponse
import com.healthcard.route.RoutePrompt
import com.healthcard.route.RoutesA2ui
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

/**
 * 跑步路线规划 ViewModel：定位 → 高德周边 POI 搜索（真实地点）→ DeepSeek 接地式组合环线
 * → 映射回真实 POI → A2UI 卡片展示。选中某条路线后由 [MapRouteViewModel] 画出完整路线。
 */
class RouteViewModel(application: Application) : AndroidViewModel(application) {

    private val locationProvider = LocationProvider(application)
    private val configRepository = ConfigRepository(application)
    private val amap = AmapWebService()
    private val json = Json { ignoreUnknownKeys = true }

    val isLoading = mutableStateOf(false)
    val errorMessage = mutableStateOf<String?>(null)
    val surface = mutableStateOf<A2uiMessage?>(null)

    val location = mutableStateOf<GeoPoint?>(null)
    val placeName = mutableStateOf<String?>(null)

    /** 规划参数：目标距离（公里）与场景偏好。 */
    val targetDistanceKm = mutableStateOf(5)
    val preference = mutableStateOf(PREF_ANY)

    /** 当前规划出的真实路线（供地图页使用）。 */
    val routes = mutableStateOf<List<RunningRoute>>(emptyList())

    fun planRoutes() {
        val apiKey = configRepository.deepSeekApiKey
        if (apiKey.isBlank()) {
            errorMessage.value = "请先在「模型配置」中填写 DeepSeek API Key"
            return
        }
        val model = configRepository.deepSeekModel
        val targetKm = targetDistanceKm.value
        val pref = preference.value
        val ctx = getApplication<Application>()

        viewModelScope.launch {
            isLoading.value = true
            errorMessage.value = null
            surface.value = null
            routes.value = emptyList()
            location.value = null
            placeName.value = null
            try {
                val point = locationProvider.getCurrentLocation()
                    ?: throw IllegalStateException("无法获取定位，请确认已授予定位权限并开启 GPS")
                location.value = point
                placeName.value = locationProvider.reverseGeocode(point)

                // 1) 真实 POI 检索（GCJ-02 中心）
                val center = AmapCoord.wgsToGcj(ctx, point)
                val radius = (targetKm * 1000 / 2).coerceIn(1000, 8000)
                val pois = amap.searchRunningPois(center, radius, keywordsFor(pref))
                if (pois.isEmpty()) {
                    errorMessage.value = "周边未找到合适的跑步地点，换个偏好或目标距离再试"
                    return@launch
                }

                // 2) DeepSeek 接地式组合环线
                val provider = ModelProviderFactory.deepSeek(apiKey, model)
                val reply = provider.chat(RoutePrompt.buildMessages(placeName.value, pois, targetKm, pref))
                val plan = json.decodeFromString<RoutePlanResponse>(RoutePrompt.extractJson(reply))

                // 3) 用 waypointIds 映射回真实 POI
                val poiById = pois.associateBy { it.id }
                val built = plan.routes.mapNotNull { item ->
                    val wps = item.waypointIds.mapNotNull { poiById[it] }
                    if (wps.isEmpty()) null
                    else RunningRoute(item.name, item.description, item.distanceEstimate, wps)
                }
                if (built.isEmpty()) {
                    errorMessage.value = "未能规划出有效路线，请重试"
                    return@launch
                }
                routes.value = built
                surface.value = RoutesA2ui.build(built, targetKm)
            } catch (e: ModelException) {
                errorMessage.value = "模型调用失败: ${e.message}"
            } catch (e: Exception) {
                errorMessage.value = "出错了: ${e.message}"
            } finally {
                isLoading.value = false
            }
        }
    }

    private fun keywordsFor(pref: String): String = when (pref) {
        PREF_PARK -> "公园|体育公园"
        PREF_RIVER -> "滨河|河|湖|绿道"
        PREF_GREENWAY -> "绿道|公园"
        else -> "公园|绿道|体育场|广场|滨河"
    }

    companion object {
        const val PREF_ANY = "不限"
        const val PREF_PARK = "公园"
        const val PREF_RIVER = "滨河"
        const val PREF_GREENWAY = "绿道"

        fun Factory(application: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    RouteViewModel(application) as T
            }
    }
}
