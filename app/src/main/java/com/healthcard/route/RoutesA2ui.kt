package com.healthcard.route

import com.healthcard.a2ui.A2uiMessage
import com.healthcard.a2ui.BeginRendering
import com.healthcard.a2ui.ComponentEntry
import com.healthcard.a2ui.SurfaceUpdate
import com.healthcard.amap.RunningRoute
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * 把结构化的 [RunningRoute] 列表转成 A2UI 消息，复用现有 A2uiSurface 渲染。
 * 每条路线一个 Card，底部按钮动作 "route:<index>" → 打开地图展示完整路线。
 */
object RoutesA2ui {

    fun build(routes: List<RunningRoute>, targetKm: Int): A2uiMessage {
        val components = mutableListOf<ComponentEntry>()
        fun add(id: String, type: String, props: JsonObject) {
            components.add(ComponentEntry(id, buildJsonObject { put(type, props) }))
        }

        val rootChildren = mutableListOf<String>()

        add("title", "Text", textProps("周边跑步路线（目标约 $targetKm 公里）", "h2"))
        rootChildren.add("title")

        routes.forEachIndexed { i, r ->
            val nameId = "r${i}_name"
            val distId = "r${i}_dist"
            val wpId = "r${i}_wp"
            val descId = "r${i}_desc"
            val btnId = "r${i}_btn"
            val cardId = "r${i}_card"

            add(nameId, "Text", textProps(r.name, "h3"))
            add(distId, "Text", textProps("距离：${r.distanceEstimate.ifBlank { "待地图测算" }}", null))
            add(wpId, "Text", textProps("途经：" + r.waypoints.joinToString(" → ") { it.name }, null))
            if (r.description.isNotBlank()) add(descId, "Text", textProps(r.description, null))
            add(btnId, "Button", buildJsonObject {
                put("label", JsonPrimitive("在高德地图查看完整路线"))
                put("action", buildJsonObject { put("name", JsonPrimitive("route:$i")) })
            })
            add(cardId, "Card", buildJsonObject {
                put("children", buildJsonArray {
                    add(JsonPrimitive(nameId))
                    add(JsonPrimitive(distId))
                    add(JsonPrimitive(wpId))
                    if (r.description.isNotBlank()) add(JsonPrimitive(descId))
                    add(JsonPrimitive(btnId))
                })
            })
            rootChildren.add(cardId)
        }

        add("root", "Column", buildJsonObject {
            put("children", buildJsonArray { rootChildren.forEach { add(JsonPrimitive(it)) } })
        })

        return A2uiMessage(
            surfaceUpdate = SurfaceUpdate(surfaceId = "routes", components = components),
            beginRendering = BeginRendering(surfaceId = "routes", root = "root"),
        )
    }

    private fun textProps(text: String, hint: String?): JsonObject = buildJsonObject {
        put("text", buildJsonObject { put("literalString", JsonPrimitive(text)) })
        if (hint != null) put("usageHint", JsonPrimitive(hint))
    }
}
