package com.ruoyi.web.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import com.ruoyi.system.service.FabricDataInitService;
import com.ruoyi.system.service.FabricImageDataInitService;
import com.ruoyi.system.service.AccessoryCsvInitService;
import com.ruoyi.system.service.InventoryDataInitService;

/**
 * 数据库初始化完成后导入面料业务数据。
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class FabricDataInitRunner implements ApplicationRunner
{
    @Autowired
    private FabricDataInitService dataInitService;

    @Autowired
    private FabricImageDataInitService imageDataInitService;

    @Autowired
    private AccessoryCsvInitService accessoryCsvInitService;

    @Autowired
    private InventoryDataInitService inventoryDataInitService;

    @Override
    public void run(ApplicationArguments args)
    {
        dataInitService.initializeData();
        accessoryCsvInitService.initialize();
        imageDataInitService.initializeImages();
        inventoryDataInitService.initialize();
    }
}
