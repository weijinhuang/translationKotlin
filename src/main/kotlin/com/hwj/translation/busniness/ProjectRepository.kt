package com.hwj.translation.busniness

import com.hwj.translation.bean.CommonResponse
import com.hwj.translation.bean.Project
import com.hwj.translation.bean.param.CommonParam
import com.hwj.translation.dao.TranslationDao

class ProjectRepository(translationDao: TranslationDao) : BaseRepository(translationDao) {

    fun getProjectsV2(): CommonResponse<List<Project>> {
        val projectList = mTranslationDao.getAllProject()
        return CommonResponse(200, null, projectList)
    }


    fun deleteProjectV2(commonParam: CommonParam<*>): CommonResponse<Void> {
        return try {
            parseRealParam(commonParam, Project::class.java)?.let { project ->
                if (project.projectId.isNullOrEmpty()) {
                    println("参数错误:projectId为空")
                    CommonResponse(-1, "参数错误：projectId为空", null)
                } else {
                    val result = mTranslationDao.deleteProject(project)
                    if (result) {
                        CommonResponse(200, "删除成功", null)
                    } else {
                        CommonResponse(-1, "删除失败", null)
                    }
                }
            } ?: CommonResponse(-1, "参数解析错误", null)

        } catch (e: Exception) {
            CommonResponse(-1, e.message, null)
        }
    }

    fun addProjectV2(commonParam: CommonParam<*>): CommonResponse<Void> {
        return parseRealParam(commonParam, Project::class.java)?.let { project ->
            return try {
                if (project.projectId.isNullOrBlank()) {
                    println("参数错误:projectId为空")
                    CommonResponse(-1, "参数错误：projectId为空", null)
                } else {
                    val queryProjectByPackageName = mTranslationDao.queryProjectsByProjectId(project.projectId!!)
                    if (null != queryProjectByPackageName && queryProjectByPackageName.isNotEmpty()) {
                        println("项目id已存在")
                        CommonResponse(-1, "项目id已存在", null)
                    } else {
                        val success = mTranslationDao.addProject(Project().apply {
                            this.projectName = project.projectName
                            this.projectId = project.projectId
                        })
                        if (success) {
                            println("添加成功")
                            project.copyFromProject?.let { targetProjectId ->
                                println("复制目标项目：${project.copyFromProject}")
                                val oldModules = mTranslationDao.getAllModules(targetProjectId)
                                var moduleId = 0
                                oldModules.forEach { module ->
                                    module.projectId = project.projectId
                                    mTranslationDao.addModule(module.moduleName, projectId = project.projectId!!)
                                }
                                moduleId = mTranslationDao.getAllModules(project.projectId!!).first().moduleId
                                val oldLanguages = mTranslationDao.getLanguageList(targetProjectId)
                                oldLanguages.forEach { oldLanguage ->

                                    mTranslationDao.addLanguage2(oldLanguage.languageDes!!, oldLanguage.languageName!!, project.projectId!!)?.let { addLanguage ->
                                        val oldTranslations = mTranslationDao.queryTranslationByLanguage(oldLanguage.languageId ?: 0, targetProjectId)
                                        println("复制语言：${addLanguage},该语言下翻译数:${oldTranslations.size}")
                                        oldTranslations.forEach { translation ->
                                            translation.translationId = null
                                            translation.moduleId = moduleId
                                            translation.projectId = project.projectId
                                            translation.languageId = addLanguage.languageId ?: 0

                                            val addTranslation = mTranslationDao.addTranslation(translation)
                                            if (addTranslation) {
                                                println("复制翻译:$translation 成功")
                                            } else {
                                                println("复制翻译:$translation 失败")
                                            }
                                        }
                                    }


                                }

                            }

                            CommonResponse(200, "添加成功", null)

                        } else {
                            println("添加项目失败")
                            CommonResponse(-1, "添加项目失败", null)
                        }
                    }
                }
            } catch (e: java.lang.Exception) {
                CommonResponse(-1, e.message, null)
            }
        } ?: CommonResponse(-1, "解析参数错误", null)
    }

}

