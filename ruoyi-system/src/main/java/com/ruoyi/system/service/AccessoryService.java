package com.ruoyi.system.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.core.domain.FabricAccessory;
import com.ruoyi.common.core.domain.FabricAccessoryImage;
import com.ruoyi.common.core.domain.FabricAccessorySupplier;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.mapper.AccessoryMapper;
import com.ruoyi.system.mapper.InventoryMapper;

@Service
public class AccessoryService
{
    private static final int MAX_IMAGES = 8;
    private static final Map<Integer, Object> CODE_SEQUENCE_LOCKS = new ConcurrentHashMap<>();

    @Autowired
    private AccessoryMapper mapper;

    @Autowired
    private InventoryMapper inventoryMapper;

    public List<FabricAccessory> selectList(FabricAccessory query)
    {
        List<FabricAccessory> accessories = mapper.selectAccessoryList(query);
        populateImages(accessories);
        return accessories;
    }

    public FabricAccessory selectById(Long id)
    {
        FabricAccessory accessory = mapper.selectAccessoryById(id);
        if (accessory != null)
        {
            populateImages(Collections.singletonList(accessory));
        }
        return accessory;
    }

    public List<FabricAccessory> selectRecent()
    {
        List<FabricAccessory> accessories = mapper.selectRecentAccessories();
        populateImages(accessories);
        return accessories;
    }

    @Transactional
    public int insert(FabricAccessory accessory)
    {
        accessory.setEntryDate(LocalDate.now());
        prepareAccessory(accessory);
        accessory.setCode(nextAccessoryCode(accessory.getYear()));
        int rows = mapper.insertAccessory(accessory);
        replaceImages(accessory);
        return rows;
    }

    @Transactional
    public int update(FabricAccessory accessory)
    {
        FabricAccessory existing = accessory.getId() == null
                ? null : mapper.selectAccessoryById(accessory.getId());
        if (existing == null)
        {
            throw new ServiceException("辅料记录不存在或已删除");
        }
        accessory.setEntryDate(existing.getEntryDate());
        prepareAccessory(accessory);
        accessory.setCode(existing.getYear().equals(accessory.getYear())
                ? existing.getCode() : nextAccessoryCode(accessory.getYear()));
        int rows = mapper.updateAccessory(accessory);
        replaceImages(accessory);
        return rows;
    }

    @Transactional
    public int delete(Long[] ids)
    {
        if (ids == null || ids.length == 0)
        {
            throw new ServiceException("请选择要删除的辅料");
        }
        // 与库存新增统一锁定档案行，避免“校验后、软删除前”并发插入库存造成悬空关联。
        inventoryMapper.selectActiveAccessoryIdsForUpdate(ids);
        List<String> referencedCodes = inventoryMapper.selectReferencedAccessoryCodes(ids);
        if (!referencedCodes.isEmpty())
        {
            throw new ServiceException("辅料" + String.join("、", referencedCodes)
                    + "已关联库存台账，不能删除；档案需永久保留用于库存和流水追溯");
        }
        return mapper.deleteAccessoryByIds(ids);
    }

    public List<FabricAccessorySupplier> selectSupplierList()
    {
        return mapper.selectSupplierList();
    }

    @Transactional
    public FabricAccessorySupplier createSupplier(FabricAccessorySupplier supplier)
    {
        prepareSupplier(supplier);
        FabricAccessorySupplier existing = mapper.selectSupplierByName(supplier.getName());
        if (existing != null)
        {
            throw new ServiceException("辅料供应商名称已存在");
        }
        mapper.insertSupplier(supplier);
        return supplier;
    }

    @Transactional
    public int updateSupplier(FabricAccessorySupplier supplier)
    {
        if (supplier.getId() == null || mapper.selectSupplierById(supplier.getId()) == null)
        {
            throw new ServiceException("辅料供应商不存在");
        }
        prepareSupplier(supplier);
        FabricAccessorySupplier sameName = mapper.selectSupplierByName(supplier.getName());
        if (sameName != null && !sameName.getId().equals(supplier.getId()))
        {
            throw new ServiceException("辅料供应商名称已存在");
        }
        return mapper.updateSupplier(supplier);
    }

    @Transactional
    public int deleteSupplier(Long id)
    {
        if (id == null || mapper.selectSupplierById(id) == null)
        {
            throw new ServiceException("辅料供应商不存在");
        }
        if (mapper.countAccessoriesBySupplierId(id) > 0)
        {
            throw new ServiceException("该辅料供应商已被使用，不能删除");
        }
        return mapper.deleteSupplierById(id);
    }

    private void prepareAccessory(FabricAccessory accessory)
    {
        if (accessory.getEntryDate() == null)
        {
            throw new ServiceException("日期不能为空");
        }
        accessory.setYear(accessory.getEntryDate().getYear());
        accessory.setSizeSpec(limit(clean(accessory.getSizeSpec()), 200));
        accessory.setBulkPrice(limit(clean(accessory.getBulkPrice()), 200));
        accessory.setRegularUse(normalizeNullableFlag(accessory.getRegularUse()));
        accessory.setCompliant(normalizeNullableFlag(accessory.getCompliant()));
        accessory.setNotes(limit(clean(accessory.getNotes()), 1000));
        if (accessory.getSupplierId() == null
                || mapper.selectSupplierById(accessory.getSupplierId()) == null)
        {
            throw new ServiceException("请选择有效的辅料供应商");
        }
        normalizeImages(accessory);
    }

    private String nextAccessoryCode(Integer year)
    {
        Object yearLock = CODE_SEQUENCE_LOCKS.computeIfAbsent(year, key -> new Object());
        synchronized (yearLock)
        {
            mapper.mergeCodeSequence(year);
            Integer currentValue = mapper.selectCodeSequenceForUpdate(year);
            if (currentValue == null || currentValue >= 9999)
            {
                throw new ServiceException(year + "年度辅料流水号已超过4位上限");
            }
            int sequence = currentValue + 1;
            if (mapper.incrementCodeSequence(year) != 1)
            {
                throw new ServiceException("辅料流水号生成失败，请重试");
            }
            return String.format(Locale.ROOT, "B-%02d%04d",
                    Math.floorMod(year, 100), sequence);
        }
    }

    private void normalizeImages(FabricAccessory accessory)
    {
        String imageUrls = clean(accessory.getImageUrls());
        if (imageUrls == null)
        {
            throw new ServiceException("请至少上传一张辅料图片");
        }
        Set<String> urls = Arrays.stream(imageUrls.split(","))
                .map(AccessoryService::clean)
                .filter(value -> value != null)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (urls.isEmpty())
        {
            throw new ServiceException("请至少上传一张辅料图片");
        }
        if (urls.size() > MAX_IMAGES)
        {
            throw new ServiceException("辅料图片不能超过" + MAX_IMAGES + "张");
        }
        accessory.setImageUrls(String.join(",", urls));
    }

    private void replaceImages(FabricAccessory accessory)
    {
        mapper.deleteImagesByAccessoryId(accessory.getId());
        int sortOrder = 0;
        for (String imageUrl : accessory.getImageUrls().split(","))
        {
            FabricAccessoryImage image = new FabricAccessoryImage();
            image.setAccessoryId(accessory.getId());
            image.setImageUrl(limit(clean(imageUrl), 500));
            image.setThumbnailUrl(limit(toThumbnailUrl(imageUrl), 500));
            image.setSortOrder(sortOrder++);
            mapper.insertAccessoryImage(image);
        }
    }

    private void populateImages(List<FabricAccessory> accessories)
    {
        if (accessories == null || accessories.isEmpty())
        {
            return;
        }
        List<Long> ids = accessories.stream()
                .map(FabricAccessory::getId)
                .filter(id -> id != null)
                .collect(Collectors.toCollection(ArrayList::new));
        if (ids.isEmpty())
        {
            return;
        }
        Map<Long, List<FabricAccessoryImage>> imagesByAccessoryId =
                mapper.selectImagesByAccessoryIds(ids).stream()
                        .collect(Collectors.groupingBy(
                                FabricAccessoryImage::getAccessoryId,
                                LinkedHashMap::new,
                                Collectors.toList()));
        for (FabricAccessory accessory : accessories)
        {
            List<FabricAccessoryImage> images = imagesByAccessoryId.getOrDefault(
                    accessory.getId(), Collections.emptyList());
            accessory.setImages(images);
            accessory.setImageUrls(images.stream()
                    .map(FabricAccessoryImage::getImageUrl)
                    .filter(url -> url != null)
                    .collect(Collectors.joining(",")));
        }
    }

    private static String toThumbnailUrl(String imageUrl)
    {
        String testMarker = "/profile/accessory/test/original/";
        if (imageUrl != null && imageUrl.contains(testMarker))
        {
            return imageUrl.replace(
                    testMarker, "/profile/accessory/test/thumbnail/");
        }
        String marker = "/profile/accessory/";
        if (imageUrl != null && imageUrl.contains(marker)
                && !imageUrl.contains("/profile/accessory/thumbnail/"))
        {
            return imageUrl.replace(marker, "/profile/accessory/thumbnail/");
        }
        return imageUrl;
    }

    private static void prepareSupplier(FabricAccessorySupplier supplier)
    {
        supplier.setName(limit(clean(supplier.getName()), 150));
        supplier.setPhone(limit(clean(supplier.getPhone()), 100));
        supplier.setAddress(limit(clean(supplier.getAddress()), 500));
        if (supplier.getName() == null) throw new ServiceException("辅料供应商名称不能为空");
        if (supplier.getPhone() == null) throw new ServiceException("联系电话不能为空");
        if (supplier.getAddress() == null) throw new ServiceException("地址不能为空");
    }

    private static String normalizeNullableFlag(String value)
    {
        String flag = clean(value);
        if (flag == null) return null;
        if (!"0".equals(flag) && !"1".equals(flag))
        {
            throw new ServiceException("状态值不正确");
        }
        return flag;
    }

    private static String clean(String value)
    {
        if (value == null) return null;
        String result = value.replace("\uFEFF", "").trim();
        return result.isEmpty() ? null : result;
    }

    private static String limit(String value, int length)
    {
        return value != null && value.length() > length ? value.substring(0, length) : value;
    }
}
