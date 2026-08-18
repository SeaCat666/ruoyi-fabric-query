package com.ruoyi.system.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.coobird.thumbnailator.Thumbnails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.core.domain.Fabric;
import com.ruoyi.common.core.domain.FabricAccessory;
import com.ruoyi.common.core.domain.FabricAccessoryImage;
import com.ruoyi.common.core.domain.FabricImage;
import com.ruoyi.system.mapper.AccessoryMapper;
import com.ruoyi.system.mapper.FabricMapper;

/**
 * 开发环境面料测试图片初始化。
 */
@Service
public class FabricImageDataInitService
{
    private static final Logger log = LoggerFactory.getLogger(FabricImageDataInitService.class);

    private static final long RANDOM_SEED = 20260725L;

    @Value("${ruoyi.fabric.image-init-enabled:true}")
    private boolean enabled;

    @Value("${ruoyi.fabric.image-source-directory:}")
    private String sourceDirectory;

    @Autowired
    private FabricMapper fabricMapper;

    @Autowired
    private AccessoryMapper accessoryMapper;

    public Map<String, Object> initializeImages()
    {
        Map<String, Object> result = new LinkedHashMap<>();
        if (!enabled)
        {
            result.put("enabled", false);
            return result;
        }

        Path sourceRoot = Paths.get(sourceDirectory).toAbsolutePath().normalize();
        List<Path> sourceImages = listSourceImages(sourceRoot);
        if (sourceImages.isEmpty())
        {
            log.warn("面料测试图片初始化跳过：目录不存在或没有jpg/png图片，目录={}", sourceRoot);
            result.put("sourceImages", 0);
            result.put("skipped", true);
            return result;
        }

        Path uploadRoot = Paths.get(RuoYiConfig.getProfile()).toAbsolutePath().normalize();
        Path originalDirectory = uploadRoot.resolve("fabric/test/original");
        Path thumbnailDirectory = uploadRoot.resolve("fabric/test/thumbnail");
        Path accessoryOriginalDirectory = uploadRoot.resolve("accessory/test/original");
        Path accessoryThumbnailDirectory = uploadRoot.resolve("accessory/test/thumbnail");
        try
        {
            Files.createDirectories(originalDirectory);
            Files.createDirectories(thumbnailDirectory);
            Files.createDirectories(accessoryOriginalDirectory);
            Files.createDirectories(accessoryThumbnailDirectory);
        }
        catch (IOException e)
        {
            throw new IllegalStateException("创建面料测试图片上传目录失败：" + e.getMessage(), e);
        }

        ImageSourceCursor sourceCursor = new ImageSourceCursor(sourceImages, new Random(RANDOM_SEED));
        int workerCount = Math.max(2, Math.min(6, Runtime.getRuntime().availableProcessors()));
        ExecutorService imageExecutor = Executors.newFixedThreadPool(workerCount);
        YearImageStats year2026;
        YearImageStats year2025;
        AccessoryImageStats accessory2026;
        try
        {
            year2026 = initializeYear(2026, 1.00D, 1, 3, sourceCursor,
                    originalDirectory, thumbnailDirectory, imageExecutor);
            year2025 = initializeYear(2025, 1.00D, 1, 3, sourceCursor,
                    originalDirectory, thumbnailDirectory, imageExecutor);
            accessory2026 = initializeAccessories(2026, sourceCursor,
                    accessoryOriginalDirectory, accessoryThumbnailDirectory, imageExecutor);
        }
        finally
        {
            imageExecutor.shutdown();
        }

        int assignedFabrics = year2026.assignedFabrics + year2025.assignedFabrics;
        int assignedImages = year2026.assignedImages + year2025.assignedImages;
        result.put("sourceImages", sourceImages.size());
        result.put("year2026", year2026.toMap());
        result.put("year2025", year2025.toMap());
        result.put("accessory2026", accessory2026.toMap());
        result.put("assignedFabrics", assignedFabrics);
        result.put("assignedImages", assignedImages);
        result.put("assignedAccessories", accessory2026.assignedAccessories);
        result.put("assignedAccessoryImages", accessory2026.assignedImages);
        log.info("测试图片初始化完成：给{}个面料分配了{}张图片，给{}个辅料分配了{}张图片"
                        + "（2026面料{}个/{}张，2025面料{}个/{}张，源图片{}张）",
                assignedFabrics, assignedImages,
                accessory2026.assignedAccessories, accessory2026.assignedImages,
                year2026.assignedFabrics, year2026.assignedImages,
                year2025.assignedFabrics, year2025.assignedImages,
                sourceImages.size());
        return result;
    }

    private YearImageStats initializeYear(int year, double coverage, int minimumImages, int maximumImages,
            ImageSourceCursor sourceCursor, Path originalDirectory, Path thumbnailDirectory,
            ExecutorService imageExecutor)
    {
        int totalFabrics = fabricMapper.countFabricByYear(year);
        List<Fabric> withoutImages = new ArrayList<>(fabricMapper.selectFabricsWithoutImagesByYear(year));
        int existingFabrics = totalFabrics - withoutImages.size();
        int targetCoverage = (int) Math.round(totalFabrics * coverage);
        int targetNewFabrics = Math.min(withoutImages.size(), Math.max(0, targetCoverage - existingFabrics));

        Collections.shuffle(withoutImages, new Random(RANDOM_SEED + year));
        Random imageCountRandom = new Random(RANDOM_SEED * 31 + year);
        YearImageStats stats = new YearImageStats(year, totalFabrics, existingFabrics, targetNewFabrics);
        List<Future<Integer>> futures = new ArrayList<>(targetNewFabrics);
        for (int index = 0; index < targetNewFabrics; index++)
        {
            Fabric fabric = withoutImages.get(index);
            int wantedImages = minimumImages
                    + imageCountRandom.nextInt(maximumImages - minimumImages + 1);
            futures.add(imageExecutor.submit(() -> saveImages(fabric, wantedImages, sourceCursor,
                    originalDirectory, thumbnailDirectory)));
        }
        for (Future<Integer> future : futures)
        {
            try
            {
                int savedImages = future.get();
                if (savedImages > 0)
                {
                    stats.assignedFabrics++;
                    stats.assignedImages += savedImages;
                }
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("面料测试图片初始化被中断", e);
            }
            catch (ExecutionException e)
            {
                log.warn("{}年面料测试图片任务执行失败：{}", year, e.getCause().getMessage());
            }
        }
        return stats;
    }

    private AccessoryImageStats initializeAccessories(int year,
            ImageSourceCursor sourceCursor, Path originalDirectory,
            Path thumbnailDirectory, ExecutorService imageExecutor)
    {
        int totalAccessories = accessoryMapper.countAccessoryByYear(year);
        List<FabricAccessory> withoutImages = new ArrayList<>(
                accessoryMapper.selectAccessoriesWithoutImagesByYear(year));
        AccessoryImageStats stats = new AccessoryImageStats(
                year, totalAccessories, totalAccessories - withoutImages.size());
        Collections.shuffle(withoutImages, new Random(RANDOM_SEED + year + 97));
        Random imageCountRandom = new Random(RANDOM_SEED * 43 + year);
        List<Future<Integer>> futures = new ArrayList<>(withoutImages.size());
        for (FabricAccessory accessory : withoutImages)
        {
            int wantedImages = 1 + imageCountRandom.nextInt(3);
            futures.add(imageExecutor.submit(() -> saveAccessoryImages(
                    accessory, wantedImages, sourceCursor,
                    originalDirectory, thumbnailDirectory)));
        }
        for (Future<Integer> future : futures)
        {
            try
            {
                int savedImages = future.get();
                if (savedImages > 0)
                {
                    stats.assignedAccessories++;
                    stats.assignedImages += savedImages;
                }
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("辅料测试图片初始化被中断", e);
            }
            catch (ExecutionException e)
            {
                log.warn("{}年辅料测试图片任务执行失败：{}", year, e.getCause().getMessage());
            }
        }
        return stats;
    }

    private int saveImages(Fabric fabric, int wantedImages, ImageSourceCursor sourceCursor,
            Path originalDirectory, Path thumbnailDirectory)
    {
        int savedImages = 0;
        int attempts = 0;
        int maximumAttempts = Math.max(wantedImages * 3, 10);
        while (savedImages < wantedImages && attempts < maximumAttempts)
        {
            attempts++;
            Path sourceImage = sourceCursor.next();
            String extension = getExtension(sourceImage);
            int sortOrder = savedImages + 1;
            String fileName = String.format(Locale.ROOT, "fabric-%d-%d-%02d%s",
                    fabric.getYear(), fabric.getId(), sortOrder, extension);
            Path originalFile = originalDirectory.resolve(fileName);
            Path thumbnailFile = thumbnailDirectory.resolve(fileName);
            try
            {
                Files.copy(sourceImage, originalFile, StandardCopyOption.REPLACE_EXISTING);
                Thumbnails.of(originalFile.toFile())
                        .width(300)
                        .keepAspectRatio(true)
                        .outputQuality(0.85D)
                        .toFile(thumbnailFile.toFile());

                FabricImage image = new FabricImage();
                image.setFabricId(fabric.getId());
                image.setImageUrl(toResourceUrl("fabric/test/original/" + fileName));
                image.setThumbnailUrl(toResourceUrl("fabric/test/thumbnail/" + fileName));
                image.setImageType(FabricImage.FABRIC_DETAIL);
                image.setSortOrder(sortOrder);
                fabricMapper.insertFabricImage(image);
                savedImages++;
            }
            catch (Exception e)
            {
                deleteQuietly(originalFile);
                deleteQuietly(thumbnailFile);
                log.warn("生成面料测试缩略图失败，面料={}，源图片={}：{}",
                        fabric.getCode(), sourceImage, e.getMessage());
            }
        }
        return savedImages;
    }

    private int saveAccessoryImages(FabricAccessory accessory, int wantedImages,
            ImageSourceCursor sourceCursor, Path originalDirectory, Path thumbnailDirectory)
    {
        int savedImages = 0;
        int attempts = 0;
        int maximumAttempts = Math.max(wantedImages * 3, 10);
        while (savedImages < wantedImages && attempts < maximumAttempts)
        {
            attempts++;
            Path sourceImage = sourceCursor.next();
            String extension = getExtension(sourceImage);
            int sortOrder = savedImages + 1;
            String fileName = String.format(Locale.ROOT, "accessory-%d-%d-%02d%s",
                    accessory.getYear(), accessory.getId(), sortOrder, extension);
            Path originalFile = originalDirectory.resolve(fileName);
            Path thumbnailFile = thumbnailDirectory.resolve(fileName);
            try
            {
                Files.copy(sourceImage, originalFile, StandardCopyOption.REPLACE_EXISTING);
                Thumbnails.of(originalFile.toFile())
                        .width(300)
                        .keepAspectRatio(true)
                        .outputQuality(0.85D)
                        .toFile(thumbnailFile.toFile());

                FabricAccessoryImage image = new FabricAccessoryImage();
                image.setAccessoryId(accessory.getId());
                image.setImageUrl(toResourceUrl("accessory/test/original/" + fileName));
                image.setThumbnailUrl(toResourceUrl("accessory/test/thumbnail/" + fileName));
                image.setSortOrder(sortOrder);
                accessoryMapper.insertAccessoryImage(image);
                savedImages++;
            }
            catch (Exception e)
            {
                deleteQuietly(originalFile);
                deleteQuietly(thumbnailFile);
                log.warn("生成辅料测试缩略图失败，辅料={}，源图片={}：{}",
                        accessory.getCode(), sourceImage, e.getMessage());
            }
        }
        return savedImages;
    }

    private List<Path> listSourceImages(Path sourceRoot)
    {
        if (!Files.isDirectory(sourceRoot))
        {
            return Collections.emptyList();
        }
        try (Stream<Path> stream = Files.walk(sourceRoot))
        {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(this::isSupportedImage)
                    .sorted()
                    .collect(Collectors.toList());
        }
        catch (IOException e)
        {
            log.warn("读取面料测试图片目录失败，目录={}：{}", sourceRoot, e.getMessage());
            return Collections.emptyList();
        }
    }

    private boolean isSupportedImage(Path path)
    {
        String extension = getExtension(path);
        return ".jpg".equals(extension) || ".jpeg".equals(extension) || ".png".equals(extension);
    }

    private String getExtension(Path path)
    {
        String fileName = path.getFileName().toString();
        int dotIndex = fileName.lastIndexOf('.');
        return dotIndex < 0 ? "" : fileName.substring(dotIndex).toLowerCase(Locale.ROOT);
    }

    private String toResourceUrl(String relativePath)
    {
        return Constants.RESOURCE_PREFIX + "/" + relativePath.replace('\\', '/');
    }

    private void deleteQuietly(Path path)
    {
        try
        {
            Files.deleteIfExists(path);
        }
        catch (IOException ignored)
        {
            // 下次初始化会覆盖同名文件。
        }
    }

    private static final class ImageSourceCursor
    {
        private final List<Path> images;
        private final Random random;
        private int index;

        private ImageSourceCursor(List<Path> images, Random random)
        {
            this.images = new ArrayList<>(images);
            this.random = random;
            Collections.shuffle(this.images, this.random);
        }

        private synchronized Path next()
        {
            if (index >= images.size())
            {
                Collections.shuffle(images, random);
                index = 0;
            }
            return images.get(index++);
        }
    }

    private static final class YearImageStats
    {
        private final int year;
        private final int totalFabrics;
        private final int existingFabrics;
        private final int targetNewFabrics;
        private int assignedFabrics;
        private int assignedImages;

        private YearImageStats(int year, int totalFabrics, int existingFabrics, int targetNewFabrics)
        {
            this.year = year;
            this.totalFabrics = totalFabrics;
            this.existingFabrics = existingFabrics;
            this.targetNewFabrics = targetNewFabrics;
        }

        private Map<String, Object> toMap()
        {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("year", year);
            result.put("totalFabrics", totalFabrics);
            result.put("existingFabrics", existingFabrics);
            result.put("targetNewFabrics", targetNewFabrics);
            result.put("assignedFabrics", assignedFabrics);
            result.put("assignedImages", assignedImages);
            return result;
        }
    }

    private static final class AccessoryImageStats
    {
        private final int year;
        private final int totalAccessories;
        private final int existingAccessories;
        private int assignedAccessories;
        private int assignedImages;

        private AccessoryImageStats(int year, int totalAccessories, int existingAccessories)
        {
            this.year = year;
            this.totalAccessories = totalAccessories;
            this.existingAccessories = existingAccessories;
        }

        private Map<String, Object> toMap()
        {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("year", year);
            result.put("totalAccessories", totalAccessories);
            result.put("existingAccessories", existingAccessories);
            result.put("assignedAccessories", assignedAccessories);
            result.put("assignedImages", assignedImages);
            return result;
        }
    }
}
