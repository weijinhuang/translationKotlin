package com.hwj.translation.busniness

import com.hwj.translation.bean.CommonResponse
import com.hwj.translation.bean.Language
import com.hwj.translation.bean.param.CommonParam
import com.hwj.translation.bean.param.QueryLanguageListParam
import com.hwj.translation.dao.TranslationDao

class LanguageRepository(translationDao: TranslationDao) : BaseRepository(translationDao) {


    fun getLanguageListV2(param: CommonParam<*>): CommonResponse<List<Language>> {
        val languageList = mutableListOf<Language>()
        parseRealParam(param, QueryLanguageListParam::class.java)?.let { realParam ->
            realParam.projectIds?.forEach { projectId ->
                val languageListOfProject = mTranslationDao.getLanguageList(projectId)
                if (languageListOfProject.isNotEmpty()) {
                    languageList.addAll(languageListOfProject)
                }
            }
            realParam.projectId?.let { projectId->
                val languageListOfProject = mTranslationDao.getLanguageList(projectId)
                if(languageListOfProject.isNotEmpty()){
                    languageList.addAll(languageListOfProject)
                }
            }
        }
        return CommonResponse(200, "", languageList)
    }


    fun deleteLanguageV2(param: CommonParam<*>): CommonResponse<Void> {
        return parseRealParam(param, Language::class.java)?.let { language ->
            if (language.languageId == null || language.projectId.isNullOrBlank()) {
                return CommonResponse(-1, "参数错误", null)
            }
            return try {
                //查询该语言下是否有翻译，有的话，先删除所有翻译。
                val translationCount = mTranslationDao.getTranslationCountOfLanguage(languageId = language.languageId!!, projectId = language.projectId!!)
                var deleteTranslationOfLanguage: Boolean = if (translationCount > 0) {
                    mTranslationDao.deleteTranslationByLanguageId(language.projectId!!, language.languageId!!)
                } else {
                    true
                }
                if (!deleteTranslationOfLanguage) {
                    CommonResponse(-1, "删除翻译错误", null)
                } else {
                    var deleteLanguageSuccess = mTranslationDao.deleteLanguage(language.languageId!!)
                    if (!deleteLanguageSuccess) {
                        CommonResponse(-1, "删除语言错误", null)
                    } else {
                        CommonResponse(200, "删除语言成功", null)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                CommonResponse(-1, e.message, null)
            }
        } ?: CommonResponse(-1, "参数错误", null)
    }


    fun addLanguagesV2(param: CommonParam<*>): CommonResponse<List<Language>> {
        return parseRealListPram(param, Language::class.java)?.let { languageList ->
            val resultList = mutableListOf<Language>()
            if (languageList.isNotEmpty()) {
                languageList.forEach { language ->
                    try {
                        val newLanguage = language.projectId?.let { projectId ->
                            language.languageName?.let { languageName ->
                                val queryLanguageList = mTranslationDao.queryLanguageByLanguageName(language.languageName!!, language.projectId!!)
                                if (!queryLanguageList.isNullOrEmpty()) {
                                    println("语言已存在")
                                    queryLanguageList[0]
                                } else {
                                    val success = mTranslationDao.addLanguage(
                                        language.languageDes!!, language.languageName!!, language.projectId!!
                                    )
                                    if (success) {
                                        println("添加成功")
                                        mTranslationDao.queryLanguageByLanguageName(languageName, projectId)?.get(0)
                                    } else {
                                        println("添加语言失败")
                                        return CommonResponse(-1, "添加语言失败", emptyList())
                                    }
                                }
                            }
                        }
                        newLanguage?.let { resultList.add(it) }
                    } catch (e: Exception) {
                        return CommonResponse(-1, e.message, emptyList())
                    }
                }
                if (resultList.size == languageList.size) {
                    CommonResponse(200, "", resultList)
                } else {
                    CommonResponse(-1, "添加语言失败", resultList)
                }
            }
            CommonResponse(200, "", resultList)
        } ?: CommonResponse(-1, "", emptyList())
    }

    fun updateLanguageV2(param: CommonParam<*>): CommonResponse<List<Language>> {

        return parseRealListPram(param, Language::class.java)?.let { languageList ->
            val resultList = mutableListOf<Language>()
            if (languageList.isNotEmpty()) {
                languageList.forEach { language ->
                    try {
                        language.languageId?.let { languageId ->
                            language.languageName?.let { languageName ->
                                val updateLanguageSuccess = mTranslationDao.updateLanguage2(
                                    languageId,
                                    languageName, language.languageDes ?: "", language.languageOrder ?: 0
                                )
                                if (updateLanguageSuccess) {
                                    resultList.add(language)
                                }
                                Unit
                            }
                        }
                    } catch (e: Exception) {
                        return CommonResponse(-1, e.message, emptyList())
                    }
                }
                CommonResponse(200, "", resultList)
            } else {
                CommonResponse(200, "", resultList)
            }
        } ?: CommonResponse(-1, "", emptyList())
    }
}