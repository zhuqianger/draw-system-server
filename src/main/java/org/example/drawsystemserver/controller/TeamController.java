package org.example.drawsystemserver.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.drawsystemserver.dto.ResponseDTO;
import org.example.drawsystemserver.dto.TeamDTO;
import org.example.drawsystemserver.entity.*;
import org.example.drawsystemserver.mapper.*;
import org.example.drawsystemserver.service.TeamService;
import org.example.drawsystemserver.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/team")
@CrossOrigin
public class TeamController {

    @Autowired
    private TeamService teamService;

    @Autowired
    private PlayerMapper playerMapper;

    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private AuctionMapper auctionMapper;
    
    @Autowired
    private BidMapper bidMapper;
    
    @Autowired
    private UserService userService;

    /**
     * 获取所有队伍
     */
    @GetMapping("/all")
    public ResponseDTO<List<TeamDTO>> getAllTeams() {
        List<Team> teams = teamService.getAllTeams();
        List<TeamDTO> dtos = teams.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return ResponseDTO.success(dtos);
    }

    /**
     * 获取指定拍卖流程的队伍列表
     */
    @GetMapping("/session/{sessionId}")
    public ResponseDTO<List<TeamDTO>> getTeamsBySession(@PathVariable Long sessionId) {
        List<Team> teams = teamService.getTeamsBySession(sessionId);
        List<TeamDTO> dtos = teams.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return ResponseDTO.success(dtos);
    }

    /**
     * 根据队长ID获取队伍
     */
    @GetMapping("/captain/{captainId}")
    public ResponseDTO<TeamDTO> getTeamByCaptain(@PathVariable Long captainId) {
        Team team = teamService.getByCaptainId(captainId);
        if (team == null) {
            return ResponseDTO.error("队伍不存在");
        }
        return ResponseDTO.success(convertToDTO(team));
    }

    /**
     * 根据队伍ID获取队伍信息
     */
    @GetMapping("/{teamId}")
    public ResponseDTO<TeamDTO> getTeam(@PathVariable Long teamId) {
        Team team = teamService.getById(teamId);
        if (team == null) {
            return ResponseDTO.error("队伍不存在");
        }
        return ResponseDTO.success(convertToDTO(team));
    }
    
    /**
     * 导出指定拍卖流程的队伍信息为Excel
     */
    @GetMapping("/export/{sessionId}")
    public void exportTeams(@PathVariable Long sessionId, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Long userId = (Long) request.getAttribute("userId");
        if (!userService.isAdmin(userId)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        
        // 获取所有队伍
        List<Team> teams = teamService.getTeamsBySession(sessionId);
        
        // 获取所有拍卖记录，用于查找队员的拍卖费用
        List<Auction> auctions = auctionMapper.selectBySessionId(sessionId);
        Map<Long, BigDecimal> playerBidAmountMap = auctions.stream()
            .filter(a -> a.getWinningBidId() != null)
            .collect(Collectors.toMap(
                Auction::getPlayerId,
                a -> {
                    Bid bid = bidMapper.selectById(a.getWinningBidId());
                    return bid != null ? bid.getAmount() : BigDecimal.ZERO;
                },
                (v1, v2) -> v1
            ));
        
        // 创建Excel工作簿
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("队伍信息");
        
        // 创建表头样式
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints((short) 12);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);
        
        // 创建数据样式
        CellStyle dataStyle = workbook.createCellStyle();
        dataStyle.setBorderBottom(BorderStyle.THIN);
        dataStyle.setBorderTop(BorderStyle.THIN);
        dataStyle.setBorderLeft(BorderStyle.THIN);
        dataStyle.setBorderRight(BorderStyle.THIN);
        dataStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        
        // 创建表头 - 每个队伍一行，包含所有队员信息
        // 格式：队伍名称、队长名字、队长费用、队员1名字、队员1费用、队员1拍卖费用、队员2名字、队员2费用、队员2拍卖费用、队员3名字、队员3费用、队员3拍卖费用、队伍总费用、剩余费用
        Row headerRow = sheet.createRow(0);
        String[] headers = {
            "队伍名称", 
            "队长名字", 
            "队长费用", 
            "队员1名字", "队员1费用", "队员1拍卖费用",
            "队员2名字", "队员2费用", "队员2拍卖费用",
            "队员3名字", "队员3费用", "队员3拍卖费用",
            "队伍总费用", 
            "剩余费用"
        };
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        
        // 填充数据 - 每个队伍一行
        int rowNum = 1;
        for (Team team : teams) {
            Row row = sheet.createRow(rowNum++);
            int colIndex = 0;
            
            // 队伍名称
            createCell(row, colIndex++, team.getTeamName(), dataStyle);
            
            // 获取队长信息
            Player captain = playerMapper.selectById(team.getCaptainId());
            String captainName = captain != null ? captain.getGroupName() : team.getCaptainName();
            BigDecimal captainCost = captain != null && captain.getCost() != null ? captain.getCost() : BigDecimal.ZERO;
            
            // 队长名字
            createCell(row, colIndex++, captainName != null ? captainName : "-", dataStyle);
            // 队长费用
            createCell(row, colIndex++, captainCost, dataStyle);
            
            // 获取所有队员（包括队长）
            List<Player> allPlayers = playerMapper.selectByTeamId(team.getId());
            
            // 在代码中过滤掉队长，确保逻辑正确
            Long captainId = team.getCaptainId();
            List<Player> nonCaptainPlayers = allPlayers.stream()
                .filter(p -> captainId == null || !p.getId().equals(captainId))
                .collect(Collectors.toList());
            
            // 调试输出（可以后续删除）
            System.out.println("Team ID: " + team.getId() + ", Team Name: " + team.getTeamName());
            System.out.println("Captain ID: " + captainId);
            System.out.println("All players count: " + allPlayers.size());
            System.out.println("Non-captain players count: " + nonCaptainPlayers.size());
            for (Player p : allPlayers) {
                System.out.println("  Player ID: " + p.getId() + ", Name: " + p.getGroupName() + ", TeamId: " + p.getTeamId());
            }
            
            // 最多3个非队长队员（因为队长已经单独列出）
            // 填充队员1、队员2、队员3的信息
            for (int i = 0; i < 3; i++) {
                if (i < nonCaptainPlayers.size()) {
                    Player player = nonCaptainPlayers.get(i);
                    // 队员名字
                    createCell(row, colIndex++, player.getGroupName() != null ? player.getGroupName() : "-", dataStyle);
                    // 队员费用
                    createCell(row, colIndex++, player.getCost() != null ? player.getCost() : BigDecimal.ZERO, dataStyle);
                    // 队员拍卖费用
                    BigDecimal bidAmount = playerBidAmountMap.getOrDefault(player.getId(), BigDecimal.ZERO);
                    createCell(row, colIndex++, bidAmount, dataStyle);
                } else {
                    // 如果没有更多队员，填充空值
                    createCell(row, colIndex++, "-", dataStyle);
                    createCell(row, colIndex++, BigDecimal.ZERO, dataStyle);
                    createCell(row, colIndex++, BigDecimal.ZERO, dataStyle);
                }
            }
            
            // 队伍总费用
            createCell(row, colIndex++, team.getTotalCost(), dataStyle);
            // 剩余费用
            createCell(row, colIndex++, team.getNowCost(), dataStyle);
        }
        
        // 自动调整列宽
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
            // 设置最小列宽
            sheet.setColumnWidth(i, Math.max(sheet.getColumnWidth(i), 3000));
        }
        
        // 设置响应头
        String filename = "队伍信息_" + System.currentTimeMillis() + ".xlsx";
        String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8.toString())
            .replaceAll("\\+", "%20");
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFilename);
        
        // 写入响应流
        workbook.write(response.getOutputStream());
        workbook.close();
    }
    
    private void createCell(Row row, int column, String value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value != null ? value : "");
        cell.setCellStyle(style);
    }
    
    private void createCell(Row row, int column, BigDecimal value, CellStyle style) {
        Cell cell = row.createCell(column);
        if (value != null) {
            cell.setCellValue(value.doubleValue());
        } else {
            cell.setCellValue(0.0);
        }
        cell.setCellStyle(style);
    }

    private TeamDTO convertToDTO(Team team) {
        TeamDTO dto = new TeamDTO();
        dto.setId(team.getId());
        dto.setCaptainId(team.getCaptainId());
        dto.setTeamName(team.getTeamName());
        dto.setPlayerCount(team.getPlayerCount());
        dto.setTotalCost(team.getTotalCost());
        dto.setNowCost(team.getNowCost());
        dto.setUserId(team.getUserId());

        // captainName从player表的groupName获取（captainId是player表的id）
        var captainPlayer = playerMapper.selectById(team.getCaptainId());
        if (captainPlayer != null) {
            dto.setCaptainName(captainPlayer.getGroupName());
        } else {
            // 如果找不到player，使用team中存储的captainName作为备选
            dto.setCaptainName(team.getCaptainName());
        }

        var players = playerMapper.selectByTeamId(team.getId());
        dto.setPlayers(players.stream()
                .map(p -> {
                    var playerDTO = new org.example.drawsystemserver.dto.PlayerDTO();
                    playerDTO.setId(p.getId());
                    playerDTO.setGroupName(p.getGroupName());
                    playerDTO.setPosition(p.getPosition());
                    playerDTO.setRank(p.getRank());
                    playerDTO.setStatus(p.getStatus());
                    return playerDTO;
                })
                .collect(Collectors.toList()));

        return dto;
    }
}
