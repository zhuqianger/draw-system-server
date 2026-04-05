package org.example.drawsystemserver.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.example.drawsystemserver.dto.PlayerDTO;
import org.example.drawsystemserver.dto.ResponseDTO;
import org.example.drawsystemserver.entity.Player;
import org.example.drawsystemserver.service.AuctionService;
import org.example.drawsystemserver.service.PlayerService;
import org.example.drawsystemserver.service.UserService;
import org.example.drawsystemserver.service.WebSocketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/player")
@CrossOrigin
public class PlayerController {

    @Autowired
    private PlayerService playerService;

    @Autowired
    private UserService userService;

    /**
     * 获取待拍卖池中的队员
     */
    @GetMapping("/pool")
    public ResponseDTO<List<PlayerDTO>> getPoolPlayers(@RequestParam Long sessionId, HttpServletRequest request) {
        List<Player> players = playerService.getPoolPlayersBySession(sessionId);
        List<PlayerDTO> dtos = players.stream()
                .map(p -> {
                    PlayerDTO dto = new PlayerDTO();
                    dto.setId(p.getId());
                    dto.setGroupId(p.getGroupId());
                    dto.setPoolType(p.getPoolType());
                    dto.setFailedOrder(p.getFailedOrder());
                    dto.setGroupName(p.getGroupName());
                    dto.setGameId(p.getGameId());
                    dto.setPosition(p.getPosition());
                    dto.setHeroes(p.getHeroes());
                    dto.setRank(p.getRank());
                    dto.setCost(p.getCost());
                    dto.setStatus(p.getStatus());
                    return dto;
                })
                .collect(Collectors.toList());

        return ResponseDTO.success(dtos);
    }

    /**
     * 获取所有队员
     */
    @GetMapping("/all")
    public ResponseDTO<List<PlayerDTO>> getAllPlayers(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (!userService.isAdmin(userId)) {
            return ResponseDTO.error(403, "无权限");
        }

        List<Player> players = playerService.getAllPlayers();
        List<PlayerDTO> dtos = players.stream()
                .map(p -> {
                    PlayerDTO dto = new PlayerDTO();
                    dto.setId(p.getId());
                    dto.setGroupId(p.getGroupId());
                    dto.setPoolType(p.getPoolType());
                    dto.setFailedOrder(p.getFailedOrder());
                    dto.setGroupName(p.getGroupName());
                    dto.setGameId(p.getGameId());
                    dto.setPosition(p.getPosition());
                    dto.setHeroes(p.getHeroes());
                    dto.setRank(p.getRank());
                    dto.setCost(p.getCost());
                    dto.setStatus(p.getStatus());
                    return dto;
                })
                .collect(Collectors.toList());

        return ResponseDTO.success(dtos);
    }

    /**
     * 获取指定队伍的队员
     */
    @GetMapping("/team/{teamId}")
    public ResponseDTO<List<PlayerDTO>> getPlayersByTeam(@PathVariable Long teamId) {
        List<Player> players = playerService.getPlayersByTeamId(teamId);
        List<PlayerDTO> dtos = players.stream()
                .map(p -> {
                    PlayerDTO dto = new PlayerDTO();
                    dto.setId(p.getId());
                    dto.setGroupId(p.getGroupId());
                    dto.setPoolType(p.getPoolType());
                    dto.setFailedOrder(p.getFailedOrder());
                    dto.setGroupName(p.getGroupName());
                    dto.setGameId(p.getGameId());
                    dto.setPosition(p.getPosition());
                    dto.setHeroes(p.getHeroes());
                    dto.setRank(p.getRank());
                    dto.setCost(p.getCost());
                    dto.setStatus(p.getStatus());
                    return dto;
                })
                .collect(Collectors.toList());

        return ResponseDTO.success(dtos);
    }

    @Autowired
    private AuctionService auctionService;

    @Autowired
    private WebSocketService webSocketService;

    /**
     * 管理员：从待拍卖池直接将队员分配到指定队伍
     */
    @PostMapping("/assign")
    public ResponseDTO<String> assignPlayerToTeam(@RequestBody AssignPlayerRequest request, HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        if (!userService.isAdmin(userId)) {
            return ResponseDTO.error(403, "只有管理员可以直接分配队员");
        }

        if (request == null || request.getPlayerId() == null || request.getTeamId() == null || request.getAmount() == null) {
            return ResponseDTO.error("参数不完整");
        }

        try {
            auctionService.assignPlayerDirect(request.getPlayerId(), request.getTeamId(), request.getAmount());
            // 广播系统状态 & 队员分配
            webSocketService.broadcastPlayerAssigned(request.getPlayerId(), request.getTeamId());
            return ResponseDTO.success("分配成功");
        } catch (Exception e) {
            return ResponseDTO.error(e.getMessage());
        }
    }

    /**
     * 管理员：将待拍卖池队员在普通池与流拍池之间调整
     */
    @PostMapping("/pool/change")
    public ResponseDTO<String> changePlayerPool(@RequestBody ChangePlayerPoolRequest request, HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        if (!userService.isAdmin(userId)) {
            return ResponseDTO.error(403, "只有管理员可以调整队员池");
        }
        if (request == null || request.getSessionId() == null || request.getPlayerId() == null
                || request.getTargetPoolType() == null) {
            return ResponseDTO.error("参数不完整");
        }
        try {
            playerService.changePlayerPool(request.getSessionId(), request.getPlayerId(), request.getTargetPoolType());
            webSocketService.broadcastSystemStatus();
            return ResponseDTO.success("已更新队员所属池");
        } catch (Exception e) {
            return ResponseDTO.error(e.getMessage());
        }
    }

    public static class ChangePlayerPoolRequest {
        private Long sessionId;
        private Long playerId;
        private String targetPoolType;

        public Long getSessionId() {
            return sessionId;
        }

        public void setSessionId(Long sessionId) {
            this.sessionId = sessionId;
        }

        public Long getPlayerId() {
            return playerId;
        }

        public void setPlayerId(Long playerId) {
            this.playerId = playerId;
        }

        public String getTargetPoolType() {
            return targetPoolType;
        }

        public void setTargetPoolType(String targetPoolType) {
            this.targetPoolType = targetPoolType;
        }
    }

    /**
     * 直接分配队员时使用的请求体
     */
    public static class AssignPlayerRequest {
        private Long playerId;
        private Long teamId;
        private BigDecimal amount;

        public Long getPlayerId() {
            return playerId;
        }

        public void setPlayerId(Long playerId) {
            this.playerId = playerId;
        }

        public Long getTeamId() {
            return teamId;
        }

        public void setTeamId(Long teamId) {
            this.teamId = teamId;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }
    }
}
