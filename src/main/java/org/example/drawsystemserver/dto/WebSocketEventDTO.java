package org.example.drawsystemserver.dto;

/**
 * WebSocket事件消息体
 */
public class WebSocketEventDTO {
    private String eventId;
    private String eventType;
    private Long sessionId;
    private Long auctionId;
    private Long bidId;
    private Long playerId;
    private Long teamId;
    private Long timestamp;
    private SystemStatusDTO systemStatus;
    private Object data;

    public WebSocketEventDTO() {
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public Long getAuctionId() {
        return auctionId;
    }

    public void setAuctionId(Long auctionId) {
        this.auctionId = auctionId;
    }

    public Long getBidId() {
        return bidId;
    }

    public void setBidId(Long bidId) {
        this.bidId = bidId;
    }

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

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public SystemStatusDTO getSystemStatus() {
        return systemStatus;
    }

    public void setSystemStatus(SystemStatusDTO systemStatus) {
        this.systemStatus = systemStatus;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }
}
