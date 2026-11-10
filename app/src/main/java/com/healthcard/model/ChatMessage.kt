package com.healthcard.model

import kotlinx.serialization.Serializable

/**
 * 一条对话消息。字段与 OpenAI / DeepSeek 的 chat/completions 协议一致，
 * 因此可直接序列化进请求体。
 */
@Serializable
data class ChatMessage(
    val role: String,
    val content: String,
) {
    companion object {
        fun system(content: String) = ChatMessage(Role.SYSTEM, content)
        fun user(content: String) = ChatMessage(Role.USER, content)
        fun assistant(content: String) = ChatMessage(Role.ASSISTANT, content)
    }
}

/** 消息角色常量。 */
object Role {
    const val SYSTEM = "system"
    const val USER = "user"
    const val ASSISTANT = "assistant"
}
