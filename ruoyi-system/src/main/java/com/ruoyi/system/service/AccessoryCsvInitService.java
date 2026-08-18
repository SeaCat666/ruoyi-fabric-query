package com.ruoyi.system.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.core.domain.FabricAccessory;
import com.ruoyi.common.core.domain.FabricAccessorySupplier;
import com.ruoyi.system.mapper.AccessoryMapper;

/**
 * 首次启动时导入2026辅料CSV；按辅料编号幂等跳过已存在记录。
 */
@Service
public class AccessoryCsvInitService
{
    private static final Logger log = LoggerFactory.getLogger(AccessoryCsvInitService.class);

    @Value("${ruoyi.fabric.accessory-init-enabled:true}")
    private boolean enabled;

    @Value("${ruoyi.fabric.accessory-csv:classpath:data/accessory-2026.csv}")
    private String csvLocation;

    @Value("${ruoyi.fabric.accessory-target-2026:100}")
    private int targetCount;

    @Autowired
    private AccessoryMapper mapper;

    @Transactional(rollbackFor = Exception.class)
    public void initialize()
    {
        if (!enabled) return;
        if (mapper.countAllAccessories() > 0)
        {
            log.info("辅料CSV初始化跳过：数据库中已经存在辅料档案");
            return;
        }
        int inserted = 0;
        try
        {
            String path = csvLocation.replaceFirst("^classpath:", "");
            ClassPathResource resource = new ClassPathResource(path);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    resource.getInputStream(), StandardCharsets.UTF_8)))
            {
                String line;
                boolean header = true;
                while ((line = reader.readLine()) != null)
                {
                    if (inserted >= Math.max(0, targetCount)) break;
                    if (header)
                    {
                        header = false;
                        continue;
                    }
                    List<String> columns = parseCsvLine(line);
                    if (cell(columns, 0) == null) continue;
                    String supplierName = cell(columns, 3);
                    if (supplierName == null) supplierName = "辅料综合供应商";
                    String phone = cell(columns, 4);
                    String address = cell(columns, 5);
                    if (phone == null) phone = "13800000000";
                    if (address == null) address = "辅料供应商测试地址";
                    FabricAccessorySupplier supplier = mapper.selectSupplierByName(supplierName);
                    if (supplier == null)
                    {
                        supplier = new FabricAccessorySupplier();
                        supplier.setName(supplierName);
                        supplier.setPhone(phone);
                        supplier.setAddress(address);
                        mapper.insertSupplier(supplier);
                    }
                    else
                    {
                        mapper.updateImportedSupplierDetails(supplier.getId(), phone, address);
                    }
                    FabricAccessory accessory = new FabricAccessory();
                    accessory.setCode(String.format("B-26%04d", inserted + 1));
                    accessory.setEntryDate(java.time.LocalDate.of(2026, 1, 1));
                    accessory.setYear(2026);
                    accessory.setSupplierId(supplier.getId());
                    accessory.setSizeSpec(cell(columns, 6));
                    accessory.setBulkPrice(cell(columns, 7));
                    accessory.setNotes(cell(columns, 9));
                    accessory.setCreateBy("csv-import");
                    mapper.insertAccessory(accessory);
                    inserted++;
                }
            }
            mapper.mergeCodeSequence(2026);
            mapper.updateCodeSequence(2026, inserted);
            log.info("2026辅料CSV初始化完成：新增{}条，编号范围B-260001起，辅料供应商独立存储", inserted);
        }
        catch (Exception e)
        {
            throw new IllegalStateException("2026辅料CSV初始化失败：" + e.getMessage(), e);
        }
    }

    private static List<String> parseCsvLine(String line)
    {
        List<String> result = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++)
        {
            char current = line.charAt(i);
            if (current == '"')
            {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"')
                {
                    cell.append('"');
                    i++;
                }
                else
                {
                    quoted = !quoted;
                }
            }
            else if (current == ',' && !quoted)
            {
                result.add(cell.toString());
                cell.setLength(0);
            }
            else
            {
                cell.append(current);
            }
        }
        result.add(cell.toString());
        return result;
    }

    private static String cell(List<String> columns, int index)
    {
        if (index >= columns.size()) return null;
        String value = columns.get(index).replace("\uFEFF", "").trim();
        return value.isEmpty() ? null : value;
    }
}
