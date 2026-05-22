package com.codeying.utils;

import ai.djl.modality.cv.Image;
import cn.smartjavaai.common.config.Config;
import cn.smartjavaai.common.cv.SmartImageFactory;
import cn.smartjavaai.common.entity.DetectionInfo;
import cn.smartjavaai.common.entity.DetectionRectangle;
import cn.smartjavaai.common.entity.DetectionResponse;
import cn.smartjavaai.common.entity.R;
import cn.smartjavaai.common.entity.face.FaceSearchResult;
import cn.smartjavaai.common.entity.face.ExpressionResult;
import cn.smartjavaai.common.enums.DeviceEnum;
import cn.smartjavaai.common.enums.SimilarityType;
import cn.smartjavaai.common.enums.face.LivenessStatus;
import cn.smartjavaai.common.utils.ImageUtils;
import cn.smartjavaai.face.config.FaceDetConfig;
import cn.smartjavaai.face.config.FaceRecConfig;
import cn.smartjavaai.face.config.LivenessConfig;
import cn.smartjavaai.face.config.FaceExpressionConfig;
import cn.smartjavaai.face.constant.FaceDetectConstant;
import cn.smartjavaai.face.constant.LivenessConstant;
import cn.smartjavaai.face.entity.FaceRegisterInfo;
import cn.smartjavaai.face.entity.FaceSearchParams;
import cn.smartjavaai.face.enums.FaceDetModelEnum;
import cn.smartjavaai.face.enums.FaceRecModelEnum;
import cn.smartjavaai.face.enums.LivenessModelEnum;
import cn.smartjavaai.face.enums.ExpressionModelEnum;
import cn.smartjavaai.face.factory.FaceDetModelFactory;
import cn.smartjavaai.face.factory.FaceRecModelFactory;
import cn.smartjavaai.face.factory.LivenessModelFactory;
import cn.smartjavaai.face.factory.ExpressionModelFactory;
import cn.smartjavaai.face.model.facedect.FaceDetModel;
import cn.smartjavaai.face.model.facerec.FaceRecModel;
import cn.smartjavaai.face.model.liveness.LivenessDetModel;
import cn.smartjavaai.face.model.expression.ExpressionModel;
import cn.smartjavaai.face.vector.config.SQLiteConfig;
import com.codeying.entity.ExpressionDetectResult;
import com.codeying.entity.LivenessDetectResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import nu.pattern.OpenCV;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.List;

import org.junit.BeforeClass;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

import javax.swing.*;

/**
 * 人脸识别工具类
 * 
 * 基于 SmartJavaAI SDK 实现人脸检测、识别、注册、查询、删除等功能
 * 
 * 使用前准备：
 * 1. 下载模型文件（约355MB）
 *    - 百度网盘：https://pan.baidu.com/s/10l22x5fRz_gwLr8EAHa1Jg?pwd=1234
 *    - 提取码：1234
 *    - 需要下载：retinaface.pt (105MB) 和 elasticface.pt (250MB)
 * 
 * 2. 修改 MODAL_PATH 配置
 *    - 将模型文件放在本地目录
 *    - 修改上面的 MODAL_PATH 常量为你的模型目录路径
 * 
 * 3. 参考文档
 *    - 官方文档：http://doc.smartjavaai.cn/
 *    - GitHub：https://github.com/SmartJavaAI
 * 
 * @author dwj
 * @version 1.0
 */
public class FaceRecUtils {

    private static Logger log = LoggerFactory.getLogger(FaceRecUtils.class);

    private static ObjectMapper objectMapper = new ObjectMapper();
    //设备类型
    public static DeviceEnum device = DeviceEnum.CPU;

    @BeforeClass
    public static void beforeAll() throws IOException {
        //将图片处理的底层引擎切换为 OpenCV
        SmartImageFactory.setEngine(SmartImageFactory.Engine.OPENCV);
    }

    // ========== 👇 重要配置：首次运行请修改 👇 ==========
    /**
     * 模型文件存储路径
     * TODO: ⚠️ 首次运行前，请将此路径修改为你本地的模型文件目录
     * 
     * 配置说明：
     * 1. 下载模型文件到本地目录（百度网盘：https://pan.baidu.com/s/10l22x5fRz_gwLr8EAHa1Jg?pwd=1234）
     * 2. 将下面的路径修改为你存放模型的目录
     * 3. 路径末尾必须包含斜杠 / 或 \\
     * 
     * 示例：
     * - Windows: "D:/models/smart_java_ai/"
     * - Linux/Mac: "/home/user/models/smart_java_ai/"
     * - 相对路径: "smart_java_ai/" (模型文件放在项目根目录下)
     */
    public static String MODAL_PATH = "smart_java_ai/";
    // ========== 👆 重要配置：首次运行请修改 👆 ==========

    static {
        //修改缓存路径
        Config.setCachePath(MODAL_PATH);
    }

    public static void init(){
        getFaceRecModelWithSQLiteConfig();
    }

    /**
     * 人脸更新
     */
    public static void updateFace(BufferedImage image,String userid,String role,String faceId){
        try {
            //高精度模型，速度慢, 追求速度请更换高速模型
            FaceRecModel faceRecModel = getFaceRecModelWithSQLiteConfig();
            //等待加载人脸库结束
            while (!faceRecModel.isLoadFaceCompleted()){
                Thread.sleep(100);
            }
            //设置人脸注册的自定义元数据，本例中使用 JSON 格式存储用户信息
            ObjectNode metadataJson = objectMapper.createObjectNode();
            metadataJson.put("userid", userid);
            metadataJson.put("role", role);

            log.info("====================人脸更新==========================");
            FaceRegisterInfo updateInfo = new FaceRegisterInfo();
            //设置人脸注册的自定义元数据，本例中使用 JSON 格式存储用户信息
            updateInfo.setMetadata(objectMapper.writeValueAsString(metadataJson));
            //更新必须设置ID
            updateInfo.setId(faceId);
            faceRecModel.upsertFace(updateInfo, image);
            log.info("更新人脸成功");
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
    /**
     * 人脸查询
     */
    public static FaceSearchResult queryFace(BufferedImage image){
        try {
            //高精度模型，速度慢, 追求速度请更换高速模型
            FaceRecModel faceRecModel = getFaceRecModelWithSQLiteConfig();
            //等待加载人脸库结束
            while (!faceRecModel.isLoadFaceCompleted()){
                Thread.sleep(100);
            }

            log.info("====================人脸查询==========================");
            //特征提取（提取分数最高人脸特征）,适用于单人脸场景
            R<float[]> featureResult2 = faceRecModel.extractTopFaceFeature(image);
            if(featureResult2.isSuccess()){
                log.info("人脸特征提取成功,{}",objectMapper.writeValueAsString(featureResult2.getData()));
            }else{
                log.info("人脸特征提取失败：{}", featureResult2.getMessage());
                return null;
            }
            FaceSearchParams faceSearchParams = new FaceSearchParams();
            faceSearchParams.setTopK(1);
            faceSearchParams.setThreshold(0.8f);
            List<FaceSearchResult> faceSearchResults = faceRecModel.search(featureResult2.getData(), faceSearchParams);
            log.info("人脸查询结果：{}",objectMapper.writeValueAsString(faceSearchResults));
            //返回第一个人脸
            if(!faceSearchResults.isEmpty()){
                return faceSearchResults.get(0);
            }
            return new FaceSearchResult(null,0.0f, null);
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }


    /**
     * 人脸删除
     */
    public static void deleteFace(String faceId){
        try {
            //高精度模型，速度慢, 追求速度请更换高速模型
            FaceRecModel faceRecModel = getFaceRecModelWithSQLiteConfig();
            //等待加载人脸库结束
            while (!faceRecModel.isLoadFaceCompleted()){
                Thread.sleep(100);
            }
            log.info("====================人脸删除==========================");
            try {
                faceRecModel.removeRegister(faceId);
                log.info("人脸删除成功");
                // 从索引文件中删除
                removeFaceFromIndex(faceId);
            }catch (Exception e){
                log.info("人脸删除失败,可能不存在：{}", e.getMessage());
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * 注册人脸信息，传入人脸图片和用户信息
     * @param image
     * @param userid
     * @param role
     */
    public static String registerFace(BufferedImage image,String userid,String role){
        try {
            //高精度模型，速度慢, 追求速度请更换高速模型
            FaceRecModel faceRecModel = getFaceRecModelWithSQLiteConfig();
            //等待加载人脸库结束
            while (!faceRecModel.isLoadFaceCompleted()){
                Thread.sleep(100);
            }
            log.info("====================人脸注册==========================");
            //特征提取（提取分数最高人脸特征）,适用于单人脸场景
            R<float[]> featureResult = faceRecModel.extractTopFaceFeature(image);
            if(featureResult.isSuccess()){
                log.info("人脸特征提取成功:{}",objectMapper.writeValueAsString(featureResult.getData()));
            }else{
                log.info("人脸特征提取失败：{}", featureResult.getMessage());
                return null;
            }
            //人脸注册信息
            FaceRegisterInfo faceRegisterInfo = new FaceRegisterInfo();
            //设置人脸注册的自定义元数据，本例中使用 JSON 格式存储用户信息
            ObjectNode metadataJson = objectMapper.createObjectNode();
            metadataJson.put("userid", userid);
            metadataJson.put("role", role);
            faceRegisterInfo.setMetadata(objectMapper.writeValueAsString(metadataJson));
            //可自定义 ID，若未设置则自动生成。
//            faceRegisterInfo.setId("00001");
            //人脸注册，返回人脸库ID
            R<String> registerResult = faceRecModel.register(faceRegisterInfo, featureResult.getData());
            if(registerResult.isSuccess()){
                log.info("注册成功：ID-{}", registerResult.getData());
                // 保存到索引文件
                saveFaceToIndex(registerResult.getData(), userid, role);
            }else{
                log.info("注册失败：{}", registerResult.getMessage());
                return null;
            }
            return registerResult.getData();
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    private static FaceDetModel faceDetModel = null;
    /**
     * 1 获取人脸检测模型（高精度模型）
     * 注意事项：
     * 1、高精度模型，识别准确度高，速度慢
     * 2、具体其他模型参数可以查看文档：http://doc.smartjavaai.cn/face.html
     * @return
     */
    public static FaceDetModel getProFaceDetModel(){
        if(faceDetModel!= null){
            return faceDetModel;
        }
        FaceDetConfig config = new FaceDetConfig();
        //人脸检测模型，SmartJavaAI提供了多种模型选择(更多模型，请查看文档)，切换模型需要同时修改modelEnum及modelPath
        config.setModelEnum(FaceDetModelEnum.RETINA_FACE);
        //下载模型并替换本地路径，下载地址：https://pan.baidu.com/s/10l22x5fRz_gwLr8EAHa1Jg?pwd=1234 提取码: 1234
        config.setModelPath(MODAL_PATH + "retinaface.pt");
        //只返回相似度大于该值的人脸,需要根据实际情况调整，分值越大越严格容易漏检，分值越小越宽松容易误识别
        config.setConfidenceThreshold(0.5f);
        //用于去除重复的人脸框，当两个框的重叠度超过该值时，只保留一个
        config.setNmsThresh(FaceDetectConstant.NMS_THRESHOLD);
        faceDetModel = FaceDetModelFactory.getInstance().getModel(config);
        return faceDetModel;
    }


    private static FaceRecModel faceRecModel = null;
    /**
     * 获取人脸识别模型(带SQLite数据库配置)
     * @return
     */
    public static FaceRecModel getFaceRecModelWithSQLiteConfig(){
        if(faceRecModel!=null){
            return faceRecModel;
        }
        FaceRecConfig config = new FaceRecConfig();
        //高精度模型，速度慢, 追求速度请更换高速模型，具体其他模型参数可以查看文档：http://doc.smartjavaai.cn/face.html
        config.setModelEnum(FaceRecModelEnum.ELASTIC_FACE_MODEL);//人脸检测模型
        config.setModelPath(MODAL_PATH + "elasticface.pt");
        //裁剪人脸：如果图片已经是裁剪过的，则请将此参数设置为false
        config.setCropFace(true);
        //开启人脸对齐：适用于人脸不正的场景，开启将提升人脸特征准确度，关闭可以提升性能
        config.setAlign(true);
        //指定人脸检测模型，可切换人脸检测模型（极速：getFastFaceDetModel，高精度：getProFaceDetModel），具体其他模型参数可以查看文档：http://doc.smartjavaai.cn/face.html
        config.setDetectModel(getProFaceDetModel());
        config.setDevice(device);

        //初始化SQLite数据库
        SQLiteConfig vectorDBConfig = new SQLiteConfig();
        vectorDBConfig.setSimilarityType(SimilarityType.IP);
        config.setVectorDBConfig(vectorDBConfig);
        faceRecModel = FaceRecModelFactory.getInstance().getModel(config);
        return faceRecModel;
    }

    /**
     * 获取活体检测模型
     * @return
     */
    public LivenessDetModel getLivenessDetModel(){
        LivenessConfig config = new LivenessConfig();
        config.setModelEnum(LivenessModelEnum.IIC_FL_MODEL);
        config.setDevice(device);
        //需替换为实际模型存储路径
        config.setModelPath(MODAL_PATH + "IIC_Fl.onnx");
        //人脸活体阈值,可选,默认0.8，超过阈值则认为是真人，低于阈值是非活体
        config.setRealityThreshold(LivenessConstant.DEFAULT_REALITY_THRESHOLD);
        /*视频检测帧数，可选，默认10，输出帧数超过这个number之后，就可以输出识别结果。
        这个数量相当于多帧识别结果融合的融合的帧数。当输入的帧数超过设定帧数的时候，会采用滑动窗口的方式，返回融合的最近输入的帧融合的识别结果。
        一般来说，在10以内，帧数越多，结果越稳定，相对性能越好，但是得到结果的延时越高。*/
        config.setFrameCount(LivenessConstant.DEFAULT_FRAME_COUNT);
        //视频最大检测帧数
        config.setMaxVideoDetectFrames(LivenessConstant.DEFAULT_MAX_VIDEO_DETECT_FRAMES);
        //指定人脸检测模型
        config.setDetectModel(getProFaceDetModel());
        return LivenessModelFactory.getInstance().getModel(config);
    }

    private static LivenessDetModel staticLivenessDetModel = null;

    /**
     * 獲取活體檢測模型（靜態緩存，供 API 使用）
     */
    public static LivenessDetModel getStaticLivenessDetModel() {
        if (staticLivenessDetModel != null) {
            return staticLivenessDetModel;
        }
        LivenessConfig config = new LivenessConfig();
        config.setModelEnum(LivenessModelEnum.IIC_FL_MODEL);
        config.setDevice(device);
        config.setModelPath(MODAL_PATH + "IIC_Fl.onnx");
        config.setRealityThreshold(LivenessConstant.DEFAULT_REALITY_THRESHOLD);
        config.setFrameCount(LivenessConstant.DEFAULT_FRAME_COUNT);
        config.setMaxVideoDetectFrames(LivenessConstant.DEFAULT_MAX_VIDEO_DETECT_FRAMES);
        config.setDetectModel(getProFaceDetModel());
        staticLivenessDetModel = LivenessModelFactory.getInstance().getModel(config);
        return staticLivenessDetModel;
    }

    private static ExpressionModel staticExpressionModel = null;

    /**
     * 获取表情识别模型（静态缓存，供 API 使用）
     */
    public static ExpressionModel getStaticExpressionModel() {
        if (staticExpressionModel != null) {
            return staticExpressionModel;
        }
        FaceExpressionConfig config = new FaceExpressionConfig();
        config.setModelEnum(ExpressionModelEnum.FrEmotion);
        config.setModelPath(MODAL_PATH + "fr_expression.onnx");
        config.setDevice(device);
        config.setAlign(true);
        config.setDetectModel(getProFaceDetModel());
        staticExpressionModel = ExpressionModelFactory.getInstance().getModel(config);
        return staticExpressionModel;
    }

    /**
     * 單張圖片活體檢測（供 API 調用）
     * @param image 人臉圖片
     * @return 活體檢測結果，若未檢測到人臉或失敗則 faceDetected 為 false
     */
    public static LivenessDetectResult livenessDetect(BufferedImage image) {
        if (image == null) {
            return new LivenessDetectResult(false, 0f, "圖片為空", false);
        }
        try {
            LivenessDetModel model = getStaticLivenessDetModel();
            SmartImageFactory factory = SmartImageFactory.getInstance();
            Mat mat = bufferedImageToMat(image);
            if (mat == null || mat.empty()) {
                return new LivenessDetectResult(false, 0f, "圖片轉換失敗", false);
            }
            Image img = factory.fromMat(mat);
            R<DetectionResponse> result = model.detect(img);
            if (!result.isSuccess()) {
                log.debug("活體檢測失敗：{}", result.getMessage());
                return new LivenessDetectResult(false, 0f, result.getMessage() != null ? result.getMessage() : "檢測失敗", false);
            }
            var list = result.getData().getDetectionInfoList();
            if (list == null || list.isEmpty()) {
                return new LivenessDetectResult(false, 0f, "未檢測到人臉", false);
            }
            // 取第一個人臉的活體結果
            var info = list.get(0);
            var liveness = info.getFaceInfo().getLivenessStatus();
            boolean live = liveness.getStatus() == LivenessStatus.LIVE;
            float score = liveness.getScore();
            String desc = liveness.getStatus().getDescription();
            return new LivenessDetectResult(live, score, desc, true);
        } catch (Exception e) {
            log.error("活體檢測異常", e);
            return new LivenessDetectResult(false, 0f, "檢測異常: " + e.getMessage(), false);
        }
    }

    /**
     * 单张图片表情检测（供 API 调用）
     * @param image 人脸图片
     * @return 表情检测结果，若未检测到人脸或失败则 faceDetected 为 false
     */
    public static ExpressionDetectResult expressionDetect(BufferedImage image) {
        if (image == null) {
            return new ExpressionDetectResult(false, null, "图片为空", 0f);
        }
        try {
            ExpressionModel model = getStaticExpressionModel();
            Mat mat = bufferedImageToMat(image);
            if (mat == null || mat.empty()) {
                return new ExpressionDetectResult(false, null, "图片转换失败", 0f);
            }
            Image img = SmartImageFactory.getInstance().fromMat(mat);
            R<ExpressionResult> result = model.detectTopFace(img);
            if (!result.isSuccess()) {
                log.debug("表情检测失败：{}", result.getMessage());
                return new ExpressionDetectResult(false, null, result.getMessage(), 0f);
            }
            ExpressionResult expressionResult = result.getData();
            if (expressionResult == null || expressionResult.getExpression() == null) {
                return new ExpressionDetectResult(false, null, "未检测到人脸", 0f);
            }
            return new ExpressionDetectResult(
                    true,
                    expressionResult.getExpression().getLabel(),
                    expressionResult.getExpression().getDescription(),
                    expressionResult.getScore()
            );
        } catch (Exception e) {
            log.error("表情检测异常", e);
            return new ExpressionDetectResult(false, null, "检测异常: " + e.getMessage(), 0f);
        }
    }

    /**
     * BufferedImage 轉 Mat（用於活體檢測）
     */
    private static Mat bufferedImageToMat(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        baos.flush();
        MatOfByte mob = new MatOfByte(baos.toByteArray());
        return Imgcodecs.imdecode(mob, Imgcodecs.IMREAD_COLOR);
    }

    //=================================//=================================//=================================//=================================
    
    // 人脸信息缓存文件路径
    private static final String FACE_INDEX_FILE = MODAL_PATH + "face_index.json";
    
    /**
     * 保存人脸信息到索引文件
     */
    private static void saveFaceToIndex(String faceId, String userId, String role) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Map<String, String>> faceIndex = loadFaceIndex();
            
            Map<String, String> faceInfo = new HashMap<>();
            faceInfo.put("userid", userId);
            faceInfo.put("role", role);
            faceInfo.put("registerTime", String.valueOf(System.currentTimeMillis()));
            
            faceIndex.put(faceId, faceInfo);
            
            // 保存到文件
            mapper.writerWithDefaultPrettyPrinter().writeValue(new java.io.File(FACE_INDEX_FILE), faceIndex);
            log.info("人脸信息已保存到索引文件: faceId={}", faceId);
        } catch (Exception e) {
            log.error("保存人脸索引失败", e);
        }
    }
    
    /**
     * 从索引文件加载所有人脸信息
     */
    private static Map<String, Map<String, String>> loadFaceIndex() {
        try {
            java.io.File indexFile = new java.io.File(FACE_INDEX_FILE);
            if (indexFile.exists()) {
                ObjectMapper mapper = new ObjectMapper();
                return mapper.readValue(indexFile, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Map<String, String>>>() {});
            }
        } catch (Exception e) {
            log.warn("加载人脸索引文件失败，将创建新文件", e);
        }
        return new HashMap<>();
    }
    
    /**
     * 从索引文件删除人脸信息
     */
    private static void removeFaceFromIndex(String faceId) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Map<String, String>> faceIndex = loadFaceIndex();
            faceIndex.remove(faceId);
            mapper.writerWithDefaultPrettyPrinter().writeValue(new java.io.File(FACE_INDEX_FILE), faceIndex);
            log.info("人脸信息已从索引文件删除: faceId={}", faceId);
        } catch (Exception e) {
            log.error("删除人脸索引失败", e);
        }
    }

    /**
     * 获取所有已注册的人脸ID列表
     * @return 人脸ID列表
     */
    public static List<String> getAllFaceIds(){
        try {
            Map<String, Map<String, String>> faceIndex = loadFaceIndex();
            List<String> faceIds = new ArrayList<>(faceIndex.keySet());
            log.info("当前人脸库共有 {} 个人脸", faceIds.size());
            return faceIds;
        }
        catch (Exception e){
            log.error("获取人脸ID列表失败", e);
            e.printStackTrace();
        }
        return new java.util.ArrayList<>();
    }
    
    /**
     * 根据人脸ID获取人脸信息（包括元数据）
     * @param faceId 人脸ID
     * @return 元数据JSON字符串
     */
    public static String getFaceMetadataById(String faceId){
        try {
            Map<String, Map<String, String>> faceIndex = loadFaceIndex();
            Map<String, String> faceInfo = faceIndex.get(faceId);
            
            if (faceInfo != null) {
                ObjectMapper mapper = new ObjectMapper();
                String metadata = mapper.writeValueAsString(faceInfo);
                log.info("人脸ID: {}, 元数据: {}", faceId, metadata);
                return metadata;
            } else {
                log.warn("未找到人脸信息: faceId={}", faceId);
                return null;
            }
        }
        catch (Exception e){
            log.error("查询人脸信息失败: {}", faceId, e);
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 人脸比对1：1（基于图像直接比对）
     * 流程：从输入图像中裁剪分数最高的人脸 → 提取其人脸特征 → 比对两张图片中提取的人脸特征。（接口内自动完成）
     * 注意事项：
     * 1、首次调用接口，可能会较慢。只要不关闭程序，后续调用会明显加快。若每次重启程序，则每次首次调用都将重新加载，仍会较慢。
     * 2、若人脸朝向不正，可开启人脸对齐以提升特征提取准确度。（方法参考自定义配置人脸特征提取）
     * @throws Exception
     */
    public static void featureComparison(){
        try {
            System.out.println(new Date().getSeconds());
            //getHighAccuracyFaceRecModel高精度模型，速度慢, 追求速度请更换高速模型: getHighSpeedFaceRecModel
            FaceRecModel faceRecModel = getFaceRecModelWithSQLiteConfig();
            //基于图像直接比对人脸特征
            R<Float> similarResult = faceRecModel.featureComparison("src/main/resources/iu_1.jpg","src/main/resources/iu_2.jpg");
            if(similarResult.isSuccess()){
                //相似度阈值不同模型不同，具体参看文档
                log.info("人脸比对相似度：{}",  objectMapper.writeValueAsString(similarResult.getData()));
            }else{
                log.info("人脸比对失败：{}", similarResult.getMessage());
            }
            System.out.println(new Date().getSeconds());
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
    /**
     * 人脸比对1：1（基于特征值比对）
     * 流程：从输入图像中裁剪分数最高的人脸 → 提取其人脸特征 → 比对两张图片中提取的人脸特征。
     * 注意事项：
     * 1、首次调用接口，可能会较慢。只要不关闭程序，后续调用会明显加快。若每次重启程序，则每次首次调用都将重新加载，仍会较慢。
     * 2、若人脸朝向不正，可开启人脸对齐以提升特征提取准确度。（方法参考自定义配置人脸特征提取）
     * @throws Exception
     */
    public static void featureComparison2(){
        try {
            //高精度模型，速度慢, 追求速度请更换高速模型: getHighSpeedFaceRecModel
            FaceRecModel faceRecModel = getFaceRecModelWithSQLiteConfig();
            //特征提取（提取分数最高人脸特征）,适用于单人脸场景
            R<float[]> featureResult1 = faceRecModel.extractTopFaceFeature("src/main/resources/iu_1.jpg");
            if(featureResult1.isSuccess()){
                log.info("图片1人脸特征提取成功：{}", objectMapper.writeValueAsString(featureResult1.getData()));
            }else{
                log.info("图片1人脸特征提取失败：{}", featureResult1.getMessage());
                return;
            }
            //特征提取（提取分数最高人脸特征）,适用于单人脸场景
            R<float[]> featureResult2 = faceRecModel.extractTopFaceFeature("src/main/resources/iu_2.jpg");
            if(featureResult2.isSuccess()){
                log.info("图片2人脸特征提取成功：{}",objectMapper.writeValueAsString(featureResult2.getData()));
            }else{
                log.info("图片2人脸特征提取失败：{}", featureResult2.getMessage());
                return;
            }
            //计算相似度
            float similar = faceRecModel.calculSimilar(featureResult1.getData(), featureResult2.getData());
            log.info("相似度：{}", similar);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }


    /**
     * 摄像头活体检测（接收外部传入的摄像头/视频源）
     * @param capture 已打开的摄像头或视频源，调用方负责创建与打开（如 new VideoCapture(0)）
     *                注意：方法结束后会调用 capture.release() 释放资源
     * 注意事项：如果视频比较卡，可以使用轻量的人脸检测模型
     */
    public void livenessDetectCamera(VideoCapture capture){
        if (capture == null || !capture.isOpened()) {
            System.out.println("No camera or video source: capture is null or not opened");
            return;
        }
        try {
            LivenessDetModel livenessDetModel = getLivenessDetModel();
            OpenCV.loadShared();

            double ratio =
                    capture.get(Videoio.CAP_PROP_FRAME_WIDTH)
                            / capture.get(Videoio.CAP_PROP_FRAME_HEIGHT);
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            int height = (int) (screenSize.height * 0.65f);
            int width = (int) (height * ratio);
            if (width > screenSize.width) {
                width = screenSize.width;
            }

            Mat image = new Mat();
            boolean captured = false;
            for (int i = 0; i < 10; ++i) {
                captured = capture.read(image);
                if (captured) {
                    break;
                }

                try {
                    Thread.sleep(50);
                } catch (InterruptedException ignore) {
                    // ignore
                }
            }
            if (!captured) {
                JOptionPane.showConfirmDialog(null, "Failed to capture image from WebCam.");
            }
            ViewerFrame frame = new ViewerFrame(width, height);
            SmartImageFactory factory = SmartImageFactory.getInstance();
            Size size = new Size(width, height);

            while (capture.isOpened()) {
                if (!capture.read(image)) {
                    break;
                }
                Mat resizeImage = new Mat();
                Imgproc.resize(image, resizeImage, size);
                Image img = factory.fromMat(resizeImage);
                R<DetectionResponse> detectedResult = livenessDetModel.detect(img);
                if(!detectedResult.isSuccess()){
                    log.debug("识别失败：{}", detectedResult.getMessage());
                    continue;
                }
                for(DetectionInfo detectionInfo : detectedResult.getData().getDetectionInfoList()){
                    DetectionRectangle detectionRectangle = detectionInfo.getDetectionRectangle();
                    Color color = detectionInfo.getFaceInfo().getLivenessStatus().getStatus() == LivenessStatus.LIVE ? Color.GREEN : Color.RED;
                    String text = detectionInfo.getFaceInfo().getLivenessStatus().getStatus().getDescription() + ":" + detectionInfo.getFaceInfo().getLivenessStatus().getScore();
                    ImageUtils.drawRectAndText(img, detectionRectangle, text);
                }
                frame.showImage(ImageUtils.toBufferedImage(img));
            }
            capture.release();
            System.exit(0);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}