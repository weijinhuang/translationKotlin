package com.hwj.translation.test

import com.hwj.translation.bean.param.CommonParam
import com.hwj.translation.bean.param.PaginatedTranslationParam
import com.hwj.translation.bean.GET_TRANSLATION_ROWS_PAGINATED

/**
 * 分页查询Translation测试示例
 * 
 * 使用示例：
 * 1. 创建分页参数
 * 2. 调用分页查询接口
 * 3. 处理返回的分页结果
 */
class PaginatedTranslationTest {
    
    /**
     * 测试分页查询Translation数据
     */
    fun testPaginatedQuery() {
        // 1. 创建分页参数
        val paginatedParam = PaginatedTranslationParam(
        ).apply {

            projectId = "your_project_id"  // 替换为实际的项目ID
            page = 0      // 第一页（从0开始）
            size = 20       // 每页20条记录
        }
        
        // 2. 构建通用参数
        val commonParam = CommonParam<PaginatedTranslationParam>().apply {
            // cmd = GET_TRANSLATION_ROWS_PAGINATED  // 注意：这里需要通过反射或其他方式设置
            // data = paginatedParam                  // 注意：这里需要通过反射或其他方式设置
        }
        
        // 3. 调用API（通过HTTP请求）
        /*
        POST /translationSystem
        Content-Type: application/json
        
        {
            "cmd": "getTranslationRowsPaginated",
            "data": {
                "projectId": "your_project_id",
                "page": 0,
                "size": 20
            }
        }
        */
        
        // 4. 期望的响应格式
        /*
        {
            "code": 200,
            "msg": "查询成功",
            "data": {
                "content": [
                    {
                        "translationKey": "example_key_1",
                        "translations": [
                            {
                                "translationId": 1,
                                "translationKey": "example_key_1",
                                "languageId": 1,
                                "translationContent": "English content",
                                "projectId": "your_project_id",
                                "moduleId": 1,
                                "comment": null,
                                "hide": 0,
                                "referto": null
                            },
                            {
                                "translationId": 2,
                                "translationKey": "example_key_1",
                                "languageId": 2,
                                "translationContent": "中文内容",
                                "projectId": "your_project_id",
                                "moduleId": 1,
                                "comment": null,
                                "hide": 0,
                                "referto": null
                            }
                        ]
                    }
                ],
                "page": 0,
                "size": 20,
                "totalElements": 100,
                "totalPages": 5,
                "hasNext": true,
                "hasPrevious": false
            }
        }
        */
    }
    
    /**
     * 测试不同页码的查询
     */
    fun testDifferentPages() {
        // 查询第二页
        val secondPageParam = PaginatedTranslationParam(

        ).apply {
            projectId = "your_project_id"
            page = 1      // 第二
            size = 20
        }
        
        // 查询更小的页面大小
        val smallPageParam = PaginatedTranslationParam(

        ).apply {
            projectId = "your_project_id"
            page = 0      // 第二
            size = 10
        }
    }
    
    /**
     * 错误处理示例
     */
    fun testErrorHandling() {
        // 无效的项目ID
        val invalidProjectParam = PaginatedTranslationParam(

        ).apply {
            projectId = ""
            page = 0      // 第二
            size = 20
        }
        
        // 负数页码（虽然参数验证可能会处理）
        val negativePageParam = PaginatedTranslationParam(
        ).apply {

            projectId = "your_project_id"
            page = -1     // 负数页码
            size = 20
        }
        
        // 过大的页面大小
        val largeSizeParam = PaginatedTranslationParam(
        ).apply {

            projectId = "your_project_id"
            page = 0
            size = 1000     // 过大的页面大小
        }
    }
}