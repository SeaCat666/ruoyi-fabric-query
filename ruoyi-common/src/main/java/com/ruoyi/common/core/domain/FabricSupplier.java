package com.ruoyi.common.core.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 面料供应商。
 */
public class FabricSupplier extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long id;

    private String name;

    private String phone;

    private String address;

    private String remarks;

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    @NotBlank(message = "供应商名称不能为空")
    @Size(max = 150, message = "供应商名称不能超过150个字符")
    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    @NotBlank(message = "联系电话不能为空")
    @Size(max = 100, message = "联系电话不能超过100个字符")
    public String getPhone()
    {
        return phone;
    }

    public void setPhone(String phone)
    {
        this.phone = phone;
    }

    @NotBlank(message = "地址不能为空")
    @Size(max = 500, message = "地址不能超过500个字符")
    public String getAddress()
    {
        return address;
    }

    public void setAddress(String address)
    {
        this.address = address;
    }

    @Size(max = 1000, message = "备注不能超过1000个字符")
    public String getRemarks()
    {
        return remarks;
    }

    public void setRemarks(String remarks)
    {
        this.remarks = remarks;
    }
}
