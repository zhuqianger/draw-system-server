package org.example.drawsystemserver.service.impl;

import org.example.drawsystemserver.dto.*;
import org.example.drawsystemserver.entity.*;
import org.example.drawsystemserver.mapper.AuctionMapper;
import org.example.drawsystemserver.mapper.AuctionPickRecordMapper;
import org.example.drawsystemserver.mapper.BidMapper;
import org.example.drawsystemserver.mapper.PlayerMapper;
import org.example.drawsystemserver.mapper.TeamMapper;
import org.example.drawsystemserver.mapper.UserMapper;
import org.example.drawsystemserver.service.SystemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SystemServiceImpl implements SystemService {

    @Autowired
    private AuctionMapper auctionMapper;

    @Autowired
    private PlayerMapper playerMapper;

    @Autowired
    private TeamMapper teamMapper;

    @Autowired
    private BidMapper bidMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private AuctionPickRecordMapper auctionPickRecordMapper;

    @Override
    public SystemStatusDTO getSystemStatus() {
        return getSystemStatusBySession(null);
    }

    @Override
    public SystemStatusDTO getSystemStatusBySession(Long sessionId) {
        SystemStatusDTO status = new SystemStatusDTO();

        // 获取当前拍卖
        Auction currentAuction = sessionId == null
                ? auctionMapper.selectCurrentActive()
                : auctionMapper.selectCurrentActiveBySessionId(sessionId);
        if (currentAuction != null) {
            status.setCurrentAuction(convertToAuctionDTO(currentAuction));
            List<Bid> bids = bidMapper.selectByAuctionId(currentAuction.getId());
            List<BidDTO> currentBidHistory = bids.stream()
                    .map(this::convertToBidLiteDTO)
                    .collect(Collectors.toList());
            Collections.reverse(currentBidHistory);
            status.setCurrentBidHistory(currentBidHistory);
        } else {
            status.setCurrentBidHistory(Collections.emptyList());
        }

        if (sessionId != null) {
            List<AuctionPickRecord> records = auctionPickRecordMapper.selectBySessionIdOrderBySequence(sessionId);
            status.setPickRecords(convertPickRecords(records));
        } else {
            status.setPickRecords(Collections.emptyList());
        }

        // 获取待拍卖池中的队员
        List<Player> poolPlayers = sessionId == null
                ? playerMapper.selectByStatus("POOL")
                : playerMapper.selectBySessionIdAndStatus(sessionId, "POOL");
        status.setPoolPlayers(poolPlayers.stream()
                .map(this::convertToPlayerDTO)
                .collect(Collectors.toList()));

        // 获取所有队伍
        List<Team> teams = sessionId == null ? teamMapper.selectAll() : teamMapper.selectBySessionId(sessionId);
        status.setTeams(teams.stream()
                .map(this::convertToTeamDTO)
                .collect(Collectors.toList()));

        // 获取已售出的队员
        List<Player> soldPlayers = sessionId == null
                ? playerMapper.selectByStatus("SOLD")
                : playerMapper.selectBySessionIdAndStatus(sessionId, "SOLD");
        status.setSoldPlayers(soldPlayers.stream()
                .map(this::convertToPlayerDTO)
                .collect(Collectors.toList()));

        return status;
    }

    private AuctionDTO convertToAuctionDTO(Auction auction) {
        AuctionDTO dto = new AuctionDTO();
        dto.setId(auction.getId());
        dto.setPlayerId(auction.getPlayerId());
        dto.setStartTime(auction.getStartTime());
        dto.setEndTime(auction.getEndTime());
        dto.setDuration(auction.getDuration());
        dto.setStatus(auction.getStatus());

        Player player = playerMapper.selectById(auction.getPlayerId());
        if (player != null) {
            dto.setPlayerName(player.getGroupName());
            dto.setPlayerGroupName(player.getGroupName());
            dto.setPlayerGameId(player.getGameId());
            dto.setPlayerPosition(player.getPosition());
            dto.setPlayerHeroes(player.getHeroes());
            dto.setPlayerRank(player.getRank());
            dto.setPlayerCost(player.getCost());
        }

        Bid highestBid = bidMapper.selectHighestByAuctionId(auction.getId());
        if (highestBid != null) {
            dto.setHighestBidAmount(highestBid.getAmount());
            dto.setHighestBidTeamId(highestBid.getTeamId());
            Team team = teamMapper.selectById(highestBid.getTeamId());
            if (team != null) {
                dto.setHighestBidTeamName(team.getTeamName());
            }
        }

        List<Bid> recentBids = bidMapper.selectRecentByAuctionId(auction.getId(), 5);
        dto.setRecentBids(recentBids.stream()
                .map(this::convertToBidDTO)
                .collect(Collectors.toList()));
        dto.setBidCount(bidMapper.selectByAuctionId(auction.getId()).size());

        return dto;
    }

    private PlayerDTO convertToPlayerDTO(Player player) {
        PlayerDTO dto = new PlayerDTO();
        dto.setId(player.getId());
        dto.setGroupName(player.getGroupName());
        dto.setGameId(player.getGameId());
        dto.setPosition(player.getPosition());
        dto.setHeroes(player.getHeroes());
        dto.setRank(player.getRank());
        dto.setCost(player.getCost());
        dto.setStatus(player.getStatus());
        dto.setTeamId(player.getTeamId());

        if (player.getTeamId() != null) {
            Team team = teamMapper.selectById(player.getTeamId());
            if (team != null) {
                dto.setTeamName(team.getTeamName());
            }
        }

        return dto;
    }

    private TeamDTO convertToTeamDTO(Team team) {
        TeamDTO dto = new TeamDTO();
        dto.setId(team.getId());
        dto.setCaptainId(team.getCaptainId());
        dto.setTeamName(team.getTeamName());
        dto.setTotalCost(team.getTotalCost());
        dto.setNowCost(team.getNowCost());
        dto.setUserId(team.getUserId());

        // captainName从player表的groupName获取（captainId是player表的id）
        Player captainPlayer = playerMapper.selectById(team.getCaptainId());
        if (captainPlayer != null) {
            dto.setCaptainName(captainPlayer.getGroupName());
        } else {
            // 如果找不到player，使用team中存储的captainName作为备选
            dto.setCaptainName(team.getCaptainName());
        }

        // 获取该队伍的所有队员（不包括队长）
        List<Player> players = playerMapper.selectByTeamIdExcludingCaptain(team.getId(), team.getCaptainId());
        
        // 基于实际队员数量计算playerCount（不包括队长）
        int actualPlayerCount = players.size();
        dto.setPlayerCount(actualPlayerCount);
        
        // 如果数据库中的playerCount和实际队员数量不一致，更新数据库
        if (team.getPlayerCount() == null || !team.getPlayerCount().equals(actualPlayerCount)) {
            team.setPlayerCount(actualPlayerCount);
            teamMapper.update(team);
        }

        // 包括队长在内的所有队员（用于显示）
        List<Player> allPlayers = playerMapper.selectByTeamId(team.getId());
        dto.setPlayers(allPlayers.stream()
                .map(this::convertToPlayerDTO)
                .collect(Collectors.toList()));

        return dto;
    }

    private BidDTO convertToBidDTO(Bid bid) {
        BidDTO dto = new BidDTO();
        dto.setId(bid.getId());
        dto.setAuctionId(bid.getAuctionId());
        dto.setTeamId(bid.getTeamId());
        dto.setCaptainId(bid.getCaptainId());
        dto.setAmount(bid.getAmount());
        dto.setBidTime(bid.getBidTime());
        dto.setIsWinner(bid.getIsWinner());

        Team team = teamMapper.selectById(bid.getTeamId());
        if (team != null) {
            dto.setTeamName(team.getTeamName());
        }

        User captain = userMapper.selectById(bid.getCaptainId());
        if (captain != null) {
            dto.setCaptainName(captain.getUsername());
        }

        return dto;
    }

    private BidDTO convertToBidLiteDTO(Bid bid) {
        BidDTO dto = new BidDTO();
        dto.setId(bid.getId());
        dto.setAuctionId(bid.getAuctionId());
        dto.setTeamId(bid.getTeamId());
        dto.setAmount(bid.getAmount());
        dto.setBidTime(bid.getBidTime());
        dto.setIsWinner(bid.getIsWinner());

        Team team = teamMapper.selectById(bid.getTeamId());
        if (team != null) {
            dto.setTeamName(team.getTeamName());
        }
        return dto;
    }

    private List<AuctionPickRecordDTO> convertPickRecords(List<AuctionPickRecord> records) {
        if (records == null || records.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> playerIds = new HashSet<>();
        Set<Long> teamIds = new HashSet<>();
        for (AuctionPickRecord record : records) {
            if (record.getPlayerId() != null) {
                playerIds.add(record.getPlayerId());
            }
            if (record.getTeamId() != null) {
                teamIds.add(record.getTeamId());
            }
        }

        Map<Long, Player> playerMap = playerIds.isEmpty()
                ? Collections.emptyMap()
                : playerMapper.selectByIds(playerIds.stream().collect(Collectors.toList())).stream()
                .collect(Collectors.toMap(Player::getId, p -> p, (a, b) -> a, HashMap::new));

        Map<Long, Team> teamMap = teamIds.isEmpty()
                ? Collections.emptyMap()
                : teamMapper.selectByIds(teamIds.stream().collect(Collectors.toList())).stream()
                .collect(Collectors.toMap(Team::getId, t -> t, (a, b) -> a, HashMap::new));

        Set<Long> captainIds = teamMap.values().stream()
                .map(Team::getCaptainId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        Map<Long, Player> captainMap = captainIds.isEmpty()
                ? Collections.emptyMap()
                : playerMapper.selectByIds(captainIds.stream().collect(Collectors.toList())).stream()
                .collect(Collectors.toMap(Player::getId, p -> p, (a, b) -> a, HashMap::new));

        return records.stream().map(record -> {
            AuctionPickRecordDTO dto = new AuctionPickRecordDTO();
            dto.setId(record.getId());
            dto.setSessionId(record.getSessionId());
            dto.setAuctionId(record.getAuctionId());
            dto.setPlayerId(record.getPlayerId());
            dto.setTeamId(record.getTeamId());
            dto.setAmount(record.getAmount());
            dto.setSequence(record.getSequence());
            dto.setCreateTime(record.getCreateTime());

            Player player = playerMap.get(record.getPlayerId());
            if (player != null) {
                dto.setPlayerGroupName(player.getGroupName());
                dto.setPlayerGameId(player.getGameId());
            }

            Team team = teamMap.get(record.getTeamId());
            if (team != null) {
                dto.setTeamName(team.getTeamName());
                Player captain = captainMap.get(team.getCaptainId());
                if (captain != null) {
                    dto.setCaptainName(captain.getGroupName());
                } else {
                    dto.setCaptainName(team.getCaptainName());
                }
            }
            return dto;
        }).collect(Collectors.toList());
    }
}
