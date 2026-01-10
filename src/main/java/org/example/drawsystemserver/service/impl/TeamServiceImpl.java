package org.example.drawsystemserver.service.impl;

import org.example.drawsystemserver.entity.Team;
import org.example.drawsystemserver.mapper.TeamMapper;
import org.example.drawsystemserver.service.TeamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TeamServiceImpl implements TeamService {

    @Autowired
    private TeamMapper teamMapper;

    @Override
    public Team createTeam(Long captainId, String teamName) {
        // 检查是否已有队伍
        Team existingTeam = teamMapper.selectByCaptainId(captainId);
        if (existingTeam != null) {
            return existingTeam;
        }

        Team team = new Team();
        team.setCaptainId(captainId);
        team.setTeamName(teamName);
        team.setPlayerCount(0);
        teamMapper.insert(team);
        return team;
    }

    @Override
    public Team getByCaptainId(Long captainId) {
        return teamMapper.selectByCaptainId(captainId);
    }

    @Override
    public Team getById(Long id) {
        return teamMapper.selectById(id);
    }

    @Override
    public List<Team> getAllTeams() {
        return teamMapper.selectAll();
    }

    @Override
    public List<Team> getTeamsBySession(Long sessionId) {
        return teamMapper.selectBySessionId(sessionId);
    }

    @Override
    public boolean isTeamFull(Long teamId) {
        Team team = teamMapper.selectById(teamId);
        return team != null && team.getPlayerCount() >= 4;
    }
}
