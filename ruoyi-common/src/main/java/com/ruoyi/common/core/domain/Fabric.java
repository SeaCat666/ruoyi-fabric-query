package com.ruoyi.common.core.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 面料档案。
 */
public class Fabric extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long id;

    /** 系统自动生成的唯一编号。 */
    private String code;

    /** 录入日期，由服务端在新增时生成，修改时保持不变。 */
    private LocalDate entryDate;

    /** 查询和归档使用的年份。 */
    private Integer year;

    private Long categoryId;

    private Long supplierId;

    private String productName;

    /** 克重，单位g/㎡。 */
    private BigDecimal weight;

    /** 包边门幅，单位cm。 */
    private BigDecimal width;

    private String colorNo;

    /** 根据多行成分自动推导的组合代码。 */
    private String compositionCode;

    /** 列表展示用的成分摘要。 */
    private String compositionSummary;

    private BigDecimal priceValue;

    /** M、Y、KG、ROLL。 */
    private String priceUnit;

    /** 折算后的元/米价格；元/卷暂不折算。 */
    private BigDecimal meterPrice;

    private String notes;

    private String delFlag;

    /** 关联查询字段。 */
    private String supplierName;

    private String categoryName;

    /** 录入员显示名称，格式为“昵称（账号）”。 */
    private String recorderName;

    private String imageUrl;

    private String thumbnailUrl;

    /** ImageUpload组件使用的逗号分隔地址。 */
    private String imageUrls;

    private List<FabricImage> images = new ArrayList<>();
    /** 当前档案关联的有效库存批次数量。 */
    private Integer inventoryStockCount;

    private List<FabricComposition> compositions = new ArrayList<>();

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public String getCode()
    {
        return code;
    }

    public void setCode(String code)
    {
        this.code = code;
    }

    public LocalDate getEntryDate()
    {
        return entryDate;
    }

    public void setEntryDate(LocalDate entryDate)
    {
        this.entryDate = entryDate;
    }

    public Integer getYear()
    {
        return year;
    }

    public void setYear(Integer year)
    {
        this.year = year;
    }

    @NotNull(message = "面料分类不能为空")
    public Long getCategoryId()
    {
        return categoryId;
    }

    public void setCategoryId(Long categoryId)
    {
        this.categoryId = categoryId;
    }

    @NotNull(message = "供应商不能为空")
    public Long getSupplierId()
    {
        return supplierId;
    }

    public void setSupplierId(Long supplierId)
    {
        this.supplierId = supplierId;
    }

    @NotBlank(message = "品名不能为空")
    @Size(max = 200, message = "品名不能超过200个字符")
    public String getProductName()
    {
        return productName;
    }

    public void setProductName(String productName)
    {
        this.productName = productName;
    }

    @NotNull(message = "克重不能为空")
    @DecimalMin(value = "1", message = "克重必须大于0")
    @Digits(integer = 10, fraction = 0, message = "克重必须填写整数")
    public BigDecimal getWeight()
    {
        return weight;
    }

    public void setWeight(BigDecimal weight)
    {
        this.weight = weight;
    }

    @NotNull(message = "包边门幅不能为空")
    @DecimalMin(value = "1", message = "包边门幅必须大于0")
    @Digits(integer = 10, fraction = 0, message = "包边门幅必须填写整数")
    public BigDecimal getWidth()
    {
        return width;
    }

    public void setWidth(BigDecimal width)
    {
        this.width = width;
    }

    @Size(max = 100, message = "色号不能超过100个字符")
    public String getColorNo()
    {
        return colorNo;
    }

    public void setColorNo(String colorNo)
    {
        this.colorNo = colorNo;
    }

    public String getCompositionCode()
    {
        return compositionCode;
    }

    public void setCompositionCode(String compositionCode)
    {
        this.compositionCode = compositionCode;
    }

    public String getCompositionSummary()
    {
        return compositionSummary;
    }

    public void setCompositionSummary(String compositionSummary)
    {
        this.compositionSummary = compositionSummary;
    }

    @NotNull(message = "价格数值不能为空")
    @DecimalMin(value = "0.01", message = "价格数值必须大于0")
    @Digits(integer = 14, fraction = 2, message = "价格最多保留两位小数")
    public BigDecimal getPriceValue()
    {
        return priceValue;
    }

    public void setPriceValue(BigDecimal priceValue)
    {
        this.priceValue = priceValue;
    }

    @NotBlank(message = "计价单位不能为空")
    @Pattern(regexp = "M|Y|KG|ROLL", message = "计价单位不正确")
    public String getPriceUnit()
    {
        return priceUnit;
    }

    public void setPriceUnit(String priceUnit)
    {
        this.priceUnit = priceUnit;
    }

    public BigDecimal getMeterPrice()
    {
        return meterPrice;
    }

    public void setMeterPrice(BigDecimal meterPrice)
    {
        this.meterPrice = meterPrice;
    }

    @Size(max = 2000, message = "备注不能超过2000个字符")
    public String getNotes()
    {
        return notes;
    }

    public void setNotes(String notes)
    {
        this.notes = notes;
    }

    public String getDelFlag()
    {
        return delFlag;
    }

    public void setDelFlag(String delFlag)
    {
        this.delFlag = delFlag;
    }

    public String getSupplierName()
    {
        return supplierName;
    }

    public void setSupplierName(String supplierName)
    {
        this.supplierName = supplierName;
    }

    public String getCategoryName()
    {
        return categoryName;
    }

    public void setCategoryName(String categoryName)
    {
        this.categoryName = categoryName;
    }

    public String getRecorderName()
    {
        return recorderName;
    }

    public void setRecorderName(String recorderName)
    {
        this.recorderName = recorderName;
    }

    public String getImageUrl()
    {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl)
    {
        this.imageUrl = imageUrl;
    }

    public String getThumbnailUrl()
    {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl)
    {
        this.thumbnailUrl = thumbnailUrl;
    }

    @NotBlank(message = "请至少上传一张面料图片")
    public String getImageUrls()
    {
        return imageUrls;
    }

    public void setImageUrls(String imageUrls)
    {
        this.imageUrls = imageUrls;
    }

    public List<FabricImage> getImages()
    {
        return images;
    }

    public void setImages(List<FabricImage> images)
    {
        this.images = images;
    }

    public Integer getInventoryStockCount()
    {
        return inventoryStockCount;
    }

    public void setInventoryStockCount(Integer inventoryStockCount)
    {
        this.inventoryStockCount = inventoryStockCount;
    }

    @Valid
    @Size(min = 1, max = 4, message = "成分必须填写且最多4项")
    public List<FabricComposition> getCompositions()
    {
        return compositions;
    }

    public void setCompositions(List<FabricComposition> compositions)
    {
        this.compositions = compositions;
    }
}
