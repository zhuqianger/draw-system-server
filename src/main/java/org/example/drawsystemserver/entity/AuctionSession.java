package org.example.drawsystemserver.entity;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 拍卖流程实体类
 */
public class AuctionSession {
    private Long id;
    private String sessionName;
    private String dataSourceFile;
    private String captainIds; // JSON格式的队长ID列表
    private String status; // CREATED-已创建，ACTIVE-进行中，FINISHED-已结束
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    
    // 关联数据（不存储在数据库）
    private List<Long> captainIdList; // 队长ID列表
    private List<Team> teams; // 队伍列表
    private Auction currentAuction; // 当前拍卖

    public AuctionSession() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSessionName() {
        return sessionName;
    }

    public void setSessionName(String sessionName) {
        this.sessionName = sessionName;
    }

    public String getDataSourceFile() {
        return dataSourceFile;
    }

    public void setDataSourceFile(String dataSourceFile) {
        this.dataSourceFile = dataSourceFile;
    }

    public String getCaptainIds() {
        return captainIds;
    }

    public void setCaptainIds(String captainIds) {
        this.captainIds = captainIds;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public List<Long> getCaptainIdList() {
        return captainIdList;
    }

    public void setCaptainIdList(List<Long> captainIdList) {
        this.captainIdList = captainIdList;
    }

    public List<Team> getTeams() {
        return teams;
    }

    public void setTeams(List<Team> teams) {
        this.teams = teams;
    }

    public Auction getCurrentAuction() {
        return currentAuction;
    }

    public void setCurrentAuction(Auction currentAuction) {
        this.currentAuction = currentAuction;
    }
}
