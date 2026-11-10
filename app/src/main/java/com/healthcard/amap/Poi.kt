package com.healthcard.amap

/** 一个真实兴趣点（高德周边搜索返回，GCJ-02 坐标）。 */
data class Poi(
    val id: String,
    val name: String,
    val lat: Double,
    val lon: Double,
    val distanceMeters: Int,
    val typeName: String,
)

/** 一条由真实 POI 组成的跑步环线（几何在打开地图时逐段规划）。 */
data class RunningRoute(
    val name: String,
    val description: String,
    val distanceEstimate: String,
    val waypoints: List<Poi>,
)
