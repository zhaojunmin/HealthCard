package com.healthcard.amap

import com.healthcard.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

/** 路线上的一个坐标点（GCJ-02 高德坐标系）。 */
data class RoutePoint(val lat: Double, val lon: Double)

/** 一条完整步行路线。 */
data class WalkRoute(
    val points: List<RoutePoint>,
    val distanceMeters: Int,
    val durationSeconds: Int,
)

/** 调用高德 Web 服务 API 做步行路径规划，返回完整折线。 */
class AmapWebService {

    private val json = Json { ignoreUnknownKeys = true }
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    /**
     * 步行路径规划。坐标均为 GCJ-02（高德）坐标系。
     * @throws IllegalStateException 未配置 Web Key / 接口返回错误时。
     */
    suspend fun walkingRoute(
        origin: RoutePoint,
        destination: RoutePoint,
    ): WalkRoute = withContext(Dispatchers.IO) {
        val key = BuildConfig.AMAP_WEB_KEY
        check(key.isNotBlank()) { "未配置高德 Web 服务 Key（local.properties: AMAP_WEB_KEY）" }

        val url = "https://restapi.amap.com/v3/direction/walking" +
            "?origin=${origin.lon},${origin.lat}" +
            "&destination=${destination.lon},${destination.lat}" +
            "&key=$key"

        val body = try {
            client.newCall(Request.Builder().url(url).get().build()).execute().use { resp ->
                if (!resp.isSuccessful) throw IllegalStateException("HTTP ${resp.code}")
                resp.body?.string().orEmpty()
            }
        } catch (e: IOException) {
            throw IllegalStateException("网络请求失败: ${e.message}", e)
        }

        val parsed = json.decodeFromString<WalkingResponse>(body)
        if (parsed.status != "1") {
            throw IllegalStateException("高德返回错误: ${parsed.info ?: "未知"}")
        }
        val path = parsed.route?.paths?.firstOrNull()
            ?: throw IllegalStateException("未规划出步行路线")

        val points = path.steps.flatMap { step ->
            step.polyline.orEmpty()
                .split(";")
                .mapNotNull { pair ->
                    val xy = pair.split(",")
                    val lon = xy.getOrNull(0)?.toDoubleOrNull()
                    val lat = xy.getOrNull(1)?.toDoubleOrNull()
                    if (lon != null && lat != null) RoutePoint(lat, lon) else null
                }
        }

        WalkRoute(
            points = points,
            distanceMeters = path.distance?.toIntOrNull() ?: 0,
            durationSeconds = path.duration?.toIntOrNull() ?: 0,
        )
    }

    /* ----------------------------- DTO ----------------------------- */

    @Serializable
    private data class WalkingResponse(
        val status: String? = null,
        val info: String? = null,
        val route: RouteObj? = null,
    )

    @Serializable
    private data class RouteObj(val paths: List<PathObj> = emptyList())

    @Serializable
    private data class PathObj(
        val distance: String? = null,
        val duration: String? = null,
        val steps: List<StepObj> = emptyList(),
    )

    @Serializable
    private data class StepObj(val polyline: String? = null)
}
