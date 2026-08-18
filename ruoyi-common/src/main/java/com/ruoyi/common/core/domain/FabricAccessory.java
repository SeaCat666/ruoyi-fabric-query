package com.ruoyi.common.core.domain;

import java.time.LocalDate;
import java.util.List;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 辅料档案。
 */
public class FabricAccessory extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long id;
    private String code;
    /** 录入日期，由服务端在新增时生成，修改时保持不变。 */
    private LocalDate entryDate;
    private Integer year;
    private Long supplierId;
    private String supplierName;
    private String supplierPhone;
    private String supplierAddress;
    private String sizeSpec;
    private String bulkPrice;
    /** 1是、0否、null未设置。 */
    private String regularUse;
    /** 1是、0否、null未设置。 */
    private String compliant;
    private String notes;
    private String delFlag;
    /** 录入员显示名称，格式为"昵称（账号）"。 */
    private String recorderName;
    /** 表单提交的逗号分隔原图地址。 */
    private String imageUrls;
    private List<FabricAccessoryImage> images;
    /** 当前档案关联的有效库存批次数量。 */
    private Integer inventoryStockCount;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    @Size(max = 64, message = "辅料编号不能超过64个字符")
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public LocalDate getEntryDate() { return entryDate; }
    public void setEntryDate(LocalDate entryDate) { this.entryDate = entryDate; }

    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }

    @NotNull(message = "辅料供应商不能为空")
    public Long getSupplierId() { return supplierId; }
    public void setSupplierId(Long supplierId) { this.supplierId = supplierId; }

    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }
    public String getSupplierPhone() { return supplierPhone; }
    public void setSupplierPhone(String supplierPhone) { this.supplierPhone = supplierPhone; }
    public String getSupplierAddress() { return supplierAddress; }
    public void setSupplierAddress(String supplierAddress) { this.supplierAddress = supplierAddress; }

    @Size(max = 200, message = "尺寸不能超过200个字符")
    public String getSizeSpec() { return sizeSpec; }
    public void setSizeSpec(String sizeSpec) { this.sizeSpec = sizeSpec; }

    @Size(max = 200, message = "大货价不能超过200个字符")
    public String getBulkPrice() { return bulkPrice; }
    public void setBulkPrice(String bulkPrice) { this.bulkPrice = bulkPrice; }

    @Pattern(regexp = "0|1", message = "常规使用状态不正确")
    public String getRegularUse() { return regularUse; }
    public void setRegularUse(String regularUse) { this.regularUse = regularUse; }

    @Pattern(regexp = "0|1", message = "合规状态不正确")
    public String getCompliant() { return compliant; }
    public void setCompliant(String compliant) { this.compliant = compliant; }

    @Size(max = 1000, message = "备注不能超过1000个字符")
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getDelFlag() { return delFlag; }
    public void setDelFlag(String delFlag) { this.delFlag = delFlag; }

    public String getRecorderName() { return recorderName; }
    public void setRecorderName(String recorderName) { this.recorderName = recorderName; }

    @NotBlank(message = "请至少上传一张辅料图片")
    public String getImageUrls() { return imageUrls; }
    public void setImageUrls(String imageUrls) { this.imageUrls = imageUrls; }

    public List<FabricAccessoryImage> getImages() { return images; }
    public void setImages(List<FabricAccessoryImage> images) { this.images = images; }
    public Integer getInventoryStockCount() { return inventoryStockCount; }
    public void setInventoryStockCount(Integer inventoryStockCount) { this.inventoryStockCount = inventoryStockCount; }
}
