package com.healthcard.model

import com.healthcard.model.internal.ChatCompletionRequest
import com.healthcard.model.internal.ChatCompletionResponse
import com.healthcard.model.internal.ErrorResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * 面向「OpenAI 兼容」接口（/chat/completions）的通用实现。
 * DeepSeek、Kimi、通义等大多兼容该协议，只需提供不同的 [ModelConfig] 即可复用。
 *
 * 非流式：一次性返回完整回复文本。网络请求在 [Dispatchers.IO] 上执行。
 */
open class OpenAiCompatibleProvider(
    protected val config: ModelConfig,
    override val name: String = "openai-compatible",
) : ModelProvider {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
    }

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(config.connectTimeoutSeconds, TimeUnit.SECONDS)
            .readTimeout(config.readTimeoutSeconds, TimeUnit.SECONDS)
            .build()
    }

    override suspend fun chat(
        messages: List<ChatMessage>,
        options: ChatOptions,
    ): String = withContext(Dispatchers.IO) {
        val payload = ChatCompletionRequest(
            model = options.model ?: config.defaultModel,
            messages = messages,
            temperature = options.temperature,
            maxTokens = options.maxTokens,
            stream = false,
        )

        val body = json.encodeToString(ChatCompletionRequest.serializer(), payload)
            .toRequestBody(JSON_MEDIA_TYPE)

        val request = Request.Builder()
            .url(config.baseUrl.trimEnd('/') + "/chat/completions")
            .addHeader("Authorization", "Bearer ${config.apiKey}")
            .addHeader("Content-Type", "application/json")
            .post(body)
            .build()

        val responseText: String = try {
            client.newCall(request).execute().use { response ->
                val text = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    throw ModelException(
                        message = parseErrorMessage(text) ?: "HTTP ${response.code}",
                        statusCode = response.code,
                    )
                }
                text
            }
        } catch (e: IOException) {
            throw ModelException("网络请求失败: ${e.message}", cause = e)
        }

        val parsed = try {
            json.decodeFromString(ChatCompletionResponse.serializer(), responseText)
        } catch (e: Exception) {
            throw ModelException("解析响应失败: ${e.message}", cause = e)
        }

        parsed.choices.firstOrNull()?.message?.content
            ?: throw ModelException("模型返回为空")
    }

    private fun parseErrorMessage(body: String): String? = try {
        json.decodeFromString(ErrorResponse.serializer(), body).error?.message
    } catch (_: Exception) {
        body.takeIf { it.isNotBlank() }
    }

    private companion object {
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
