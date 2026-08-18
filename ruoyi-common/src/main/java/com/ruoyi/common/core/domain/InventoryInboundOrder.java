package com.ruoyi.common.core.domain;

import java.time.LocalDate;
import java.util.List;

/** 入库单。 */
public class InventoryInboundOrder extends BaseEntity
{
    private static final long serialVersionUID = 1L;
    private Long id;
    private String orderNo;
    private LocalDate orderDate;
    private String status;
    private String operatorName;
    private List<InventoryInboundDetail> details;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public LocalDate getOrderDate() { return orderDate; }
    public void setOrderDate(LocalDate orderDate) { this.orderDate = orderDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getOperatorName() { return operatorName; }
    public void setOperatorName(String operatorName) { this.operatorName = operatorName; }
    public List<InventoryInboundDetail> getDetails() { return details; }
    public void setDetails(List<InventoryInboundDetail> details) { this.details = details; }
}
