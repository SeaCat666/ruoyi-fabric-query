package com.ruoyi.common.core.domain;

import java.io.Serializable;

/**
 * 可选的单一面料成分。
 */
public class FabricComponentType implements Serializable
{
    private static final long serialVersionUID = 1L;

    private String code;

    private String nameCn;

    private String nameEn;

    private Integer sortOrder;

    public String getCode()
    {
        return code;
    }

    public void setCode(String code)
    {
        this.code = code;
    }

    public String getNameCn()
    {
        return nameCn;
    }

    public void setNameCn(String nameCn)
    {
        this.nameCn = nameCn;
    }

    public String getNameEn()
    {
        return nameEn;
    }

    public void setNameEn(String nameEn)
    {
        this.nameEn = nameEn;
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
