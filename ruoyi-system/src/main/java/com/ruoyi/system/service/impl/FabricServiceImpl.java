package com.ruoyi.system.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
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
import com.ruoyi.common.core.domain.Fabric;
import com.ruoyi.common.core.domain.FabricAccessory;
import com.ruoyi.common.core.domain.FabricCategory;
import com.ruoyi.common.core.domain.FabricComponentType;
import com.ruoyi.common.core.domain.FabricComposition;
import com.ruoyi.common.core.domain.FabricImage;
import com.ruoyi.common.core.domain.FabricSupplier;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.mapper.AccessoryMapper;
import com.ruoyi.system.mapper.FabricMapper;
import com.ruoyi.system.mapper.InventoryMapper;
import com.ruoyi.system.service.AccessoryService;
import com.ruoyi.system.service.IFabricService;

/**
 * 面料服务实现。
 */
@Service
public class FabricServiceImpl implements IFabricService
{
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    private static final BigDecimal YARD_IN_METERS = new BigDecimal("0.9144");
    private static final BigDecimal ONE_THOUSAND = new BigDecimal("1000");
    private static final int MAX_IMAGES = 8;
    private static final Map<Integer, Object> CODE_SEQUENCE_LOCKS = new ConcurrentHashMap<>();

    private static final Map<String, String> COMPONENT_NAMES = new LinkedHashMap<>();
    private static final Map<String, String> COMBINATION_CODES = new HashMap<>();

    static
    {
        COMPONENT_NAMES.put("PO", "涤纶/聚酯");
        COMPONENT_NAMES.put("CO", "全棉");
        COMPONENT_NAMES.put("VI", "人丝/粘胶");
        COMPONENT_NAMES.put("SP", "氨纶");
        COMPONENT_NAMES.put("LI", "亚麻");
        COMPONENT_NAMES.put("PA", "尼龙");
        COMPONENT_NAMES.put("PU", "人造皮革");
        COMPONENT_NAMES.put("OT", "其他");

        COMBINATION_CODES.put("PO", "PO");
        COMBINATION_CODES.put("CO", "CO");
        COMBINATION_CODES.put("VI", "VI");
        COMBINATION_CODES.put("SP", "SP");
        COMBINATION_CODES.put("LI", "LI");
        COMBINATION_CODES.put("PA", "PA");
        COMBINATION_CODES.put("PU", "PU");
        COMBINATION_CODES.put("OT", "OT");
        COMBINATION_CODES.put("CO+PO", "TC");
        COMBINATION_CODES.put("PO+VI", "TR");
        COMBINATION_CODES.put("CO+SP", "COSP");
        COMBINATION_CODES.put("PO+SP", "POSP");
        COMBINATION_CODES.put("PA+SP", "PASP");
        COMBINATION_CODES.put("CO+LI", "LICO");
        COMBINATION_CODES.put("LI+VI", "LIVI");
        COMBINATION_CODES.put("LI+PO", "LIPO");
        COMBINATION_CODES.put("PO+SP+VI", "TRSP");
    }

    @Autowired
    private FabricMapper fabricMapper;

    @Autowired
    private AccessoryMapper accessoryMapper;

    @Autowired
    private AccessoryService accessoryService;

    @Autowired
    private InventoryMapper inventoryMapper;

    @Override
    public List<Fabric> selectFabricList(Fabric fabric)
    {
        List<Fabric> fabrics = fabricMapper.selectFabricList(fabric);
        populateDetails(fabrics);
        return fabrics;
    }

    @Override
    public Fabric selectFabricById(Long id)
    {
        Fabric fabric = fabricMapper.selectFabricById(id);
        if (fabric != null)
        {
            populateDetails(Collections.singletonList(fabric));
            fabric.setImageUrls(fabric.getImages().stream()
                    .map(FabricImage::getImageUrl)
                    .collect(Collectors.joining(",")));
        }
        return fabric;
    }

    @Override
    public Map<String, Object> selectDashboard()
    {
        int currentYear = LocalDate.now().getYear();
        int total = fabricMapper.countFabricTotal();
        int withImages = fabricMapper.countFabricsWithImages();
        int totalAccessories = accessoryMapper.countAccessoryTotal();
        int accessoriesWithImages = accessoryMapper.countAccessoriesWithImages();
        List<Fabric> recentFabrics = fabricMapper.selectRecentFabrics();
        List<FabricAccessory> recentAccessories = accessoryService.selectRecent();
        populateDetails(recentFabrics);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalFabrics", total);
        summary.put("totalAccessories", totalAccessories);
        summary.put("totalRecords", total + totalAccessories);
        summary.put("currentYearFabrics", fabricMapper.countFabricByYear(currentYear));
        summary.put("currentYearAccessories", accessoryMapper.countAccessoryByYear(currentYear));
        summary.put("supplierCount", fabricMapper.countFabricSuppliers());
        summary.put("fabricSupplierCount", fabricMapper.countFabricSuppliers());
        summary.put("accessorySupplierCount", accessoryMapper.countAccessorySuppliers());
        summary.put("imageCount", fabricMapper.countFabricImages());
        summary.put("accessoryImageCount", accessoryMapper.countAccessoryImages());
        summary.put("imageCoverage", total == 0 ? 0
                : BigDecimal.valueOf(withImages * 100D / total)
                        .setScale(1, RoundingMode.HALF_UP));
        summary.put("accessoryImageCoverage", totalAccessories == 0 ? 0
                : BigDecimal.valueOf(accessoriesWithImages * 100D / totalAccessories)
                        .setScale(1, RoundingMode.HALF_UP));

        Map<String, Object> dashboard = new LinkedHashMap<>();
        dashboard.put("currentYear", currentYear);
        dashboard.put("summary", summary);
        dashboard.put("yearStats", fabricMapper.selectFabricYearStats());
        dashboard.put("accessoryYearStats", accessoryMapper.selectAccessoryYearStats());
        dashboard.put("categoryStats", fabricMapper.selectFabricCategoryStats());
        dashboard.put("priceUnitStats", fabricMapper.selectFabricPriceUnitStats());
        dashboard.put("recentFabrics", recentFabrics);
        dashboard.put("recentAccessories", recentAccessories);
        return dashboard;
    }

    @Override
    public List<FabricSupplier> selectSupplierList()
    {
        return fabricMapper.selectSupplierList();
    }

    @Override
    @Transactional
    public FabricSupplier createSupplier(FabricSupplier supplier)
    {
        prepareSupplier(supplier);

        FabricSupplier existing = fabricMapper.selectSupplierByName(supplier.getName());
        if (existing != null)
        {
            throw new ServiceException("供应商名称已存在");
        }
        fabricMapper.insertSupplier(supplier);
        return supplier;
    }

    @Override
    @Transactional
    public int updateSupplier(FabricSupplier supplier)
    {
        if (supplier.getId() == null || fabricMapper.selectSupplierById(supplier.getId()) == null)
        {
            throw new ServiceException("供应商不存在");
        }
        prepareSupplier(supplier);
        FabricSupplier sameName = fabricMapper.selectSupplierByName(supplier.getName());
        if (sameName != null && !sameName.getId().equals(supplier.getId()))
        {
            throw new ServiceException("供应商名称已存在");
        }
        return fabricMapper.updateSupplier(supplier);
    }

    @Override
    @Transactional
    public int deleteSupplier(Long id)
    {
        if (id == null || fabricMapper.selectSupplierById(id) == null)
        {
            throw new ServiceException("供应商不存在");
        }
        if (fabricMapper.countFabricsBySupplierId(id) > 0)
        {
            throw new ServiceException("该供应商已被面料档案（含已删除记录）使用，不能删除");
        }
        return fabricMapper.deleteSupplierById(id);
    }

    @Override
    public List<FabricCategory> selectCategoryList()
    {
        List<FabricCategory> categories = fabricMapper.selectCategoryList();
        Map<Long, FabricCategory> categoriesById = categories.stream()
                .collect(Collectors.toMap(
                        FabricCategory::getId,
                        category -> category,
                        (left, right) -> left,
                        LinkedHashMap::new));
        List<FabricCategory> roots = new ArrayList<>();
        for (FabricCategory category : categories)
        {
            if (category.getParentId() == null)
            {
                roots.add(category);
                continue;
            }
            FabricCategory parent = categoriesById.get(category.getParentId());
            if (parent != null)
            {
                parent.getChildren().add(category);
            }
        }
        return roots;
    }

    @Override
    @Transactional
    public FabricCategory createCategory(FabricCategory category)
    {
        prepareCategory(category);
        if (fabricMapper.selectCategoryByName(
                category.getParentId(), category.getName(), null) != null)
        {
            throw new ServiceException("同级分类名称已存在");
        }
        fabricMapper.insertCategory(category);
        return category;
    }

    @Override
    @Transactional
    public int updateCategory(FabricCategory category)
    {
        if (category.getId() == null)
        {
            throw new ServiceException("分类编号不能为空");
        }
        FabricCategory existing = fabricMapper.selectCategoryById(category.getId());
        if (existing == null)
        {
            throw new ServiceException("面料分类不存在");
        }
        if (category.getId().equals(category.getParentId()))
        {
            throw new ServiceException("分类不能选择自身作为上级");
        }
        prepareCategory(category);
        if (Integer.valueOf(1).equals(existing.getLevel())
                && Integer.valueOf(2).equals(category.getLevel())
                && fabricMapper.countCategoryChildren(category.getId()) > 0)
        {
            throw new ServiceException("存在下级分类，不能调整为二级分类");
        }
        if (Integer.valueOf(2).equals(existing.getLevel())
                && Integer.valueOf(1).equals(category.getLevel())
                && fabricMapper.countFabricsByCategoryId(category.getId()) > 0)
        {
            throw new ServiceException("该分类已被面料档案（含已删除记录）使用，不能调整为一级分类");
        }
        if (fabricMapper.selectCategoryByName(
                category.getParentId(), category.getName(), category.getId()) != null)
        {
            throw new ServiceException("同级分类名称已存在");
        }
        return fabricMapper.updateCategory(category);
    }

    @Override
    @Transactional
    public int deleteCategory(Long id)
    {
        FabricCategory category = id == null ? null : fabricMapper.selectCategoryById(id);
        if (category == null)
        {
            throw new ServiceException("面料分类不存在");
        }
        if (fabricMapper.countCategoryChildren(id) > 0)
        {
            throw new ServiceException("存在下级分类，不能删除");
        }
        if (fabricMapper.countFabricsByCategoryId(id) > 0)
        {
            throw new ServiceException("该分类已被面料档案（含已删除记录）使用，不能删除");
        }
        return fabricMapper.deleteCategoryById(id);
    }

    @Override
    public List<FabricComponentType> selectComponentTypeList()
    {
        return fabricMapper.selectComponentTypeList();
    }

    @Override
    @Transactional
    public int insertFabric(Fabric fabric)
    {
        fabric.setEntryDate(LocalDate.now());
        prepareFabric(fabric);
        fabric.setCode(nextFabricCode(fabric.getYear(), fabric.getCompositionCode()));
        int rows = fabricMapper.insertFabric(fabric);
        replaceCompositions(fabric);
        replaceImages(fabric);
        return rows;
    }

    @Override
    @Transactional
    public int updateFabric(Fabric fabric)
    {
        Fabric existing = fabric.getId() == null
                ? null : fabricMapper.selectFabricById(fabric.getId());
        if (existing == null)
        {
            throw new ServiceException("面料记录不存在或已删除");
        }
        fabric.setEntryDate(existing.getEntryDate());
        prepareFabric(fabric);
        fabric.setCode(replaceCompositionCode(
                existing.getCode(), fabric.getCompositionCode()));
        int rows = fabricMapper.updateFabric(fabric);
        replaceCompositions(fabric);
        replaceImages(fabric);
        return rows;
    }

    @Override
    @Transactional
    public int deleteFabricByIds(Long[] ids)
    {
        if (ids == null || ids.length == 0)
        {
            throw new ServiceException("请选择要删除的面料");
        }
        // 与库存新增统一锁定档案行，避免“校验后、软删除前”并发插入库存造成悬空关联。
        inventoryMapper.selectActiveFabricIdsForUpdate(ids);
        List<String> referencedCodes = inventoryMapper.selectReferencedFabricCodes(ids);
        if (!referencedCodes.isEmpty())
        {
            throw new ServiceException("面料" + String.join("、", referencedCodes)
                    + "已关联库存台账，不能删除；档案需永久保留用于库存和流水追溯");
        }
        return fabricMapper.deleteFabricByIds(ids);
    }

    private void prepareFabric(Fabric fabric)
    {
        if (fabric.getEntryDate() == null)
        {
            throw new ServiceException("日期不能为空");
        }
        fabric.setYear(fabric.getEntryDate().getYear());
        fabric.setProductName(limit(clean(fabric.getProductName()), 200));
        fabric.setColorNo(limit(clean(fabric.getColorNo()), 100));
        fabric.setNotes(limit(clean(fabric.getNotes()), 2000));

        if (fabric.getProductName() == null)
        {
            throw new ServiceException("品名不能为空");
        }
        if (fabric.getCategoryId() == null)
        {
            throw new ServiceException("面料分类不能为空");
        }
        FabricCategory category = fabricMapper.selectCategoryById(fabric.getCategoryId());
        if (category == null || !Integer.valueOf(2).equals(category.getLevel())
                || category.getParentId() == null)
        {
            throw new ServiceException("请选择二级面料分类");
        }
        if (fabric.getSupplierId() == null)
        {
            throw new ServiceException("供应商不能为空");
        }
        if (fabricMapper.selectSupplierById(fabric.getSupplierId()) == null)
        {
            throw new ServiceException("所选供应商不存在，请重新选择");
        }
        if (fabric.getWeight() == null || fabric.getWeight().compareTo(BigDecimal.ZERO) <= 0)
        {
            throw new ServiceException("克重必须大于0");
        }
        if (fabric.getWidth() == null || fabric.getWidth().compareTo(BigDecimal.ZERO) <= 0)
        {
            throw new ServiceException("包边门幅必须大于0");
        }
        if (fabric.getPriceValue() == null || fabric.getPriceValue().compareTo(BigDecimal.ZERO) <= 0)
        {
            throw new ServiceException("价格数值必须大于0");
        }

        requireInteger(fabric.getWeight(), "克重");
        requireInteger(fabric.getWidth(), "包边门幅");
        requireScale(fabric.getPriceValue(), 2, "价格最多保留两位小数");
        fabric.setWeight(fabric.getWeight().setScale(0, RoundingMode.UNNECESSARY));
        fabric.setWidth(fabric.getWidth().setScale(0, RoundingMode.UNNECESSARY));
        fabric.setPriceValue(fabric.getPriceValue().setScale(2, RoundingMode.UNNECESSARY));
        fabric.setPriceUnit(normalizePriceUnit(fabric.getPriceUnit()));

        prepareCompositions(fabric);
        fabric.setMeterPrice(calculateMeterPrice(fabric));
        normalizeImages(fabric);
    }

    private void prepareCompositions(Fabric fabric)
    {
        List<FabricComposition> compositions = fabric.getCompositions();
        if (compositions == null || compositions.isEmpty())
        {
            throw new ServiceException("成分不能为空");
        }
        if (compositions.size() > 4)
        {
            throw new ServiceException("成分最多填写4项");
        }

        BigDecimal total = BigDecimal.ZERO;
        Set<String> componentCodes = new LinkedHashSet<>();
        List<String> summaries = new ArrayList<>();
        for (int i = 0; i < compositions.size(); i++)
        {
            FabricComposition composition = compositions.get(i);
            String code = clean(composition.getComponentCode());
            code = code == null ? null : code.toUpperCase(Locale.ROOT);
            if (!COMPONENT_NAMES.containsKey(code))
            {
                throw new ServiceException("存在不支持的成分代码");
            }
            if (!componentCodes.add(code))
            {
                throw new ServiceException("同一种成分不能重复填写");
            }
            BigDecimal percentage = composition.getPercentage();
            if (percentage == null
                    || percentage.compareTo(BigDecimal.ONE) < 0
                    || percentage.compareTo(ONE_HUNDRED) > 0)
            {
                throw new ServiceException("成分比例必须在1%到100%之间");
            }

            requireInteger(percentage, "成分比例");
            percentage = percentage.setScale(0, RoundingMode.UNNECESSARY);
            composition.setComponentCode(code);
            composition.setComponentNameCn(COMPONENT_NAMES.get(code));
            composition.setPercentage(percentage);
            composition.setSortOrder(i);
            total = total.add(percentage);
            summaries.add(formatDecimal(percentage) + "%" + COMPONENT_NAMES.get(code));
        }

        if (total.compareTo(ONE_HUNDRED) != 0)
        {
            throw new ServiceException("成分配比合计需等于100%");
        }

        String key = componentCodes.stream().sorted().collect(Collectors.joining("+"));
        fabric.setCompositionCode(COMBINATION_CODES.getOrDefault(key, "OT"));
        fabric.setCompositionSummary(String.join(" + ", summaries));
    }

    private BigDecimal calculateMeterPrice(Fabric fabric)
    {
        BigDecimal price = fabric.getPriceValue();
        BigDecimal meterPrice;
        switch (fabric.getPriceUnit())
        {
            case "M":
                meterPrice = price;
                break;
            case "Y":
                meterPrice = price.divide(YARD_IN_METERS, 8, RoundingMode.HALF_UP);
                break;
            case "KG":
                BigDecimal widthMeters = fabric.getWidth()
                        .divide(ONE_HUNDRED, 8, RoundingMode.HALF_UP);
                BigDecimal kilogramsPerMeter = widthMeters.multiply(fabric.getWeight())
                        .divide(ONE_THOUSAND, 8, RoundingMode.HALF_UP);
                meterPrice = price.multiply(kilogramsPerMeter);
                break;
            case "ROLL":
                return null;
            default:
                throw new ServiceException("计价单位不正确");
        }
        return meterPrice.setScale(2, RoundingMode.HALF_UP);
    }

    private String nextFabricCode(Integer year, String compositionCode)
    {
        Object yearLock = CODE_SEQUENCE_LOCKS.computeIfAbsent(year, key -> new Object());
        synchronized (yearLock)
        {
            fabricMapper.mergeCodeSequence(year);
            Integer currentValue = fabricMapper.selectCodeSequenceForUpdate(year);
            if (currentValue == null || currentValue >= 9999)
            {
                throw new ServiceException(year + "年度面料流水号已超过4位上限");
            }
            int sequence = currentValue + 1;
            if (fabricMapper.incrementCodeSequence(year) != 1)
            {
                throw new ServiceException("面料流水号生成失败，请重试");
            }
            return String.format(Locale.ROOT, "A-%02d%04d-%s",
                    Math.floorMod(year, 100), sequence, compositionCode);
        }
    }

    private void replaceCompositions(Fabric fabric)
    {
        fabricMapper.deleteCompositionsByFabricId(fabric.getId());
        for (FabricComposition composition : fabric.getCompositions())
        {
            composition.setFabricId(fabric.getId());
            fabricMapper.insertFabricComposition(composition);
        }
    }

    private void normalizeImages(Fabric fabric)
    {
        String imageUrls = clean(fabric.getImageUrls());
        if (imageUrls == null)
        {
            throw new ServiceException("请至少上传一张面料图片");
        }
        Set<String> urls = Arrays.stream(imageUrls.split(","))
                .map(FabricServiceImpl::clean)
                .filter(value -> value != null)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (urls.isEmpty())
        {
            throw new ServiceException("请至少上传一张面料图片");
        }
        if (urls.size() > MAX_IMAGES)
        {
            throw new ServiceException("面料图片不能超过" + MAX_IMAGES + "张");
        }
        fabric.setImageUrls(String.join(",", urls));
    }

    private void replaceImages(Fabric fabric)
    {
        fabricMapper.deleteImagesByFabricId(fabric.getId());
        int sort = 0;
        for (String url : fabric.getImageUrls().split(","))
        {
            FabricImage image = new FabricImage();
            image.setFabricId(fabric.getId());
            image.setImageUrl(limit(clean(url), 500));
            image.setThumbnailUrl(limit(toThumbnailUrl(url), 500));
            image.setImageType(FabricImage.FABRIC_DETAIL);
            image.setSortOrder(sort++);
            fabricMapper.insertFabricImage(image);
        }
    }

    private void populateDetails(List<Fabric> fabrics)
    {
        if (fabrics == null || fabrics.isEmpty())
        {
            return;
        }
        List<Long> fabricIds = fabrics.stream()
                .map(Fabric::getId)
                .filter(id -> id != null)
                .collect(Collectors.toList());
        if (fabricIds.isEmpty())
        {
            return;
        }

        Map<Long, List<FabricImage>> imagesByFabricId = fabricMapper.selectImagesByFabricIds(fabricIds)
                .stream()
                .collect(Collectors.groupingBy(
                        FabricImage::getFabricId,
                        LinkedHashMap::new,
                        Collectors.toList()));
        Map<Long, List<FabricComposition>> compositionsByFabricId =
                fabricMapper.selectCompositionsByFabricIds(fabricIds)
                        .stream()
                        .collect(Collectors.groupingBy(
                                FabricComposition::getFabricId,
                                LinkedHashMap::new,
                                Collectors.toList()));

        for (Fabric current : fabrics)
        {
            List<FabricImage> images = imagesByFabricId.getOrDefault(
                    current.getId(), Collections.emptyList());
            current.setImages(images);
            current.setCompositions(compositionsByFabricId.getOrDefault(
                    current.getId(), Collections.emptyList()));
            if (!images.isEmpty())
            {
                FabricImage firstImage = images.get(0);
                current.setImageUrl(firstImage.getImageUrl());
                current.setThumbnailUrl(firstImage.getThumbnailUrl());
            }
        }
    }

    private static String normalizePriceUnit(String value)
    {
        String unit = clean(value);
        if (unit == null)
        {
            throw new ServiceException("计价单位不能为空");
        }
        unit = unit.toUpperCase(Locale.ROOT);
        if (!Set.of("M", "Y", "KG", "ROLL").contains(unit))
        {
            throw new ServiceException("计价单位不正确");
        }
        return unit;
    }

    private void prepareSupplier(FabricSupplier supplier)
    {
        supplier.setName(limit(clean(supplier.getName()), 150));
        supplier.setPhone(limit(clean(supplier.getPhone()), 100));
        supplier.setAddress(limit(clean(supplier.getAddress()), 500));
        supplier.setRemarks(limit(clean(supplier.getRemarks()), 1000));
        if (supplier.getName() == null)
        {
            throw new ServiceException("供应商名称不能为空");
        }
        if (supplier.getPhone() == null)
        {
            throw new ServiceException("联系电话不能为空");
        }
        if (supplier.getAddress() == null)
        {
            throw new ServiceException("地址不能为空");
        }
    }

    private void prepareCategory(FabricCategory category)
    {
        category.setName(limit(clean(category.getName()), 100));
        if (category.getName() == null)
        {
            throw new ServiceException("分类名称不能为空");
        }
        category.setSortOrder(category.getSortOrder() == null
                ? 0 : Math.max(category.getSortOrder(), 0));
        if (category.getParentId() == null)
        {
            category.setLevel(1);
            return;
        }
        FabricCategory parent = fabricMapper.selectCategoryById(category.getParentId());
        if (parent == null || !Integer.valueOf(1).equals(parent.getLevel()))
        {
            throw new ServiceException("上级分类必须是一级分类");
        }
        category.setLevel(2);
    }

    private static String replaceCompositionCode(String code, String compositionCode)
    {
        int marker = code == null ? -1 : code.lastIndexOf('-');
        if (marker < 0)
        {
            throw new ServiceException("面料编号格式不正确，无法更新成分代码");
        }
        return code.substring(0, marker + 1) + compositionCode;
    }

    private static String toThumbnailUrl(String imageUrl)
    {
        String testMarker = "/profile/fabric/test/original/";
        if (imageUrl != null && imageUrl.contains(testMarker))
        {
            return imageUrl.replace(testMarker, "/profile/fabric/test/thumbnail/");
        }
        String marker = "/profile/fabric/";
        if (imageUrl != null && imageUrl.contains(marker)
                && !imageUrl.contains("/profile/fabric/thumbnail/"))
        {
            return imageUrl.replace(marker, "/profile/fabric/thumbnail/");
        }
        return imageUrl;
    }

    private static String formatDecimal(BigDecimal value)
    {
        return value.stripTrailingZeros().toPlainString();
    }

    private static String clean(String value)
    {
        if (value == null)
        {
            return null;
        }
        String cleaned = value.replace("\uFEFF", "").trim();
        return cleaned.isEmpty() ? null : cleaned;
    }

    private static String limit(String value, int length)
    {
        return value != null && value.length() > length ? value.substring(0, length) : value;
    }

    private static void requireInteger(BigDecimal value, String fieldName)
    {
        if (value.stripTrailingZeros().scale() > 0)
        {
            throw new ServiceException(fieldName + "必须填写整数");
        }
    }

    private static void requireScale(BigDecimal value, int scale, String message)
    {
        if (value.stripTrailingZeros().scale() > scale)
        {
            throw new ServiceException(message);
        }
    }
}
