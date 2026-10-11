package com.mealsubsidy.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class AuditOrderRequest {

    /** true 通过，false 驳回 */
    @NotNull(message = "审核结果不能为空")
    private Boolean passed;

    /** 驳回时必填 */
    private String rejectReason;
}
