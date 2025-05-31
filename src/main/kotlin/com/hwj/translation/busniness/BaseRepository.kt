package com.hwj.translation.busniness

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.gson.reflect.TypeToken
import com.hwj.translation.bean.param.CommonParam
import com.hwj.translation.dao.TranslationDao
import java.lang.reflect.Type

open class BaseRepository(val mTranslationDao: TranslationDao) {

    private val mObjectMapper = ObjectMapper().apply {
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    }


    fun <PARAM> parseRealParam(param: CommonParam<*>, clazz: Class<PARAM>): PARAM? {
        return mObjectMapper.convertValue(param.data, clazz)
    }


    fun <PARAM> parseRealListPram(param: CommonParam<*>, clazz: Class<PARAM>): List<PARAM>? {
        val type = object : TypeReference<List<PARAM>>() {
            override fun getType(): Type {
                val typeToken = TypeToken.getParameterized(List::class.java, clazz)
                return typeToken.type
            }
        }

        val listResult = mObjectMapper.convertValue(param.data, type)
        return listResult
    }
}