package com.hwj.translation.bean

class Project() {

    var projectId: String? = null

    var projectName: String? = null

    var translationCount: Int = 0

    var copyFromProject: String? = null
    override fun toString(): String {
        return "Project(package_name=$projectId, project_name=$projectName)"
    }


}