package com.ruoyi.web.controller.fabric;

import java.io.File;
import java.io.IOException;
import java.util.List;
import jakarta.validation.Valid;
import net.coobird.thumbnailator.Thumbnails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.domain.Fabric;
import com.ruoyi.common.core.domain.FabricCategory;
import com.ruoyi.common.core.domain.FabricSupplier;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.file.FileUploadUtils;
import com.ruoyi.common.utils.file.FileUtils;
import com.ruoyi.common.utils.file.MimeTypeUtils;
import com.ruoyi.framework.config.ServerConfig;
import com.ruoyi.system.service.IFabricService;

/**
 * 面料档案接口。
 */
@RestController
@RequestMapping("/fabric/fabric")
public class FabricController extends BaseController
{
    private static final Logger log = LoggerFactory.getLogger(FabricController.class);

    private static final long MAX_IMAGE_SIZE = 10L * 1024L * 1024L;

    @Autowired
    private IFabricService fabricService;

    @Autowired
    private ServerConfig serverConfig;

    @PreAuthorize("@ss.hasPermi('fabric:fabric:list')")
    @GetMapping("/list")
    public TableDataInfo list(Fabric fabric)
    {
        startPage();
        List<Fabric> list = fabricService.selectFabricList(fabric);
        return getDataTable(list);
    }

    /**
     * 所有已登录账号共用的面料业务首页数据。
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/dashboard")
    public AjaxResult dashboard()
    {
        return success(fabricService.selectDashboard());
    }

    @PreAuthorize("@ss.hasPermi('fabric:fabric:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id)
    {
        return success(fabricService.selectFabricById(id));
    }

    @PreAuthorize("@ss.hasPermi('fabric:fabric:list')")
    @GetMapping("/suppliers")
    public AjaxResult suppliers()
    {
        return success(fabricService.selectSupplierList());
    }

    /**
     * 新增表单内快速创建供应商。
     */
    @PreAuthorize("@ss.hasAnyPermi('fabric:fabric:add,fabric:master:edit')")
    @Log(title = "供应商快捷新增", businessType = BusinessType.INSERT)
    @PostMapping("/suppliers")
    public AjaxResult createSupplier(@Valid @RequestBody FabricSupplier supplier)
    {
        return success(fabricService.createSupplier(supplier));
    }

    @PreAuthorize("@ss.hasPermi('fabric:master:edit')")
    @Log(title = "供应商管理", businessType = BusinessType.UPDATE)
    @PutMapping("/suppliers")
    public AjaxResult updateSupplier(@Valid @RequestBody FabricSupplier supplier)
    {
        return toAjax(fabricService.updateSupplier(supplier));
    }

    @PreAuthorize("@ss.hasPermi('fabric:master:edit')")
    @Log(title = "供应商管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/suppliers/{id}")
    public AjaxResult deleteSupplier(@PathVariable Long id)
    {
        return toAjax(fabricService.deleteSupplier(id));
    }

    @PreAuthorize("@ss.hasPermi('fabric:fabric:list')")
    @GetMapping("/categories")
    public AjaxResult categories()
    {
        return success(fabricService.selectCategoryList());
    }

    @PreAuthorize("@ss.hasPermi('fabric:master:edit')")
    @Log(title = "面料分类管理", businessType = BusinessType.INSERT)
    @PostMapping("/categories")
    public AjaxResult createCategory(@Valid @RequestBody FabricCategory category)
    {
        return success(fabricService.createCategory(category));
    }

    @PreAuthorize("@ss.hasPermi('fabric:master:edit')")
    @Log(title = "面料分类管理", businessType = BusinessType.UPDATE)
    @PutMapping("/categories")
    public AjaxResult updateCategory(@Valid @RequestBody FabricCategory category)
    {
        return toAjax(fabricService.updateCategory(category));
    }

    @PreAuthorize("@ss.hasPermi('fabric:master:edit')")
    @Log(title = "面料分类管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/categories/{id}")
    public AjaxResult deleteCategory(@PathVariable Long id)
    {
        return toAjax(fabricService.deleteCategory(id));
    }

    @PreAuthorize("@ss.hasPermi('fabric:fabric:list')")
    @GetMapping("/components")
    public AjaxResult components()
    {
        return success(fabricService.selectComponentTypeList());
    }

    @PreAuthorize("@ss.hasPermi('fabric:fabric:add')")
    @Log(title = "面料管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Valid @RequestBody Fabric fabric)
    {
        fabric.setCreateBy(getUsername());
        return toAjax(fabricService.insertFabric(fabric));
    }

    @PreAuthorize("@ss.hasPermi('fabric:fabric:edit')")
    @Log(title = "面料管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Valid @RequestBody Fabric fabric)
    {
        checkEditDataScope(fabric.getId());
        fabric.setUpdateBy(getUsername());
        return toAjax(fabricService.updateFabric(fabric));
    }

    @PreAuthorize("@ss.hasPermi('fabric:fabric:remove')")
    @Log(title = "面料管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        checkManagerRole();
        return toAjax(fabricService.deleteFabricByIds(ids));
    }

    /**
     * 上传不超过10MB的原图并生成300px宽缩略图。
     */
    @PreAuthorize("@ss.hasPermi('fabric:fabric:upload')")
    @Log(title = "面料图片", businessType = BusinessType.INSERT)
    @PostMapping("/upload")
    public AjaxResult upload(MultipartFile file)
    {
        File originalFile = null;
        File thumbnailFile = null;
        try
        {
            if (file == null || file.isEmpty())
            {
                return AjaxResult.error("请选择要上传的图片");
            }
            if (file.getSize() > MAX_IMAGE_SIZE)
            {
                return AjaxResult.error("图片大小不能超过10MB");
            }

            String baseDir = RuoYiConfig.getProfile() + "/fabric";
            String fileName = FileUploadUtils.upload(
                    baseDir, file, MimeTypeUtils.IMAGE_EXTENSION, true);
            String relativePath = StringUtils.substringAfter(
                    fileName, Constants.RESOURCE_PREFIX);
            originalFile = new File(RuoYiConfig.getProfile() + relativePath);

            String thumbnailRelative = relativePath.replaceFirst(
                    "^/fabric/", "/fabric/thumbnail/");
            thumbnailFile = new File(RuoYiConfig.getProfile() + thumbnailRelative);
            if (!thumbnailFile.getParentFile().exists()
                    && !thumbnailFile.getParentFile().mkdirs())
            {
                throw new IOException("无法创建缩略图目录");
            }
            Thumbnails.of(originalFile)
                    .width(300)
                    .keepAspectRatio(true)
                    .toFile(thumbnailFile);

            String thumbnailFileName = Constants.RESOURCE_PREFIX + thumbnailRelative;
            AjaxResult ajax = AjaxResult.success();
            ajax.put("url", serverConfig.getUrl() + fileName);
            ajax.put("fileName", fileName);
            ajax.put("thumbnailUrl", serverConfig.getUrl() + thumbnailFileName);
            ajax.put("thumbnailFileName", thumbnailFileName);
            ajax.put("originalFilename", file.getOriginalFilename());
            return ajax;
        }
        catch (Exception e)
        {
            deleteUploadedFile(thumbnailFile);
            deleteUploadedFile(originalFile);
            log.error("面料图片上传失败", e);
            return AjaxResult.error("图片上传失败，请确认文件是有效的 JPG 或 PNG 图片后重试");
        }
    }

    private void deleteUploadedFile(File file)
    {
        if (file != null && file.exists() && !FileUtils.deleteFile(file.getAbsolutePath()))
        {
            log.warn("无法清理上传失败产生的文件：{}", file.getAbsolutePath());
        }
    }

    /**
     * 面料主管和超级管理员可修改全部面料，其他具备修改权限的账号只能修改本人录入的数据。
     */
    private void checkEditDataScope(Long fabricId)
    {
        if (fabricId == null)
        {
            throw new ServiceException("面料记录编号不能为空");
        }
        if (SecurityUtils.isAdmin(getUserId()) || SecurityUtils.hasRole("fabric_manager"))
        {
            return;
        }
        Fabric existing = fabricService.selectFabricById(fabricId);
        if (existing == null)
        {
            throw new ServiceException("面料记录不存在或已删除");
        }
        if (!getUsername().equals(existing.getCreateBy()))
        {
            throw new ServiceException("录入员只能修改本人录入的面料");
        }
    }

    /**
     * 删除属于管理操作，不因菜单误授权而向普通角色开放。
     */
    private void checkManagerRole()
    {
        if (!SecurityUtils.isAdmin(getUserId()) && !SecurityUtils.hasRole("fabric_manager"))
        {
            throw new ServiceException("只有面料主管或系统管理员可以删除面料");
        }
    }
}
