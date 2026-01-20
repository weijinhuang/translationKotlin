package com.hwj.translation.bean

class TranslationEnginePreference {
    var ip: String? = null
    var engine: String? = null // "Google", "DeepSeek"
    var updateTime: Int? = null

    override fun toString(): String {
        return "TranslationEnginePreference(ip=$ip, engine=$engine, updateTime=$updateTime)"
    }
}
