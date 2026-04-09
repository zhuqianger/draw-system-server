package org.example.drawsystemserver.dto;

/**
 * WebSocket事件消息体
 */
public class WebSocketEventDTO {
    private String eventId;
    private String actionId;
    private String eventType;
    private Long sessionId;
    private Long sessionVersion;
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

    public String getActionId() {
        return actionId;
    }

    public void setActionId(String actionId) {
        this.actionId = actionId;
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

    public Long getSessionVersion() {
        return sessionVersion;
    }

    public void setSessionVersion(Long sessionVersion) {
        this.sessionVersion = sessionVersion;
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
