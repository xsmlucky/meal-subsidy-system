package com.mealsubsidy.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mealsubsidy.common.constant.MealType;
import com.mealsubsidy.common.context.AuthUser;
import com.mealsubsidy.common.exception.BizException;
import com.mealsubsidy.common.result.ResultCode;
import com.mealsubsidy.common.util.AuthGuard;
import com.mealsubsidy.dto.DishQueryDTO;
import com.mealsubsidy.dto.DishSaveDTO;
import com.mealsubsidy.dto.DishVO;
import com.mealsubsidy.entity.Dish;
import com.mealsubsidy.mapper.DishMapper;
import com.mealsubsidy.service.DishService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DishServiceImpl extends ServiceImpl<DishMapper, Dish> implements DishService {

    private final DishMapper dishMapper;

    @Override
    public Page<DishVO> pageDishes(DishQueryDTO query) {
        Long deptId = currentAdminDeptId();
        long current = query.getCurrent() == null || query.getCurrent() < 1 ? 1L : query.getCurrent();
        long size = query.getSize() == null || query.getSize() < 1 ? 10L : query.getSize();
        LambdaQueryWrapper<Dish> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasText(query.getMealType()), Dish::getMealType, query.getMealType())
                .eq(query.getStatus() != null, Dish::getStatus, query.getStatus())
                .like(StringUtils.hasText(query.getDishName()), Dish::getDishName, query.getDishName())
                .orderByAsc(Dish::getSortNo)
                .orderByDesc(Dish::getId);
        Page<Dish> entityPage = dishMapper.selectPage(new Page<>(current, size), wrapper);
        Page<DishVO> voPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        voPage.setRecords(entityPage.getRecords().stream()
                .map(item -> toVo(item, deptId))
                .collect(Collectors.toList()));
        return voPage;
    }

    @Override
    public DishVO getDish(Long id) {
        Long deptId = currentAdminDeptId();
        return toVo(requireDish(id), deptId);
    }

    @Override
    public void saveDish(DishSaveDTO dto) {
        currentAdminDeptId();
        validateMealType(dto.getMealType());
        Dish dish = new Dish();
        dish.setDishName(dto.getDishName());
        dish.setMealType(dto.getMealType());
        dish.setPrice(dto.getDishPrice());
        dish.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        validateStatus(dish.getStatus());
        dish.setSortNo(0);
        dishMapper.insert(dish);
    }

    @Override
    public void updateDish(DishSaveDTO dto) {
        if (dto.getId() == null) {
            throw new BizException("菜品ID不能为空");
        }
        validateMealType(dto.getMealType());
        Dish db = requireDish(dto.getId());
        db.setDishName(dto.getDishName());
        db.setMealType(dto.getMealType());
        db.setPrice(dto.getDishPrice());
        if (dto.getStatus() != null) {
            validateStatus(dto.getStatus());
            db.setStatus(dto.getStatus());
        }
        dishMapper.updateById(db);
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        validateStatus(status);
        Dish db = requireDish(id);
        db.setStatus(status);
        dishMapper.updateById(db);
    }

    private Long currentAdminDeptId() {
        AuthUser admin = AuthGuard.requireAdmin();
        return admin.getDeptId();
    }

    private Dish requireDish(Long id) {
        Dish dish = dishMapper.selectById(id);
        if (dish == null) {
            throw new BizException(ResultCode.NOT_FOUND, "菜品不存在");
        }
        return dish;
    }

    private void validateMealType(String mealType) {
        if (!MealType.BREAKFAST.equals(mealType)
                && !MealType.LUNCH.equals(mealType)
                && !MealType.DINNER.equals(mealType)) {
            throw new BizException(ResultCode.BAD_REQUEST, "餐次不合法");
        }
    }

    private void validateStatus(Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BizException("状态只能是 0-下架 或 1-上架");
        }
    }

    private DishVO toVo(Dish dish, Long deptId) {
        DishVO vo = new DishVO();
        vo.setId(dish.getId());
        vo.setDishName(dish.getDishName());
        vo.setMealType(dish.getMealType());
        vo.setDishPrice(dish.getPrice());
        vo.setStatus(dish.getStatus());
        vo.setDeptId(deptId);
        vo.setCreateTime(dish.getCreateTime());
        return vo;
    }
}
