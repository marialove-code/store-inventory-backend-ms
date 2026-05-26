package com.inventory.entity.goods;

import lombok.Data;

import java.util.List;

/**
 * 批量上下架DTO
 * 用于接收前端传递的ID集合 + 状态
 */
@Data
public  class BatchShelfDTO {
    private List<String> ids;
    private Integer shelfStatus;

    public List<String> getIds() { return ids; }
    public void setIds(List<String> ids) { this.ids = ids; }
    public Integer getShelfStatus() { return shelfStatus; }
    public void setShelfStatus(Integer shelfStatus) { this.shelfStatus = shelfStatus; }
}