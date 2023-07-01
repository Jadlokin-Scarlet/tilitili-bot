package com.tilitili.bot.service;

import com.tilitili.bot.entity.request.UpdateBotSenderTaskRequest;
import com.tilitili.common.constant.BotRoleConstant;
import com.tilitili.common.entity.*;
import com.tilitili.common.entity.query.BotSenderQuery;
import com.tilitili.common.entity.view.BaseModel;
import com.tilitili.common.entity.view.PageModel;
import com.tilitili.common.mapper.mysql.BotRobotMapper;
import com.tilitili.common.mapper.mysql.BotSenderMapper;
import com.tilitili.common.mapper.mysql.BotSenderTaskMappingMapper;
import com.tilitili.common.mapper.mysql.automapper.BotRoleAdminMappingAutoMapper;
import com.tilitili.common.utils.Asserts;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class BotSenderService {
    private final BotSenderMapper botSenderMapper;
    private final BotRobotMapper botRobotMapper;
    private final BotSenderTaskMappingMapper botSenderTaskMappingMapper;
    private final BotRoleAdminMappingAutoMapper botRoleAdminMappingMapper;

    public BotSenderService(BotSenderMapper botSenderMapper, BotSenderTaskMappingMapper botSenderTaskMappingMapper, BotRobotMapper botRobotMapper, BotRoleAdminMappingAutoMapper botRoleAdminMappingMapper) {
        this.botSenderMapper = botSenderMapper;
        this.botRobotMapper = botRobotMapper;
        this.botSenderTaskMappingMapper = botSenderTaskMappingMapper;
        this.botRoleAdminMappingMapper = botRoleAdminMappingMapper;
    }

    public BaseModel<PageModel<Map<String, Object>>> listBotSender(BotSenderQuery query) {
        int count = botSenderMapper.countBotSender(query);
        List<BotSender> list = botSenderMapper.listBotSender(query);
        List<Map<String, Object>> result = list.stream().map(botSender -> {
            List<BotSenderTaskMapping> mappingList = botSenderTaskMappingMapper.getBotSenderTaskMappingBySenderId(botSender.getId());
            Map<String, Object> botSenderDTO = new HashMap<>();
            botSenderDTO.put("id", botSender.getId());
            botSenderDTO.put("name", botSender.getName());
            botSenderDTO.put("sendType", botSender.getSendType());
            mappingList.forEach(mapping -> botSenderDTO.put(String.valueOf(mapping.getTaskId()), true));
            return botSenderDTO;
        }).collect(Collectors.toList());
        return PageModel.of(count, query.getPageSize(), query.getCurrent(), result);
    }

    public void updateBotSenderTask(BotAdmin botAdmin, UpdateBotSenderTaskRequest request) {
        Asserts.notNull(request, "参数异常");
        Long id = request.getId();
        Long taskId = request.getTaskId();
        Boolean checked = request.getChecked();

        Asserts.notNull(id, "参数异常");
        Asserts.notNull(taskId, "参数异常");
        Asserts.notNull(checked, "参数异常");

        BotRoleAdminMapping adminMapping = botRoleAdminMappingMapper.getBotRoleAdminMappingByAdminIdAndRoleId(botAdmin.getId(), BotRoleConstant.adminRole);
        if (adminMapping == null) {
            BotSender botSender = botSenderMapper.getValidBotSenderById(id);
            BotRobot bot1 = botRobotMapper.getValidBotRobotById(botSender.getBot());
            BotRobot bot2 = botRobotMapper.getValidBotRobotById(botSender.getSendBot());
            Asserts.isTrue(Objects.equals(botAdmin.getId(), bot1.getAdminId()) || Objects.equals(botAdmin.getId(), bot2.getAdminId()), "权限不足");
        }

        BotSenderTaskMapping botSenderTaskMapping = botSenderTaskMappingMapper.getBotSenderTaskMappingBySenderIdAndTaskId(id, taskId);
        if (checked && botSenderTaskMapping == null) {
            botSenderTaskMappingMapper.addBotSenderTaskMappingSelective(new BotSenderTaskMapping().setTaskId(taskId).setSenderId(id));
        } else if (!checked && botSenderTaskMapping != null) {
            botSenderTaskMappingMapper.deleteBotSenderTaskMappingById(botSenderTaskMapping.getId());
        }
    }
}
