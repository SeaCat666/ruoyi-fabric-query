package com.ruoyi.web.controller.inventory;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.domain.InventoryInboundOrder;
import com.ruoyi.common.core.domain.InventoryMovement;
import com.ruoyi.common.core.domain.InventoryRequisitionOrder;
import com.ruoyi.common.core.domain.InventoryStock;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.service.InventoryService;

/** 面辅料库存管理接口。 */
@RestController
@RequestMapping("/inventory")
public class InventoryController extends BaseController
{
    @Autowired
    private InventoryService service;

    @PreAuthorize("@ss.hasPermi('inventory:stock:list')")
    @GetMapping("/stock/list")
    public TableDataInfo stockList(InventoryStock query)
    {
        startPage();
        List<InventoryStock> list = service.selectStockList(query);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('inventory:stock:query')")
    @GetMapping("/stock/{id}")
    public AjaxResult stock(@PathVariable Long id) { return success(service.selectStockById(id)); }

    @PreAuthorize("@ss.hasPermi('inventory:stock:add')")
    @Log(title = "库存台账", businessType = BusinessType.INSERT)
    @PostMapping("/stock")
    public AjaxResult addStock(@RequestBody InventoryStock stock)
    {
        stock.setCreateBy(getUsername());
        return toAjax(service.insertStock(stock));
    }

    @PreAuthorize("@ss.hasPermi('inventory:stock:edit')")
    @Log(title = "库存台账", businessType = BusinessType.UPDATE)
    @PutMapping("/stock")
    public AjaxResult editStock(@RequestBody InventoryStock stock)
    {
        stock.setUpdateBy(getUsername());
        return toAjax(service.updateStock(stock));
    }

    @PreAuthorize("@ss.hasPermi('inventory:stock:adjust')")
    @Log(title = "库存调整", businessType = BusinessType.UPDATE)
    @PutMapping("/stock/adjust")
    public AjaxResult adjustStock(@RequestBody InventoryStock stock)
    {
        service.adjustStock(stock, getUsername());
        return success();
    }

    @PreAuthorize("@ss.hasPermi('inventory:stock:remove')")
    @Log(title = "库存台账", businessType = BusinessType.DELETE)
    @DeleteMapping("/stock/{id}")
    public AjaxResult removeStock(@PathVariable Long id)
    {
        checkInventoryManagerRole();
        return toAjax(service.deleteStock(id, getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('inventory:inbound:list')")
    @GetMapping("/inbound/list")
    public TableDataInfo inboundList(InventoryInboundOrder query)
    {
        startPage();
        return getDataTable(service.selectInboundList(query));
    }

    @PreAuthorize("@ss.hasPermi('inventory:inbound:query')")
    @GetMapping("/inbound/{id}")
    public AjaxResult inbound(@PathVariable Long id) { return success(service.selectInboundById(id)); }

    @PreAuthorize("@ss.hasPermi('inventory:inbound:add')")
    @Log(title = "库存入库", businessType = BusinessType.INSERT)
    @PostMapping("/inbound")
    public AjaxResult addInbound(@RequestBody InventoryInboundOrder order)
    {
        order.setCreateBy(getUsername());
        if (order.getOperatorName() == null) order.setOperatorName(getUsername());
        return toAjax(service.saveInbound(order));
    }

    @PreAuthorize("@ss.hasPermi('inventory:inbound:edit')")
    @Log(title = "库存入库", businessType = BusinessType.UPDATE)
    @PutMapping("/inbound")
    public AjaxResult editInbound(@RequestBody InventoryInboundOrder order)
    {
        order.setUpdateBy(getUsername());
        return toAjax(service.saveInbound(order));
    }

    @PreAuthorize("@ss.hasPermi('inventory:inbound:remove')")
    @Log(title = "库存入库", businessType = BusinessType.DELETE)
    @DeleteMapping("/inbound/{id}")
    public AjaxResult removeInbound(@PathVariable Long id) { return toAjax(service.deleteInbound(id)); }

    @PreAuthorize("@ss.hasPermi('inventory:inbound:post')")
    @Log(title = "入库过账", businessType = BusinessType.UPDATE)
    @PutMapping("/inbound/{id}/post")
    public AjaxResult postInbound(@PathVariable Long id)
    {
        service.postInbound(id, getUsername()); return success();
    }

    @PreAuthorize("@ss.hasPermi('inventory:inbound:cancel')")
    @Log(title = "入库冲销", businessType = BusinessType.UPDATE)
    @PutMapping("/inbound/{id}/cancel")
    public AjaxResult cancelInbound(@PathVariable Long id)
    {
        service.cancelInbound(id, getUsername()); return success();
    }

    @PreAuthorize("@ss.hasPermi('inventory:requisition:list')")
    @GetMapping("/requisition/list")
    public TableDataInfo requisitionList(InventoryRequisitionOrder query)
    {
        startPage();
        return getDataTable(service.selectRequisitionList(query));
    }

    @PreAuthorize("@ss.hasPermi('inventory:requisition:query')")
    @GetMapping("/requisition/{id}")
    public AjaxResult requisition(@PathVariable Long id) { return success(service.selectRequisitionById(id)); }

    @PreAuthorize("@ss.hasPermi('inventory:requisition:add')")
    @Log(title = "库存领用", businessType = BusinessType.INSERT)
    @PostMapping("/requisition")
    public AjaxResult addRequisition(@RequestBody InventoryRequisitionOrder order)
    {
        order.setCreateBy(getUsername());
        return toAjax(service.saveRequisition(order));
    }

    @PreAuthorize("@ss.hasPermi('inventory:requisition:edit')")
    @Log(title = "库存领用", businessType = BusinessType.UPDATE)
    @PutMapping("/requisition")
    public AjaxResult editRequisition(@RequestBody InventoryRequisitionOrder order)
    {
        order.setUpdateBy(getUsername());
        return toAjax(service.saveRequisition(order));
    }

    @PreAuthorize("@ss.hasPermi('inventory:requisition:remove')")
    @Log(title = "库存领用", businessType = BusinessType.DELETE)
    @DeleteMapping("/requisition/{id}")
    public AjaxResult removeRequisition(@PathVariable Long id) { return toAjax(service.deleteRequisition(id)); }

    @PreAuthorize("@ss.hasPermi('inventory:requisition:lock')")
    @Log(title = "领用锁定", businessType = BusinessType.UPDATE)
    @PutMapping("/requisition/{id}/lock")
    public AjaxResult lockRequisition(@PathVariable Long id)
    { service.lockRequisition(id, getUsername()); return success(); }

    @PreAuthorize("@ss.hasPermi('inventory:requisition:issue')")
    @Log(title = "领用发料", businessType = BusinessType.UPDATE)
    @PutMapping("/requisition/{id}/issue")
    public AjaxResult issueRequisition(@PathVariable Long id)
    { service.issueRequisition(id, getUsername()); return success(); }

    @PreAuthorize("@ss.hasPermi('inventory:requisition:cancel')")
    @Log(title = "领用取消", businessType = BusinessType.UPDATE)
    @PutMapping("/requisition/{id}/cancel")
    public AjaxResult cancelRequisition(@PathVariable Long id)
    { service.cancelRequisition(id, getUsername()); return success(); }

    @PreAuthorize("@ss.hasPermi('inventory:requisition:return')")
    @Log(title = "领用退回", businessType = BusinessType.UPDATE)
    @PutMapping("/requisition/{id}/return")
    public AjaxResult returnRequisition(@PathVariable Long id)
    { service.returnRequisition(id, getUsername()); return success(); }

    @PreAuthorize("@ss.hasPermi('inventory:movement:list')")
    @GetMapping("/movement/list")
    public TableDataInfo movementList(InventoryMovement query)
    {
        startPage();
        return getDataTable(service.selectMovementList(query));
    }

    private void checkInventoryManagerRole()
    {
        if (!SecurityUtils.isAdmin(getUserId()) && !SecurityUtils.hasRole("fabric_manager"))
        {
            throw new ServiceException("只有面料主管或系统管理员可以删除误建库存行");
        }
    }
}
