package com.mealsubsidy.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class MealRefundSubmitDTO {

    @NotNull(message = "订单不能为空")
    private Long orderId;

    @NotBlank(message = "退款原因不能为空")
    private String refundReason;
}
