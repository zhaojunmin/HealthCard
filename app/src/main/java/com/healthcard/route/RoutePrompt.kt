package com.healthcard.route

import com.healthcard.location.GeoPoint
import com.healthcard.model.ChatMessage

/**
 * 构造「周边跑步路线规划」的提示词，并约定大模型以 A2UI JSON 子集输出，
 * 使其可被 [com.healthcard.a2ui.A2uiSurface] 直接渲染。
 */
object RoutePrompt {

    private val SYSTEM = """
        你是一个跑步路线规划助手。根据用户提供的经纬度，推荐 3 条周边适合跑步的路线。
        你必须且只能输出一个 JSON 对象（不要任何解释文字，不要使用 ```json 代码块围栏），
        且严格遵循下面的 A2UI 子集结构：

        {
          "surfaceUpdate": {
            "surfaceId": "routes",
            "components": [
              { "id": "<唯一id>", "component": { "<类型>": { ...属性 } } }
            ]
          },
          "beginRendering": { "surfaceId": "routes", "root": "<根组件id>" }
        }

        可用组件类型与属性：
        - Text：  { "text": { "literalString": "文字" }, "usageHint": "h1|h2|h3|body|caption" }
        - Column：{ "children": ["<子id>", ...] }   纵向排列
        - Row：   { "children": ["<子id>", ...] }   横向排列
        - Card：  { "children": ["<子id>", ...] }   卡片容器
        - List：  { "children": ["<子id>", ...] }   列表容器
        - Divider：{}                               分隔线

        约定：根组件用一个 Column，内含标题 Text 和每条路线一个 Card。
        每条路线的 Card 内至少包含：路线名称(Text,h3)、距离与预估时长(Text)、路线描述与途经点(Text)。
        所有 id 必须唯一，children 引用的 id 必须在 components 中存在。
    """.trimIndent()

    fun buildMessages(point: GeoPoint): List<ChatMessage> = listOf(
        ChatMessage.system(SYSTEM),
        ChatMessage.user(
            "我的当前位置经纬度：纬度 ${point.latitude}，经度 ${point.longitude}。" +
                "请推荐 3 条周边跑步路线，按上述 A2UI JSON 结构返回。"
        ),
    )

    /**
     * 从模型回复中提取纯 JSON：去掉可能的 ```json 围栏，并截取首个 '{' 到末个 '}'。
     */
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
