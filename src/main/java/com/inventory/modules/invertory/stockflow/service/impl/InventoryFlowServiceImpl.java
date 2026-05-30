package com.inventory.modules.invertory.stockflow.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.inventory.common.response.Result;
import com.inventory.modules.invertory.stockflow.dto.InventoryFlowQueryDTO;
import com.inventory.modules.invertory.stockflow.entity.InventoryFlow;
import com.inventory.modules.invertory.stockflow.mapper.InventoryFlowMapper;
import com.inventory.modules.invertory.stockflow.service.InventoryFlowService;
import com.inventory.modules.invertory.stockflow.vo.InventoryFlowVO;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author 95349
 * @description 针对表【inventory_flow(库存流水表)】的数据库操作Service实现
 * @createDate 2026-05-29 19:11:19
 * 库存流水业务实现类
 * 核心逻辑：查询库存变动记录，支持分页、导出
*/

@Service
@RequiredArgsConstructor
public class InventoryFlowServiceImpl extends ServiceImpl<InventoryFlowMapper, InventoryFlow>
    implements InventoryFlowService {
    /**
     * 注入库存流水Mapper
     */
    private final InventoryFlowMapper flowMapper;

    /**
     * 库存流水分页查询
     */
    @Override
    public Result<?> pageFlowList(InventoryFlowQueryDTO queryDTO) {
        // 1. 构建分页对象
        Page<InventoryFlow> page = new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize());

        // 2. 构造查询条件
        LambdaQueryWrapper<InventoryFlow> wrapper = Wrappers.lambdaQuery();

        // 商品名称模糊查询
        if (StrUtil.isNotBlank(queryDTO.getGoodsName())) {
            wrapper.like(InventoryFlow::getGoodsName, queryDTO.getGoodsName());
        }

        // 操作类型查询（前端传枚举，后端转换为数字）
        if (StrUtil.isNotBlank(queryDTO.getOperateType())) {
            Integer typeCode = convertOperateTypeToCode(queryDTO.getOperateType());
            if (typeCode != null) {
                wrapper.eq(InventoryFlow::getOperateType, typeCode);
            }
        }

        // 时间范围查询
        if (StrUtil.isNotBlank(queryDTO.getStartTime())) {
            wrapper.ge(InventoryFlow::getCreateTime, queryDTO.getStartTime());
        }
        if (StrUtil.isNotBlank(queryDTO.getEndTime())) {
            wrapper.le(InventoryFlow::getCreateTime, queryDTO.getEndTime());
        }

        // 排序：操作时间倒序
        wrapper.orderByDesc(InventoryFlow::getCreateTime);

        // 3. 执行分页查询
        Page<InventoryFlow> flowPage = flowMapper.selectPage(page, wrapper);

        // 4. 转换为VO
        Page<InventoryFlowVO> voPage = new Page<>(flowPage.getCurrent(), flowPage.getSize(), flowPage.getTotal());
        List<InventoryFlowVO> voList = flowPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        voPage.setRecords(voList);

        return Result.success(voPage);
    }

    /**
     * 获取库存流水详情
     */
    @Override
    public Result<?> getFlowDetail(String id) {
        InventoryFlow flow = flowMapper.selectById(Long.valueOf(id));
        if (flow == null) {
            return Result.fail("库存流水记录不存在");
        }
        return Result.success(convertToVO(flow));
    }

    /**
     * 导出库存流水Excel（简化版，可根据实际需求完善）
     */
    @Override
    public void exportFlowList(InventoryFlowQueryDTO queryDTO, HttpServletResponse response) throws IOException {
        // 1. 全量查询数据（不分页）
        LambdaQueryWrapper<InventoryFlow> wrapper = Wrappers.lambdaQuery();
        if (StrUtil.isNotBlank(queryDTO.getGoodsName())) {
            wrapper.like(InventoryFlow::getGoodsName, queryDTO.getGoodsName());
        }
        if (StrUtil.isNotBlank(queryDTO.getOperateType())) {
            Integer typeCode = convertOperateTypeToCode(queryDTO.getOperateType());
            if (typeCode != null) {
                wrapper.eq(InventoryFlow::getOperateType, typeCode);
            }
        }
        if (StrUtil.isNotBlank(queryDTO.getStartTime())) {
            wrapper.ge(InventoryFlow::getCreateTime, queryDTO.getStartTime());
        }
        if (StrUtil.isNotBlank(queryDTO.getEndTime())) {
            wrapper.le(InventoryFlow::getCreateTime, queryDTO.getEndTime());
        }
        wrapper.orderByDesc(InventoryFlow::getCreateTime);
        List<InventoryFlow> flowList = flowMapper.selectList(wrapper);

        // 2. 设置响应头（简化示例，实际项目建议用EasyPOI/阿里POI实现）
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        String fileName = URLEncoder.encode("库存流水.xlsx", "UTF-8");
        response.setHeader("Content-Disposition", "attachment;filename=" + fileName);

        // 实际导出逻辑请替换为你的Excel生成代码
        response.getWriter().write("Excel导出功能请根据项目依赖完善，当前为占位实现");
    }

    /**
     * 将实体转换为VO
     */
    private InventoryFlowVO convertToVO(InventoryFlow flow) {
        InventoryFlowVO vo = new InventoryFlowVO();
        vo.setId(flow.getId().toString());
        vo.setGoodsId(flow.getGoodsId().toString());
        vo.setGoodsName(flow.getGoodsName());
        vo.setBeforeStock(flow.getBeforeStock());
        vo.setChangeStock(flow.getChangeStock());
        vo.setAfterStock(flow.getAfterStock());
        vo.setOperateType(convertOperateTypeToEnum(flow.getOperateType()));
        vo.setOperator(flow.getOperator());
        vo.setCreateTime(flow.getCreateTime());
        vo.setBizNo(flow.getBizNo());
        vo.setRemark(flow.getRemark());
        vo.setOperateTypeName(convertOperateTypeToName(flow.getOperateType()));
        return vo;
    }

    /**
     * 操作类型枚举转数字
     */
    private Integer convertOperateTypeToCode(String operateType) {
        if (operateType == null) return null;
        switch (operateType) {
            case "RECEIPT": return 1;
            case "OUTBOUND": return 2;
            case "LOCK": return 3;
            case "UNLOCK": return 4;
            case "ADJUST": return 5;
            default: return null;
        }
    }

    /**
     * 操作类型数字转枚举
     */
    private String convertOperateTypeToEnum(Integer operateType) {
        if (operateType == null) return null;
        switch (operateType) {
            case 1: return "RECEIPT";
            case 2: return "OUTBOUND";
            case 3: return "LOCK";
            case 4: return "UNLOCK";
            case 5: return "ADJUST";
            default: return null;
        }
    }

    /**
     * 操作类型数字转中文名称
     */
    private String convertOperateTypeToName(Integer operateType) {
        if (operateType == null) return null;
        switch (operateType) {
            case 1: return "入库";
            case 2: return "出库";
            case 3: return "锁定";
            case 4: return "解锁";
            case 5: return "调整";
            default: return "未知";
        }
    }
}




