package com.mealsubsidy.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mealsubsidy.dto.MealAuditQueryDTO;
import com.mealsubsidy.dto.MealAuditVO;
import com.mealsubsidy.entity.MealAudit;

public interface MealAuditService extends IService<MealAudit> {

    Page<MealAuditVO> pagePending(MealAuditQueryDTO query);

    MealAuditVO getOrder(Long id);

    void pass(Long id);

    void reject(Long id);
}
