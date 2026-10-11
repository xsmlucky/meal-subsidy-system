package com.mealsubsidy.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.mealsubsidy.interceptor.DataPermissionInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatisPlus配置类：分页插件 + 数据权限插件
 * 注意顺序：数据权限拦截器 必须放在分页插件前面
 */
@Configuration
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor(DataPermissionInterceptor dataPermissionInterceptor){
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 先注册数据权限
        interceptor.addInnerInterceptor(dataPermissionInterceptor);
        // 再注册分页插件
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor());
        return interceptor;
    }
}
