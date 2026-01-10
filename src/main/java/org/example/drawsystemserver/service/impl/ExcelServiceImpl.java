package org.example.drawsystemserver.service.impl;

import org.apache.poi.ss.usermodel.*;
import org.example.drawsystemserver.entity.Player;
import org.example.drawsystemserver.service.ExcelService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Service
public class ExcelServiceImpl implements ExcelService {

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    @Override
    public List<Player> parsePlayersFromExcel(MultipartFile file, Long sessionId) throws Exception {
        List<Player> players = new ArrayList<>();
        
        Workbook workbook = WorkbookFactory.create(file.getInputStream());
        Sheet sheet = workbook.getSheetAt(0); // 读取第一个工作表
        
        // 跳过表头，从第二行开始读取
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;
            
            Player player = new Player();
            player.setSessionId(sessionId);
            
            // 读取各列数据（根据实际Excel格式调整）
            // 假设列顺序：序号、群内名字、游戏ID、位置、英雄、段位、费用
            int colIndex = 0;
            
            // 序号
            Cell idCell = row.getCell(colIndex++);
            if (idCell != null) {
                if (idCell.getCellType() == CellType.NUMERIC) {
                    player.setGroupId((int) idCell.getNumericCellValue());
                } else if (idCell.getCellType() == CellType.STRING) {
                    try {
                        player.setGroupId(Integer.parseInt(idCell.getStringCellValue()));
                    } catch (NumberFormatException e) {
                        continue; // 跳过无效行
                    }
                }
            } else {
                continue; // 序号为空则跳过
            }
            
            // 群内名字
            Cell groupNameCell = row.getCell(colIndex++);
            if (groupNameCell != null) {
                player.setGroupName(getCellValueAsString(groupNameCell));
            }
            
            // 游戏ID
            Cell gameIdCell = row.getCell(colIndex++);
            if (gameIdCell != null) {
                player.setGameId(getCellValueAsString(gameIdCell));
            }
            
            // 擅长位置
            Cell positionCell = row.getCell(colIndex++);
            if (positionCell != null) {
                player.setPosition(getCellValueAsString(positionCell));
            }
            
            // 擅长英雄
            Cell heroesCell = row.getCell(colIndex++);
            if (heroesCell != null) {
                player.setHeroes(getCellValueAsString(heroesCell));
            }
            
            // 段位
            Cell rankCell = row.getCell(colIndex++);
            if (rankCell != null) {
                player.setRank(getCellValueAsString(rankCell));
            }
            
            // 费用
            Cell costCell = row.getCell(colIndex++);
            if (costCell != null) {
                if (costCell.getCellType() == CellType.NUMERIC) {
                    player.setCost(BigDecimal.valueOf(costCell.getNumericCellValue()));
                } else if (costCell.getCellType() == CellType.STRING) {
                    try {
                        player.setCost(new BigDecimal(costCell.getStringCellValue()));
                    } catch (NumberFormatException e) {
                        // 费用为空则设置为0
                        player.setCost(BigDecimal.ZERO);
                    }
                }
            } else {
                player.setCost(BigDecimal.ZERO);
            }
            
            player.setStatus("POOL");
            players.add(player);
        }
        
        workbook.close();
        return players;
    }

    @Override
    public Map<Integer, String> parseCaptainsFromExcel(MultipartFile file, List<Integer> captainIndices) throws Exception {
        Map<Integer, String> captains = new HashMap<>();
        
        Workbook workbook = WorkbookFactory.create(file.getInputStream());
        Sheet sheet = workbook.getSheetAt(0);
        
        // 假设队长信息在第一行（表头行）或者单独的工作表
        // 这里假设队长信息在单独的列或者第一个工作表的特定行
        // 根据实际需求调整：从表头行读取队长信息
        Row headerRow = sheet.getRow(0);
        if (headerRow != null) {
            for (Integer index : captainIndices) {
                if (index > 0 && index <= headerRow.getLastCellNum()) {
                    Cell cell = headerRow.getCell(index - 1); // Excel索引从1开始，Java从0开始
                    if (cell != null) {
                        String captainName = getCellValueAsString(cell);
                        captains.put(index, captainName);
                    }
                }
            }
        }
        
        // 如果队长信息不在表头，可以尝试从第一列读取
        // 这里简化处理，实际应根据Excel格式调整
        workbook.close();
        return captains;
    }

    @Override
    public String saveFile(MultipartFile file) throws Exception {
        // 创建上传目录
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        
        // 生成唯一文件名
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null && originalFilename.contains(".") 
            ? originalFilename.substring(originalFilename.lastIndexOf(".")) 
            : ".xlsx";
        String filename = System.currentTimeMillis() + "_" + UUID.randomUUID().toString() + extension;
        
        // 保存文件
        Path filePath = uploadPath.resolve(filename);
        try (FileOutputStream fos = new FileOutputStream(filePath.toFile())) {
            fos.write(file.getBytes());
        }
        
        return filePath.toString();
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    // 避免科学计数法
                    double numericValue = cell.getNumericCellValue();
                    if (numericValue == (long) numericValue) {
                        return String.valueOf((long) numericValue);
                    } else {
                        return String.valueOf(numericValue);
                    }
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }
}
