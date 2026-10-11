package com.mealsubsidy.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mealsubsidy.dto.MealRefundQueryDTO;
import com.mealsubsidy.dto.MealRefundSubmitDTO;
import com.mealsubsidy.dto.MealRefundVO;
import com.mealsubsidy.entity.MealRefund;

public interface MealRefundService extends IService<MealRefund> {

    Page<MealRefundVO> pageRefunds(MealRefundQueryDTO query);

    MealRefundVO getRefund(Long id);

    MealRefundVO submit(MealRefundSubmitDTO dto);

    void approve(Long id);

    void reject(Long id);
}
