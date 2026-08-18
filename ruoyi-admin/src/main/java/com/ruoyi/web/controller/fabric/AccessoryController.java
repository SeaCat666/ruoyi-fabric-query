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
import com.ruoyi.common.core.domain.FabricAccessory;
import com.ruoyi.common.core.domain.FabricAccessorySupplier;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.file.FileUploadUtils;
import com.ruoyi.common.utils.file.FileUtils;
import com.ruoyi.common.utils.file.MimeTypeUtils;
import com.ruoyi.framework.config.ServerConfig;
import com.ruoyi.system.service.AccessoryService;

@RestController
@RequestMapping("/fabric/accessory")
public class AccessoryController extends BaseController
{
    private static final Logger log = LoggerFactory.getLogger(AccessoryController.class);
    private static final long MAX_IMAGE_SIZE = 10L * 1024L * 1024L;

    @Autowired
    private AccessoryService service;

    @Autowired
    private ServerConfig serverConfig;

    @PreAuthorize("@ss.hasPermi('fabric:fabric:list')")
    @GetMapping("/list")
    public TableDataInfo list(FabricAccessory query)
    {
        startPage();
        List<FabricAccessory> list = service.selectList(query);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('fabric:fabric:query')")
    @GetMapping("/{id}")
    public AjaxResult get(@PathVariable Long id) { return success(service.selectById(id)); }

    @PreAuthorize("@ss.hasPermi('fabric:fabric:add')")
    @Log(title = "辅料管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Valid @RequestBody FabricAccessory accessory)
    {
        accessory.setCreateBy(getUsername());
        return toAjax(service.insert(accessory));
    }

    @PreAuthorize("@ss.hasPermi('fabric:fabric:edit')")
    @Log(title = "辅料管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Valid @RequestBody FabricAccessory accessory)
    {
        checkEditDataScope(accessory.getId());
        accessory.setUpdateBy(getUsername());
        return toAjax(service.update(accessory));
    }

    @PreAuthorize("@ss.hasPermi('fabric:fabric:remove')")
    @Log(title = "辅料管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        checkManagerRole();
        return toAjax(service.delete(ids));
    }

    /**
     * 上传辅料原图并在独立目录生成300px宽缩略图。
     */
    @PreAuthorize("@ss.hasPermi('fabric:fabric:upload')")
    @Log(title = "辅料图片", businessType = BusinessType.INSERT)
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

            String baseDir = RuoYiConfig.getProfile() + "/accessory";
            String fileName = FileUploadUtils.upload(
                    baseDir, file, MimeTypeUtils.IMAGE_EXTENSION, true);
            String relativePath = StringUtils.substringAfter(
                    fileName, Constants.RESOURCE_PREFIX);
            originalFile = new File(RuoYiConfig.getProfile() + relativePath);

            String thumbnailRelative = relativePath.replaceFirst(
                    "^/accessory/", "/accessory/thumbnail/");
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
            log.error("辅料图片上传失败", e);
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
     * 面料主管和超级管理员可修改全部辅料，其他具备修改权限的账号只能修改本人录入的数据。
     */
    private void checkEditDataScope(Long accessoryId)
    {
        if (accessoryId == null)
        {
            throw new ServiceException("辅料记录编号不能为空");
        }
        if (SecurityUtils.isAdmin(getUserId()) || SecurityUtils.hasRole("fabric_manager"))
        {
            return;
        }
        FabricAccessory existing = service.selectById(accessoryId);
        if (existing == null)
        {
            throw new ServiceException("辅料记录不存在或已删除");
        }
        if (!getUsername().equals(existing.getCreateBy()))
        {
            throw new ServiceException("录入员只能修改本人录入的辅料");
        }
    }

    /**
     * 删除属于管理操作，不因菜单误授权而向普通角色开放。
     */
    private void checkManagerRole()
    {
        if (!SecurityUtils.isAdmin(getUserId()) && !SecurityUtils.hasRole("fabric_manager"))
        {
            throw new ServiceException("只有面料主管或系统管理员可以删除辅料");
        }
    }

    @PreAuthorize("@ss.hasPermi('fabric:fabric:list')")
    @GetMapping("/suppliers")
    public AjaxResult suppliers() { return success(service.selectSupplierList()); }

    @PreAuthorize("@ss.hasAnyPermi('fabric:fabric:add,fabric:master:edit')")
    @PostMapping("/suppliers")
    public AjaxResult createSupplier(@Valid @RequestBody FabricAccessorySupplier supplier)
    {
        return success(service.createSupplier(supplier));
    }

    @PreAuthorize("@ss.hasPermi('fabric:master:edit')")
    @PutMapping("/suppliers")
    public AjaxResult updateSupplier(@Valid @RequestBody FabricAccessorySupplier supplier)
    {
        return toAjax(service.updateSupplier(supplier));
    }

    @PreAuthorize("@ss.hasPermi('fabric:master:edit')")
    @DeleteMapping("/suppliers/{id}")
    public AjaxResult deleteSupplier(@PathVariable Long id)
    {
        return toAjax(service.deleteSupplier(id));
    }
}
