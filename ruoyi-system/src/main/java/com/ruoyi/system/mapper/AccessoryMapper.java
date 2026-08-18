package com.ruoyi.system.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.common.core.domain.FabricAccessory;
import com.ruoyi.common.core.domain.FabricAccessoryImage;
import com.ruoyi.common.core.domain.FabricAccessorySupplier;

public interface AccessoryMapper
{
    List<FabricAccessory> selectAccessoryList(FabricAccessory query);
    FabricAccessory selectAccessoryById(Long id);
    FabricAccessory selectAccessoryByCode(String code);
    List<FabricAccessory> selectRecentAccessories();
    List<FabricAccessory> selectAccessoriesWithoutImagesByYear(Integer year);
    int countAllAccessories();
    int countAccessoryTotal();
    int countAccessoryByYear(Integer year);
    int countAccessorySuppliers();
    int countAccessoryImages();
    int countAccessoriesWithImages();
    List<Map<String, Object>> selectAccessoryYearStats();
    int insertAccessory(FabricAccessory accessory);
    int updateAccessory(FabricAccessory accessory);
    int deleteAccessoryByIds(Long[] ids);
    List<FabricAccessoryImage> selectImagesByAccessoryIds(
            @Param("accessoryIds") List<Long> accessoryIds);
    int insertAccessoryImage(FabricAccessoryImage image);
    int deleteImagesByAccessoryId(Long accessoryId);

    List<FabricAccessorySupplier> selectSupplierList();
    FabricAccessorySupplier selectSupplierById(Long id);
    FabricAccessorySupplier selectSupplierByName(String name);
    int insertSupplier(FabricAccessorySupplier supplier);
    int updateSupplier(FabricAccessorySupplier supplier);
    int countAccessoriesBySupplierId(Long supplierId);
    int deleteSupplierById(Long id);

    int updateImportedSupplierDetails(@Param("id") Long id,
            @Param("phone") String phone, @Param("address") String address);

    int mergeCodeSequence(Integer year);
    int updateCodeSequence(@Param("year") Integer year,
            @Param("lastValue") Integer lastValue);
    int incrementCodeSequence(Integer year);
    Integer selectCodeSequenceForUpdate(Integer year);
}
