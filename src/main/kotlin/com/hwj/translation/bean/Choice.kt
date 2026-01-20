package com.hwj.translation.bean

data class Choice(
    val index: Int,
    val message: ChatMessage,
    val finish_reason: String
)