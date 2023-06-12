package com.tilitili.bot.entity;

import com.tilitili.common.entity.dto.BaseDTO;

import java.util.List;

public class MessageRecordVO extends BaseDTO {
    private Long id;
    private String content;
    private List<String> picList;
    private String sendType;
    private String senderName;
    private String userName;
    private Boolean hasReply;
    private String replyContent;
    private String replyPicList;

    public String getContent() {
        return content;
    }

    public MessageRecordVO setContent(String content) {
        this.content = content;
        return this;
    }

    public List<String> getPicList() {
        return picList;
    }

    public MessageRecordVO setPicList(List<String> picList) {
        this.picList = picList;
        return this;
    }

    public String getUserName() {
        return userName;
    }

    public MessageRecordVO setUserName(String userName) {
        this.userName = userName;
        return this;
    }

    public String getSenderName() {
        return senderName;
    }

    public MessageRecordVO setSenderName(String senderName) {
        this.senderName = senderName;
        return this;
    }

    public String getSendType() {
        return sendType;
    }

    public MessageRecordVO setSendType(String sendType) {
        this.sendType = sendType;
        return this;
    }

    public Boolean getHasReply() {
        return hasReply;
    }

    public MessageRecordVO setHasReply(Boolean hasReply) {
        this.hasReply = hasReply;
        return this;
    }

    public Long getId() {
        return id;
    }

    public MessageRecordVO setId(Long id) {
        this.id = id;
        return this;
    }

    public String getReplyContent() {
        return replyContent;
    }

    public MessageRecordVO setReplyContent(String replyContent) {
        this.replyContent = replyContent;
        return this;
    }

    public String getReplyPicList() {
        return replyPicList;
    }

    public MessageRecordVO setReplyPicList(String replyPicList) {
        this.replyPicList = replyPicList;
        return this;
    }
}
