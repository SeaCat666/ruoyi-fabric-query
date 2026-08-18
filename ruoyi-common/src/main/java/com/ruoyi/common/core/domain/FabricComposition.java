package com.ruoyi.common.core.domain;

import java.io.Serializable;
import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * 面料成分明细。
 */
public class FabricComposition implements Serializable
{
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long fabricId;

    private String componentCode;

    private String componentNameCn;

    private String componentNameEn;

    private BigDecimal percentage;

    private Integer sortOrder;

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public Long getFabricId()
    {
        return fabricId;
    }

    public void setFabricId(Long fabricId)
    {
        this.fabricId = fabricId;
    }

    @NotBlank(message = "请选择成分")
    @Pattern(regexp = "PO|CO|VI|SP|LI|PA|PU|OT", message = "成分代码不正确")
    public String getComponentCode()
    {
        return componentCode;
    }

    public void setComponentCode(String componentCode)
    {
        this.componentCode = componentCode;
    }

    public String getComponentNameCn()
    {
        return componentNameCn;
    }

    public void setComponentNameCn(String componentNameCn)
    {
        this.componentNameCn = componentNameCn;
    }

    public String getComponentNameEn()
    {
        return componentNameEn;
    }

    public void setComponentNameEn(String componentNameEn)
    {
        this.componentNameEn = componentNameEn;
    }

    @NotNull(message = "成分比例不能为空")
    @DecimalMin(value = "1", message = "成分比例不能小于1%")
    @DecimalMax(value = "100", message = "成分比例不能大于100%")
    @Digits(integer = 3, fraction = 0, message = "成分比例必须填写整数")
    public BigDecimal getPercentage()
    {
        return percentage;
    }

    public void setPercentage(BigDecimal percentage)
    {
        this.percentage = percentage;
    }

    public Integer getSortOrder()
    {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder)
    {
        this.sortOrder = sortOrder;
    }
}
