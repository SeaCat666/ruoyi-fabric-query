package com.ruoyi.system.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.core.domain.InventoryInboundDetail;
import com.ruoyi.common.core.domain.InventoryInboundOrder;
import com.ruoyi.common.core.domain.InventoryMovement;
import com.ruoyi.common.core.domain.InventoryRequisitionDetail;
import com.ruoyi.common.core.domain.InventoryRequisitionOrder;
import com.ruoyi.common.core.domain.InventoryStock;
import com.ruoyi.common.core.domain.InventoryStockImage;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.mapper.InventoryMapper;
import com.ruoyi.system.utils.InventoryNumberUtils;

/** 面辅料库存业务服务。 */
@Service
public class InventoryService
{
    private static final BigDecimal ZERO = BigDecimal.ZERO;

    @Autowired
    private InventoryMapper mapper;
    @Autowired
    private InventoryNumberUtils numberUtils;

    public List<InventoryStock> selectStockList(InventoryStock query)
    {
        List<InventoryStock> rows = mapper.selectStockList(query);
        populateImages(rows);
        return rows;
    }

    public InventoryStock selectStockById(Long id)
    {
        InventoryStock stock = mapper.selectStockById(id);
        if (stock != null) populateImages(Collections.singletonList(stock));
        return stock;
    }

    @Transactional
    public int insertStock(InventoryStock stock)
    {
        normalizeStock(stock);
        stock.setStockCode(numberUtils.nextStockCode());
        stock.setOnHandQty(ZERO);
        stock.setLockedQty(ZERO);
        stock.setOnHandAuxQty(ZERO);
        stock.setLockedAuxQty(ZERO);
        stock.setLastCountTime(LocalDateTime.now());
        return mapper.insertStock(stock);
    }

    @Transactional
    public int updateStock(InventoryStock stock)
    {
        InventoryStock existing = stock.getId() == null ? null : mapper.selectStockForUpdate(stock.getId());
        if (existing == null)
            throw new ServiceException("库存记录不存在");
        normalizeStock(stock);
        protectStockIdentity(existing, stock);
        return mapper.updateStock(stock);
    }

    @Transactional
    public int deleteStock(Long id, String operator)
    {
        InventoryStock stock = lockStock(id);
        if (value(stock.getOnHandQty()).signum() != 0
                || value(stock.getLockedQty()).signum() != 0
                || value(stock.getOnHandAuxQty()).signum() != 0
                || value(stock.getLockedAuxQty()).signum() != 0)
        {
            throw new ServiceException("库存余额或锁定量不为零，不能删除；请通过业务单据或库存调整处理");
        }
        if (mapper.countMovementsByStockId(id) > 0
                || mapper.countInboundDetailsByStockId(id) > 0
                || mapper.countRequisitionDetailsByStockId(id) > 0)
        {
            throw new ServiceException("库存已产生单据或流水，不能删除；历史关系必须保留用于追溯");
        }
        return mapper.deleteStock(id, operator);
    }

    @Transactional
    public void adjustStock(InventoryStock input, String operator)
    {
        InventoryStock stock = lockStock(input.getId());
        BigDecimal qtyChange = value(input.getAdjustQty());
        BigDecimal auxChange = value(input.getAdjustAuxQty());
        if (qtyChange.signum() == 0 && auxChange.signum() == 0)
            throw new ServiceException("调整数量不能全部为零");
        BigDecimal newQty = value(stock.getOnHandQty()).add(qtyChange);
        BigDecimal newAux = value(stock.getOnHandAuxQty()).add(auxChange);
        ensureBalance(stock, newQty, value(stock.getLockedQty()), newAux,
                value(stock.getLockedAuxQty()));
        updateBalance(stock, newQty, value(stock.getLockedQty()), newAux,
                value(stock.getLockedAuxQty()), operator);
        writeMovement(stock, "ADJUST", "STOCK", stock.getId(), stock.getStockCode(),
                qtyChange, ZERO, auxChange, ZERO, operator, input.getRemark());
    }

    public List<InventoryInboundOrder> selectInboundList(InventoryInboundOrder query)
    {
        return mapper.selectInboundList(query);
    }

    public InventoryInboundOrder selectInboundById(Long id)
    {
        InventoryInboundOrder order = mapper.selectInboundById(id);
        if (order != null) order.setDetails(mapper.selectInboundDetails(id));
        return order;
    }

    @Transactional
    public int saveInbound(InventoryInboundOrder order)
    {
        prepareInbound(order);
        if (order.getId() == null)
        {
            order.setOrderNo(numberUtils.nextInboundNo(order.getOrderDate()));
            order.setStatus("DRAFT");
            mapper.insertInbound(order);
        }
        else
        {
            InventoryInboundOrder existing = requireInbound(order.getId());
            requireStatus(existing.getStatus(), "DRAFT", "只有草稿入库单可以修改");
            if (mapper.updateInbound(order) != 1) throw new ServiceException("入库单修改失败");
            mapper.deleteInboundDetails(order.getId());
        }
        insertInboundDetails(order);
        return 1;
    }

    @Transactional
    public int deleteInbound(Long id)
    {
        requireStatus(requireInbound(id).getStatus(), "DRAFT", "只有草稿入库单可以删除");
        mapper.deleteInboundDetails(id);
        return mapper.deleteInbound(id);
    }

    @Transactional
    public void postInbound(Long id, String operator)
    {
        InventoryInboundOrder order = requireInbound(id);
        requireStatus(order.getStatus(), "DRAFT", "只有草稿入库单可以过账");
        List<InventoryInboundDetail> details = mapper.selectInboundDetails(id);
        for (InventoryInboundDetail detail : sortInbound(details))
        {
            InventoryStock stock = lockStock(detail.getStockId());
            BigDecimal qty = value(detail.getQuantity());
            BigDecimal aux = value(detail.getAuxiliaryQuantity());
            updateBalance(stock, value(stock.getOnHandQty()).add(qty), value(stock.getLockedQty()),
                    value(stock.getOnHandAuxQty()).add(aux), value(stock.getLockedAuxQty()), operator);
            writeMovement(stock, "INBOUND", "INBOUND", id, order.getOrderNo(),
                    qty, ZERO, aux, ZERO, operator, order.getRemark());
        }
        changeInboundStatus(order, "POSTED", operator);
    }

    @Transactional
    public void cancelInbound(Long id, String operator)
    {
        InventoryInboundOrder order = requireInbound(id);
        requireStatus(order.getStatus(), "POSTED", "只有已过账入库单可以冲销");
        for (InventoryInboundDetail detail : sortInbound(mapper.selectInboundDetails(id)))
        {
            InventoryStock stock = lockStock(detail.getStockId());
            BigDecimal qty = value(detail.getQuantity());
            BigDecimal aux = value(detail.getAuxiliaryQuantity());
            BigDecimal newQty = value(stock.getOnHandQty()).subtract(qty);
            BigDecimal newAux = value(stock.getOnHandAuxQty()).subtract(aux);
            ensureBalance(stock, newQty, value(stock.getLockedQty()), newAux,
                    value(stock.getLockedAuxQty()));
            updateBalance(stock, newQty, value(stock.getLockedQty()), newAux,
                    value(stock.getLockedAuxQty()), operator);
            writeMovement(stock, "INBOUND_CANCEL", "INBOUND", id, order.getOrderNo(),
                    qty.negate(), ZERO, aux.negate(), ZERO, operator, "入库冲销");
        }
        changeInboundStatus(order, "CANCELED", operator);
    }

    public List<InventoryRequisitionOrder> selectRequisitionList(InventoryRequisitionOrder query)
    {
        return mapper.selectRequisitionList(query);
    }

    public InventoryRequisitionOrder selectRequisitionById(Long id)
    {
        InventoryRequisitionOrder order = mapper.selectRequisitionById(id);
        if (order != null) order.setDetails(mapper.selectRequisitionDetails(id));
        return order;
    }

    @Transactional
    public int saveRequisition(InventoryRequisitionOrder order)
    {
        prepareRequisition(order);
        if (order.getId() == null)
        {
            order.setOrderNo(numberUtils.nextRequisitionNo(order.getOrderDate()));
            order.setStatus("DRAFT");
            mapper.insertRequisition(order);
        }
        else
        {
            InventoryRequisitionOrder existing = requireRequisition(order.getId());
            requireStatus(existing.getStatus(), "DRAFT", "只有草稿领用单可以修改");
            if (mapper.updateRequisition(order) != 1) throw new ServiceException("领用单修改失败");
            mapper.deleteRequisitionDetails(order.getId());
        }
        insertRequisitionDetails(order);
        return 1;
    }

    @Transactional
    public int deleteRequisition(Long id)
    {
        requireStatus(requireRequisition(id).getStatus(), "DRAFT", "只有草稿领用单可以删除");
        mapper.deleteRequisitionDetails(id);
        return mapper.deleteRequisition(id);
    }

    @Transactional
    public void lockRequisition(Long id, String operator)
    {
        InventoryRequisitionOrder order = requireRequisition(id);
        requireStatus(order.getStatus(), "DRAFT", "只有草稿领用单可以锁定");
        for (InventoryRequisitionDetail detail : sortRequisition(mapper.selectRequisitionDetails(id)))
        {
            InventoryStock stock = lockStock(detail.getStockId());
            BigDecimal qty = value(detail.getQuantity());
            BigDecimal aux = value(detail.getAuxiliaryQuantity());
            BigDecimal locked = value(stock.getLockedQty()).add(qty);
            BigDecimal lockedAux = value(stock.getLockedAuxQty()).add(aux);
            ensureBalance(stock, value(stock.getOnHandQty()), locked,
                    value(stock.getOnHandAuxQty()), lockedAux);
            updateBalance(stock, value(stock.getOnHandQty()), locked,
                    value(stock.getOnHandAuxQty()), lockedAux, operator);
            writeMovement(stock, "LOCK", "REQUISITION", id, order.getOrderNo(),
                    ZERO, qty, ZERO, aux, operator, order.getRemark());
        }
        changeRequisitionStatus(order, "LOCKED", operator);
    }

    @Transactional
    public void issueRequisition(Long id, String operator)
    {
        InventoryRequisitionOrder order = requireRequisition(id);
        requireStatus(order.getStatus(), "LOCKED", "只有已锁定领用单可以发料");
        for (InventoryRequisitionDetail detail : sortRequisition(mapper.selectRequisitionDetails(id)))
        {
            InventoryStock stock = lockStock(detail.getStockId());
            BigDecimal qty = value(detail.getQuantity());
            BigDecimal aux = value(detail.getAuxiliaryQuantity());
            BigDecimal newQty = value(stock.getOnHandQty()).subtract(qty);
            BigDecimal newLocked = value(stock.getLockedQty()).subtract(qty);
            BigDecimal newAux = value(stock.getOnHandAuxQty()).subtract(aux);
            BigDecimal newLockedAux = value(stock.getLockedAuxQty()).subtract(aux);
            ensureBalance(stock, newQty, newLocked, newAux, newLockedAux);
            updateBalance(stock, newQty, newLocked, newAux, newLockedAux, operator);
            writeMovement(stock, "ISSUE", "REQUISITION", id, order.getOrderNo(),
                    qty.negate(), qty.negate(), aux.negate(), aux.negate(), operator, order.getRemark());
        }
        changeRequisitionStatus(order, "ISSUED", operator);
    }

    @Transactional
    public void cancelRequisition(Long id, String operator)
    {
        InventoryRequisitionOrder order = requireRequisition(id);
        if ("DRAFT".equals(order.getStatus()))
        {
            changeRequisitionStatus(order, "CANCELED", operator);
            return;
        }
        requireStatus(order.getStatus(), "LOCKED", "只有草稿或已锁定领用单可以取消");
        for (InventoryRequisitionDetail detail : sortRequisition(mapper.selectRequisitionDetails(id)))
        {
            InventoryStock stock = lockStock(detail.getStockId());
            BigDecimal qty = value(detail.getQuantity());
            BigDecimal aux = value(detail.getAuxiliaryQuantity());
            BigDecimal newLocked = value(stock.getLockedQty()).subtract(qty);
            BigDecimal newLockedAux = value(stock.getLockedAuxQty()).subtract(aux);
            ensureBalance(stock, value(stock.getOnHandQty()), newLocked,
                    value(stock.getOnHandAuxQty()), newLockedAux);
            updateBalance(stock, value(stock.getOnHandQty()), newLocked,
                    value(stock.getOnHandAuxQty()), newLockedAux, operator);
            writeMovement(stock, "RELEASE", "REQUISITION", id, order.getOrderNo(),
                    ZERO, qty.negate(), ZERO, aux.negate(), operator, "取消领用锁定");
        }
        changeRequisitionStatus(order, "CANCELED", operator);
    }

    @Transactional
    public void returnRequisition(Long id, String operator)
    {
        InventoryRequisitionOrder order = requireRequisition(id);
        requireStatus(order.getStatus(), "ISSUED", "只有已发料领用单可以退回");
        for (InventoryRequisitionDetail detail : sortRequisition(mapper.selectRequisitionDetails(id)))
        {
            InventoryStock stock = lockStock(detail.getStockId());
            BigDecimal qty = value(detail.getQuantity());
            BigDecimal aux = value(detail.getAuxiliaryQuantity());
            updateBalance(stock, value(stock.getOnHandQty()).add(qty), value(stock.getLockedQty()),
                    value(stock.getOnHandAuxQty()).add(aux), value(stock.getLockedAuxQty()), operator);
            writeMovement(stock, "RETURN", "REQUISITION", id, order.getOrderNo(),
                    qty, ZERO, aux, ZERO, operator, "领用退回");
        }
        changeRequisitionStatus(order, "RETURNED", operator);
    }

    public List<InventoryMovement> selectMovementList(InventoryMovement query)
    {
        return mapper.selectMovementList(query);
    }

    private void normalizeStock(InventoryStock stock)
    {
        stock.setMaterialType(clean(stock.getMaterialType()));
        if (!"F".equals(stock.getMaterialType()) && !"A".equals(stock.getMaterialType()))
            throw new ServiceException("物料类型必须是面料或辅料");
        if ("F".equals(stock.getMaterialType())) stock.setAccessoryId(null);
        if ("A".equals(stock.getMaterialType())) stock.setFabricId(null);
        if ("F".equals(stock.getMaterialType()) && stock.getFabricId() != null
                && mapper.selectActiveFabricForUpdate(stock.getFabricId()) == null)
            throw new ServiceException("所选面料档案不存在、已删除或已停用");
        if ("A".equals(stock.getMaterialType()) && stock.getAccessoryId() != null
                && mapper.selectActiveAccessoryForUpdate(stock.getAccessoryId()) == null)
            throw new ServiceException("所选辅料档案不存在、已删除或已停用");
        stock.setMaterialCode(limit(clean(stock.getMaterialCode()), 80));
        stock.setDevelopmentStyleNo(limit(clean(stock.getDevelopmentStyleNo()), 100));
        stock.setSkc(limit(clean(stock.getSkc()), 100));
        stock.setColorNo(limit(clean(stock.getColorNo()), 100));
        stock.setGoodsType(limit(clean(stock.getGoodsType()), 100));
        stock.setPrimaryUnit(limit(clean(stock.getPrimaryUnit()), 20));
        stock.setAuxiliaryUnit(limit(clean(stock.getAuxiliaryUnit()), 20));
        stock.setRemark(limit(clean(stock.getRemark()), 1000));
        if (stock.getPrimaryUnit() == null) throw new ServiceException("主计量单位不能为空");
    }

    private void protectStockIdentity(InventoryStock existing, InventoryStock updated)
    {
        boolean hasHistory = mapper.countMovementsByStockId(existing.getId()) > 0
                || mapper.countInboundDetailsByStockId(existing.getId()) > 0
                || mapper.countRequisitionDetailsByStockId(existing.getId()) > 0;
        if (!hasHistory) return;
        if (!Objects.equals(existing.getMaterialType(), updated.getMaterialType()))
            throw new ServiceException("库存已产生单据或流水，不能更改面料/辅料类型");
        if (!Objects.equals(existing.getPrimaryUnit(), updated.getPrimaryUnit())
                || !Objects.equals(existing.getAuxiliaryUnit(), updated.getAuxiliaryUnit()))
            throw new ServiceException("库存已产生单据或流水，不能更改计量单位");
        Long existingArchiveId = "F".equals(existing.getMaterialType())
                ? existing.getFabricId() : existing.getAccessoryId();
        Long updatedArchiveId = "F".equals(updated.getMaterialType())
                ? updated.getFabricId() : updated.getAccessoryId();
        if (existingArchiveId != null && !Objects.equals(existingArchiveId, updatedArchiveId))
            throw new ServiceException("库存已关联档案并产生历史记录，不能改绑或取消关联");
    }

    private void prepareInbound(InventoryInboundOrder order)
    {
        if (order.getOrderDate() == null) order.setOrderDate(LocalDate.now());
        order.setOperatorName(limit(clean(order.getOperatorName()), 100));
        order.setRemark(limit(clean(order.getRemark()), 1000));
        validateInboundDetails(order.getDetails());
    }

    private void prepareRequisition(InventoryRequisitionOrder order)
    {
        if (order.getOrderDate() == null) order.setOrderDate(LocalDate.now());
        order.setApplicantName(limit(clean(order.getApplicantName()), 100));
        order.setDevelopmentStyleNo(limit(clean(order.getDevelopmentStyleNo()), 100));
        order.setRemark(limit(clean(order.getRemark()), 1000));
        if (order.getApplicantName() == null) throw new ServiceException("领用人不能为空");
        validateRequisitionDetails(order.getDetails());
    }

    private void validateInboundDetails(List<InventoryInboundDetail> details)
    {
        if (details == null || details.isEmpty()) throw new ServiceException("入库单至少需要一条明细");
        Set<Long> ids = new LinkedHashSet<>(); int sort = 0;
        for (InventoryInboundDetail detail : details)
        {
            validateDetail(detail.getStockId(), detail.getQuantity(), detail.getAuxiliaryQuantity(), ids);
            detail.setQuantity(value(detail.getQuantity()));
            detail.setAuxiliaryQuantity(value(detail.getAuxiliaryQuantity()));
            detail.setSortOrder(sort++);
        }
    }

    private void validateRequisitionDetails(List<InventoryRequisitionDetail> details)
    {
        if (details == null || details.isEmpty()) throw new ServiceException("领用单至少需要一条明细");
        Set<Long> ids = new LinkedHashSet<>(); int sort = 0;
        for (InventoryRequisitionDetail detail : details)
        {
            validateDetail(detail.getStockId(), detail.getQuantity(), detail.getAuxiliaryQuantity(), ids);
            detail.setQuantity(value(detail.getQuantity()));
            detail.setAuxiliaryQuantity(value(detail.getAuxiliaryQuantity()));
            detail.setSortOrder(sort++);
        }
    }

    private void validateDetail(Long stockId, BigDecimal quantity, BigDecimal aux, Set<Long> ids)
    {
        if (stockId == null || mapper.selectStockById(stockId) == null) throw new ServiceException("请选择有效库存");
        if (!ids.add(stockId)) throw new ServiceException("同一库存行不能重复添加");
        if (value(quantity).signum() <= 0) throw new ServiceException("主数量必须大于零");
        if (value(aux).signum() < 0) throw new ServiceException("辅助数量不能小于零");
    }

    private void insertInboundDetails(InventoryInboundOrder order)
    {
        for (InventoryInboundDetail detail : order.getDetails()) { detail.setOrderId(order.getId()); mapper.insertInboundDetail(detail); }
    }

    private void insertRequisitionDetails(InventoryRequisitionOrder order)
    {
        for (InventoryRequisitionDetail detail : order.getDetails()) { detail.setOrderId(order.getId()); mapper.insertRequisitionDetail(detail); }
    }

    private InventoryInboundOrder requireInbound(Long id)
    {
        InventoryInboundOrder order = id == null ? null : mapper.selectInboundById(id);
        if (order == null) throw new ServiceException("入库单不存在");
        return order;
    }

    private InventoryRequisitionOrder requireRequisition(Long id)
    {
        InventoryRequisitionOrder order = id == null ? null : mapper.selectRequisitionById(id);
        if (order == null) throw new ServiceException("领用单不存在");
        return order;
    }

    private void changeInboundStatus(InventoryInboundOrder order, String target, String operator)
    {
        if (mapper.updateInboundStatus(order.getId(), target, order.getStatus(), operator) != 1)
            throw new ServiceException("入库单状态已变化，请刷新后重试");
    }

    private void changeRequisitionStatus(InventoryRequisitionOrder order, String target, String operator)
    {
        if (mapper.updateRequisitionStatus(order.getId(), target, order.getStatus(), operator) != 1)
            throw new ServiceException("领用单状态已变化，请刷新后重试");
    }

    private InventoryStock lockStock(Long id)
    {
        InventoryStock stock = id == null ? null : mapper.selectStockForUpdate(id);
        if (stock == null) throw new ServiceException("库存记录不存在");
        return stock;
    }

    private void updateBalance(InventoryStock stock, BigDecimal qty, BigDecimal locked,
            BigDecimal aux, BigDecimal lockedAux, String operator)
    {
        ensureBalance(stock, qty, locked, aux, lockedAux);
        if (mapper.updateStockBalance(stock.getId(), stock.getVersion(), qty, locked, aux, lockedAux, operator) != 1)
            throw new ServiceException("库存已被其他操作修改，请重试");
        stock.setOnHandQty(qty); stock.setLockedQty(locked);
        stock.setOnHandAuxQty(aux); stock.setLockedAuxQty(lockedAux);
        stock.setVersion(stock.getVersion() + 1);
    }

    private void ensureBalance(InventoryStock stock, BigDecimal qty, BigDecimal locked,
            BigDecimal aux, BigDecimal lockedAux)
    {
        if (qty.signum() < 0 || locked.signum() < 0 || locked.compareTo(qty) > 0)
            throw new ServiceException(stock.getStockCode() + "库存数量不足或锁定数量异常");
        if (aux.signum() < 0 || lockedAux.signum() < 0 || lockedAux.compareTo(aux) > 0)
            throw new ServiceException(stock.getStockCode() + "辅助库存不足或锁定数量异常");
    }

    private void writeMovement(InventoryStock stock, String movementType, String businessType,
            Long businessId, String businessNo, BigDecimal qtyChange, BigDecimal lockedChange,
            BigDecimal auxChange, BigDecimal lockedAuxChange, String operator, String remark)
    {
        InventoryMovement movement = new InventoryMovement();
        movement.setMovementNo(numberUtils.nextMovementNo()); movement.setStockId(stock.getId());
        movement.setMovementType(movementType); movement.setBusinessType(businessType);
        movement.setBusinessId(businessId); movement.setBusinessNo(businessNo);
        movement.setQuantityChange(qtyChange); movement.setLockedChange(lockedChange);
        movement.setAuxiliaryChange(auxChange); movement.setLockedAuxiliaryChange(lockedAuxChange);
        movement.setBalanceQty(stock.getOnHandQty()); movement.setBalanceLockedQty(stock.getLockedQty());
        movement.setBalanceAuxQty(stock.getOnHandAuxQty()); movement.setBalanceLockedAuxQty(stock.getLockedAuxQty());
        movement.setOperatorName(operator); movement.setOperationTime(LocalDateTime.now()); movement.setRemark(remark);
        mapper.insertMovement(movement);
    }

    private void populateImages(List<InventoryStock> rows)
    {
        if (rows == null || rows.isEmpty()) return;
        List<Long> ids = rows.stream().map(InventoryStock::getId).collect(Collectors.toList());
        Map<Long, List<InventoryStockImage>> grouped = mapper.selectStockImages(ids).stream()
                .collect(Collectors.groupingBy(InventoryStockImage::getStockId, LinkedHashMap::new, Collectors.toList()));
        rows.forEach(row -> row.setImages(grouped.getOrDefault(row.getId(), Collections.emptyList())));
    }

    private static List<InventoryInboundDetail> sortInbound(List<InventoryInboundDetail> rows)
    { return rows.stream().sorted(Comparator.comparing(InventoryInboundDetail::getStockId)).collect(Collectors.toCollection(ArrayList::new)); }
    private static List<InventoryRequisitionDetail> sortRequisition(List<InventoryRequisitionDetail> rows)
    { return rows.stream().sorted(Comparator.comparing(InventoryRequisitionDetail::getStockId)).collect(Collectors.toCollection(ArrayList::new)); }
    private static void requireStatus(String actual, String expected, String message)
    { if (!expected.equals(actual)) throw new ServiceException(message); }
    private static BigDecimal value(BigDecimal value) { return value == null ? ZERO : value; }
    private static String clean(String value) { if (value == null) return null; String v=value.trim(); return v.isEmpty()?null:v; }
    private static String limit(String value, int max) { return value != null && value.length() > max ? value.substring(0, max) : value; }
}
