package org.example.drawsystemserver.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.example.drawsystemserver.dto.PlayerDTO;
import org.example.drawsystemserver.dto.ResponseDTO;
import org.example.drawsystemserver.entity.Player;
import org.example.drawsystemserver.service.PlayerService;
import org.example.drawsystemserver.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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
}
