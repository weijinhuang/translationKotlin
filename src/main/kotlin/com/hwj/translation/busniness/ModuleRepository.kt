package com.hwj.translation.busniness

import com.hwj.translation.bean.CommonResponse
import com.hwj.translation.bean.Module
import com.hwj.translation.bean.param.CommonParam
import com.hwj.translation.dao.TranslationDao

class ModuleRepository(translationDao: TranslationDao) : BaseRepository(translationDao) {

    fun getAllModulesV2(commonParam: CommonParam<*>): CommonResponse<List<Module>> {
        return parseRealParam(commonParam, String::class.java)?.let { projectId ->
            var moduleList = mTranslationDao.getAllModules(projectId)
            if (moduleList.size == 0) {
                mTranslationDao.addModule("default", projectId)
            }
            moduleList = mTranslationDao.getAllModules(projectId)
            CommonResponse(200, null, moduleList)
        } ?: CommonResponse(-1, "解析参数出错", emptyList())

    }


    fun addModuleV2(commonParam: CommonParam<*>): CommonResponse<Void> {
        return parseRealParam(commonParam, Module::class.java)?.let { module ->
            if (module.moduleName.isNullOrEmpty() || module.projectId.isNullOrEmpty()) {
                return CommonResponse(-1, "參數錯誤", null)
            }
            val queryModuleByName = mTranslationDao.queryModuleByName(module.moduleName!!, module.projectId!!)
            if (queryModuleByName.isNotEmpty()) {
                return CommonResponse(-1, "已有相同模块", null)
            }
            mTranslationDao.addModule(module.moduleName!!, module.projectId!!)
            CommonResponse(200, "", null)
        } ?: CommonResponse(-1, "参数解析出错", null)

    }

    fun deleteModuleV2(commonParam: CommonParam<*>): CommonResponse<Void> {
        return parseRealParam(commonParam, Module::class.java)?.let { module ->
            if (module.moduleName.isNullOrEmpty() || module.projectId.isNullOrEmpty()) {
                return CommonResponse(-1, "參數錯誤", null)
            }
            return try {
                mTranslationDao.deleteModule(module.moduleId!!, module.projectId!!)
                return CommonResponse(200, "", null)
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
                return CommonResponse(-1, e.message, null)
            }
        } ?: CommonResponse(-1, "参数解析出错", null)

    }

}