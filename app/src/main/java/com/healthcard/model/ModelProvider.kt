package com.healthcard.model

/**
 * 大模型提供方统一抽象。新增模型（如通义、Kimi、OpenAI 等）时实现此接口即可，
 * 上层只依赖该接口，不感知具体厂商。
 */
interface ModelProvider {

    /** 提供方标识，便于日志与多模型切换，例如 "deepseek"。 */
    val name: String

    /**
     * 发起一次（非流式）对话补全，返回助手回复的完整文本。
     *
     * @throws ModelException 网络错误、鉴权失败或服务端返回异常时抛出。
     */
    suspend fun chat(
        messages: List<ChatMessage>,
        options: ChatOptions = ChatOptions(),
    ): String
}

/** 调用大模型过程中产生的异常。[statusCode] 为 HTTP 状态码（若有）。 */
class ModelException(
    message: String,
    val statusCode: Int? = null,
    cause: Throwable? = null,
) : Exception(message, cause)
