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
        
        // 跳过表头，从第二行开始读取（第一行是字段描述）
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;
            
            Player player = new Player();
            player.setSessionId(sessionId);
            
            // Excel列索引映射（根据实际Excel格式，从0开始）
            // 0: 序号, 1: 提交时间, 2: 采用时间, 3: 来源, 4: 来源详情,
            // 5: 来源IP, 6: 游戏ID, 7: 群内名称, 8: 历史最高段位
            // 9: 当前段位, 10: 报名截图, 11: 常用位置, 12: 是否报名队长
            // 13: 报名队长理由, 14: 自我介绍 15:费用
            
            // colIndex 0: 序号 → groupId
            Cell idCell = row.getCell(0);
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
            
            // colIndex 6: 游戏ID → gameId
            Cell gameIdCell = row.getCell(6);
            if (gameIdCell != null) {
                player.setGameId(getCellValueAsString(gameIdCell));
            }
            
            // colIndex 7: 群内名称 → groupName
            Cell groupNameCell = row.getCell(7);
            if (groupNameCell != null) {
                player.setGroupName(getCellValueAsString(groupNameCell));
            }
            
            // colIndex 8: 历史最高段位 - 跳过（只需要当前段位）
            
            // colIndex 9: 当前段位 → rank
            Cell rankCell = row.getCell(9);
            if (rankCell != null) {
                player.setRank(getCellValueAsString(rankCell));
            }
            
            // colIndex 10: 报名截图 - 跳过
            
            // colIndex 11: 常用位置 → position
            Cell positionCell = row.getCell(11);
            if (positionCell != null) {
                String position = getCellValueAsString(positionCell);
                // Excel中用"|"分隔（如"上单 | 中单"），转换为逗号分隔
                player.setPosition(position.replace(" | ", ",").replace("|", ","));
            }
            
            // colIndex 12: 是否报名队长 - 跳过
            // colIndex 13: 报名队长理由 - 跳过
            
            // colIndex 14: 自我介绍 → heroes
            Cell heroesCell = row.getCell(14);
            if (heroesCell != null) {
                player.setHeroes(getCellValueAsString(heroesCell));
            }
            
            // colIndex 15: 费用 → cost
            Cell costCell = row.getCell(15);
            if (costCell != null) {
                try {
                    BigDecimal cost = getCellValueAsBigDecimal(costCell);
                    if (cost != null) {
                        player.setCost(cost);
                    } else {
                        // 如果解析失败，使用默认值3
                        player.setCost(new BigDecimal("3"));
                    }
                } catch (Exception e) {
                    // 解析失败，使用默认值3
                    player.setCost(new BigDecimal("3"));
                }
            } else {
                // 如果单元格为空，使用默认值3
                player.setCost(new BigDecimal("3"));
            }
            
            player.setStatus("POOL");
            
            players.add(player);
        }
        
        workbook.close();
        return players;
    }

    @Override
    public List<Player> parsePlayersFromFilePath(String filePath, Long sessionId) throws Exception {
        List<Player> players = new ArrayList<>();
        
        // 从文件路径读取Excel文件
        try (java.io.FileInputStream fis = new java.io.FileInputStream(filePath)) {
            Workbook workbook = WorkbookFactory.create(fis);
            Sheet sheet = workbook.getSheetAt(0); // 读取第一个工作表
            
            // 跳过表头，从第二行开始读取（第一行是字段描述）
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                
                Player player = new Player();
                player.setSessionId(sessionId);
                
                // Excel列索引映射（根据实际Excel格式，从0开始）
                // 0: 序号, 1: 提交时间, 2: 采用时间, 3: 来源, 4: 来源详情,
                // 5: 来源IP, 6: 游戏ID, 7: 群内名称, 8: 历史最高段位
                // 9: 当前段位, 10: 报名截图, 11: 常用位置, 12: 是否报名队长
                // 13: 报名队长理由, 14: 自我介绍, 15: 费用
                
                // colIndex 0: 序号 → groupId
                Cell idCell = row.getCell(0);
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
                
                // colIndex 6: 游戏ID → gameId
                Cell gameIdCell = row.getCell(6);
                if (gameIdCell != null) {
                    player.setGameId(getCellValueAsString(gameIdCell));
                }
                
                // colIndex 7: 群内名称 → groupName
                Cell groupNameCell = row.getCell(7);
                if (groupNameCell != null) {
                    player.setGroupName(getCellValueAsString(groupNameCell));
                }
                
                // colIndex 8: 历史最高段位 - 跳过（只需要当前段位）
                
                // colIndex 9: 当前段位 → rank
                Cell rankCell = row.getCell(9);
                if (rankCell != null) {
                    player.setRank(getCellValueAsString(rankCell));
                }
                
                // colIndex 10: 报名截图 - 跳过
                
                // colIndex 11: 常用位置 → position
                Cell positionCell = row.getCell(11);
                if (positionCell != null) {
                    String position = getCellValueAsString(positionCell);
                    // Excel中用"|"分隔（如"上单 | 中单"），转换为逗号分隔
                    player.setPosition(position.replace(" | ", ",").replace("|", ","));
                }
                
                // colIndex 12: 是否报名队长 - 跳过
                // colIndex 13: 报名队长理由 - 跳过
                
                // colIndex 14: 自我介绍 → heroes
                Cell heroesCell = row.getCell(14);
                if (heroesCell != null) {
                    player.setHeroes(getCellValueAsString(heroesCell));
                }
                
                // colIndex 15: 费用 → cost
                Cell costCell = row.getCell(15);
                if (costCell != null) {
                    try {
                        BigDecimal cost = getCellValueAsBigDecimal(costCell);
                        if (cost != null) {
                            player.setCost(cost);
                        } else {
                            // 如果解析失败，使用默认值3
                            player.setCost(new BigDecimal("3"));
                        }
                    } catch (Exception e) {
                        // 解析失败，使用默认值3
                        player.setCost(new BigDecimal("3"));
                    }
                } else {
                    // 如果单元格为空，使用默认值3
                    player.setCost(new BigDecimal("3"));
                }
                
                player.setStatus("POOL");
                
                players.add(player);
            }
            
            workbook.close();
        }
        
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
    public Map<Integer, String> parseCaptainsFromFilePath(String filePath, List<Integer> captainIndices) throws Exception {
        Map<Integer, String> captains = new HashMap<>();
        
        // 从文件路径读取Excel文件
        try (java.io.FileInputStream fis = new java.io.FileInputStream(filePath)) {
            Workbook workbook = WorkbookFactory.create(fis);
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
            
            workbook.close();
        }
        
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
        
        String value;
        switch (cell.getCellType()) {
            case STRING:
                value = cell.getStringCellValue().trim();
                break;
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    value = cell.getDateCellValue().toString();
                } else {
                    // 避免科学计数法
                    double numericValue = cell.getNumericCellValue();
                    if (numericValue == (long) numericValue) {
                        value = String.valueOf((long) numericValue);
                    } else {
                        value = String.valueOf(numericValue);
                    }
                }
                break;
            case BOOLEAN:
                value = String.valueOf(cell.getBooleanCellValue());
                break;
            case FORMULA:
                value = cell.getCellFormula();
                break;
            default:
                return "";
        }
        
        // 清理特殊字符（Unicode不可见字符）
        return cleanSpecialCharacters(value);
    }
    
    /**
     * 将单元格值转换为BigDecimal
     */
    private BigDecimal getCellValueAsBigDecimal(Cell cell) {
        if (cell == null) {
            return null;
        }
        
        try {
            switch (cell.getCellType()) {
                case NUMERIC:
                    // 如果是数字类型，直接转换为BigDecimal
                    double numericValue = cell.getNumericCellValue();
                    return BigDecimal.valueOf(numericValue);
                case STRING:
                    // 如果是字符串类型，尝试解析为数字
                    String strValue = cell.getStringCellValue().trim();
                    if (strValue.isEmpty()) {
                        return null;
                    }
                    // 移除可能的货币符号、逗号等
                    strValue = strValue.replaceAll("[￥$€£,，]", "").trim();
                    return new BigDecimal(strValue);
                case FORMULA:
                    // 如果是公式，尝试获取计算结果
                    try {
                        double formulaValue = cell.getNumericCellValue();
                        return BigDecimal.valueOf(formulaValue);
                    } catch (Exception e) {
                        // 如果公式结果是字符串，尝试解析
                        String formulaStr = cell.getStringCellValue().trim();
                        if (formulaStr.isEmpty()) {
                            return null;
                        }
                        formulaStr = formulaStr.replaceAll("[￥$€£,，]", "").trim();
                        return new BigDecimal(formulaStr);
                    }
                default:
                    return null;
            }
        } catch (Exception e) {
            // 解析失败返回null
            return null;
        }
    }
    
    /**
     * 清理字符串中的特殊字符（Unicode不可见字符等）
     */
    private String cleanSpecialCharacters(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        
        // 移除常见的Unicode不可见字符
        // \u2066 (LEFT-TO-RIGHT ISOLATE)
        // \u2067 (RIGHT-TO-LEFT ISOLATE)
        // \u2068 (FIRST STRONG ISOLATE)
        // \u2069 (POP DIRECTIONAL ISOLATE)
        // \u200B (ZERO WIDTH SPACE)
        // \u200C (ZERO WIDTH NON-JOINER)
        // \u200D (ZERO WIDTH JOINER)
        // \uFEFF (ZERO WIDTH NO-BREAK SPACE)
        return str.replaceAll("[\u2066\u2067\u2068\u2069\u200B\u200C\u200D\uFEFF]", "")
                  .trim();
    }
}
