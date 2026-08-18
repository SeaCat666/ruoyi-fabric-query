package com.ruoyi.system.utils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.mapper.InventoryMapper;

/** 库存业务编号生成器，序列行由当前事务锁定。 */
@Component
public class InventoryNumberUtils
{
    private static final DateTimeFormatter DATE = DateTimeFormatter.BASIC_ISO_DATE;

    @Autowired
    private InventoryMapper mapper;

    public String nextStockCode()
    {
        LocalDate now = LocalDate.now();
        return String.format(Locale.ROOT, "K-%02d%06d", now.getYear() % 100,
                next("STOCK", String.valueOf(now.getYear()), 999999));
    }

    public String nextInboundNo(LocalDate date)
    {
        return "RK-" + DATE.format(date) + "-" + String.format(Locale.ROOT, "%04d",
                next("INBOUND", DATE.format(date), 9999));
    }

    public String nextRequisitionNo(LocalDate date)
    {
        return "LY-" + DATE.format(date) + "-" + String.format(Locale.ROOT, "%04d",
                next("REQUISITION", DATE.format(date), 9999));
    }

    public String nextMovementNo()
    {
        LocalDate now = LocalDate.now();
        return "LS-" + DATE.format(now) + "-" + String.format(Locale.ROOT, "%06d",
                next("MOVEMENT", DATE.format(now), 999999));
    }

    private int next(String type, String date, int max)
    {
        mapper.mergeSequence(type, date);
        Integer current = mapper.selectSequenceForUpdate(type, date);
        if (current == null || current >= max)
        {
            throw new ServiceException("库存编号序列已达到上限");
        }
        if (mapper.incrementSequence(type, date) != 1)
        {
            throw new ServiceException("库存编号生成失败，请重试");
        }
        return current + 1;
    }
}
