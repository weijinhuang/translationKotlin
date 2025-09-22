package com.hwj.translation.test

import com.hwj.translation.bean.param.*

/**
 * ProjectIp CRUD操作测试示例
 * 
 * 功能说明：
 * 1. 支持项目IP地址的增删查改操作
 * 2. 提供灵活的查询方式（按项目ID、IP地址或两者组合）
 * 3. Upsert操作：根据IP和projectId判断，如果记录存在则更新时间戳，否则新增记录
 * 4. 使用Unix时间戳(int)存储更新时间
 * 5. 自动维护时间戳信息
 */
class ProjectIpTest {
    
    /**
     * 测试Upsert ProjectIp记录（新增或更新）
     */
    fun testUpsertProjectIp() {
        // 新增内网IP（首次调用）
        val upsertInternalIpParam = AddProjectIpParam(
            ip = "192.168.1.100",
            projectId = "mobile_app_v1"
        )
        
        // 再次调用相同IP和projectId（将更新时间戳）
        val updateSameIpParam = AddProjectIpParam(
            ip = "192.168.1.100",
            projectId = "mobile_app_v1"
        )
        
        /*
        API调用示例：
        POST /translationSystem
        {
            "cmd": "upsertProjectIp",
            "data": {
                "ip": "192.168.1.100",
                "projectId": "mobile_app_v1"
            }
        }
        
        首次调用期望响应（新增）：
        {
            "code": 200,
            "msg": "操作成功",
            "data": {
                "id": 1,
                "ip": "192.168.1.100",
                "projectId": "mobile_app_v1",
                "updateTime": 1726567800
            }
        }
        
        再次调用期望响应（更新时间戳）：
        {
            "code": 200,
            "msg": "操作成功",
            "data": {
                "id": 1,
                "ip": "192.168.1.100",
                "projectId": "mobile_app_v1",
                "updateTime": 1726568900
            }
        }
        */
    }
    
    /**
     * 测试查询ProjectIp记录的不同方式
     */
    fun testQueryProjectIps() {
        // 查询指定项目的所有IP
        val queryByProjectParam = QueryProjectIpParam(
            projectId = "mobile_app_v1"
        )
        
        // 查询指定IP的所有项目
        val queryByIpParam = QueryProjectIpParam(
            ip = "192.168.1.100"
        )
        
        // 查询特定项目和IP的组合
        val queryByBothParam = QueryProjectIpParam(
            projectId = "mobile_app_v1",
            ip = "192.168.1.100"
        )
        
        // 查询所有记录
        val queryAllParam = QueryProjectIpParam()
        
        /*
        API调用示例：
        POST /translationSystem
        {
            "cmd": "queryProjectIps",
            "data": {
                "projectId": "mobile_app_v1"
            }
        }
        
        期望响应：
        {
            "code": 200,
            "msg": "查询成功",
            "data": [
                {
                    "id": 1,
                    "ip": "192.168.1.100",
                    "projectId": "mobile_app_v1",
                    "updateTime": "2025-09-17T10:30:00"
                },
                {
                    "id": 3,
                    "ip": "10.0.0.50",
                    "projectId": "mobile_app_v1",
                    "updateTime": "2025-09-17T09:45:00"
                }
            ]
        }
        */
    }
    
    /**
     * 测试删除ProjectIp记录
     */
    fun testDeleteProjectIp() {
        // 删除指定记录
        val deleteParam = DeleteProjectIpParam(
            id = 1
        )
        
        /*
        API调用示例：
        POST /translationSystem
        {
            "cmd": "deleteProjectIp",
            "data": {
                "id": 1
            }
        }
        
        期望响应：
        {
            "code": 200,
            "msg": "删除成功",
            "data": null
        }
        */
        
        // 删除不存在的记录
        val deleteNonExistentParam = DeleteProjectIpParam(
            id = 999
        )
        
        /*
        期望响应（记录不存在）：
        {
            "code": -1,
            "msg": "删除失败，记录不存在",
            "data": null
        }
        */
    }
    
    /**
     * 测试业务场景
     */
    fun testBusinessScenarios() {
        // 场景1：项目部署新服务器，添加新IP（首次调用会新增记录）
        val deployNewServerParam = AddProjectIpParam(
            ip = "172.16.10.200",
            projectId = "e_commerce_api"
        )
        
        // 场景2：项目定期心跳检测，更新IP访问时间（相同IP和projectId会更新时间戳）
        val heartbeatUpdateParam = AddProjectIpParam(
            ip = "172.16.10.200",
            projectId = "e_commerce_api"
        )
        
        // 场景3：检查哪些项目在使用特定IP
        val checkIpUsageParam = QueryProjectIpParam(
            ip = "203.0.113.100"
        )
        
        // 场景4：服务器下线，删除IP记录
        val serverDecommissionParam = DeleteProjectIpParam(
            id = 8
        )
        
        // 场景5：查看项目的所有IP配置
        val viewProjectConfigParam = QueryProjectIpParam(
            projectId = "data_analytics_platform"
        )
    }
    
    /**
     * 测试错误处理
     */
    fun testErrorHandling() {
        // 空IP地址
        val emptyIpParam = AddProjectIpParam(
            ip = "",
            projectId = "test_project"
        )
        // 期望返回: {"code": -1, "msg": "IP地址不能为空", "data": null}
        
        // 空项目ID
        val emptyProjectParam = AddProjectIpParam(
            ip = "192.168.1.1",
            projectId = ""
        )
        // 期望返回: {"code": -1, "msg": "项目ID不能为空", "data": null}
        
        // 查询不存在的项目
        val queryNonExistentProjectParam = QueryProjectIpParam(
            projectId = "non_existent_project"
        )
        // 期望返回: {"code": 200, "msg": "查询成功", "data": []}
        
        // 查询不存在的IP
        val queryNonExistentIpParam = QueryProjectIpParam(
            ip = "999.999.999.999"
        )
        // 期望返回: {"code": 200, "msg": "查询成功", "data": []}
    }
    
    /**
     * 测试IP地址格式
     */
    fun testIpAddressFormats() {
        // IPv4地址
        val ipv4Param = AddProjectIpParam(
            ip = "192.168.1.100",
            projectId = "ipv4_service"
        )
        
        // IPv6地址
        val ipv6Param = AddProjectIpParam(
            ip = "2001:0db8:85a3:0000:0000:8a2e:0370:7334",
            projectId = "ipv6_service"
        )
        
        // 本地回环地址
        val localhostParam = AddProjectIpParam(
            ip = "127.0.0.1",
            projectId = "local_development"
        )
        
        // 通配符地址
        val wildcardParam = AddProjectIpParam(
            ip = "0.0.0.0",
            projectId = "all_interfaces"
        )
    }
    
    /**
     * 最佳实践示例
     */
    fun bestPracticesExample() {
        // 实践1：使用描述性的项目ID
        val descriptiveProjectParam = AddProjectIpParam(
            ip = "10.1.2.100",
            projectId = "user_authentication_service_prod"
        )
        
        // 实践2：定期更新活跃IP的时间戳（使用upsert自动更新）
        val keepAliveUpdateParam = AddProjectIpParam(
            ip = "10.1.2.100",
            projectId = "user_authentication_service_prod"
        )
        
        // 实践3：按项目查询进行IP管理
        val projectIpManagementParam = QueryProjectIpParam(
            projectId = "payment_gateway_service"
        )
        
        // 实践4：清理不再使用的IP记录
        val cleanupUnusedIpParam = DeleteProjectIpParam(
            id = 15
        )
    }
}