package com.healthcard.a2ui

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * A2UI（Agent-to-UI）声明式 UI 协议的最小子集数据模型。
 *
 * 一条消息包含组件定义（[surfaceUpdate]）与渲染根（[beginRendering]）。
 * 组件内容用动态 [JsonObject] 承载，以容忍大模型输出的细节差异，
 * 由 [A2uiSurface] 在渲染时按组件类型解释。
 */
@Serializable
data class A2uiMessage(
    val surfaceUpdate: SurfaceUpdate? = null,
    val beginRendering: BeginRendering? = null,
)

@Serializable
data class SurfaceUpdate(
    val surfaceId: String = "",
    val components: List<ComponentEntry> = emptyList(),
)

/**
 * 单个组件条目。[component] 形如 `{"Text": { ... }}`，其唯一键即组件类型，
 * 值为该组件的属性对象。
 */
@Serializable
data class ComponentEntry(
    val id: String,
    val component: JsonObject,
)

@Serializable
data class BeginRendering(
    val surfaceId: String = "",
    val root: String = "",
)
