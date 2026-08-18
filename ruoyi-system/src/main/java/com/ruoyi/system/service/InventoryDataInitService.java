package com.ruoyi.system.service;

import java.io.File;
import java.io.FileInputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.coobird.thumbnailator.Thumbnails;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFPicture;
import org.apache.poi.xssf.usermodel.XSSFPictureData;
import org.apache.poi.xssf.usermodel.XSSFShape;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.core.domain.InventoryMovement;
import com.ruoyi.common.core.domain.InventoryStock;
import com.ruoyi.common.core.domain.InventoryStockImage;
import com.ruoyi.system.mapper.InventoryMapper;
import com.ruoyi.system.utils.InventoryNumberUtils;

/** 首次启动导入库存工作簿及嵌入图片。 */
@Service
public class InventoryDataInitService
{
    private static final Logger log = LoggerFactory.getLogger(InventoryDataInitService.class);
    private static final Pattern METERS = Pattern.compile("([0-9]+(?:\\.[0-9]+)?)\\s*米");
    private static final String SHEET_NAME = "一料款多款开发备料";

    @Value("${ruoyi.inventory.data-init-enabled:true}")
    private boolean enabled;
    @Value("${ruoyi.inventory.workbook:}")
    private String workbookPath;

    @Autowired
    private InventoryMapper mapper;
    @Autowired
    private InventoryNumberUtils numberUtils;

    @Transactional
    public void initialize()
    {
        if (!enabled) return;
        File source = workbookPath == null ? null : new File(workbookPath);
        if (source == null || !source.isFile())
        {
            log.warn("库存期初工作簿不存在，已跳过导入：{}", workbookPath);
            return;
        }
        int stockCount = 0;
        int imageCount = 0;
        try (FileInputStream input = new FileInputStream(source);
             XSSFWorkbook workbook = new XSSFWorkbook(input))
        {
            XSSFSheet sheet = workbook.getSheet(SHEET_NAME);
            if (sheet == null)
            {
                log.warn("库存期初工作簿缺少工作表：{}", SHEET_NAME);
                return;
            }
            DataFormatter formatter = new DataFormatter(Locale.CHINA);
            Map<Integer, InventoryStock> stocksByRow = new HashMap<>();
            for (int rowIndex = 2; rowIndex <= sheet.getLastRowNum(); rowIndex++)
            {
                Row row = sheet.getRow(rowIndex);
                if (row == null || allBlank(row, formatter)) continue;
                String sourceKey = "inventory-workbook:main:row:" + (rowIndex + 1);
                if (mapper.countStockBySourceKey(sourceKey) > 0) continue;
                InventoryStock stock = toStock(row, rowIndex, formatter, sourceKey);
                mapper.insertStock(stock);
                insertOpeningMovement(stock);
                stocksByRow.put(rowIndex, stock);
                stockCount++;
            }
            imageCount = importPictures(sheet, stocksByRow);
        }
        catch (Exception e)
        {
            throw new IllegalStateException("库存期初数据导入失败：" + e.getMessage(), e);
        }
        log.info("库存期初导入完成：新增库存 {} 条，导入图片 {} 张；已存在的来源行自动跳过", stockCount, imageCount);
    }

    private InventoryStock toStock(Row row, int rowIndex, DataFormatter formatter, String sourceKey)
    {
        InventoryStock stock = new InventoryStock();
        stock.setStockCode(numberUtils.nextStockCode());
        stock.setMaterialType("F");
        stock.setMaterialCode(text(row, 0, formatter));
        stock.setDevelopmentStyleNo(text(row, 1, formatter));
        stock.setSkc(text(row, 3, formatter));
        stock.setColorNo(text(row, 4, formatter));
        stock.setGoodsType(text(row, 8, formatter));
        stock.setPrimaryUnit(defaultText(text(row, 6, formatter), "条"));
        stock.setAuxiliaryUnit("米");
        BigDecimal onHand = number(row, 14, formatter);
        stock.setOnHandQty(onHand);
        stock.setLockedQty(BigDecimal.ZERO);
        BigDecimal meters = meters(text(row, 9, formatter));
        stock.setOnHandAuxQty(meters);
        stock.setLockedAuxQty(BigDecimal.ZERO);
        stock.setLastCountTime(dateTime(row.getCell(15), formatter));
        stock.setVersion(0);
        stock.setSourceKey(sourceKey);
        stock.setCreateBy("inventory_import");
        stock.setRemark(text(row, 9, formatter));
        return stock;
    }

    private void insertOpeningMovement(InventoryStock stock)
    {
        InventoryMovement movement = new InventoryMovement();
        movement.setMovementNo(numberUtils.nextMovementNo());
        movement.setStockId(stock.getId());
        movement.setMovementType("OPENING");
        movement.setBusinessType("OPENING");
        movement.setBusinessId(stock.getId());
        movement.setBusinessNo(stock.getSourceKey());
        movement.setQuantityChange(stock.getOnHandQty());
        movement.setLockedChange(BigDecimal.ZERO);
        movement.setAuxiliaryChange(stock.getOnHandAuxQty());
        movement.setLockedAuxiliaryChange(BigDecimal.ZERO);
        movement.setBalanceQty(stock.getOnHandQty());
        movement.setBalanceLockedQty(BigDecimal.ZERO);
        movement.setBalanceAuxQty(stock.getOnHandAuxQty());
        movement.setBalanceLockedAuxQty(BigDecimal.ZERO);
        movement.setOperatorName("inventory_import");
        movement.setOperationTime(stock.getLastCountTime() == null ? LocalDateTime.now() : stock.getLastCountTime());
        movement.setRemark("Excel期初库存导入");
        mapper.insertMovement(movement);
    }

    private int importPictures(XSSFSheet sheet, Map<Integer, InventoryStock> stocksByRow) throws Exception
    {
        XSSFDrawing drawing = sheet.getDrawingPatriarch();
        if (drawing == null || stocksByRow.isEmpty()) return 0;
        File originalDir = new File(RuoYiConfig.getProfile(), "inventory/opening/original");
        File thumbnailDir = new File(RuoYiConfig.getProfile(), "inventory/opening/thumbnail");
        Files.createDirectories(originalDir.toPath());
        Files.createDirectories(thumbnailDir.toPath());
        Map<Integer, Integer> sortByRow = new HashMap<>();
        int count = 0;
        for (XSSFShape shape : drawing.getShapes())
        {
            if (!(shape instanceof XSSFPicture picture) || !(picture.getAnchor() instanceof XSSFClientAnchor anchor)) continue;
            int rowIndex = anchor.getRow1();
            InventoryStock stock = stocksByRow.get(rowIndex);
            if (stock == null) continue;
            XSSFPictureData data = picture.getPictureData();
            byte[] bytes = data.getData();
            int sort = sortByRow.getOrDefault(rowIndex, 0);
            sortByRow.put(rowIndex, sort + 1);
            String extension = normalizeExtension(data.suggestFileExtension());
            String baseName = String.format(Locale.ROOT, "row-%04d-%02d", rowIndex + 1, sort + 1);
            File original = new File(originalDir, baseName + "." + extension);
            File thumbnail = new File(thumbnailDir, baseName + ".jpg");
            Files.write(original.toPath(), bytes);
            Thumbnails.of(original).width(300).keepAspectRatio(true).outputFormat("jpg").toFile(thumbnail);
            InventoryStockImage image = new InventoryStockImage();
            image.setStockId(stock.getId());
            image.setImageUrl("/profile/inventory/opening/original/" + original.getName());
            image.setThumbnailUrl("/profile/inventory/opening/thumbnail/" + thumbnail.getName());
            // Excel 会把多种原图统一封装为 PNG，不能再用嵌入格式推断款式图。
            image.setImageType("material");
            image.setSourceName(baseName + "." + extension);
            image.setContentHash(sha256(bytes));
            image.setSortOrder(sort);
            mapper.insertStockImage(image);
            count++;
        }
        return count;
    }

    private static boolean allBlank(Row row, DataFormatter formatter)
    {
        for (int index : new int[] {0, 1, 4, 14})
            if (text(row, index, formatter) != null) return false;
        return true;
    }

    private static String text(Row row, int index, DataFormatter formatter)
    {
        Cell cell = row.getCell(index);
        if (cell == null) return null;
        String value = formatter.formatCellValue(cell).replace("\uFEFF", "").trim();
        return value.isEmpty() ? null : value;
    }

    private static BigDecimal number(Row row, int index, DataFormatter formatter)
    {
        String value = text(row, index, formatter);
        if (value == null) return BigDecimal.ZERO;
        try { return new BigDecimal(value.replace(",", "")); }
        catch (NumberFormatException ignored) { return BigDecimal.ZERO; }
    }

    private static BigDecimal meters(String notes)
    {
        if (notes == null) return BigDecimal.ZERO;
        Matcher matcher = METERS.matcher(notes);
        return matcher.find() ? new BigDecimal(matcher.group(1)) : BigDecimal.ZERO;
    }

    private static LocalDateTime dateTime(Cell cell, DataFormatter formatter)
    {
        if (cell == null) return LocalDateTime.now();
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell))
            return cell.getDateCellValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        String value = formatter.formatCellValue(cell).trim().replace('/', '-');
        try { return LocalDateTime.parse(value.replace(' ', 'T')); }
        catch (Exception ignored)
        {
            try { return LocalDate.parse(value, DateTimeFormatter.ofPattern("yyyy-M-d")).atStartOfDay(); }
            catch (Exception ignoredAgain) { return LocalDateTime.now(); }
        }
    }

    private static String defaultText(String value, String defaultValue) { return value == null ? defaultValue : value; }
    private static String normalizeExtension(String value) { return "png".equalsIgnoreCase(value) ? "png" : "jpg"; }
    private static String sha256(byte[] bytes) throws Exception
    {
        byte[] hash = MessageDigest.getInstance("SHA-256").digest(bytes);
        StringBuilder value = new StringBuilder(hash.length * 2);
        for (byte item : hash) value.append(String.format(Locale.ROOT, "%02x", item));
        return value.toString();
    }
}
