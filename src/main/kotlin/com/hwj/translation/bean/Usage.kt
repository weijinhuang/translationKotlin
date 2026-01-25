package com.hwj.translation.bean


data class Usage(
    val prompt_tokens: Int,
    val completion_tokens: Int,
    val total_tokens: Int,
    val prompt_tokens_details: PromptTokensDetails? = null,
    val prompt_cache_hit_tokens: Int? = null,
    val prompt_cache_miss_tokens: Int? = null
)
