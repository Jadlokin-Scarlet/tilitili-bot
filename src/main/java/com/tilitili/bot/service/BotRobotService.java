package com.tilitili.bot.service;

import com.tilitili.bot.config.WebSocketConfig;
import com.tilitili.bot.entity.BotRobotDTO;
import com.tilitili.bot.socket.BaseWebSocketHandler;
import com.tilitili.bot.socket.BotWebSocketHandler;
import com.tilitili.common.entity.BotAdmin;
import com.tilitili.common.entity.BotRobot;
import com.tilitili.common.entity.query.BotRobotQuery;
import com.tilitili.common.entity.view.BaseModel;
import com.tilitili.common.entity.view.PageModel;
import com.tilitili.common.mapper.mysql.BotRobotMapper;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class BotRobotService {
    private final BotRobotMapper botRobotMapper;
    private final Map<Long, BotWebSocketHandler> botHandleMap;

    public BotRobotService(BotRobotMapper botRobotMapper, @Nullable WebSocketConfig webSocketConfig) {
        this.botRobotMapper = botRobotMapper;
        botHandleMap = new HashMap<>();
        if (webSocketConfig != null) {
            for (BaseWebSocketHandler handle : webSocketConfig.getBotWebSocketHandlerList()) {
                if (handle instanceof BotWebSocketHandler) {
                    BotWebSocketHandler botHandle = (BotWebSocketHandler) handle;
                    botHandleMap.put(botHandle.getBot().getId(), botHandle);
                }
            }
        }
    }

    public BaseModel<PageModel<BotRobotDTO>> list(BotAdmin botAdmin, BotRobotQuery query) throws InvocationTargetException, IllegalAccessException {
        query.setAdminId(botAdmin.getId());
        int total = botRobotMapper.countBotRobotByCondition(query);
        List<BotRobot> list = botRobotMapper.getBotRobotByCondition(query.setAdminId(botAdmin.getId()));
        List<BotRobotDTO> result = new ArrayList<>();
        for (BotRobot robot : list) {
            BotRobotDTO robotDTO = new BotRobotDTO(robot);
            BotWebSocketHandler handler = botHandleMap.get(robot.getId());
            robotDTO.setWsStatus(handler == null? -1: handler.getStatus());
            result.add(robotDTO);
        }
        return PageModel.of(total, query.getPageSize(), query.getCurrent(), result);
    }
}
