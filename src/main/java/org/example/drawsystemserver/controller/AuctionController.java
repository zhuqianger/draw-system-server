package org.example.drawsystemserver.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.example.drawsystemserver.dto.AuctionDTO;
import org.example.drawsystemserver.dto.BidDTO;
import org.example.drawsystemserver.dto.ResponseDTO;
import org.example.drawsystemserver.entity.Auction;
import org.example.drawsystemserver.entity.Bid;
import org.example.drawsystemserver.entity.Player;
import org.example.drawsystemserver.entity.Team;
import org.example.drawsystemserver.mapper.BidMapper;
import org.example.drawsystemserver.mapper.PlayerMapper;
import org.example.drawsystemserver.mapper.TeamMapper;
import org.example.drawsystemserver.mapper.UserMapper;
import org.example.drawsystemserver.service.AuctionService;
import org.example.drawsystemserver.service.TeamService;
import org.example.drawsystemserver.service.UserService;
import org.example.drawsystemserver.service.WebSocketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auction")
@CrossOrigin
public class AuctionController {

    @Autowired
    private AuctionService auctionService;

    @Autowired
    private UserService userService;

    @Autowired
    private TeamService teamService;

    @Autowired
    private WebSocketService webSocketService;

    @Autowired
    private BidMapper bidMapper;

    @Autowired
    private TeamMapper teamMapper;

    @Autowired
    private PlayerMapper playerMapper;

    @Autowired
    private UserMapper userMapper;

    /**
     * 管理员开始拍卖（从待拍卖池抽取队员）
     */
    @PostMapping("/start")
    public ResponseDTO<AuctionDTO> startAuction(@RequestParam Long sessionId, @RequestParam Long playerId, @RequestParam(defaultValue = "60") Integer duration, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (!userService.isAdmin(userId)) {
            return ResponseDTO.error(403, "只有管理员可以开始拍卖");
        }

        try {
            Auction auction = auctionService.startAuction(sessionId, playerId, duration);
            webSocketService.broadcastAuctionStart(auction.getId());

            AuctionDTO dto = convertToDTO(auction);
            return ResponseDTO.success("拍卖开始", dto);
        } catch (Exception e) {
            return ResponseDTO.error(e.getMessage());
        }
    }

    /**
     * 获取当前拍卖
     */
    @GetMapping("/current")
    public ResponseDTO<AuctionDTO> getCurrentAuction(@RequestParam(required = false) Long sessionId) {
        Auction auction;
        if (sessionId != null) {
            auction = auctionService.getCurrentAuctionBySession(sessionId);
        } else {
            auction = auctionService.getCurrentAuction();
        }
        if (auction == null) {
            return ResponseDTO.success(null);
        }
        return ResponseDTO.success(convertToDTO(auction));
    }

    /**
     * 队长出价
     */
    @PostMapping("/bid")
    public ResponseDTO<BidDTO> placeBid(@RequestBody BidDTO bidDTO, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (!userService.isCaptain(userId)) {
            return ResponseDTO.error(403, "只有队长可以出价");
        }

        try {
            Team team = teamService.getByCaptainId(userId);
            if (team == null) {
                return ResponseDTO.error("队伍不存在");
            }

            if (teamService.isTeamFull(team.getId())) {
                return ResponseDTO.error("队伍已满员（最多4人）");
            }

            Bid bid = auctionService.placeBid(bidDTO.getAuctionId(), team.getId(), userId, bidDTO.getAmount());
            webSocketService.broadcastBidPlaced(bid.getAuctionId(), bid.getId());

            BidDTO result = convertToBidDTO(bid);
            return ResponseDTO.success("出价成功", result);
        } catch (Exception e) {
            return ResponseDTO.error(e.getMessage());
        }
    }

    /**
     * 管理员结束拍卖
     */
    @PostMapping("/finish/{auctionId}")
    public ResponseDTO<String> finishAuction(@PathVariable Long auctionId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (!userService.isAdmin(userId)) {
            return ResponseDTO.error(403, "只有管理员可以结束拍卖");
        }

        try {
            auctionService.finishAuction(auctionId);
            webSocketService.broadcastAuctionFinished(auctionId);
            return ResponseDTO.success("拍卖已结束");
        } catch (Exception e) {
            return ResponseDTO.error(e.getMessage());
        }
    }

    /**
     * 获取拍卖的竞价列表
     */
    @GetMapping("/{auctionId}/bids")
    public ResponseDTO<List<BidDTO>> getBids(@PathVariable Long auctionId) {
        List<Bid> bids = bidMapper.selectByAuctionId(auctionId);
        List<BidDTO> dtos = bids.stream()
                .map(this::convertToBidDTO)
                .collect(Collectors.toList());
        return ResponseDTO.success(dtos);
    }

    private AuctionDTO convertToDTO(Auction auction) {
        AuctionDTO dto = new AuctionDTO();
        dto.setId(auction.getId());
        dto.setPlayerId(auction.getPlayerId());
        dto.setStartTime(auction.getStartTime());
        dto.setEndTime(auction.getEndTime());
        dto.setDuration(auction.getDuration());
        dto.setStatus(auction.getStatus());

        Player player = playerMapper.selectById(auction.getPlayerId());
        if (player != null) {
            dto.setPlayerName(player.getGroupName()); // 使用群内名字作为显示名称
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

        var captain = userMapper.selectById(bid.getCaptainId());
        if (captain != null) {
            dto.setCaptainName(captain.getUsername());
        }

        return dto;
    }
}
