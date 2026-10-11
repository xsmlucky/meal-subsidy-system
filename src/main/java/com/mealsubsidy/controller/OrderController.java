package com.mealsubsidy.controller;

import com.mealsubsidy.common.annotation.RequireRole;
import com.mealsubsidy.common.constant.RoleConstant;
import com.mealsubsidy.common.result.Result;
import com.mealsubsidy.dto.AuditOrderRequest;
import com.mealsubsidy.dto.MonthlyReportVO;
import com.mealsubsidy.dto.OrderVO;
import com.mealsubsidy.dto.RefundApplyRequest;
import com.mealsubsidy.dto.SubmitOrderRequest;
import com.mealsubsidy.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/orders")
    @RequireRole(RoleConstant.EMPLOYEE)
    public Result<OrderVO> submit(@Valid @RequestBody SubmitOrderRequest request) {
        return Result.ok(orderService.submit(request));
    }

    @PostMapping("/orders/{id}/cancel")
    @RequireRole(RoleConstant.EMPLOYEE)
    public Result<OrderVO> cancel(@PathVariable("id") Long id) {
        return Result.ok(orderService.cancel(id));
    }

    @PostMapping("/orders/{id}/audit")
    @RequireRole(RoleConstant.ADMIN)
    public Result<OrderVO> audit(@PathVariable("id") Long id, @Valid @RequestBody AuditOrderRequest request) {
        return Result.ok(orderService.audit(id, request));
    }

    @PostMapping("/orders/{id}/refund")
    @RequireRole(RoleConstant.EMPLOYEE)
    public Result<OrderVO> applyRefund(@PathVariable("id") Long id,
                                       @RequestBody(required = false) RefundApplyRequest request) {
        return Result.ok(orderService.applyRefund(id, request == null ? new RefundApplyRequest() : request));
    }

    @PostMapping("/refunds/{id}/audit")
    @RequireRole(RoleConstant.ADMIN)
    public Result<OrderVO> auditRefund(@PathVariable("id") Long id, @Valid @RequestBody AuditOrderRequest request) {
        return Result.ok(orderService.auditRefund(id, request));
    }

    @GetMapping("/orders/{id}")
    public Result<OrderVO> detail(@PathVariable("id") Long id) {
        return Result.ok(orderService.detail(id));
    }

    @GetMapping("/orders")
    public Result<List<OrderVO>> list() {
        return Result.ok(orderService.myOrders());
    }

    @GetMapping("/reports/monthly")
    @RequireRole(RoleConstant.ADMIN)
    public Result<MonthlyReportVO> monthly(@RequestParam("month") String month) {
        return Result.ok(orderService.monthlyReport(month));
    }

    @GetMapping("/reports/monthly/export")
    @RequireRole(RoleConstant.ADMIN)
    public void exportMonthly(@RequestParam("month") String month, HttpServletResponse response) {
        orderService.exportMonthlyReport(month, response);
    }
}
