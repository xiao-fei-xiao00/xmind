package com.example.xmind.Controller;

import com.example.xmind.Service.MindMapService;
import org.codehaus.plexus.util.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.springframework.web.servlet.function.RequestPredicates.contentType;

@RestController
@RequestMapping("/mindmap")
public class MindMapController {
    // 从配置文件中读取上传文件的存储目录
    @Value("${upload.directory}")
    private String uploadDirectory;

    private final MindMapService mindMapService;

    @Autowired
    public MindMapController(MindMapService mindMapService) {
        this.mindMapService = mindMapService;
    }

    //    @GetMapping("/download")
//    public ResponseEntity<InputStreamResource> downloadMindMap(@RequestParam("file") MultipartFile file, @RequestParam("userId") String userId) {
//        try {
//            if (userId == null) {
//                return ResponseEntity.badRequest().body(null);
//            }
//            // 保存上传的Excel文件到临时目录
//            String excelPath = saveUploadedFile(file, userId);
//            String originalFileName = file.getOriginalFilename(); // 获取原始文件名
//            // 生成唯一的输出脑图文件名
//            String uniqueFileName = generateUniqueFileName(userId);
//            String outputPath = uploadDirectory + File.separator + userId + File.separator + uniqueFileName; // 指定输出脑图文件路径
//            mindMapService.generateMindMap(excelPath, outputPath,originalFileName);
//            // 读取生成的脑图文件内容为字节数组
//            File outputFile = new File(outputPath);
//            // 创建文件输入流
//            InputStreamResource inputStreamResource = new InputStreamResource(new FileInputStream(outputFile));
//            // 设置HTTP头信息
//            HttpHeaders headers = new HttpHeaders();
//            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + uniqueFileName);
//            // 返回响应实体
//            return ResponseEntity.ok()
//                    .headers(headers)
//                    .contentLength(outputFile.length())
//                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
//                    .body(inputStreamResource);
//        } catch (Exception e) {
//            // 处理异常情况
//            e.printStackTrace();
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
//        }
//    }
//
//    private String generateUniqueFileName(String userId) {
//        // 使用当前时间戳作为文件名
//        return userId + ".xmind";
//    }
//
//    private String saveUploadedFile(MultipartFile file, String userId) throws IOException {
//        String[] split = file.getOriginalFilename().split("\\.");
//
//        String ext = split[split.length - 1];
//
//        String filePath = uploadDirectory + File.separator + userId + File.separator + userId + "." + ext;
//
//        File directory = new File(uploadDirectory + File.separator + userId);
//        if (!directory.exists()) {
//            directory.mkdirs();
//        }
//
//        if (!file.isEmpty()) {
//            try (FileOutputStream fos = new FileOutputStream(filePath)) {
//                fos.write(file.getBytes());
//            }
//        }
//
//        return filePath;
//    }
    @GetMapping("/download")
    public ResponseEntity<Resource> downloadMindMap(@RequestParam("files") MultipartFile[] files, @RequestParam("userId") String userId) {
        try {
            if (userId == null) {
                return ResponseEntity.badRequest().body(null);
            }

            List<File> xmindFiles = new ArrayList<>();

            for (int i = 0; i < files.length; i++) {
                MultipartFile file = files[i];
                String originalFileName = file.getOriginalFilename(); // 获取原始文件名
                String excelPath = saveUploadedFile(file, userId, i + 1);
                String uniqueFileName = generateUniqueFileName(userId, i + 1);
                String outputPath = uploadDirectory + File.separator + userId + File.separator + uniqueFileName; // 指定输出脑图文件路径

                mindMapService.generateMindMap(excelPath, outputPath, originalFileName);

                File xmindFile = new File(outputPath);
                xmindFiles.add(xmindFile);
            }

            // 创建临时目录来存储所有的 XMind 文件
            File tempDir = Files.createTempDirectory("xmind-temp").toFile();

            // 将所有的 XMind 文件复制到临时目录中
            for (File xmindFile : xmindFiles) {
                File destFile = new File(tempDir, xmindFile.getName());
                FileUtils.copyFile(xmindFile, destFile);
            }

            // 创建一个 zip 文件并将临时目录中的所有文件添加到 zip 中
            String zipFileName = "mindmaps.zip";
            String zipFilePath = uploadDirectory + File.separator + userId + File.separator + zipFileName;
            createZip(tempDir, zipFilePath);

            // 将 zip 文件转换为输入流
            File zipFile = new File(zipFilePath);
            InputStreamResource inputStreamResource = new InputStreamResource(new FileInputStream(zipFile));

            // 设置HTTP头信息
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + zipFileName);

            // 返回响应实体
            return ResponseEntity.ok()
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(inputStreamResource);
        } catch (Exception e) {
            // 处理异常情况
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    private String generateUniqueFileName(String userId, int fileIndex) {
        // 使用当前时间戳和文件索引作为文件名
        return userId + "_" + fileIndex + ".xmind";
    }

    private String saveUploadedFile(MultipartFile file, String userId, int fileIndex) throws IOException {
        String[] split = file.getOriginalFilename().split("\\.");

        String ext = split[split.length - 1];

        String fileName = "mindmap_" + fileIndex + "." + ext;
        String filePath = uploadDirectory + File.separator + userId + File.separator + fileName;

        File directory = new File(uploadDirectory + File.separator + userId);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        if (!file.isEmpty()) {
            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                fos.write(file.getBytes());
            }
        }

        return filePath;
    }

    private void createZip(File sourceDir, String zipFilePath) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(zipFilePath);
             ZipOutputStream zos = new ZipOutputStream(fos)) {
            addDirToZip(sourceDir, sourceDir.getName(), zos);
        }
    }

    private void addDirToZip(File dir, String baseName, ZipOutputStream zos) throws IOException {
        File[] files = dir.listFiles();
        if (files != null) {
            byte[] buffer = new byte[1024];
            for (File file : files) {
                if (file.isDirectory()) {
                    addDirToZip(file, baseName + "/" + file.getName(), zos);
                } else {
                    FileInputStream fis = new FileInputStream(file);
                    zos.putNextEntry(new ZipEntry(baseName + "/" + file.getName()));
                    int length;
                    while ((length = fis.read(buffer)) > 0) {
                        zos.write(buffer, 0, length);
                    }
                    zos.closeEntry();
                    fis.close();
                }
            }
        }
    }



}



