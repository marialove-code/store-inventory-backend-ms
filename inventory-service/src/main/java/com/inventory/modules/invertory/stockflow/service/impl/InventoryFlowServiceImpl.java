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
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 库存流水业务实现：分页查询、详情、导出占位。
 */
@Service
@RequiredArgsConstructor
public class InventoryFlowServiceImpl extends ServiceImpl<InventoryFlowMapper, InventoryFlow>
        implements InventoryFlowService {

    private final InventoryFlowMapper flowMapper;

    @Override
    public Result<?> pageFlowList(InventoryFlowQueryDTO queryDTO) {
        Page<InventoryFlow> page = new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize());
        LambdaQueryWrapper<InventoryFlow> wrapper = buildWrapper(queryDTO);
        wrapper.orderByDesc(InventoryFlow::getCreateTime);

        Page<InventoryFlow> flowPage = flowMapper.selectPage(page, wrapper);

        Page<InventoryFlowVO> voPage = new Page<>(flowPage.getCurrent(), flowPage.getSize(), flowPage.getTotal());
        List<InventoryFlowVO> voList = flowPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        voPage.setRecords(voList);
        return Result.success(voPage);
    }

    @Override
    public Result<?> getFlowDetail(String id) {
        InventoryFlow flow = flowMapper.selectById(Long.valueOf(id));
        if (flow == null) {
            return Result.fail("库存流水记录不存在");
        }
        return Result.success(convertToVO(flow));
    }

    @Override
    public void exportFlowList(InventoryFlowQueryDTO queryDTO, HttpServletResponse response) throws IOException {
        // 与单体一致：占位导出，后续可换 EasyExcel / POI
        LambdaQueryWrapper<InventoryFlow> wrapper = buildWrapper(queryDTO);
        wrapper.orderByDesc(InventoryFlow::getCreateTime);
        flowMapper.selectList(wrapper);

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        String fileName = URLEncoder.encode("库存流水.xlsx", StandardCharsets.UTF_8);
        response.setHeader("Content-Disposition", "attachment;filename=" + fileName);
        response.getWriter().write("Excel导出功能请根据项目依赖完善，当前为占位实现");
    }

    private LambdaQueryWrapper<InventoryFlow> buildWrapper(InventoryFlowQueryDTO queryDTO) {
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
        return wrapper;
    }

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

    private Integer convertOperateTypeToCode(String operateType) {
        if (operateType == null) {
            return null;
        }
        return switch (operateType) {
            case "RECEIPT" -> 1;
            case "OUTBOUND" -> 2;
            case "LOCK" -> 3;
            case "UNLOCK" -> 4;
            case "ADJUST" -> 5;
            default -> null;
        };
    }

    private String convertOperateTypeToEnum(Integer operateType) {
        if (operateType == null) {
            return null;
        }
        return switch (operateType) {
            case 1 -> "RECEIPT";
            case 2 -> "OUTBOUND";
            case 3 -> "LOCK";
            case 4 -> "UNLOCK";
            case 5 -> "ADJUST";
            default -> null;
        };
    }

    private String convertOperateTypeToName(Integer operateType) {
        if (operateType == null) {
            return null;
        }
        return switch (operateType) {
            case 1 -> "入库";
            case 2 -> "出库";
            case 3 -> "锁定";
            case 4 -> "解锁";
            case 5 -> "调整";
            default -> "未知";
        };
    }
}
