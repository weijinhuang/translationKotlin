package com.hwj.translation.bean


data class DeepSeekTranslationResult(
    val id: String,
    val choices: List<Choice>,
    val created: Long,
    val model: String,
    val usage: Usage
)