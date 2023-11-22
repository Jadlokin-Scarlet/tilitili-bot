package com.tilitili.bot.entity;

import com.tilitili.common.entity.BotRobot;

public class BotRobotDTO extends BotRobot {
    private Integer wsStatus;
    private String hookUrl;

    public BotRobotDTO(BotRobot robot) {
        this.setId(robot.getId());
        this.setName(robot.getName());
        this.setType(robot.getType());
        this.setStatus(robot.getStatus());
        this.setPushType(robot.getPushType());
        this.setHost(robot.getHost());
        this.setVerifyKey(robot.getVerifyKey());
        this.setQq(robot.getQq());
        this.setDefaultTaskIdList(robot.getDefaultTaskIdList());
    }

    public Integer getWsStatus() {
        return wsStatus;
    }

    public BotRobotDTO setWsStatus(Integer wsStatus) {
        this.wsStatus = wsStatus;
        return this;
    }

    public String getHookUrl() {
        return hookUrl;
    }

    public BotRobotDTO setHookUrl(String hookUrl) {
        this.hookUrl = hookUrl;
        return this;
    }
}
