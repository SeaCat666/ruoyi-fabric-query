package com.ruoyi.system.mapper;

import java.math.BigDecimal;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.common.core.domain.InventoryInboundDetail;
import com.ruoyi.common.core.domain.InventoryInboundOrder;
import com.ruoyi.common.core.domain.InventoryMovement;
import com.ruoyi.common.core.domain.InventoryRequisitionDetail;
import com.ruoyi.common.core.domain.InventoryRequisitionOrder;
import com.ruoyi.common.core.domain.InventoryStock;
import com.ruoyi.common.core.domain.InventoryStockImage;

public interface InventoryMapper
{
    List<InventoryStock> selectStockList(InventoryStock query);
    InventoryStock selectStockById(Long id);
    InventoryStock selectStockForUpdate(Long id);
    int insertStock(InventoryStock stock);
    int updateStock(InventoryStock stock);
    int updateStockBalance(@Param("id") Long id, @Param("version") Integer version,
            @Param("onHandQty") BigDecimal onHandQty, @Param("lockedQty") BigDecimal lockedQty,
            @Param("onHandAuxQty") BigDecimal onHandAuxQty,
            @Param("lockedAuxQty") BigDecimal lockedAuxQty,
            @Param("updateBy") String updateBy);
    int insertStockImage(InventoryStockImage image);
    List<InventoryStockImage> selectStockImages(@Param("stockIds") List<Long> stockIds);
    int countStockBySourceKey(String sourceKey);
    Long selectActiveFabricForUpdate(Long id);
    Long selectActiveAccessoryForUpdate(Long id);
    List<Long> selectActiveFabricIdsForUpdate(@Param("ids") Long[] ids);
    List<Long> selectActiveAccessoryIdsForUpdate(@Param("ids") Long[] ids);
    List<String> selectReferencedFabricCodes(@Param("ids") Long[] ids);
    List<String> selectReferencedAccessoryCodes(@Param("ids") Long[] ids);
    int countMovementsByStockId(Long stockId);
    int countInboundDetailsByStockId(Long stockId);
    int countRequisitionDetailsByStockId(Long stockId);
    int deleteStock(@Param("id") Long id, @Param("updateBy") String updateBy);

    List<InventoryInboundOrder> selectInboundList(InventoryInboundOrder query);
    InventoryInboundOrder selectInboundById(Long id);
    int insertInbound(InventoryInboundOrder order);
    int updateInbound(InventoryInboundOrder order);
    int updateInboundStatus(@Param("id") Long id, @Param("status") String status,
            @Param("expectedStatus") String expectedStatus, @Param("updateBy") String updateBy);
    int deleteInbound(Long id);
    int deleteInboundDetails(Long orderId);
    int insertInboundDetail(InventoryInboundDetail detail);
    List<InventoryInboundDetail> selectInboundDetails(Long orderId);

    List<InventoryRequisitionOrder> selectRequisitionList(InventoryRequisitionOrder query);
    InventoryRequisitionOrder selectRequisitionById(Long id);
    int insertRequisition(InventoryRequisitionOrder order);
    int updateRequisition(InventoryRequisitionOrder order);
    int updateRequisitionStatus(@Param("id") Long id, @Param("status") String status,
            @Param("expectedStatus") String expectedStatus, @Param("updateBy") String updateBy);
    int deleteRequisition(Long id);
    int deleteRequisitionDetails(Long orderId);
    int insertRequisitionDetail(InventoryRequisitionDetail detail);
    List<InventoryRequisitionDetail> selectRequisitionDetails(Long orderId);

    List<InventoryMovement> selectMovementList(InventoryMovement query);
    int insertMovement(InventoryMovement movement);
    int mergeSequence(@Param("sequenceType") String sequenceType,
            @Param("sequenceDate") String sequenceDate);
    Integer selectSequenceForUpdate(@Param("sequenceType") String sequenceType,
            @Param("sequenceDate") String sequenceDate);
    int incrementSequence(@Param("sequenceType") String sequenceType,
            @Param("sequenceDate") String sequenceDate);
}
