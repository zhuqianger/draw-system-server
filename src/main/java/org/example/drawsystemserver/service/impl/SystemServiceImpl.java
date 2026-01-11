package org.example.drawsystemserver.service.impl;

import org.example.drawsystemserver.dto.*;
import org.example.drawsystemserver.entity.*;
import org.example.drawsystemserver.mapper.AuctionMapper;
import org.example.drawsystemserver.mapper.BidMapper;
import org.example.drawsystemserver.mapper.PlayerMapper;
import org.example.drawsystemserver.mapper.TeamMapper;
import org.example.drawsystemserver.mapper.UserMapper;
import org.example.drawsystemserver.service.SystemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
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

    @Override
    public SystemStatusDTO getSystemStatus() {
        SystemStatusDTO status = new SystemStatusDTO();

        // 获取当前拍卖
        Auction currentAuction = auctionMapper.selectCurrentActive();
        if (currentAuction != null) {
            status.setCurrentAuction(convertToAuctionDTO(currentAuction));
        }

        // 获取待拍卖池中的队员
        List<Player> poolPlayers = playerMapper.selectByStatus("POOL");
        status.setPoolPlayers(poolPlayers.stream()
                .map(this::convertToPlayerDTO)
                .collect(Collectors.toList()));

        // 获取所有队伍
        List<Team> teams = teamMapper.selectAll();
        status.setTeams(teams.stream()
                .map(this::convertToTeamDTO)
                .collect(Collectors.toList()));

        // 获取已售出的队员
        List<Player> soldPlayers = playerMapper.selectByStatus("SOLD");
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
        dto.setPlayerCount(team.getPlayerCount());

        // captainName从player表的groupName获取（captainId是player表的id）
        Player captainPlayer = playerMapper.selectById(team.getCaptainId());
        if (captainPlayer != null) {
            dto.setCaptainName(captainPlayer.getGroupName());
        } else {
            // 如果找不到player，使用team中存储的captainName作为备选
            dto.setCaptainName(team.getCaptainName());
        }

        List<Player> players = playerMapper.selectByTeamId(team.getId());
        dto.setPlayers(players.stream()
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
}
