package com.mealsubsidy.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mealsubsidy.dto.MealOrderQueryDTO;
import com.mealsubsidy.dto.MealOrderSubmitDTO;
import com.mealsubsidy.dto.MealOrderVO;
import com.mealsubsidy.entity.MealOrder;

public interface MealOrderService extends IService<MealOrder> {

    Page<MealOrderVO> pageOrders(MealOrderQueryDTO query);

    MealOrderVO getOrder(Long id);

    MealOrderVO submit(MealOrderSubmitDTO dto);

    void cancel(Long id);
}
