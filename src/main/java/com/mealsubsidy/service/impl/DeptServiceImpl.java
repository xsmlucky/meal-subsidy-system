package com.mealsubsidy.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mealsubsidy.common.exception.BizException;
import com.mealsubsidy.common.result.ResultCode;
import com.mealsubsidy.common.util.AuthGuard;
import com.mealsubsidy.dto.DeptQueryDTO;
import com.mealsubsidy.dto.DeptSaveDTO;
import com.mealsubsidy.dto.DeptVO;
import com.mealsubsidy.entity.Dept;
import com.mealsubsidy.mapper.DeptMapper;
import com.mealsubsidy.service.DeptService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DeptServiceImpl extends ServiceImpl<DeptMapper, Dept> implements DeptService {

    private final DeptMapper deptMapper;

    @Override
    public Page<DeptVO> pageDepts(DeptQueryDTO query) {
        AuthGuard.requireAdmin();
        long current = query.getCurrent() == null || query.getCurrent() < 1 ? 1L : query.getCurrent();
        long size = query.getSize() == null || query.getSize() < 1 ? 10L : query.getSize();
        LambdaQueryWrapper<Dept> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(query.getDeptName()), Dept::getDeptName, query.getDeptName())
                .eq(query.getStatus() != null, Dept::getStatus, query.getStatus())
                .orderByDesc(Dept::getId);
        Page<Dept> entityPage = deptMapper.selectPage(new Page<>(current, size), wrapper);
        Page<DeptVO> voPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        voPage.setRecords(entityPage.getRecords().stream().map(this::toVo).collect(Collectors.toList()));
        return voPage;
    }

    @Override
    public DeptVO getDept(Long id) {
        AuthGuard.requireAdmin();
        return toVo(requireDept(id));
    }

    @Override
    public void saveDept(DeptSaveDTO dto) {
        AuthGuard.requireAdmin();
        assertNameUnique(dto.getDeptName(), null);
        Dept dept = new Dept();
        dept.setDeptName(dto.getDeptName());
        dept.setDeptCode("D" + System.currentTimeMillis());
        dept.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        validateStatus(dept.getStatus());
        deptMapper.insert(dept);
    }

    @Override
    public void updateDept(DeptSaveDTO dto) {
        AuthGuard.requireAdmin();
        if (dto.getId() == null) {
            throw new BizException("部门ID不能为空");
        }
        Dept db = requireDept(dto.getId());
        assertNameUnique(dto.getDeptName(), dto.getId());
        db.setDeptName(dto.getDeptName());
        if (dto.getStatus() != null) {
            validateStatus(dto.getStatus());
            db.setStatus(dto.getStatus());
        }
        deptMapper.updateById(db);
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        AuthGuard.requireAdmin();
        validateStatus(status);
        Dept db = requireDept(id);
        db.setStatus(status);
        deptMapper.updateById(db);
    }

    private Dept requireDept(Long id) {
        Dept dept = deptMapper.selectById(id);
        if (dept == null) {
            throw new BizException(ResultCode.NOT_FOUND, "部门不存在");
        }
        return dept;
    }

    private void assertNameUnique(String deptName, Long excludeId) {
        Dept exists = deptMapper.selectOne(new LambdaQueryWrapper<Dept>()
                .eq(Dept::getDeptName, deptName)
                .last("LIMIT 1"));
        if (exists != null && (excludeId == null || !exists.getId().equals(excludeId))) {
            throw new BizException("部门名称已存在");
        }
    }

    private void validateStatus(Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BizException("状态只能是 0-禁用 或 1-启用");
        }
    }

    private DeptVO toVo(Dept dept) {
        DeptVO vo = new DeptVO();
        vo.setId(dept.getId());
        vo.setDeptName(dept.getDeptName());
        vo.setStatus(dept.getStatus());
        vo.setCreateTime(dept.getCreateTime());
        return vo;
    }
}
