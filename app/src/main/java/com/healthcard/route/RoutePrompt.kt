package com.healthcard.route

import com.healthcard.amap.Poi
import com.healthcard.model.ChatMessage

/**
 * 构造「周边跑步路线规划」的提示词。采用接地式(grounding)：只给大模型真实 POI 列表，
 * 要求它从中挑选途经点组成环线，输出结构化 JSON（不输出坐标、不编造地点）。
 */
object RoutePrompt {

    private val SYSTEM = """
        你是一个专业的跑步路线规划助手。我会给你用户当前位置、目标距离、场景偏好，以及一份
        【真实 POI 列表】（每项含 id、名称、类型、距起点米数）。

        规划要求：
        - 只能从给定 POI 列表中选择途经点，用它们的 id 引用；严禁编造不在列表中的地点或输出坐标。
        - 每条路线是一条环线：起点=用户当前位置，依次经过 2~4 个 POI，最后回到起点。
        - 尽量让总距离接近目标距离；优先选择贴合场景偏好、彼此顺路、适合跑步的 POI（公园/绿道/滨河等）。
        - 给出 2~3 条不同风格的路线，并简述每条的特点（风景/路况/难度）。

        你必须且只能输出一个 JSON 对象（不要解释文字，不要 ```json 围栏），结构如下：
        {
          "routes": [
            {
              "name": "路线名称",
              "description": "一句话特点描述",
              "distanceEstimate": "约 X 公里",
              "waypointIds": ["<poiId>", "<poiId>", ...]
            }
          ]
        }
        waypointIds 必须全部来自给定 POI 列表的 id。
    """.trimIndent()

    fun buildMessages(
        placeName: String?,
        pois: List<Poi>,
        targetKm: Int,
        preference: String,
    ): List<ChatMessage> {
        val where = placeName?.takeIf { it.isNotBlank() } ?: "当前位置"
        val poiLines = pois.joinToString("\n") { p ->
            "${p.id} | ${p.name} | ${p.typeName.ifBlank { "地点" }} | 距起点约${p.distanceMeters}米"
        }
        val user = buildString {
            append("用户当前位置：$where。\n")
            append("目标距离：约 $targetKm 公里。\n")
            append("场景偏好：$preference。\n")
            append("【真实 POI 列表】\n")
            append(poiLines)
            append("\n请据此规划 2~3 条环线跑步路线，按要求的 JSON 结构返回。")
        }
        return listOf(ChatMessage.system(SYSTEM), ChatMessage.user(user))
    }

    /** 从模型回复中提取纯 JSON：去掉可能的 ```json 围栏，并截取首个 '{' 到末个 '}'。 */
    fun extractJson(raw: String): String {
        var s = raw.trim()
        if (s.startsWith("```")) {
            s = s.substringAfter('\n', s).substringBeforeLast("```").trim()
        }
        val start = s.indexOf('{')
        val end = s.lastIndexOf('}')
        return if (start in 0 until end) s.substring(start, end + 1) else s
    }
}
