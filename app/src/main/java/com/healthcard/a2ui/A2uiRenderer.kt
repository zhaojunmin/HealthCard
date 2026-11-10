package com.healthcard.a2ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * 把一条 [A2uiMessage] 渲染为原生 Compose UI。
 *
 * 实现 A2UI 协议的最小子集：从 [BeginRendering.root] 出发，按 id 索引递归渲染
 * Text / Column / Row / Card / List / Button / Divider。未知组件安全跳过。
 *
 * @param onAction Button 点击时回传其 `action.name`。
 */
@Composable
fun A2uiSurface(
    message: A2uiMessage,
    modifier: Modifier = Modifier,
    onAction: (String) -> Unit = {},
) {
    val components = message.surfaceUpdate?.components ?: emptyList()
    val index = rememberIndex(components)
    val rootId = message.beginRendering?.root ?: components.firstOrNull()?.id

    Column(modifier = modifier) {
        if (rootId != null) {
            RenderNode(rootId, index, onAction)
        } else {
            Text("空的 A2UI 内容")
        }
    }
}

@Composable
private fun RenderNode(
    id: String,
    index: Map<String, ComponentEntry>,
    onAction: (String) -> Unit,
) {
    val entry = index[id] ?: return
    val type = entry.component.keys.firstOrNull() ?: return
    val props = entry.component[type]?.let { it as? JsonObject } ?: JsonObject(emptyMap())

    when (type) {
        "Text" -> RenderText(props)
        "Column" -> Column(modifier = Modifier.fillMaxWidth()) {
            props.childIds().forEach { RenderNode(it, index, onAction) }
        }
        "Row" -> Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            props.childIds().forEach { RenderNode(it, index, onAction) }
        }
        "List" -> Column(modifier = Modifier.fillMaxWidth()) {
            props.childIds().forEach { RenderNode(it, index, onAction) }
        }
        "Card" -> Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                props.childIds().forEach { RenderNode(it, index, onAction) }
            }
        }
        "Button" -> {
            val label = props.childIds().firstOrNull()?.let { childId ->
                index[childId]?.textContent()
            } ?: props.stringField("label") ?: "确定"
            val action = (props["action"] as? JsonObject)?.stringField("name").orEmpty()
            Button(onClick = { onAction(action) }) { Text(label) }
        }
        "Divider" -> HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
        else -> Unit // 未知组件：安全跳过
    }
}

@Composable
private fun RenderText(props: JsonObject) {
    val text = props.textValue()
    val (size, weight) = when (props.stringField("usageHint")?.lowercase()) {
        "h1" -> 24.sp to FontWeight.Bold
        "h2" -> 20.sp to FontWeight.Bold
        "h3", "title" -> 18.sp to FontWeight.SemiBold
        "caption" -> 12.sp to FontWeight.Normal
        else -> 15.sp to FontWeight.Normal
    }
    Text(
        text = text,
        fontSize = size,
        fontWeight = weight,
        modifier = Modifier.padding(vertical = 2.dp),
    )
}

/* ----------------------------- JSON 辅助 ----------------------------- */

@Composable
private fun rememberIndex(components: List<ComponentEntry>): Map<String, ComponentEntry> =
    androidx.compose.runtime.remember(components) { components.associateBy { it.id } }

/** 读取 "children":[ids] 或单个 "child":id。 */
private fun JsonObject.childIds(): List<String> {
    (this["children"] as? JsonArray)?.let { arr ->
        return arr.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
    }
    stringField("child")?.let { return listOf(it) }
    return emptyList()
}

/** 取字符串字段（JsonPrimitive）。 */
private fun JsonObject.stringField(key: String): String? =
    (this[key] as? JsonPrimitive)?.contentOrNull

/** 解析 Text 组件的文本：支持 {"text":{"literalString":".."}} 或 {"text":".."}。 */
private fun JsonObject.textValue(): String {
    val node = this["text"] ?: return stringField("content") ?: ""
    return when (node) {
        is JsonObject -> node.stringField("literalString") ?: node.stringField("value") ?: ""
        is JsonPrimitive -> node.contentOrNull ?: ""
        else -> ""
    }
}

/** 若组件是 Text，返回其文本，用于 Button 子节点作为按钮文案。 */
private fun ComponentEntry.textContent(): String? {
    val props = component["Text"]?.let { it as? JsonObject } ?: return null
    return props.textValue()
}
