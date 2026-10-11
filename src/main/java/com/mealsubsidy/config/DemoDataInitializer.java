package com.mealsubsidy.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mealsubsidy.common.constant.MealType;
import com.mealsubsidy.common.constant.RoleConstant;
import com.mealsubsidy.entity.Dept;
import com.mealsubsidy.entity.Dish;
import com.mealsubsidy.entity.MealMonthStat;
import com.mealsubsidy.entity.SysUser;
import com.mealsubsidy.mapper.DeptMapper;
import com.mealsubsidy.mapper.DishMapper;
import com.mealsubsidy.mapper.MealMonthStatMapper;
import com.mealsubsidy.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.YearMonth;

@Slf4j
@Component
@RequiredArgsConstructor
public class DemoDataInitializer implements ApplicationRunner {

    public static final String DEFAULT_RAW_PASSWORD = "123456";

    private final DeptMapper deptMapper;
    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final DishMapper dishMapper;
    private final MealMonthStatMapper monthStatMapper;

    @Override
    public void run(ApplicationArguments args) {
        Dept dept = ensureDept();
        ensureUser("admin", "后勤管理员", "A001", RoleConstant.ADMIN, dept.getId());
        SysUser employee = ensureUser("zhangsan", "张三", "E001", RoleConstant.EMPLOYEE, dept.getId());
        ensureDishes();
        ensureMonthStat(employee);
    }

    private Dept ensureDept() {
        Dept exists = deptMapper.selectOne(new LambdaQueryWrapper<Dept>()
                .eq(Dept::getDeptCode, "HQ001")
                .last("LIMIT 1"));
        if (exists != null) {
            return exists;
        }
        Dept dept = new Dept();
        dept.setDeptName("后勤部");
        dept.setDeptCode("HQ001");
        dept.setStatus(1);
        deptMapper.insert(dept);
        return dept;
    }

    private SysUser ensureUser(String username, String realName, String employeeNo, String role, Long deptId) {
        SysUser exists = sysUserMapper.selectByUsername(username);
        if (exists != null) {
            if (!passwordEncoder.matches(DEFAULT_RAW_PASSWORD, exists.getPassword())) {
                exists.setPassword(passwordEncoder.encode(DEFAULT_RAW_PASSWORD));
                sysUserMapper.updateById(exists);
            }
            return exists;
        }
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(DEFAULT_RAW_PASSWORD));
        user.setRealName(realName);
        user.setEmployeeNo(employeeNo);
        user.setDeptId(deptId);
        user.setRole(role);
        user.setStatus(1);
        sysUserMapper.insert(user);
        log.info("已初始化账号 {}，密码已 BCrypt 加密", username);
        return user;
    }

    private void ensureDishes() {
        Long count = dishMapper.selectCount(null);
        if (count != null && count > 0) {
            return;
        }
        insertDish("午餐套餐A", new BigDecimal("15.00"), MealType.LUNCH, 1);
        insertDish("晚餐套餐B", new BigDecimal("18.00"), MealType.DINNER, 2);
    }

    private void insertDish(String name, BigDecimal price, String mealType, int sortNo) {
        Dish dish = new Dish();
        dish.setDishName(name);
        dish.setPrice(price);
        dish.setMealType(mealType);
        dish.setStatus(1);
        dish.setSortNo(sortNo);
        dishMapper.insert(dish);
    }

    private void ensureMonthStat(SysUser employee) {
        String month = YearMonth.now().toString();
        MealMonthStat exists = monthStatMapper.selectOne(new LambdaQueryWrapper<MealMonthStat>()
                .eq(MealMonthStat::getUserId, employee.getId())
                .eq(MealMonthStat::getStatMonth, month)
                .last("LIMIT 1"));
        if (exists != null) {
            return;
        }
        BigDecimal quota = new BigDecimal("300.00");
        MealMonthStat stat = new MealMonthStat();
        stat.setUserId(employee.getId());
        stat.setDeptId(employee.getDeptId());
        stat.setStatMonth(month);
        stat.setQuotaAmount(quota);
        stat.setFrozenAmount(BigDecimal.ZERO);
        stat.setUsedAmount(BigDecimal.ZERO);
        stat.setOrderCount(0);
        stat.setRefundCount(0);
        stat.setRefundAmount(BigDecimal.ZERO);
        monthStatMapper.insert(stat);
        log.info("已为 {} 发放 {} 餐补额度 300", employee.getUsername(), month);
    }
}
