package com.mealsubsidy.service;

import com.mealsubsidy.dto.AuditOrderRequest;
import com.mealsubsidy.dto.MonthlyReportVO;
import com.mealsubsidy.dto.OrderVO;
import com.mealsubsidy.dto.RefundApplyRequest;
import com.mealsubsidy.dto.SubmitOrderRequest;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

public interface OrderService {

    OrderVO submit(SubmitOrderRequest request);

    OrderVO cancel(Long orderId);

    OrderVO audit(Long orderId, AuditOrderRequest request);

    OrderVO applyRefund(Long orderId, RefundApplyRequest request);

    OrderVO auditRefund(Long refundId, AuditOrderRequest request);

    OrderVO detail(Long orderId);

    List<OrderVO> myOrders();

    MonthlyReportVO monthlyReport(String accountMonth);

    void exportMonthlyReport(String accountMonth, HttpServletResponse response);
}
