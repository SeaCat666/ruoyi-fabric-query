package com.ruoyi.common.core.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** 库存台账。 */
public class InventoryStock extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long id;
    private String stockCode;
    private String materialType;
    private Long fabricId;
    private Long accessoryId;
    private String materialCode;
    private String developmentStyleNo;
    private String skc;
    private String colorNo;
    private String goodsType;
    private String primaryUnit;
    private String auxiliaryUnit;
    private BigDecimal onHandQty;
    private BigDecimal lockedQty;
    private BigDecimal onHandAuxQty;
    private BigDecimal lockedAuxQty;
    private LocalDateTime lastCountTime;
    private Integer version;
    private String delFlag;
    private String sourceKey;
    private BigDecimal adjustQty;
    private BigDecimal adjustAuxQty;
    private List<InventoryStockImage> images;
    private String archiveCode;
    private String archiveName;
    private String archiveSupplierName;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getStockCode() { return stockCode; }
    public void setStockCode(String stockCode) { this.stockCode = stockCode; }
    public String getMaterialType() { return materialType; }
    public void setMaterialType(String materialType) { this.materialType = materialType; }
    public Long getFabricId() { return fabricId; }
    public void setFabricId(Long fabricId) { this.fabricId = fabricId; }
    public Long getAccessoryId() { return accessoryId; }
    public void setAccessoryId(Long accessoryId) { this.accessoryId = accessoryId; }
    public String getMaterialCode() { return materialCode; }
    public void setMaterialCode(String materialCode) { this.materialCode = materialCode; }
    public String getDevelopmentStyleNo() { return developmentStyleNo; }
    public void setDevelopmentStyleNo(String developmentStyleNo) { this.developmentStyleNo = developmentStyleNo; }
    public String getSkc() { return skc; }
    public void setSkc(String skc) { this.skc = skc; }
    public String getColorNo() { return colorNo; }
    public void setColorNo(String colorNo) { this.colorNo = colorNo; }
    public String getGoodsType() { return goodsType; }
    public void setGoodsType(String goodsType) { this.goodsType = goodsType; }
    public String getPrimaryUnit() { return primaryUnit; }
    public void setPrimaryUnit(String primaryUnit) { this.primaryUnit = primaryUnit; }
    public String getAuxiliaryUnit() { return auxiliaryUnit; }
    public void setAuxiliaryUnit(String auxiliaryUnit) { this.auxiliaryUnit = auxiliaryUnit; }
    public BigDecimal getOnHandQty() { return onHandQty; }
    public void setOnHandQty(BigDecimal onHandQty) { this.onHandQty = onHandQty; }
    public BigDecimal getLockedQty() { return lockedQty; }
    public void setLockedQty(BigDecimal lockedQty) { this.lockedQty = lockedQty; }
    public BigDecimal getOnHandAuxQty() { return onHandAuxQty; }
    public void setOnHandAuxQty(BigDecimal onHandAuxQty) { this.onHandAuxQty = onHandAuxQty; }
    public BigDecimal getLockedAuxQty() { return lockedAuxQty; }
    public void setLockedAuxQty(BigDecimal lockedAuxQty) { this.lockedAuxQty = lockedAuxQty; }
    public LocalDateTime getLastCountTime() { return lastCountTime; }
    public void setLastCountTime(LocalDateTime lastCountTime) { this.lastCountTime = lastCountTime; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public String getDelFlag() { return delFlag; }
    public void setDelFlag(String delFlag) { this.delFlag = delFlag; }
    public String getSourceKey() { return sourceKey; }
    public void setSourceKey(String sourceKey) { this.sourceKey = sourceKey; }
    public BigDecimal getAdjustQty() { return adjustQty; }
    public void setAdjustQty(BigDecimal adjustQty) { this.adjustQty = adjustQty; }
    public BigDecimal getAdjustAuxQty() { return adjustAuxQty; }
    public void setAdjustAuxQty(BigDecimal adjustAuxQty) { this.adjustAuxQty = adjustAuxQty; }
    public List<InventoryStockImage> getImages() { return images; }
    public void setImages(List<InventoryStockImage> images) { this.images = images; }
    public String getArchiveCode() { return archiveCode; }
    public void setArchiveCode(String archiveCode) { this.archiveCode = archiveCode; }
    public String getArchiveName() { return archiveName; }
    public void setArchiveName(String archiveName) { this.archiveName = archiveName; }
    public String getArchiveSupplierName() { return archiveSupplierName; }
    public void setArchiveSupplierName(String archiveSupplierName) { this.archiveSupplierName = archiveSupplierName; }
}
