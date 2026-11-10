package com.healthcard.model

/**
 * DeepSeek 模型连接实现。DeepSeek 接口与 OpenAI 兼容，故直接复用
 * [OpenAiCompatibleProvider]，仅预置默认地址与模型名。
 *
 * 用法：
 * ```
 * val provider = DeepSeekProvider(apiKey = "sk-xxx")
 * val reply = provider.chat(listOf(ChatMessage.user("你好")))
 * ```
 */
class DeepSeekProvider(
    apiKey: String,
    model: String = DeepSeekModels.CHAT,
    baseUrl: String = DEFAULT_BASE_URL,
    readTimeoutSeconds: Long = 60,
) : OpenAiCompatibleProvider(
    config = ModelConfig(
        apiKey = apiKey,
        baseUrl = baseUrl,
        defaultModel = model,
        readTimeoutSeconds = readTimeoutSeconds,
    ),
    name = "deepseek",
) {
    companion object {
        /** DeepSeek 开放平台地址。 */
        const val DEFAULT_BASE_URL = "https://api.deepseek.com"
    }
}

/** DeepSeek 可用模型名。 */
object DeepSeekModels {
    /** DeepSeek-V3，通用对话。 */
    const val CHAT = "deepseek-chat"

    /** DeepSeek-R1，带推理链。响应较慢，建议调大读取超时。 */
    const val REASONER = "deepseek-reasoner"
}
