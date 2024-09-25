package com.github.theprez.manzan.routes.event;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MsgEvent {

    @JsonProperty("event_type")
    private String eventType;

    @JsonProperty("session_id")
    private String sessionId;

    private String job;

    @JsonProperty("msgid")
    private String msgId;

    @JsonProperty("msgtype")
    private String msgType;

    private int severity;

    @JsonProperty("message_timestamp")
    private String messageTimestamp;

    @JsonProperty("sending_usrprf")
    private String sendingUsrprf;

    private String message;

    @JsonProperty("sending_program_name")
    private String sendingProgramName;

    @JsonProperty("sending_module_name")
    private String sendingModuleName;

    @JsonProperty("sending_procedure_name")
    private String sendingProcedureName;

    // Getters and Setters

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getJob() {
        return job;
    }

    public void setJob(String job) {
        this.job = job;
    }

    public String getMsgId() {
        return msgId;
    }

    public void setMsgId(String msgId) {
        this.msgId = msgId;
    }

    public String getMsgType() {
        return msgType;
    }

    public void setMsgType(String msgType) {
        this.msgType = msgType;
    }

    public int getSeverity() {
        return severity;
    }

    public void setSeverity(int severity) {
        this.severity = severity;
    }

    public String getMessageTimestamp() {
        return messageTimestamp;
    }

    public void setMessageTimestamp(String messageTimestamp) {
        this.messageTimestamp = messageTimestamp;
    }

    public String getSendingUsrprf() {
        return sendingUsrprf;
    }

    public void setSendingUsrprf(String sendingUsrprf) {
        this.sendingUsrprf = sendingUsrprf;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSendingProgramName() {
        return sendingProgramName;
    }

    public void setSendingProgramName(String sendingProgramName) {
        this.sendingProgramName = sendingProgramName;
    }

    public String getSendingModuleName() {
        return sendingModuleName;
    }

    public void setSendingModuleName(String sendingModuleName) {
        this.sendingModuleName = sendingModuleName;
    }

    public String getSendingProcedureName() {
        return sendingProcedureName;
    }

    public void setSendingProcedureName(String sendingProcedureName) {
        this.sendingProcedureName = sendingProcedureName;
    }

    @Override
    public String toString() {
        return "MessageEvent{" +
                "eventType='" + eventType + '\'' +
                ", sessionId='" + sessionId + '\'' +
                ", job='" + job + '\'' +
                ", msgId='" + msgId + '\'' +
                ", msgType='" + msgType + '\'' +
                ", severity=" + severity +
                ", messageTimestamp='" + messageTimestamp + '\'' +
                ", sendingUsrprf='" + sendingUsrprf + '\'' +
                ", message='" + message + '\'' +
                ", sendingProgramName='" + sendingProgramName + '\'' +
                ", sendingModuleName='" + sendingModuleName + '\'' +
                ", sendingProcedureName='" + sendingProcedureName + '\'' +
                '}';
    }
}
