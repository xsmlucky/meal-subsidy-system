package com.mealsubsidy.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.GroupedOpenApi;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j OpenAPI3 文档。生产环境设置 knife4j.enable=false 关闭。
 */
@Configuration
@ConditionalOnProperty(name = "knife4j.enable", havingValue = "true", matchIfMissing = true)
public class Knife4jConfig {

    @Bean
    public OpenAPI mealSubsidyOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("员工后勤餐补订餐管理系统API文档")
                        .version("V1.0")
                        .description("统一响应结构 Result：code 业务码，message 提示信息，data 业务数据。"
                                + "校验失败 code=400，系统内部错误 code=500，乐观锁冲突 code=50001。"));
    }

    @Bean
    public GroupedOpenApi mealSubsidyGroupedOpenApi() {
        return GroupedOpenApi.builder()
                .group("meal-subsidy")
                .packagesToScan("com.mealsubsidy.controller")
                .build();
    }
}
