package com.mealsubsidy.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mealsubsidy.dto.DishQueryDTO;
import com.mealsubsidy.dto.DishSaveDTO;
import com.mealsubsidy.dto.DishVO;
import com.mealsubsidy.entity.Dish;

public interface DishService extends IService<Dish> {

    Page<DishVO> pageDishes(DishQueryDTO query);

    DishVO getDish(Long id);

    void saveDish(DishSaveDTO dto);

    void updateDish(DishSaveDTO dto);

    void updateStatus(Long id, Integer status);
}
