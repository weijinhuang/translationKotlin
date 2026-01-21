package com.hwj.translation.bean
data class ChatMessage(
    val role: String,  // "system", "user", "assistant"
    val content:String,
//    val content: List<Pair<String,String>>
)