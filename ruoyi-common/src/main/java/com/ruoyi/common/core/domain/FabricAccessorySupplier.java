package com.ruoyi.common.core.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 辅料专用供应商，与面料供应商完全独立。
 */
public class FabricAccessorySupplier extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private String phone;
    private String address;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    @NotBlank(message = "辅料供应商名称不能为空")
    @Size(max = 150, message = "辅料供应商名称不能超过150个字符")
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    @NotBlank(message = "联系电话不能为空")
    @Size(max = 100, message = "联系电话不能超过100个字符")
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    @NotBlank(message = "地址不能为空")
    @Size(max = 500, message = "地址不能超过500个字符")
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
}
