package com.ruoyi.common.core.domain;

/**
 * 面料图片。
 */
public class FabricImage extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    public static final String COLOR_CARD_FRONT = "color_card_front";
    public static final String COLOR_CARD_BACK = "color_card_back";
    public static final String FABRIC_DETAIL = "fabric_detail";
    public static final String OTHER = "other";

    private Long id;

    private Long fabricId;

    private String imageUrl;

    private String thumbnailUrl;

    private String imageType;

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

    public String getImageType()
    {
        return imageType;
    }

    public void setImageType(String imageType)
    {
        this.imageType = imageType;
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
