package com.ruoyi.common.core.domain;

/**
 * 辅料图片。
 */
public class FabricAccessoryImage extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long accessoryId;
    private String imageUrl;
    private String thumbnailUrl;
    private Integer sortOrder;

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public Long getAccessoryId()
    {
        return accessoryId;
    }

    public void setAccessoryId(Long accessoryId)
    {
        this.accessoryId = accessoryId;
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

    public Integer getSortOrder()
    {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder)
    {
        this.sortOrder = sortOrder;
    }
}
