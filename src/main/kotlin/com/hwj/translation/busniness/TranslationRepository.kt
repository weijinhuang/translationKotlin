package com.hwj.translation.busniness

import com.hwj.translation.bean.CommonResponse
import com.hwj.translation.bean.Module
import com.hwj.translation.bean.Translation
import com.hwj.translation.bean.param.CommonParam
import com.hwj.translation.bean.param.DeleteTranslationParam
import com.hwj.translation.bean.param.GetTranslationParam
import com.hwj.translation.dao.TranslationDao

class TranslationRepository(translationDao: TranslationDao) : BaseRepository(translationDao) {


    fun getTranslationListV2(param: CommonParam<*>): CommonResponse<List<Translation>> {
        return parseRealParam(param, GetTranslationParam::class.java)?.let { realParam ->
            val translationList = if (null == realParam.moduleId) {
                mTranslationDao.getAllTranslationByProjectId(realParam.projectId)
            } else {
                mTranslationDao.queryTranslationByModule(realParam.moduleId!!, realParam.projectId)
            }
            println("查詢到翻譯：${translationList.size}")
            CommonResponse(200, "", translationList)
        } ?: CommonResponse(-1, "参数错误", emptyList())
    }


    fun deleteTranslationByTranslationKeyV2(param: CommonParam<*>): CommonResponse<Void> {
        return parseRealParam(param, DeleteTranslationParam::class.java)?.let { deleteTranslationParam ->
            if (deleteTranslationParam.translationKey.isNullOrBlank() || deleteTranslationParam.projectId.isNullOrBlank()) {
                return CommonResponse(-1, "参数错误", null)
            }
            return try {
                var deleteTranslationSuccess = mTranslationDao.deleteTranslationByTranslationKey(
                    deleteTranslationParam.projectId!!, deleteTranslationParam.translationKey!!
                )
                if (!deleteTranslationSuccess) {
                    CommonResponse(-1, "删除翻译错误", null)
                } else {
                    CommonResponse(200, "删除翻译成功", null)
                }
            } catch (e: Exception) {
                CommonResponse(-1, e.message, null)
            }
        } ?: CommonResponse(-1, "参数错误", null)

    }


    fun addTranslationsV2(commonParam: CommonParam<*>): CommonResponse<List<Translation>> {
        return parseRealListPram(commonParam, Translation::class.java)?.let { translationList ->
            val failedList = mutableListOf<Translation>()
            return try {
                translationList.forEach { translation ->
                    translation.projectId?.let { projectId ->
                        translation.languageId?.let { languageId ->
                            translation.translationKey?.let { translationKey ->
                                val translationDB = mTranslationDao.queryTranslationByKeyInLanguage(translationKey, projectId, languageId)
                                if (translationDB.isNotEmpty()) {
                                    if (translation.forceAdd) {
                                        translation.translationId = translationDB[0].translationId
                                        var updateSuccess = mTranslationDao.updateTranslation(translation)
                                        if (!updateSuccess) {
                                            failedList.add(translation)
                                        }
                                    } else {
                                        print(" ${translation.translationKey} 已存在")
                                        if (translation.translationContent != translationDB[0].translationContent) {
                                            translation.oldTranslationContent = translationDB[0].translationContent
                                            failedList.add(translation)
                                        }
                                    }
                                } else {
                                    val module = getModule(translation, projectId) ?: return CommonResponse(-1, "添加模块失败", null)
                                    translation.moduleId = module.moduleId
                                    val success = mTranslationDao.addTranslation(translation)
                                    if (!success) {
                                        print(" ${translation.translationKey} 添加失败, content:${translation.translationContent}")
                                        failedList.add(translation)
                                    } else {
                                        print(" ${translation.translationKey} 添加成功, content:${translation.translationContent}")
                                    }
                                }
                            }
                        }
                    }
                }
                CommonResponse(200, "success", failedList)
            } catch (e: Exception) {
                CommonResponse(-1, e.message, emptyList())
            }
        } ?: CommonResponse(-1, "参数解析错误", emptyList())
    }

    fun updateTranslationsV2(commonParam: CommonParam<*>): CommonResponse<List<Translation>> {

        return parseRealListPram(commonParam, Translation::class.java)?.let { translationList ->
            if (translationList.isNullOrEmpty()) {
                CommonResponse(200, "", emptyList())
            } else {
                val failedList = mutableListOf<Translation>()
                try {
                    translationList.forEach {
                        val success = mTranslationDao.updateTranslation(it)
                        print("更新结果：$success $it")
                        if (!success) {
                            failedList.add(it)
                        }
                    }
                    CommonResponse(200, "", failedList)
                } catch (e: Exception) {
                    e.printStackTrace()
                    CommonResponse(200, e.message, failedList)
                }
            }

        } ?: CommonResponse(-1, "解析參數錯誤", emptyList())

    }

    val moduleCaches = HashMap<Int, Module>()

    fun getModule(translation: Translation, projectId: String): Module? {
        var module = moduleCaches[translation.moduleId]
        if (module == null) {
            var moduleDB = mTranslationDao.queryModuleById(translation.moduleId ?: 0, projectId)
            if (moduleDB.isEmpty()) {
                module = Module()
                module.moduleName = ""
                module.projectId = projectId
                var addModuleResult = mTranslationDao.addModule(module.moduleName, module.projectId!!)
                if (!addModuleResult) {
                    return null
                }
            } else {
                module = moduleDB[0]
                moduleCaches.put(translation.moduleId ?: 0, module)
            }
        }
        return module
    }
}