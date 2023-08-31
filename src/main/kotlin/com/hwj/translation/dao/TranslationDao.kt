package com.hwj.translation.dao

import com.hwj.translation.bean.Language
import com.hwj.translation.bean.Project
import com.hwj.translation.bean.Translation

interface TranslationDao {

    fun getAllTranslationByProjectId(projectId: String): List<Translation>

    fun queryTranslationByKey(key: String, projectId: String, languageId: String): List<Translation>

    fun addTranslation(translation: Translation): Boolean

    fun updateTranslation(translation: Translation): Boolean

    fun deleteTranslationByKey(translationKey: String, projectId: String): Boolean

    fun addLanguage(languageId: String, languageName: String, projectId: String): Boolean

    fun getAllProject(): List<Project>

    fun addProject(project: Project): Boolean

    fun queryProjectByPackageName(projectId: String): Project?

    fun queryProjectsByPackageName(projectId: String): List<Project?>?

    fun queryLanguageByLanguageId(languageId: String, projectId: String): List<Language?>?

    fun getLanguageList(projectId: String): List<Language>

    fun deleteTranslationByLanguageId(projectId: String, languageId: String): Boolean

    fun deleteTranslationByTranslationKey(projectId: String, translationKey: String): Boolean

    fun deleteLanguage(language: Language): Boolean
}