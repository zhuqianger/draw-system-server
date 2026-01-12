package org.example.drawsystemserver.controller;

import org.example.drawsystemserver.dto.ResponseDTO;
import org.example.drawsystemserver.dto.TeamDTO;
import org.example.drawsystemserver.entity.Team;
import org.example.drawsystemserver.mapper.PlayerMapper;
import org.example.drawsystemserver.mapper.UserMapper;
import org.example.drawsystemserver.service.TeamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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
