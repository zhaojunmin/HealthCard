package com.healthcard.amap

import com.healthcard.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

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

    /**
     * 周边 POI 搜索（高德 v5/place/around），返回真实可跑地点（GCJ-02）。
     * @param center 搜索中心（GCJ-02）。
     * @param keywords 以 "|" 分隔的关键词，如 "公园|绿道|体育场"。
     */
    suspend fun searchRunningPois(
        center: RoutePoint,
        radiusMeters: Int,
        keywords: String,
    ): List<Poi> = withContext(Dispatchers.IO) {
        val key = BuildConfig.AMAP_WEB_KEY
        check(key.isNotBlank()) { "未配置高德 Web 服务 Key（local.properties: AMAP_WEB_KEY）" }

        val url = "https://restapi.amap.com/v5/place/around" +
            "?location=${center.lon},${center.lat}" +
            "&radius=$radiusMeters" +
            "&keywords=${URLEncoder.encode(keywords, "UTF-8")}" +
            "&sortrule=distance&page_size=25&page_num=1" +
            "&key=$key"

        val body = try {
            client.newCall(Request.Builder().url(url).get().build()).execute().use { resp ->
                if (!resp.isSuccessful) throw IllegalStateException("HTTP ${resp.code}")
                resp.body?.string().orEmpty()
            }
        } catch (e: IOException) {
            throw IllegalStateException("网络请求失败: ${e.message}", e)
        }

        val parsed = json.decodeFromString<PlaceResponse>(body)
        if (parsed.status != "1") {
            throw IllegalStateException("高德返回错误: ${parsed.info ?: "未知"}")
        }

        parsed.pois.mapNotNull { poi ->
            val loc = poi.location?.split(",")
            val lon = loc?.getOrNull(0)?.toDoubleOrNull()
            val lat = loc?.getOrNull(1)?.toDoubleOrNull()
            if (lon == null || lat == null || poi.id == null || poi.name == null) return@mapNotNull null
            val dist = poi.distance?.toIntOrNull()
                ?: haversineMeters(center.lat, center.lon, lat, lon)
            Poi(
                id = poi.id,
                name = poi.name,
                lat = lat,
                lon = lon,
                distanceMeters = dist,
                typeName = poi.type?.substringAfterLast(';').orEmpty(),
            )
        }
    }

    private fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Int {
        val r = 6_371_000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2) * sin(dLon / 2)
        return (r * 2 * atan2(sqrt(a), sqrt(1 - a))).toInt()
    }

    /* ----------------------------- DTO ----------------------------- */

    @Serializable
    private data class PlaceResponse(
        val status: String? = null,
        val info: String? = null,
        val pois: List<PoiDto> = emptyList(),
    )

    @Serializable
    private data class PoiDto(
        val id: String? = null,
        val name: String? = null,
        val location: String? = null,
        val type: String? = null,
        val distance: String? = null,
    )

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
