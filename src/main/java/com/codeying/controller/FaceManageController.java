package com.codeying.controller;

import cn.smartjavaai.common.entity.face.FaceSearchResult;
import com.codeying.entity.*;
import com.codeying.service.ImageStorageService;
import com.codeying.utils.FaceRecUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.*;

/**
 * 人脸管理控制器
 * 提供人脸注册、查询、删除等功能
 */
@Controller
@RequestMapping("/face")
public class FaceManageController {

    private static final Logger log = LoggerFactory.getLogger(FaceManageController.class);

    @Autowired
    private ImageStorageService imageStorageService;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 访问人脸管理页面
     */
    @GetMapping("/manage")
    public String managePage() {
        return "manage";
    }

    /**
     * 人脸注册接口
     * @param request 注册请求（包含Base64图片、用户ID、角色）
     * @return 注册结果
     */
    @ResponseBody
    @PostMapping("/register")
    public ApiResponse<FaceInfo> registerFace(@RequestBody FaceRegisterRequest request) {
        try {
            log.info("收到人脸注册请求: userId={}, role={}", request.getUserId(), request.getRole());

            // 参数校验
            if (request.getUserId() == null || request.getUserId().trim().isEmpty()) {
                return ApiResponse.error("用户ID不能为空");
            }
            if (request.getRole() == null || request.getRole().trim().isEmpty()) {
                return ApiResponse.error("角色不能为空");
            }
            if (request.getBase64Image() == null || request.getBase64Image().trim().isEmpty()) {
                return ApiResponse.error("人脸图片不能为空");
            }

            // 解析Base64图片
            String base64Data = request.getBase64Image();
            if (base64Data.contains(",")) {
                base64Data = base64Data.substring(base64Data.indexOf(",") + 1);
            }
            byte[] imageBytes = Base64.getDecoder().decode(base64Data);

            // 转换为BufferedImage
            BufferedImage image;
            try (ByteArrayInputStream bis = new ByteArrayInputStream(imageBytes)) {
                image = ImageIO.read(bis);
            }

            if (image == null) {
                return ApiResponse.error("图片格式无效");
            }

            // 调用人脸识别SDK进行注册
            String faceId = FaceRecUtils.registerFace(image, request.getUserId(), request.getRole());

            if (faceId == null || faceId.trim().isEmpty()) {
                return ApiResponse.error("人脸注册失败，可能未检测到人脸");
            }

            // 保存图片到文件系统
            String fileName = imageStorageService.saveBufferedImage(image, request.getUserId());

            // 构建返回信息
            FaceInfo faceInfo = new FaceInfo();
            faceInfo.setFaceId(faceId);
            faceInfo.setUserId(request.getUserId());
            faceInfo.setRole(request.getRole());
            faceInfo.setImageFileName(fileName);
            faceInfo.setImageUrl(imageStorageService.getImageUrl(fileName));
            faceInfo.setRegisterTime(System.currentTimeMillis());

            log.info("人脸注册成功: faceId={}, userId={}", faceId, request.getUserId());
            return ApiResponse.success("注册成功", faceInfo);

        } catch (Exception e) {
            log.error("人脸注册失败", e);
            return ApiResponse.error("注册失败: " + e.getMessage());
        }
    }

    /**
     * 人脸搜索接口（上传图片进行1:N搜索）
     * @param requestData 包含Base64图片的请求数据
     * @return 搜索结果
     */
    @ResponseBody
    @PostMapping("/search")
    public ApiResponse<FaceSearchResponse> searchFace(@RequestBody Map<String, String> requestData) {
        try {
            log.info("收到人脸搜索请求");

            String base64Image = requestData.get("base64Image");
            if (base64Image == null || base64Image.trim().isEmpty()) {
                return ApiResponse.error("图片数据不能为空");
            }

            // 解析Base64图片
            String base64Data = base64Image;
            if (base64Image.contains(",")) {
                base64Data = base64Image.substring(base64Image.indexOf(",") + 1);
            }
            byte[] imageBytes = Base64.getDecoder().decode(base64Data);

            // 转换为BufferedImage
            BufferedImage image;
            try (ByteArrayInputStream bis = new ByteArrayInputStream(imageBytes)) {
                image = ImageIO.read(bis);
            }

            if (image == null) {
                return ApiResponse.error("图片格式无效");
            }

            // 调用人脸识别SDK进行搜索
            FaceSearchResult result = FaceRecUtils.queryFace(image);

            if (result == null || result.getId() == null || result.getSimilarity() < 0.8f) {
                log.info("未找到匹配的人脸");
                return ApiResponse.success("未找到匹配", FaceSearchResponse.notFound());
            }

            // 解析元数据
            Map<String, String> metadata = objectMapper.readValue(result.getMetadata(), new TypeReference<Map<String, String>>() {});
            String userId = metadata.get("userid");
            String role = metadata.get("role");

            // 构建响应
            FaceSearchResponse response = new FaceSearchResponse();
            response.setFound(true);
            response.setFaceId(result.getId());
            response.setUserId(userId);
            response.setRole(role);
            response.setSimilarity(result.getSimilarity());
            response.setMetadata(result.getMetadata());

            // 尝试获取图片URL（根据userId查找）
            String imageFileName = findImageFileByUserId(userId);
            if (imageFileName != null) {
                response.setImageUrl(imageStorageService.getImageUrl(imageFileName));
            }

            log.info("人脸搜索成功: faceId={}, similarity={}", result.getId(), result.getSimilarity());
            return ApiResponse.success("找到匹配", response);

        } catch (Exception e) {
            log.error("人脸搜索失败", e);
            return ApiResponse.error("搜索失败: " + e.getMessage());
        }
    }

    /**
     * 活體檢測接口（上傳單張圖片判斷是否為真人）
     * @param requestData 請求體，需包含 base64Image（Base64 編碼的圖片，可帶 data:image/xxx;base64, 前綴）
     * @return 活體檢測結果：live、score、statusDescription、faceDetected
     */
    @ResponseBody
    @PostMapping("/liveness")
    public ApiResponse<LivenessDetectResult> livenessDetect(@RequestBody Map<String, String> requestData) {
        try {
            log.info("收到活體檢測請求");

            String base64Image = requestData.get("base64Image");
            if (base64Image == null || base64Image.trim().isEmpty()) {
                return ApiResponse.error("圖片數據不能為空");
            }

            String base64Data = base64Image;
            if (base64Image.contains(",")) {
                base64Data = base64Image.substring(base64Image.indexOf(",") + 1);
            }
            byte[] imageBytes = Base64.getDecoder().decode(base64Data);

            BufferedImage image;
            try (ByteArrayInputStream bis = new ByteArrayInputStream(imageBytes)) {
                image = ImageIO.read(bis);
            }

            if (image == null) {
                return ApiResponse.error("圖片格式無效");
            }

            LivenessDetectResult result = FaceRecUtils.livenessDetect(image);
            log.info("活體檢測完成: live={}, score={}, faceDetected={}", result.isLive(), result.getScore(), result.isFaceDetected());
            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("活體檢測失敗", e);
            return ApiResponse.error("活體檢測失敗: " + e.getMessage());
        }
    }

    /**
     * 表情检测接口（上传单张图片识别表情）
     * @param requestData 请求体，需包含 base64Image（Base64 编码的图片，可带 data:image/xxx;base64, 前缀）
     * @return 表情检测结果：expressionLabel、expressionDescription、score、faceDetected
     */
    @ResponseBody
    @PostMapping("/expression")
    public ApiResponse<ExpressionDetectResult> expressionDetect(@RequestBody Map<String, String> requestData) {
        try {
            log.info("收到表情检测请求");

            String base64Image = requestData.get("base64Image");
            if (base64Image == null || base64Image.trim().isEmpty()) {
                return ApiResponse.error("图片数据不能为空");
            }

            String base64Data = base64Image;
            if (base64Image.contains(",")) {
                base64Data = base64Image.substring(base64Image.indexOf(",") + 1);
            }
            byte[] imageBytes = Base64.getDecoder().decode(base64Data);

            BufferedImage image;
            try (ByteArrayInputStream bis = new ByteArrayInputStream(imageBytes)) {
                image = ImageIO.read(bis);
            }

            if (image == null) {
                return ApiResponse.error("图片格式无效");
            }

            ExpressionDetectResult result = FaceRecUtils.expressionDetect(image);
            log.info("表情检测完成: label={}, score={}, faceDetected={}",
                    result.getExpressionLabel(), result.getScore(), result.isFaceDetected());
            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("表情检测失败", e);
            return ApiResponse.error("表情检测失败: " + e.getMessage());
        }
    }

    /**
     * 获取所有已注册人脸列表
     * @return 人脸信息列表
     */
    @ResponseBody
    @GetMapping("/list")
    public ApiResponse<List<FaceInfo>> listAllFaces() {
        try {
            log.info("获取所有人脸列表");

            // 获取所有人脸ID
            List<String> faceIds = FaceRecUtils.getAllFaceIds();

            List<FaceInfo> faceList = new ArrayList<>();

            // 遍历每个人脸ID，获取详细信息
            for (String faceId : faceIds) {
                try {
                    String metadata = FaceRecUtils.getFaceMetadataById(faceId);
                    
                    if (metadata != null && !metadata.trim().isEmpty()) {
                        // 解析元数据
                        Map<String, String> metaMap = objectMapper.readValue(metadata, new TypeReference<Map<String, String>>() {});
                        String userId = metaMap.get("userid");
                        String role = metaMap.get("role");

                        // 构建FaceInfo
                        FaceInfo faceInfo = new FaceInfo();
                        faceInfo.setFaceId(faceId);
                        faceInfo.setUserId(userId);
                        faceInfo.setRole(role);
                        faceInfo.setMetadata(metadata);

                        // 查找对应的图片文件
                        String imageFileName = findImageFileByUserId(userId);
                        if (imageFileName != null) {
                            faceInfo.setImageFileName(imageFileName);
                            faceInfo.setImageUrl(imageStorageService.getImageUrl(imageFileName));
                        }

                        faceList.add(faceInfo);
                    }
                } catch (Exception e) {
                    log.warn("解析人脸信息失败: faceId={}", faceId, e);
                }
            }

            log.info("成功获取 {} 个人脸信息", faceList.size());
            return ApiResponse.success(faceList);

        } catch (Exception e) {
            log.error("获取人脸列表失败", e);
            return ApiResponse.error("获取列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取人脸详情
     * @param faceId 人脸ID
     * @return 人脸详细信息
     */
    @ResponseBody
    @GetMapping("/detail/{faceId}")
    public ApiResponse<FaceInfo> getFaceDetail(@PathVariable String faceId) {
        try {
            log.info("获取人脸详情: faceId={}", faceId);

            String metadata = FaceRecUtils.getFaceMetadataById(faceId);

            if (metadata == null || metadata.trim().isEmpty()) {
                return ApiResponse.error("人脸信息不存在");
            }

            // 解析元数据
            Map<String, String> metaMap = objectMapper.readValue(metadata, new TypeReference<Map<String, String>>() {});
            String userId = metaMap.get("userid");
            String role = metaMap.get("role");

            // 构建FaceInfo
            FaceInfo faceInfo = new FaceInfo();
            faceInfo.setFaceId(faceId);
            faceInfo.setUserId(userId);
            faceInfo.setRole(role);
            faceInfo.setMetadata(metadata);

            // 查找对应的图片文件
            String imageFileName = findImageFileByUserId(userId);
            if (imageFileName != null) {
                faceInfo.setImageFileName(imageFileName);
                faceInfo.setImageUrl(imageStorageService.getImageUrl(imageFileName));
            }

            return ApiResponse.success(faceInfo);

        } catch (Exception e) {
            log.error("获取人脸详情失败: faceId={}", faceId, e);
            return ApiResponse.error("获取详情失败: " + e.getMessage());
        }
    }

    /**
     * 删除人脸
     * @param faceId 人脸ID
     * @return 删除结果
     */
    @ResponseBody
    @DeleteMapping("/delete/{faceId}")
    public ApiResponse<String> deleteFace(@PathVariable String faceId) {
        try {
            log.info("删除人脸: faceId={}", faceId);

            // 先获取元数据，以便删除对应的图片文件
            String metadata = FaceRecUtils.getFaceMetadataById(faceId);
            String imageFileName = null;

            if (metadata != null && !metadata.trim().isEmpty()) {
                try {
                    Map<String, String> metaMap = objectMapper.readValue(metadata, new TypeReference<Map<String, String>>() {});
                    String userId = metaMap.get("userid");
                    imageFileName = findImageFileByUserId(userId);
                } catch (Exception e) {
                    log.warn("解析元数据失败", e);
                }
            }

            // 从人脸库删除
            FaceRecUtils.deleteFace(faceId);

            // 删除图片文件
            if (imageFileName != null) {
                boolean deleted = imageStorageService.deleteImage(imageFileName);
                if (deleted) {
                    log.info("图片文件删除成功: {}", imageFileName);
                }
            }

            log.info("人脸删除成功: faceId={}", faceId);
            return ApiResponse.success("删除成功", faceId);

        } catch (Exception e) {
            log.error("删除人脸失败: faceId={}", faceId, e);
            return ApiResponse.error("删除失败: " + e.getMessage());
        }
    }

    /**
     * 根据用户ID查找图片文件名
     * 在face-imgs目录中查找以userId开头的文件
     */
    private String findImageFileByUserId(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return null;
        }

        try {
            java.io.File dir = new java.io.File("face-imgs");
            if (!dir.exists() || !dir.isDirectory()) {
                return null;
            }

            // 查找以userId开头的文件
            java.io.File[] files = dir.listFiles((d, name) -> 
                name.startsWith(userId + "_") && name.endsWith(".jpg")
            );

            if (files != null && files.length > 0) {
                // 返回最新的文件（根据文件名中的时间戳）
                Arrays.sort(files, Comparator.comparing(java.io.File::getName).reversed());
                return files[0].getName();
            }

        } catch (Exception e) {
            log.warn("查找图片文件失败: userId={}", userId, e);
        }

        return null;
    }
}

