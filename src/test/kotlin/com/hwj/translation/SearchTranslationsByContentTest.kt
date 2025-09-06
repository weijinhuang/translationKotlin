package com.hwj.translation.test

import com.hwj.translation.bean.param.CommonParam
import com.hwj.translation.bean.param.SearchTranslationByContentParam
import com.hwj.translation.bean.SEARCH_TRANSLATIONS_BY_CONTENT

/**
 * 根据内容搜索翻译测试示例
 * 
 * 功能说明：
 * 1. 内容搜索模式：当languageId有效时，在指定语言中模糊搜索包含目标内容的翻译
 * 2. 键搜索模式：当languageId为null或负值时，将目标内容当作translationKey进行精确匹配
 * 3. 返回匹配翻译键的所有语言翻译，按translationKey分组
 * 4. 支持模糊匹配搜索（内容模式）和精确匹配搜索（键模式）
 */
class SearchTranslationsByContentTest {
    
    /**
     * 测试基本的内容搜索功能
     */
    fun testBasicContentSearch() {
        // 1. 搜索包含"Login"的英文翻译
        val searchParam = SearchTranslationByContentParam(
            projectId = "my_project",
            targetTranslationContent = "Login",
            languageId = 1  // 假设1是英文语言ID
        )
        
        // 2. 构建通用参数
        val commonParam = CommonParam<SearchTranslationByContentParam>().apply {
            // cmd = SEARCH_TRANSLATIONS_BY_CONTENT
            // data = searchParam
        }
        
        // 3. API调用示例（通过HTTP请求）
        /*
        POST /translationSystem
        Content-Type: application/json
        
        {
            "cmd": "searchTranslationsByContent",
            "data": {
                "projectId": "my_project",
                "targetTranslationContent": "Login",
                "languageId": 1
            }
        }
        */
        
        // 4. 期望的响应格式
        /*
        {
            "code": 200,
            "msg": "搜索成功",
            "data": [
                {
                    "translationKey": "login_button",
                    "translations": [
                        {
                            "translationId": 1,
                            "translationKey": "login_button",
                            "languageId": 1,
                            "translationContent": "Login",
                            "projectId": "my_project"
                            // ... 其他字段
                        },
                        {
                            "translationId": 2,
                            "translationKey": "login_button",
                            "languageId": 2,
                            "translationContent": "登录",
                            "projectId": "my_project"
                            // ... 其他字段
                        }
                    ]
                }
            ]
        }
        */
    }
    
    /**
     * 测试多语言搜索场景
     */
    fun testMultiLanguageSearch() {
        // 搜索中文翻译
        val chineseSearchParam = SearchTranslationByContentParam(
            projectId = "my_project",
            targetTranslationContent = "登录",
            languageId = 2  // 假设2是中文语言ID
        )
        
        // 搜索西班牙语翻译
        val spanishSearchParam = SearchTranslationByContentParam(
            projectId = "my_project", 
            targetTranslationContent = "Iniciar",
            languageId = 3  // 假设3是西班牙语语言ID
        )
        
        // 搜索日语翻译
        val japaneseSearchParam = SearchTranslationByContentParam(
            projectId = "my_project",
            targetTranslationContent = "ログイン",
            languageId = 4  // 假设4是日语语言ID
        )
    }
    
    /**
     * 测试模糊匹配搜索
     */
    fun testFuzzySearch() {
        // 搜索包含"error"的所有翻译
        val errorSearchParam = SearchTranslationByContentParam(
            projectId = "my_project",
            targetTranslationContent = "error",
            languageId = 1
        )
        // 可能匹配: "Network Error", "Login Error", "Validation Error" 等
        
        // 搜索包含"成功"的中文翻译
        val successSearchParam = SearchTranslationByContentParam(
            projectId = "my_project",
            targetTranslationContent = "成功",
            languageId = 2
        )
        // 可能匹配: "登录成功", "保存成功", "提交成功" 等
        
        // 搜索部分单词
        val partialSearchParam = SearchTranslationByContentParam(
            projectId = "my_project",
            targetTranslationContent = "log",
            languageId = 1
        )
        // 可能匹配: "Login", "Logout", "Dialog", "Log file" 等
    }
    
    /**
     * 测试特殊字符和符号搜索
     */
    fun testSpecialCharacterSearch() {
        // 搜索包含特殊符号的翻译
        val symbolSearchParam = SearchTranslationByContentParam(
            projectId = "my_project",
            targetTranslationContent = "%",
            languageId = 1
        )
        
        // 搜索包含数字的翻译
        val numberSearchParam = SearchTranslationByContentParam(
            projectId = "my_project",
            targetTranslationContent = "100",
            languageId = 1
        )
        
        // 搜索包含标点符号的翻译
        val punctuationSearchParam = SearchTranslationByContentParam(
            projectId = "my_project",
            targetTranslationContent = "?",
            languageId = 1
        )
    }
    
    /**
     * 测试实际业务场景
     */
    fun testBusinessScenarios() {
        // 场景1：查找所有提示信息
        val tipSearchParam = SearchTranslationByContentParam(
            projectId = "my_project",
            targetTranslationContent = "tip",
            languageId = 1
        )
        
        // 场景2：查找所有按钮文本
        val buttonSearchParam = SearchTranslationByContentParam(
            projectId = "my_project",
            targetTranslationContent = "button",
            languageId = 1
        )
        
        // 场景3：查找所有错误消息
        val messageSearchParam = SearchTranslationByContentParam(
            projectId = "my_project",
            targetTranslationContent = "message",
            languageId = 1
        )
        
        // 场景4：查找特定功能模块的翻译
        val moduleSearchParam = SearchTranslationByContentParam(
            projectId = "my_project",
            targetTranslationContent = "user",
            languageId = 1
        )
    }
    
    /**
     * 测试新增的translationKey搜索模式
     */
    fun testTranslationKeySearchMode() {
        // 情况1：languageId为null，将targetTranslationContent当作translationKey搜索
        val nullLanguageParam = SearchTranslationByContentParam(
            projectId = "my_project",
            targetTranslationContent = "login_button",  // 这里是translationKey而不是内容
            languageId = null
        )
        
        // 情况2：languageId为负值，将targetTranslationContent当作translationKey搜索
        val negativeLanguageParam = SearchTranslationByContentParam(
            projectId = "my_project",
            targetTranslationContent = "user_profile_title",
            languageId = -1
        )
        
        // 情况3：languageId为0（边界情况），仍然使用键搜索模式
        val zeroLanguageParam = SearchTranslationByContentParam(
            projectId = "my_project",
            targetTranslationContent = "error_message_network",
            languageId = 0
        )
        
        // 这些请求将直接按translationKey进行精确匹配，而不是模糊匹配内容
        
        /*
        期望的API调用：
        POST /translationSystem
        {
            "cmd": "searchTranslationsByContent",
            "data": {
                "projectId": "my_project",
                "targetTranslationContent": "login_button",
                "languageId": null
            }
        }
        
        期望的响应：
        {
            "code": 200,
            "msg": "搜索成功",
            "data": [
                {
                    "translationKey": "login_button",
                    "translations": [
                        {
                            "translationId": 1,
                            "translationKey": "login_button",
                            "languageId": 1,
                            "translationContent": "Login",
                            "projectId": "my_project"
                        },
                        {
                            "translationId": 2,
                            "translationKey": "login_button",
                            "languageId": 2,
                            "translationContent": "登录",
                            "projectId": "my_project"
                        }
                    ]
                }
            ]
        }
        */
    }
    fun testEdgeCasesAndErrorHandling() {
        // 空项目ID
        val emptyProjectParam = SearchTranslationByContentParam(
            projectId = "",
            targetTranslationContent = "test",
            languageId = 1
        )
        // 期望返回: {"code": -1, "msg": "项目ID不能为空", "data": []}
        
        // 空搜索内容
        val emptyContentParam = SearchTranslationByContentParam(
            projectId = "my_project",
            targetTranslationContent = "",
            languageId = 1
        )
        // 期望返回: {"code": -1, "msg": "搜索内容不能为空", "data": []}
        
        // 不存在的项目ID
        val nonExistentProjectParam = SearchTranslationByContentParam(
            projectId = "non_existent_project",
            targetTranslationContent = "test",
            languageId = 1
        )
        // 期望返回: {"code": 200, "msg": "未找到匹配的翻译", "data": []}
        
        // 不存在的语言ID
        val nonExistentLanguageParam = SearchTranslationByContentParam(
            projectId = "my_project",
            targetTranslationContent = "test",
            languageId = 999
        )
        // 期望返回: {"code": 200, "msg": "未找到匹配的翻译", "data": []}
        
        // 搜索不存在的内容
        val nonExistentContentParam = SearchTranslationByContentParam(
            projectId = "my_project",
            targetTranslationContent = "xyz_non_existent_content_123",
            languageId = 1
        )
        // 期望返回: {"code": 200, "msg": "未找到匹配的翻译", "data": []}
    }
    
    /**
     * 性能测试场景
     */
    fun testPerformanceScenarios() {
        // 搜索常见词汇（可能返回大量结果）
        val commonWordParam = SearchTranslationByContentParam(
            projectId = "large_project",
            targetTranslationContent = "the",
            languageId = 1
        )
        
        // 搜索特定词汇（期望返回少量结果）
        val specificWordParam = SearchTranslationByContentParam(
            projectId = "large_project",
            targetTranslationContent = "authentication_token_validation",
            languageId = 1
        )
        
        // 搜索单个字符（可能返回大量结果）
        val singleCharParam = SearchTranslationByContentParam(
            projectId = "large_project",
            targetTranslationContent = "a",
            languageId = 1
        )
    }
    
    /**
     * 使用建议和最佳实践
     */
    fun bestPracticesExample() {
        // 建议1：搜索关键词而不是完整句子
        val keywordSearchParam = SearchTranslationByContentParam(
            projectId = "my_project",
            targetTranslationContent = "save",  // 而不是 "Please save your changes"
            languageId = 1
        )
        
        // 建议2：使用特定术语提高搜索精度
        val specificTermParam = SearchTranslationByContentParam(
            projectId = "my_project",
            targetTranslationContent = "validation",  // 比 "valid" 更精确
            languageId = 1
        )
        
        // 建议3：区分大小写进行精确搜索
        val caseSensitiveParam = SearchTranslationByContentParam(
            projectId = "my_project",
            targetTranslationContent = "API",  // 而不是 "api"
            languageId = 1
        )
        
        // 建议4：搜索功能性关键词
        val functionalKeywordParam = SearchTranslationByContentParam(
            projectId = "my_project",
            targetTranslationContent = "confirm",  // 查找所有确认相关的翻译
            languageId = 1
        )
    }
}