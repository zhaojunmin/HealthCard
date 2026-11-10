package com.healthcard.model

/**
 * 大模型连接配置。
 *
 * @param apiKey   访问密钥（Bearer Token）。
 * @param baseUrl  服务根地址，如 DeepSeek 的 "https://api.deepseek.com"。
 * @param defaultModel 默认模型名，可在单次调用中通过 [ChatOptions.model] 覆盖。
 * @param connectTimeoutSeconds 连接超时（秒）。
 * @param readTimeoutSeconds    读取超时（秒），推理类模型耗时较长可适当调大。
 */
data class ModelConfig(
    val apiKey: String,
    val baseUrl: String,
    val defaultModel: String,
    val connectTimeoutSeconds: Long = 30,
    val readTimeoutSeconds: Long = 60,
)

/**
 * 单次对话的可选参数。为空的字段不会写入请求体，由服务端取默认值。
 *
 * @param model       覆盖 [ModelConfig.defaultModel]。
 * @param temperature 采样温度。
 * @param maxTokens   最大生成 token 数。
 */
data class ChatOptions(
    val model: String? = null,
    val temperature: Double? = null,
    val maxTokens: Int? = null,
)
