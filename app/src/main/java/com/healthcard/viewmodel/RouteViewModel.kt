package com.healthcard.viewmodel

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.healthcard.a2ui.A2uiMessage
import com.healthcard.config.ConfigRepository
import com.healthcard.location.GeoPoint
import com.healthcard.location.LocationProvider
import com.healthcard.model.ModelException
import com.healthcard.model.ModelProviderFactory
import com.healthcard.route.RoutePrompt
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

/**
 * 运动路线规划的 ViewModel：定位 → 调 DeepSeek → 解析 A2UI → 暴露给界面渲染。
 */
class RouteViewModel(application: Application) : AndroidViewModel(application) {

    private val locationProvider = LocationProvider(application)
    private val configRepository = ConfigRepository(application)
    private val json = Json { ignoreUnknownKeys = true }

    val isLoading = mutableStateOf(false)
    val errorMessage = mutableStateOf<String?>(null)
    val surface = mutableStateOf<A2uiMessage?>(null)

    /** 当前定位坐标与逆地理编码得到的地点名，用于界面展示。 */
    val location = mutableStateOf<GeoPoint?>(null)
    val placeName = mutableStateOf<String?>(null)

    /** 读取已保存的 Key，获取定位并请求 DeepSeek 规划周边跑步路线。调用前需已授予定位权限。 */
    fun planRoutes() {
        val apiKey = configRepository.deepSeekApiKey
        if (apiKey.isBlank()) {
            errorMessage.value = "请先在「模型配置」中填写 DeepSeek API Key"
            return
        }
        val model = configRepository.deepSeekModel
        viewModelScope.launch {
            isLoading.value = true
            errorMessage.value = null
            surface.value = null
            location.value = null
            placeName.value = null
            try {
                val point = locationProvider.getCurrentLocation()
                    ?: throw IllegalStateException("无法获取定位，请确认已授予定位权限并开启 GPS")
                location.value = point

                // 先把经纬度转成地点信息，再交给大模型
                val place = locationProvider.reverseGeocode(point)
                placeName.value = place

                val provider = ModelProviderFactory.deepSeek(apiKey, model)
                val reply = provider.chat(RoutePrompt.buildMessages(point, place))

                surface.value = json.decodeFromString<A2uiMessage>(RoutePrompt.extractJson(reply))
            } catch (e: ModelException) {
                errorMessage.value = "模型调用失败: ${e.message}"
            } catch (e: Exception) {
                errorMessage.value = "出错了: ${e.message}"
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
                    RouteViewModel(application) as T
            }
    }
}
