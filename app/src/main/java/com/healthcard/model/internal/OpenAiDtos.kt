package com.healthcard.model.internal

import com.healthcard.model.ChatMessage
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * OpenAI 兼容协议（DeepSeek 亦遵循）的请求/响应 DTO。仅模块内部使用。
 */
@Serializable
internal data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val temperature: Double? = null,
    @SerialName("max_tokens") val maxTokens: Int? = null,
    val stream: Boolean = false,
)

@Serializable
internal data class ChatCompletionResponse(
    val id: String? = null,
    val model: String? = null,
    val choices: List<Choice> = emptyList(),
    val usage: Usage? = null,
)

@Serializable
internal data class Choice(
    val index: Int = 0,
    val message: ChatMessage? = null,
    @SerialName("finish_reason") val finishReason: String? = null,
)

@Serializable
internal data class Usage(
    @SerialName("prompt_tokens") val promptTokens: Int = 0,
    @SerialName("completion_tokens") val completionTokens: Int = 0,
    @SerialName("total_tokens") val totalTokens: Int = 0,
)

/** 服务端错误响应：{ "error": { "message": ..., "type": ..., "code": ... } } */
@Serializable
internal data class ErrorResponse(
    val error: ErrorBody? = null,
)

@Serializable
internal data class ErrorBody(
    val message: String? = null,
    val type: String? = null,
    val code: String? = null,
)
