package com.ruoyi.system.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.core.domain.Fabric;
import com.ruoyi.common.core.domain.FabricCategory;
import com.ruoyi.common.core.domain.FabricComposition;
import com.ruoyi.common.core.domain.FabricSupplier;
import com.ruoyi.system.mapper.FabricMapper;

/**
 * 开发环境结构化面料测试数据初始化。
 *
 * <p>初始化按年度目标总数补齐，已软删除的测试记录也计入总数，避免每次启动重复补数。
 * 当年测试日期不超过系统当天，流水号按日期升序生成。</p>
 */
@Service
public class FabricDataInitService
{
    private static final Logger log = LoggerFactory.getLogger(FabricDataInitService.class);

    private static final long RANDOM_SEED = 20260727L;

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private static final BigDecimal ONE_THOUSAND = new BigDecimal("1000");

    private static final BigDecimal YARD_IN_METERS = new BigDecimal("0.9144");

    private static final String[] PRODUCT_NAMES = {
        "全棉府绸", "涤棉斜纹", "弹力贡缎", "人棉绉布", "亚麻混纺",
        "锦氨罗马布", "涤氨雪纺", "粘胶针织", "双面呢", "空气层",
        "提花布", "网眼布", "蕾丝花边", "抓绒卫衣布", "四面弹",
        "仿真丝缎", "金银丝提花", "烫金针织", "复合麂皮绒", "轻薄风衣布"
    };

    private static final String[] COLOR_NUMBERS = {
        "本白", "米白", "象牙白", "浅卡其", "深卡其", "雾霾蓝", "藏青",
        "宝蓝", "墨绿", "军绿", "酒红", "砖红", "浅灰", "炭灰", "黑色",
        "C-101", "C-203", "C-318", "C-426", "C-508"
    };

    private static final String[] PRICE_UNITS = { "M", "Y", "KG", "ROLL" };

    private static final Map<String, String> COMPONENT_NAMES;

    private static final List<CompositionTemplate> COMPOSITION_TEMPLATES;

    private static final List<SupplierTemplate> SUPPLIER_TEMPLATES;

    static
    {
        Map<String, String> componentNames = new LinkedHashMap<>();
        componentNames.put("PO", "涤纶/聚酯");
        componentNames.put("CO", "棉");
        componentNames.put("VI", "粘胶");
        componentNames.put("SP", "氨纶");
        componentNames.put("LI", "亚麻");
        componentNames.put("PA", "锦纶/尼龙");
        componentNames.put("PU", "聚氨酯");
        componentNames.put("OT", "其他纤维");
        COMPONENT_NAMES = Collections.unmodifiableMap(componentNames);

        COMPOSITION_TEMPLATES = Arrays.asList(
                new CompositionTemplate("TC", new String[] { "CO", "PO" }, new int[] { 60, 40 }),
                new CompositionTemplate("TR", new String[] { "PO", "VI" }, new int[] { 65, 35 }),
                new CompositionTemplate("COSP", new String[] { "CO", "SP" }, new int[] { 95, 5 }),
                new CompositionTemplate("POSP", new String[] { "PO", "SP" }, new int[] { 92, 8 }),
                new CompositionTemplate("PASP", new String[] { "PA", "SP" }, new int[] { 90, 10 }),
                new CompositionTemplate("LICO", new String[] { "LI", "CO" }, new int[] { 55, 45 }),
                new CompositionTemplate("LIVI", new String[] { "LI", "VI" }, new int[] { 50, 50 }),
                new CompositionTemplate("LIPO", new String[] { "LI", "PO" }, new int[] { 40, 60 }),
                new CompositionTemplate("TRSP", new String[] { "PO", "VI", "SP" }, new int[] { 62, 33, 5 }),
                new CompositionTemplate("OT", new String[] { "PU", "PO" }, new int[] { 70, 30 }));

        SUPPLIER_TEMPLATES = Arrays.asList(
                new SupplierTemplate("苏州锦华纺织", "13800001001", "江苏省苏州市吴江区盛泽镇"),
                new SupplierTemplate("绍兴越彩布业", "13800001002", "浙江省绍兴市柯桥区"),
                new SupplierTemplate("广州新纬纺织", "13800001003", "广东省广州市海珠区中大市场"),
                new SupplierTemplate("杭州云纱面料", "13800001004", "浙江省杭州市临平区"),
                new SupplierTemplate("常州经纬织造", "13800001005", "江苏省常州市武进区"),
                new SupplierTemplate("南通恒棉纺织", "13800001006", "江苏省南通市通州区"),
                new SupplierTemplate("嘉兴禾润布业", "13800001007", "浙江省嘉兴市秀洲区"),
                new SupplierTemplate("无锡尚品针织", "13800001008", "江苏省无锡市锡山区"),
                new SupplierTemplate("泉州海丝纺织", "13800001009", "福建省泉州市石狮市"),
                new SupplierTemplate("佛山粤丰面料", "13800001010", "广东省佛山市南海区"),
                new SupplierTemplate("宁波东方纺织", "13800001011", "浙江省宁波市鄞州区"),
                new SupplierTemplate("东莞联发布业", "13800001012", "广东省东莞市虎门镇"),
                new SupplierTemplate("湖州丝韵纺织", "13800001013", "浙江省湖州市吴兴区"),
                new SupplierTemplate("桐乡锦成面料", "13800001014", "浙江省嘉兴市桐乡市"),
                new SupplierTemplate("青岛海源纺织", "13800001015", "山东省青岛市即墨区"),
                new SupplierTemplate("武汉楚风布业", "13800001016", "湖北省武汉市硚口区"),
                new SupplierTemplate("成都蜀锦纺织", "13800001017", "四川省成都市金牛区"),
                new SupplierTemplate("厦门华纶面料", "13800001018", "福建省厦门市集美区"),
                new SupplierTemplate("上海品织贸易", "13800001019", "上海市青浦区"),
                new SupplierTemplate("北京华彩纺织", "13800001020", "北京市大兴区"));
    }

    @Value("${ruoyi.fabric.data-init-enabled:false}")
    private boolean enabled;

    @Value("${ruoyi.fabric.data-target-2026:100}")
    private int target2026;

    @Value("${ruoyi.fabric.data-target-2025:100}")
    private int target2025;

    @Autowired
    private FabricMapper fabricMapper;

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> initializeData()
    {
        Map<String, Object> result = new LinkedHashMap<>();
        if (!enabled)
        {
            result.put("enabled", false);
            return result;
        }

        ensureTestSuppliers();
        List<FabricSupplier> suppliers = fabricMapper.selectSupplierList();
        List<FabricCategory> categories = new ArrayList<>();
        for (FabricCategory category : fabricMapper.selectCategoryList())
        {
            if (Integer.valueOf(2).equals(category.getLevel()) && category.getParentId() != null)
            {
                categories.add(category);
            }
        }
        if (suppliers.isEmpty() || categories.isEmpty())
        {
            throw new IllegalStateException("面料测试数据初始化失败：供应商或面料分类为空");
        }

        YearDataStats year2026 = initializeYear(2026, Math.max(0, target2026), suppliers, categories);
        YearDataStats year2025 = initializeYear(2025, Math.max(0, target2025), suppliers, categories);
        result.put("enabled", true);
        result.put("year2026", year2026.toMap());
        result.put("year2025", year2025.toMap());
        result.put("suppliers", suppliers.size());

        log.info("面料测试数据初始化完成：2026年现有{}条、补充{}条，2025年现有{}条、补充{}条，供应商{}家",
                year2026.existingCount, year2026.insertedCount,
                year2025.existingCount, year2025.insertedCount,
                suppliers.size());
        return result;
    }

    private void ensureTestSuppliers()
    {
        for (SupplierTemplate template : SUPPLIER_TEMPLATES)
        {
            if (fabricMapper.selectSupplierByName(template.name) != null)
            {
                continue;
            }
            FabricSupplier supplier = new FabricSupplier();
            supplier.setName(template.name);
            supplier.setPhone(template.phone);
            supplier.setAddress(template.address);
            supplier.setRemarks("自动生成测试供应商");
            fabricMapper.insertSupplier(supplier);
        }
    }

    private YearDataStats initializeYear(int year, int targetCount, List<FabricSupplier> suppliers,
            List<FabricCategory> categories)
    {
        int existingCount = fabricMapper.countAllFabricsByYear(year);
        int insertCount = Math.max(0, targetCount - existingCount);
        YearDataStats stats = new YearDataStats(year, targetCount, existingCount);
        if (insertCount == 0)
        {
            return stats;
        }

        fabricMapper.mergeCodeSequence(year);
        fabricMapper.reserveCodeSequence(year, insertCount);
        Integer sequenceEnd = fabricMapper.selectCodeSequence(year);
        if (sequenceEnd == null || sequenceEnd > 9999)
        {
            throw new IllegalStateException(year + "年度测试数据流水号已超过4位上限");
        }

        int sequenceStart = sequenceEnd - insertCount + 1;
        Random dateRandom = new Random(RANDOM_SEED + year * 31L);
        List<LocalDate> entryDates = new ArrayList<>(insertCount);
        for (int index = 0; index < insertCount; index++)
        {
            entryDates.add(randomDate(year, dateRandom));
        }
        // 保证当年最后一条测试数据落在今天，历史年度最后一条落在年末，
        // 这样按入库日期倒序时，最新流水号始终位于列表顶部且不会生成未来日期。
        entryDates.set(entryDates.size() - 1, latestDateOfYear(year));
        Collections.sort(entryDates);

        Random random = new Random(RANDOM_SEED + year);
        for (int offset = 0; offset < insertCount; offset++)
        {
            int sequence = sequenceStart + offset;
            CompositionTemplate composition = COMPOSITION_TEMPLATES.get(random.nextInt(COMPOSITION_TEMPLATES.size()));
            Fabric fabric = buildFabric(year, sequence, entryDates.get(offset),
                    composition, suppliers, categories, random);
            fabricMapper.insertFabric(fabric);
            insertCompositions(fabric.getId(), composition);
            stats.insertedCount++;
            stats.insertedCompositions += composition.componentCodes.length;
        }
        return stats;
    }

    private Fabric buildFabric(int year, int sequence, LocalDate entryDate, CompositionTemplate composition,
            List<FabricSupplier> suppliers, List<FabricCategory> categories, Random random)
    {
        Fabric fabric = new Fabric();
        fabric.setCode(String.format(Locale.ROOT, "A-%02d%04d-%s",
                Math.floorMod(year, 100), sequence, composition.compositionCode));
        fabric.setEntryDate(entryDate);
        fabric.setYear(year);
        fabric.setCategoryId(categories.get(random.nextInt(categories.size())).getId());
        fabric.setSupplierId(suppliers.get(random.nextInt(suppliers.size())).getId());
        fabric.setProductName(PRODUCT_NAMES[random.nextInt(PRODUCT_NAMES.length)]);
        fabric.setWeight(BigDecimal.valueOf(80 + random.nextInt(341)));
        fabric.setWidth(BigDecimal.valueOf(90 + random.nextInt(91)));
        fabric.setColorNo(COLOR_NUMBERS[random.nextInt(COLOR_NUMBERS.length)]);
        fabric.setCompositionCode(composition.compositionCode);
        fabric.setCompositionSummary(buildCompositionSummary(composition));
        fabric.setPriceValue(BigDecimal.valueOf(100 + random.nextInt(39901), 2)
                .setScale(2, RoundingMode.HALF_UP));
        fabric.setPriceUnit(PRICE_UNITS[random.nextInt(PRICE_UNITS.length)]);
        fabric.setMeterPrice(calculateMeterPrice(fabric));
        fabric.setNotes("自动生成测试数据");
        fabric.setCreateBy("test-data");
        return fabric;
    }

    private LocalDate randomDate(int year, Random random)
    {
        LocalDate firstDay = LocalDate.of(year, 1, 1);
        LocalDate lastDay = latestDateOfYear(year);
        int availableDays = Math.toIntExact(ChronoUnit.DAYS.between(firstDay, lastDay) + 1);
        return firstDay.plusDays(random.nextInt(availableDays));
    }

    private LocalDate latestDateOfYear(int year)
    {
        LocalDate today = LocalDate.now();
        if (year > today.getYear())
        {
            throw new IllegalStateException("不能生成未来年度的测试数据：" + year);
        }
        return year == today.getYear() ? today : LocalDate.of(year, 12, 31);
    }

    private String buildCompositionSummary(CompositionTemplate template)
    {
        List<String> parts = new ArrayList<>(template.componentCodes.length);
        for (int index = 0; index < template.componentCodes.length; index++)
        {
            parts.add(template.percentages[index] + "%" + COMPONENT_NAMES.get(template.componentCodes[index]));
        }
        return String.join(" + ", parts);
    }

    private void insertCompositions(Long fabricId, CompositionTemplate template)
    {
        for (int index = 0; index < template.componentCodes.length; index++)
        {
            FabricComposition composition = new FabricComposition();
            composition.setFabricId(fabricId);
            composition.setComponentCode(template.componentCodes[index]);
            composition.setPercentage(BigDecimal.valueOf(template.percentages[index]));
            composition.setSortOrder(index + 1);
            fabricMapper.insertFabricComposition(composition);
        }
    }

    private BigDecimal calculateMeterPrice(Fabric fabric)
    {
        BigDecimal price = fabric.getPriceValue();
        switch (fabric.getPriceUnit())
        {
            case "M":
                return price.setScale(2, RoundingMode.HALF_UP);
            case "Y":
                return price.divide(YARD_IN_METERS, 2, RoundingMode.HALF_UP);
            case "KG":
                return price.multiply(fabric.getWidth())
                        .multiply(fabric.getWeight())
                        .divide(ONE_HUNDRED.multiply(ONE_THOUSAND), 2, RoundingMode.HALF_UP);
            case "ROLL":
                return null;
            default:
                throw new IllegalStateException("不支持的测试数据计价单位：" + fabric.getPriceUnit());
        }
    }

    private static final class CompositionTemplate
    {
        private final String compositionCode;
        private final String[] componentCodes;
        private final int[] percentages;

        private CompositionTemplate(String compositionCode, String[] componentCodes, int[] percentages)
        {
            this.compositionCode = compositionCode;
            this.componentCodes = componentCodes;
            this.percentages = percentages;
        }
    }

    private static final class SupplierTemplate
    {
        private final String name;
        private final String phone;
        private final String address;

        private SupplierTemplate(String name, String phone, String address)
        {
            this.name = name;
            this.phone = phone;
            this.address = address;
        }
    }

    private static final class YearDataStats
    {
        private final int year;
        private final int targetCount;
        private final int existingCount;
        private int insertedCount;
        private int insertedCompositions;

        private YearDataStats(int year, int targetCount, int existingCount)
        {
            this.year = year;
            this.targetCount = targetCount;
            this.existingCount = existingCount;
        }

        private Map<String, Object> toMap()
        {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("year", year);
            result.put("targetCount", targetCount);
            result.put("existingCount", existingCount);
            result.put("insertedCount", insertedCount);
            result.put("insertedCompositions", insertedCompositions);
            return result;
        }
    }
}
