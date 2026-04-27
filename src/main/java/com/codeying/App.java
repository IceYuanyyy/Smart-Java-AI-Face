package com.codeying;

import com.codeying.utils.FaceRecUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;

/**
 * 人脸识别管理系统 - 启动类
 * 
 * 快速开始：
 * 1. 下载模型文件（百度网盘：https://pan.baidu.com/s/10l22x5fRz_gwLr8EAHa1Jg?pwd=1234 提取码：1234）
 * 2. 修改 FaceRecUtils.java 中的 MODAL_PATH 配置
 * 3. 运行本类的 main 方法启动应用
 * 4. 浏览器访问 http://localhost:8080/face/manage
 * 
 * @author CodeYing
 * @version 1.0
 */
@SpringBootApplication
public class App {

    public static void main(String[] args) {
        
        // 打印启动提示
        printStartupBanner();
        
        // 检查模型文件是否存在
        checkModelFiles();

        // 设置PyTorch配置
        System.setProperty("PYTORCH_FLAVOR", "cpu");
        System.setProperty("PYTORCH_VERSION", "2.5.1");
        System.setProperty("PYTORCH_PRECXX11", "true");

        // 启动Spring Boot应用
        SpringApplication.run(App.class,args);
    }
    
    /**
     * 打印启动横幅
     */
    private static void printStartupBanner() {
        System.out.println("\n========================================");
        System.out.println("   人脸识别管理系统 v1.0");
        System.out.println("   Face Recognition Management System");
        System.out.println("========================================\n");
    }
    
    /**
     * 检查模型文件是否存在
     */
    private static void checkModelFiles() {
        String modelPath = FaceRecUtils.MODAL_PATH;
        File retinaface = new File(modelPath + "retinaface.pt");
        File elasticface = new File(modelPath + "elasticface.pt");
        File iicFl = new File(modelPath + "IIC_Fl.onnx");
        File frExpression = new File(modelPath + "fr_expression.onnx");
        
        boolean hasError = false;
        
        if (!retinaface.exists()) {
            System.err.println("❌ 错误：找不到模型文件 retinaface.pt");
            System.err.println("   路径：" + retinaface.getAbsolutePath());
            hasError = true;
        }
        
        if (!elasticface.exists()) {
            System.err.println("❌ 错误：找不到模型文件 elasticface.pt");
            System.err.println("   路径：" + elasticface.getAbsolutePath());
            hasError = true;
        }

        if (!iicFl.exists()) {
            System.err.println("❌ 错误：找不到模型文件 IIC_Fl.onnx（活体检测）");
            System.err.println("   路径：" + iicFl.getAbsolutePath());
            hasError = true;
        }

        if (!frExpression.exists()) {
            System.err.println("❌ 错误：找不到模型文件 fr_expression.onnx（表情检测）");
            System.err.println("   路径：" + frExpression.getAbsolutePath());
            hasError = true;
        }
        
        if (hasError) {
            System.err.println("\n⚠️  请按以下步骤操作：");
            System.err.println("1. 下载模型文件（包含人脸识别、活体检测、表情检测）");
            System.err.println("   百度网盘：https://pan.baidu.com/s/10l22x5fRz_gwLr8EAHa1Jg?pwd=1234");
            System.err.println("   提取码：1234");
            System.err.println("2. 将模型文件放到目录：" + new File(modelPath).getAbsolutePath());
            System.err.println("   需要文件：retinaface.pt、elasticface.pt、IIC_Fl.onnx、fr_expression.onnx");
            System.err.println("3. 或修改 FaceRecUtils.java 中的 MODAL_PATH 配置为你的模型目录");
            System.err.println("\n按任意键继续（可能会报错）或 Ctrl+C 退出...\n");
            
            try {
                System.in.read();
            } catch (Exception ignored) {
            }
        } else {
            System.out.println("✅ 模型文件检查通过");
            System.out.println("   - retinaface.pt: " + retinaface.getAbsolutePath());
            System.out.println("   - elasticface.pt: " + elasticface.getAbsolutePath());
            System.out.println("   - IIC_Fl.onnx: " + iicFl.getAbsolutePath());
            System.out.println("   - fr_expression.onnx: " + frExpression.getAbsolutePath());
            System.out.println();
        }
    }
}
