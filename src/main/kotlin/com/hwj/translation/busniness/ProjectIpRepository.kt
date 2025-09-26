package com.hwj.translation.busniness

import com.hwj.translation.bean.CommonResponse
import com.hwj.translation.bean.ProjectIp
import com.hwj.translation.bean.param.*
import com.hwj.translation.dao.TranslationDao

class ProjectIpRepository(translationDao: TranslationDao) : BaseRepository(translationDao) {

    fun upsertProjectIp(ip: String, param: CommonParam<*>): CommonResponse<ProjectIp?> {
        return parseRealParam(param, AddProjectIpParam::class.java)?.let { realParam ->
            try {
                // 验证参数
                if (ip.isBlank()) {
                    return CommonResponse(-1, "获取不到当前平台的IP地址", null)
                }
                
                if (realParam.projectId.isBlank()) {
                    return CommonResponse(-1, "项目ID不能为空", null)
                }
                
                val result = mTranslationDao.upsertProjectIp(ip, realParam.projectId)
                if (result != null) {
                    CommonResponse(200, "操作成功", result)
                } else {
                    CommonResponse(-1, "操作失败", null)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                CommonResponse(-1, "操作失败：${e.message}", null)
            }
        } ?: CommonResponse(-1, "参数解析错误", null)
    }

    fun deleteProjectIpV2(param: CommonParam<*>): CommonResponse<Void> {
        return parseRealParam(param, DeleteProjectIpParam::class.java)?.let { realParam ->
            try {
                val result = mTranslationDao.deleteProjectIp(realParam.id)
                if (result) {
                    CommonResponse(200, "删除成功", null)
                } else {
                    CommonResponse(-1, "删除失败，记录不存在", null)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                CommonResponse(-1, "删除失败：${e.message}", null)
            }
        } ?: CommonResponse(-1, "参数解析错误", null)
    }

    fun queryProjectIpsV2(ip:String,param: CommonParam<*>): CommonResponse<List<ProjectIp>> {
        return parseRealParam(param, QueryProjectIpParam::class.java)?.let { realParam ->
            try {
                val results = when {
                    ip.isNotEmpty()-> {
                        // 只提供了ip
                        mTranslationDao.queryProjectIpsByIp(ip)
                    }
                    else -> {
                        // 都没提供，查询所有
                        emptyList<ProjectIp>()
                    }
                }
                
                CommonResponse(200, "查询成功", results)
            } catch (e: Exception) {
                e.printStackTrace()
                CommonResponse(-1, "查询失败：${e.message}", emptyList())
            }
        } ?: CommonResponse(-1, "参数解析错误", emptyList())
    }
}