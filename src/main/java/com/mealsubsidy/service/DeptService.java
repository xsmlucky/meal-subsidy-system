package com.mealsubsidy.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mealsubsidy.dto.DeptQueryDTO;
import com.mealsubsidy.dto.DeptSaveDTO;
import com.mealsubsidy.dto.DeptVO;
import com.mealsubsidy.entity.Dept;

public interface DeptService extends IService<Dept> {

    Page<DeptVO> pageDepts(DeptQueryDTO query);

    DeptVO getDept(Long id);

    void saveDept(DeptSaveDTO dto);

    void updateDept(DeptSaveDTO dto);

    void updateStatus(Long id, Integer status);
}
