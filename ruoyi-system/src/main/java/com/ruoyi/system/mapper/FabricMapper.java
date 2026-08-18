package com.ruoyi.system.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.common.core.domain.Fabric;
import com.ruoyi.common.core.domain.FabricCategory;
import com.ruoyi.common.core.domain.FabricComponentType;
import com.ruoyi.common.core.domain.FabricComposition;
import com.ruoyi.common.core.domain.FabricImage;
import com.ruoyi.common.core.domain.FabricSupplier;

/**
 * 面料数据层。
 */
public interface FabricMapper
{
    List<Fabric> selectFabricList(Fabric fabric);

    Fabric selectFabricById(Long id);

    List<Fabric> selectFabricsWithoutImagesByYear(Integer year);

    int countFabricByYear(Integer year);

    int countAllFabricsByYear(Integer year);

    int countFabricTotal();

    int countFabricSuppliers();

    int countFabricImages();

    int countFabricsWithImages();

    List<Map<String, Object>> selectFabricYearStats();

    List<Map<String, Object>> selectFabricCategoryStats();

    List<Map<String, Object>> selectFabricPriceUnitStats();

    List<Fabric> selectRecentFabrics();

    int insertFabric(Fabric fabric);

    int updateFabric(Fabric fabric);

    int deleteFabricByIds(Long[] ids);

    List<FabricSupplier> selectSupplierList();

    FabricSupplier selectSupplierById(Long id);

    FabricSupplier selectSupplierByName(String name);

    int insertSupplier(FabricSupplier supplier);

    int updateSupplier(FabricSupplier supplier);

    int deleteSupplierById(Long id);

    int countFabricsBySupplierId(Long supplierId);

    List<FabricCategory> selectCategoryList();

    FabricCategory selectCategoryById(Long id);

    FabricCategory selectCategoryByName(
            @Param("parentId") Long parentId,
            @Param("name") String name,
            @Param("excludeId") Long excludeId);

    int insertCategory(FabricCategory category);

    int updateCategory(FabricCategory category);

    int deleteCategoryById(Long id);

    int countCategoryChildren(Long parentId);

    int countFabricsByCategoryId(Long categoryId);

    List<FabricComponentType> selectComponentTypeList();

    int mergeCodeSequence(Integer year);

    int reserveCodeSequence(@Param("year") Integer year, @Param("amount") Integer amount);

    int incrementCodeSequence(Integer year);

    Integer selectCodeSequence(Integer year);

    Integer selectCodeSequenceForUpdate(Integer year);

    List<FabricComposition> selectCompositionsByFabricId(Long fabricId);

    List<FabricComposition> selectCompositionsByFabricIds(@Param("fabricIds") List<Long> fabricIds);

    int insertFabricComposition(FabricComposition composition);

    int deleteCompositionsByFabricId(Long fabricId);

    List<FabricImage> selectImagesByFabricId(Long fabricId);

    List<FabricImage> selectImagesByFabricIds(@Param("fabricIds") List<Long> fabricIds);

    int insertFabricImage(FabricImage image);

    int deleteImagesByFabricId(Long fabricId);
}
