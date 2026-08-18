package com.ruoyi.common.core.domain;

import java.io.Serializable;
import java.math.BigDecimal;

/** 领用单明细。 */
public class InventoryRequisitionDetail implements Serializable
{
    private static final long serialVersionUID = 1L;
    private Long id;
    private Long orderId;
    private Long stockId;
    private String stockCode;
    private String materialCode;
    private String colorNo;
    private String primaryUnit;
    private String auxiliaryUnit;
    private BigDecimal quantity;
    private BigDecimal auxiliaryQuantity;
    private Integer sortOrder;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public Long getStockId() { return stockId; }
    public void setStockId(Long stockId) { this.stockId = stockId; }
    public String getStockCode() { return stockCode; }
    public void setStockCode(String stockCode) { this.stockCode = stockCode; }
    public String getMaterialCode() { return materialCode; }
    public void setMaterialCode(String materialCode) { this.materialCode = materialCode; }
    public String getColorNo() { return colorNo; }
    public void setColorNo(String colorNo) { this.colorNo = colorNo; }
    public String getPrimaryUnit() { return primaryUnit; }
    public void setPrimaryUnit(String primaryUnit) { this.primaryUnit = primaryUnit; }
    public String getAuxiliaryUnit() { return auxiliaryUnit; }
    public void setAuxiliaryUnit(String auxiliaryUnit) { this.auxiliaryUnit = auxiliaryUnit; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public BigDecimal getAuxiliaryQuantity() { return auxiliaryQuantity; }
    public void setAuxiliaryQuantity(BigDecimal auxiliaryQuantity) { this.auxiliaryQuantity = auxiliaryQuantity; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
}
