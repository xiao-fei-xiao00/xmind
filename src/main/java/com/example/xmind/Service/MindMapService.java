package com.example.xmind.Service;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.xmind.core.*;
import org.xmind.core.ITopic;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
@Service
public class MindMapService {

    public byte[] generateMindMap(String excelPath, String outputPath, String originalFileName) {
        try {
            int extensionIndex = originalFileName.lastIndexOf('.');
            String fileNameWithoutExtension = originalFileName.substring(0, extensionIndex);
            // 创建思维导图的工作空间
            IWorkbookBuilder workbookBuilder = Core.getWorkbookBuilder();
            IWorkbook workbook = workbookBuilder.createWorkbook();

            // 获得默认sheet
            ISheet primarySheet = workbook.getPrimarySheet();

            // 获得根主题
            ITopic rootTopic = primarySheet.getRootTopic();
            rootTopic.setTitleText(fileNameWithoutExtension);

            // 读取Excel表格中的数据
            FileInputStream inputStream = new FileInputStream(excelPath);
            Workbook excelWorkbook = new XSSFWorkbook(inputStream);
            Sheet sheet = excelWorkbook.getSheetAt(0);
            Iterator<Row> rowIterator = sheet.iterator();

            Map<Integer, ITopic> levelToTopicMap = new HashMap<>();
            levelToTopicMap.put(0, rootTopic);

            int columnLimit = 8; // 忽略从第八列开始的列
            int currentLevel = 0;
            ITopic currentRootTopic = rootTopic;

            int rowCount = 0;
            for (Row row : sheet) {
                rowCount++;

                // 跳过第0行和第1行
                if (rowCount <= 1) {
                    continue;
                }

                for (Cell cell : row) {
                    int columnIndex = cell.getColumnIndex();

                    if (columnIndex >= columnLimit) {
                        continue; // 跳过超过限制的列
                    }

                    String title = getCellValue(cell);
                    if (title.isEmpty()) {
                        continue; // 跳过空单元格
                    }

                    currentLevel = columnIndex;

                    if (currentLevel == 0) {
                        // 创建新的根主题用于存放1级主题
                        currentRootTopic = workbook.createTopic();
                        currentRootTopic.setTitleText(title);
                        rootTopic.add(currentRootTopic);

                        levelToTopicMap.put(currentLevel, currentRootTopic);
                    } else {
                        if (currentLevel == 1) {
                            // 若当前级别为1，将1级主题添加到新的根主题下
                            ITopic currentTopic = workbook.createTopic();
                            currentTopic.setTitleText(title);
                            currentRootTopic.add(currentTopic);

                            levelToTopicMap.put(currentLevel, currentTopic);
                        } else {
                            // 其他级别的主题按原逻辑处理
                            ITopic parentTopic = levelToTopicMap.get(currentLevel - 1);
                            if (parentTopic != null) {
                                ITopic currentTopic = workbook.createTopic();
                                currentTopic.setTitleText(title);
                                parentTopic.add(currentTopic);

                                levelToTopicMap.put(currentLevel, currentTopic);
                            }
                        }
                    }

                    // 设置主题折叠状态为折叠
                    if (currentLevel > 0) {
                        ITopic currentTopic = levelToTopicMap.get(currentLevel);
                        if (currentTopic != null) {
                            currentTopic.setFolded(true);
                        }
                    }
                }
            }

            workbook.save(outputPath);
            excelWorkbook.close();
            inputStream.close();
            // 读取生成的脑图文件内容为字节数组
            File outputFile = new File(outputPath);
            byte[] xmindData = Files.readAllBytes(outputFile.toPath());
            return xmindData;
        } catch (IOException | CoreException e) {
            // 处理异常情况
            e.printStackTrace();
        }
        return new byte[0];
    }

    private String getCellValue(Cell cell) {
        if (cell == null) {
            return "";
        }
        if (cell.getCellType() == CellType.STRING) {
            return cell.getStringCellValue();
        } else if (cell.getCellType() == CellType.NUMERIC) {
            return String.valueOf(cell.getNumericCellValue());
        } else if (cell.getCellType() == CellType.BOOLEAN) {
            return String.valueOf(cell.getBooleanCellValue());
        } else {
            return "";
        }
    }
}
