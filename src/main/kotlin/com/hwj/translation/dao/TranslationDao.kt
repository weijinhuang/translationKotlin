package com.hwj.translation.dao

import com.hwj.translation.bean.Language
import com.hwj.translation.bean.Project
import com.hwj.translation.bean.Translation

interface TranslationDao {


    fun getAllProject(): List<Project>

    fun deleteProject(project: Project): Boolean
    fun addProject(project: Project): Boolean

    fun queryProjectsByProjectId(projectId: String): List<Project?>?


    fun addLanguage(languageDes: String, languageName: String, projectId: String): Boolean
    fun addLanguage2(languageDes: String, languageName: String, projectId: String): Language?
    fun updateLanguage2(languageId:Int, languageName: String, languageDes: String, order: Int = 0): Boolean

    fun deleteLanguage(languageId: Int): Boolean
    fun deleteLanguageByProjectId(projectId: String): Boolean

    fun queryLanguageByLanguageName(languageName: String, projectId: String): List<Language?>?

    fun getLanguageList(projectId: String): List<Language>


    fun queryTranslationByLanguage(languageId: Int, projectId: String): List<Translation>
    fun queryTranslationByLanguageWithHide(languageId: Int, projectId: String): List<Translation>
    fun getAllTranslationByProjectId(projectId: String): List<Translation>

    fun queryTranslationByKeyInLanguage(key: String, projectId: String, languageId: Int): List<Translation>
    fun queryTranslationByKey(key: String, projectId: String): List<Translation>
    fun queryTranslationByModule(moduleId: Int, projectId: String): List<Translation>

    fun addTranslation(translation: Translation): Boolean

    fun updateTranslation(translation: Translation): Boolean

    fun deleteTranslationByKey(translationKey: String, projectId: String): Boolean

    fun deleteTranslationByLanguageId(projectId: String, languageId: Int): Boolean

    fun deleteTranslationByTranslationKey(projectId: String, translationKey: String): Boolean

    fun deleteTranslationByProjectId(projectId: String): Boolean

    fun deleteTranslationByTranslationId(translationId: Int): Boolean

    /**Module*/
    fun addModule(moduleName: String, projectId: String): Boolean

    fun deleteModule(moduleId: Int?, projectId: String): Boolean

    fun getAllModules(projectId: String): List<com.hwj.translation.bean.Module>

    fun queryModuleByName(moduleName: String, projectId: String): List<com.hwj.translation.bean.Module>
    fun queryModuleById(moduleId: Int, projectId: String): List<com.hwj.translation.bean.Module>

}