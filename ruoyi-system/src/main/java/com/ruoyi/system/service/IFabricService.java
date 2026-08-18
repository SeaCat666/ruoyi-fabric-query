package com.ruoyi.system.service;

import java.util.List;
import java.util.Map;
import com.ruoyi.common.core.domain.Fabric;
import com.ruoyi.common.core.domain.FabricCategory;
import com.ruoyi.common.core.domain.FabricComponentType;
import com.ruoyi.common.core.domain.FabricSupplier;

/**
 * 面料服务。
 */
public interface IFabricService
{
    List<Fabric> selectFabricList(Fabric fabric);

    Fabric selectFabricById(Long id);

    Map<String, Object> selectDashboard();

    List<FabricSupplier> selectSupplierList();

    FabricSupplier createSupplier(FabricSupplier supplier);

    int updateSupplier(FabricSupplier supplier);

    int deleteSupplier(Long id);

    List<FabricCategory> selectCategoryList();

    FabricCategory createCategory(FabricCategory category);

    int updateCategory(FabricCategory category);

    int deleteCategory(Long id);

    List<FabricComponentType> selectComponentTypeList();

    int insertFabric(Fabric fabric);

    int updateFabric(Fabric fabric);

    int deleteFabricByIds(Long[] ids);
}
