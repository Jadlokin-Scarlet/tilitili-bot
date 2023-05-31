package com.tilitili.bot.entity;

import com.tilitili.common.entity.BotRobot;

public class BotRobotDTO extends BotRobot {
    private Integer wsStatus;

    public BotRobotDTO(BotRobot robot) {
        this.setId(robot.getId());
        this.setName(robot.getName());
        this.setType(robot.getType());
        this.setStatus(robot.getStatus());
    }

    public Integer getWsStatus() {
        return wsStatus;
    }

    public BotRobotDTO setWsStatus(Integer wsStatus) {
        this.wsStatus = wsStatus;
        return this;
    }
}
