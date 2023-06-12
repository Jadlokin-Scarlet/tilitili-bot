package com.tilitili.bot.service;

import com.tilitili.bot.config.WebSocketConfig;
import com.tilitili.bot.entity.BotRobotDTO;
import com.tilitili.bot.socket.BotWebSocketHandler;
import com.tilitili.common.entity.BotAdmin;
import com.tilitili.common.entity.BotRobot;
import com.tilitili.common.entity.query.BotRobotQuery;
import com.tilitili.common.entity.view.BaseModel;
import com.tilitili.common.entity.view.PageModel;
import com.tilitili.common.mapper.mysql.BotRobotMapper;
import com.tilitili.common.utils.Asserts;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

@Service
public class BotRobotService {
    private final BotRobotMapper botRobotMapper;
    private final WebSocketConfig webSocketConfig;

    public BotRobotService(BotRobotMapper botRobotMapper, @Nullable WebSocketConfig webSocketConfig) {
        this.botRobotMapper = botRobotMapper;
        this.webSocketConfig = webSocketConfig;
    }

    public BaseModel<PageModel<BotRobotDTO>> list(BotAdmin botAdmin, BotRobotQuery query) throws InvocationTargetException, IllegalAccessException {
        query.setAdminId(botAdmin.getId());
        int total = botRobotMapper.countBotRobotByCondition(query);
        List<BotRobot> list = botRobotMapper.getBotRobotByCondition(query.setAdminId(botAdmin.getId()));
        List<BotRobotDTO> result = new ArrayList<>();
        for (BotRobot robot : list) {
            BotRobotDTO robotDTO = new BotRobotDTO(robot);
            if (webSocketConfig != null) {
                BotWebSocketHandler handler = webSocketConfig.getBotWebSocketHandlerMap().get(robot.getId());
                robotDTO.setWsStatus(handler == null ? -1 : handler.getStatus());
            }
            result.add(robotDTO);
        }
        return PageModel.of(total, query.getPageSize(), query.getCurrent(), result);
    }

    public void upBot(Long id) {
//        Asserts.notNull(webSocketConfig, "测试环境无效");
        int cnt = botRobotMapper.updateBotRobotSelective(new BotRobot().setId(id).setStatus(0));
        Asserts.checkEquals(cnt, 1, "上线失败");
        webSocketConfig.upBot(id);
    }

    public void downBot(Long id) {
//        Asserts.notNull(webSocketConfig, "测试环境无效");
        int cnt = botRobotMapper.updateBotRobotSelective(new BotRobot().setId(id).setStatus(-1));
        Asserts.checkEquals(cnt, 1, "下线失败");
        webSocketConfig.downBot(id);
    }
}
