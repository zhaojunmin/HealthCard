package com.healthcard.route

import kotlinx.serialization.Serializable

/** DeepSeek 接地式输出：只引用真实 POI id 组成的跑步环线。 */
@Serializable
data class RoutePlanResponse(
    val routes: List<RoutePlanItem> = emptyList(),
)

@Serializable
data class RoutePlanItem(
    val name: String = "跑步路线",
    val description: String = "",
    val distanceEstimate: String = "",
    val waypointIds: List<String> = emptyList(),
)
