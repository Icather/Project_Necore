package icather.pages.dev.api.plugin

import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName

data class ProtocolPluginJson(
    @SerializedName("provider_info") val providerInfo: ProviderInfo? = null,
    @SerializedName("feature_reasoning") val featureReasoning: FeatureReasoning? = null,
    @SerializedName("feature_cache") val featureCache: FeatureCache? = null,
    @SerializedName("feature_vision") val featureVision: FeatureVision? = null,
    @SerializedName("feature_tools") val featureTools: FeatureTools? = null,
    @SerializedName("feature_structured_output") val featureStructuredOutput: FeatureStructuredOutput? = null,
    @SerializedName("feature_roles") val featureRoles: FeatureRoles? = null,
    @SerializedName("feature_streaming") val featureStreaming: FeatureStreaming? = null,
    @SerializedName("billing_metadata") val billingMetadata: BillingMetadata? = null,
    val constraints: Constraints? = null
)

data class ProviderInfo(
    val id: String,
    @SerializedName("display_name") val displayName: String,
    @SerializedName("base_url") val baseUrl: String,
    @SerializedName("is_openai_compatible") val isOpenAiCompatible: Boolean
)

data class FeatureReasoning(
    val supported: Boolean = false,
    @SerializedName("trigger_type") val triggerType: String? = null,
    @SerializedName("trigger_payload") val triggerPayload: JsonObject? = null,
    @SerializedName("response_field") val responseField: String? = null,
    @SerializedName("allows_temperature") val allowsTemperature: Boolean = true
)

data class FeatureCache(
    val supported: Boolean = false,
    val strategy: String? = null,
    @SerializedName("explicit_max_breakpoints") val explicitMaxBreakpoints: Int? = null,
    @SerializedName("explicit_tag_format") val explicitTagFormat: JsonObject? = null,
    @SerializedName("minimum_tokens_to_trigger") val minimumTokensToTrigger: Int? = null
)

data class FeatureVision(
    val supported: Boolean = false,
    @SerializedName("max_images_per_request") val maxImagesPerRequest: Int? = null,
    @SerializedName("detail_control_supported") val detailControlSupported: Boolean = false,
    @SerializedName("input_format") val inputFormat: String? = null
)

data class FeatureTools(
    val supported: Boolean = false,
    @SerializedName("strict_mode_supported") val strictModeSupported: Boolean = false,
    @SerializedName("parallel_tool_calls_supported") val parallelToolCallsSupported: Boolean = false
)

data class FeatureStructuredOutput(
    @SerializedName("json_mode_supported") val jsonModeSupported: Boolean = false,
    @SerializedName("requires_json_keyword_in_prompt") val requiresJsonKeywordInPrompt: Boolean = false
)

data class FeatureRoles(
    @SerializedName("system_role_name") val systemRoleName: String = "system",
    @SerializedName("supports_system_role") val supportsSystemRole: Boolean = true
)

data class FeatureStreaming(
    val supported: Boolean = true
)

data class BillingMetadata(
    @SerializedName("input_price_per_1m") val inputPricePer1m: Double = 0.0,
    @SerializedName("output_price_per_1m") val outputPricePer1m: Double = 0.0,
    @SerializedName("cache_hit_price_per_1m") val cacheHitPricePer1m: Double = 0.0
)

data class Constraints(
    @SerializedName("max_input_tokens") val maxInputTokens: Int = 8192,
    @SerializedName("max_output_tokens") val maxOutputTokens: Int = 4096,
    @SerializedName("requests_per_minute_limit") val requestsPerMinuteLimit: Int? = null,
    @SerializedName("context_window_safe_threshold") val contextWindowSafeThreshold: Double = 0.95
)
