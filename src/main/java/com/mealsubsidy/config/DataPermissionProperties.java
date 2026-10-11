package com.mealsubsidy.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Arrays;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "data-permission")
public class DataPermissionProperties {

    private boolean enabled = true;

    /** 菜品表 dish 不在名单内，全员可见 */
    private List<String> tables = Arrays.asList(
            "meal_order", "meal_month_stat", "meal_audit", "meal_refund", "sys_user", "dept"
    );
}
