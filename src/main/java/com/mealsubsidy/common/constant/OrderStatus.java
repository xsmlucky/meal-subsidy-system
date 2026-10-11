package com.mealsubsidy.common.constant;

/**
 * 订单状态，对应 meal_order.order_status
 */
public final class OrderStatus {

    public static final String PENDING = "PENDING";
    public static final String APPROVED = "APPROVED";
    public static final String REJECTED = "REJECTED";
    public static final String CANCELLED = "CANCELLED";
    public static final String REFUND_PENDING = "REFUND_PENDING";
    public static final String REFUNDED = "REFUNDED";

    private OrderStatus() {
    }
}
