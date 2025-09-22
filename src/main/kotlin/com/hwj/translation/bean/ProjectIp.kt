package com.hwj.translation.bean


class ProjectIp {
    var id: Int? = null
    var ip: String? = null
    var projectId: String? = null
    var updateTime: Int? = null

    override fun toString(): String {
        return "ProjectIp(id=$id, ip=$ip, projectId=$projectId, updateTime=$updateTime)"
    }
}