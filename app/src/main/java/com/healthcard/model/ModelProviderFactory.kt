package com.healthcard.model

/** 支持的大模型类型。新增厂商时在此登记，并在 [ModelProviderFactory.create] 中补充分支。 */
enum class ModelType {
    DEEPSEEK,

    /** 其它 OpenAI 兼容服务（需在 [ModelConfig] 中自行指定 baseUrl/model）。 */
    OPENAI_COMPATIBLE,
}

/**
 * 统一的提供方工厂，屏蔽各厂商差异。
 */
object ModelProviderFactory {

    /** 按配置创建提供方。 */
    fun create(type: ModelType, config: ModelConfig): ModelProvider = when (type) {
        ModelType.DEEPSEEK ->
            OpenAiCompatibleProvider(config, name = "deepseek")
        ModelType.OPENAI_COMPATIBLE ->
            OpenAiCompatibleProvider(config)
    }

    /** 便捷创建 DeepSeek 提供方。 */
    fun deepSeek(
        apiKey: String,
        model: String = DeepSeekModels.CHAT,
    ): ModelProvider = DeepSeekProvider(apiKey = apiKey, model = model)
}
