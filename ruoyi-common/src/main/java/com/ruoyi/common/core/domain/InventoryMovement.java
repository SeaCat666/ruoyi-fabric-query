package com.ruoyi.common.core.domain;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 不可变库存流水。 */
public class InventoryMovement implements Serializable
{
    private static final long serialVersionUID = 1L;
    private Long id;
    private String movementNo;
    private Long stockId;
    private String stockCode;
    private String materialCode;
    private String colorNo;
    private String movementType;
    private String businessType;
    private Long businessId;
    private String businessNo;
    private BigDecimal quantityChange;
    private BigDecimal lockedChange;
    private BigDecimal auxiliaryChange;
    private BigDecimal lockedAuxiliaryChange;
    private BigDecimal balanceQty;
    private BigDecimal balanceLockedQty;
    private BigDecimal balanceAuxQty;
    private BigDecimal balanceLockedAuxQty;
    private String operatorName;
    private LocalDateTime operationTime;
    private String remark;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getMovementNo() { return movementNo; }
    public void setMovementNo(String movementNo) { this.movementNo = movementNo; }
    public Long getStockId() { return stockId; }
    public void setStockId(Long stockId) { this.stockId = stockId; }
    public String getStockCode() { return stockCode; }
    public void setStockCode(String stockCode) { this.stockCode = stockCode; }
    public String getMaterialCode() { return materialCode; }
    public void setMaterialCode(String materialCode) { this.materialCode = materialCode; }
    public String getColorNo() { return colorNo; }
    public void setColorNo(String colorNo) { this.colorNo = colorNo; }
    public String getMovementType() { return movementType; }
    public void setMovementType(String movementType) { this.movementType = movementType; }
    public String getBusinessType() { return businessType; }
    public void setBusinessType(String businessType) { this.businessType = businessType; }
    public Long getBusinessId() { return businessId; }
    public void setBusinessId(Long businessId) { this.businessId = businessId; }
    public String getBusinessNo() { return businessNo; }
    public void setBusinessNo(String businessNo) { this.businessNo = businessNo; }
    public BigDecimal getQuantityChange() { return quantityChange; }
    public void setQuantityChange(BigDecimal quantityChange) { this.quantityChange = quantityChange; }
    public BigDecimal getLockedChange() { return lockedChange; }
    public void setLockedChange(BigDecimal lockedChange) { this.lockedChange = lockedChange; }
    public BigDecimal getAuxiliaryChange() { return auxiliaryChange; }
    public void setAuxiliaryChange(BigDecimal auxiliaryChange) { this.auxiliaryChange = auxiliaryChange; }
    public BigDecimal getLockedAuxiliaryChange() { return lockedAuxiliaryChange; }
    public void setLockedAuxiliaryChange(BigDecimal lockedAuxiliaryChange) { this.lockedAuxiliaryChange = lockedAuxiliaryChange; }
    public BigDecimal getBalanceQty() { return balanceQty; }
    public void setBalanceQty(BigDecimal balanceQty) { this.balanceQty = balanceQty; }
    public BigDecimal getBalanceLockedQty() { return balanceLockedQty; }
    public void setBalanceLockedQty(BigDecimal balanceLockedQty) { this.balanceLockedQty = balanceLockedQty; }
    public BigDecimal getBalanceAuxQty() { return balanceAuxQty; }
    public void setBalanceAuxQty(BigDecimal balanceAuxQty) { this.balanceAuxQty = balanceAuxQty; }
    public BigDecimal getBalanceLockedAuxQty() { return balanceLockedAuxQty; }
    public void setBalanceLockedAuxQty(BigDecimal balanceLockedAuxQty) { this.balanceLockedAuxQty = balanceLockedAuxQty; }
    public String getOperatorName() { return operatorName; }
    public void setOperatorName(String operatorName) { this.operatorName = operatorName; }
    public LocalDateTime getOperationTime() { return operationTime; }
    public void setOperationTime(LocalDateTime operationTime) { this.operationTime = operationTime; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
