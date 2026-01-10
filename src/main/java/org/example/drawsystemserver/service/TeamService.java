package org.example.drawsystemserver.service;

import org.example.drawsystemserver.entity.Team;

import java.util.List;

public interface TeamService {
    Team createTeam(Long captainId, String teamName);
    Team getByCaptainId(Long captainId);
    Team getById(Long id);
    List<Team> getAllTeams();
    List<Team> getTeamsBySession(Long sessionId);
    boolean isTeamFull(Long teamId);
}
