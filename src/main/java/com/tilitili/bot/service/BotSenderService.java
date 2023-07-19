package com.tilitili.bot.service;

import com.tilitili.bot.entity.BotRobotSenderMappingDTO;
import com.tilitili.bot.entity.request.UpdateBotRobotSenderMappingIndexRequest;
import com.tilitili.bot.entity.request.UpdateBotSenderTaskRequest;
import com.tilitili.common.constant.BotRoleConstant;
import com.tilitili.common.entity.*;
import com.tilitili.common.entity.query.BotRobotSenderMappingQuery;
import com.tilitili.common.entity.query.BotSenderQuery;
import com.tilitili.common.entity.view.BaseModel;
import com.tilitili.common.entity.view.PageModel;
import com.tilitili.common.entity.view.resource.Resource;
import com.tilitili.common.exception.AssertException;
import com.tilitili.common.manager.BotRobotCacheManager;
import com.tilitili.common.mapper.mysql.BotRobotSenderMappingMapper;
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
    private final BotRobotCacheManager botRobotCacheManager;
    private final BotSenderTaskMappingMapper botSenderTaskMappingMapper;
    private final BotRoleAdminMappingAutoMapper botRoleAdminMappingMapper;
    private final BotRobotSenderMappingMapper botRobotSenderMappingMapper;

    public BotSenderService(BotSenderMapper botSenderMapper, BotSenderTaskMappingMapper botSenderTaskMappingMapper, BotRobotCacheManager botRobotCacheManager, BotRoleAdminMappingAutoMapper botRoleAdminMappingMapper, BotRobotSenderMappingMapper botRobotSenderMappingMapper) {
        this.botSenderMapper = botSenderMapper;
        this.botRobotCacheManager = botRobotCacheManager;
        this.botSenderTaskMappingMapper = botSenderTaskMappingMapper;
        this.botRoleAdminMappingMapper = botRoleAdminMappingMapper;
        this.botRobotSenderMappingMapper = botRobotSenderMappingMapper;
    }

    public BaseModel<PageModel<Map<String, Object>>> listBotSender(BotSenderQuery query) {
        int count = botSenderMapper.countBotSender(query);
        List<BotSender> list = botSenderMapper.listBotSender(query);
        List<Map<String, Object>> result = list.stream().map(botSender -> {
            BotRobot listenBot = botRobotCacheManager.getValidBotRobotById(botSender.getBot());
            BotRobot sendBot = botRobotCacheManager.getValidBotRobotById(botSender.getSendBot());
            List<BotSenderTaskMapping> mappingList = botSenderTaskMappingMapper.getBotSenderTaskMappingBySenderId(botSender.getId());
            Map<String, Object> botSenderDTO = new HashMap<>();
            botSenderDTO.put("id", botSender.getId());
            botSenderDTO.put("name", botSender.getName());
            botSenderDTO.put("sendType", botSender.getSendType());
            botSenderDTO.put("listenBot", listenBot.getName());
            botSenderDTO.put("sendBot", sendBot.getName());
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
            BotRobot bot1 = botRobotCacheManager.getValidBotRobotById(botSender.getBot());
            BotRobot bot2 = botRobotCacheManager.getValidBotRobotById(botSender.getSendBot());
            Asserts.isTrue(Objects.equals(botAdmin.getId(), bot1.getAdminId()) || Objects.equals(botAdmin.getId(), bot2.getAdminId()), "权限不足");
        }

        BotSenderTaskMapping botSenderTaskMapping = botSenderTaskMappingMapper.getBotSenderTaskMappingBySenderIdAndTaskId(id, taskId);
        if (checked && botSenderTaskMapping == null) {
            botSenderTaskMappingMapper.addBotSenderTaskMappingSelective(new BotSenderTaskMapping().setTaskId(taskId).setSenderId(id));
        } else if (!checked && botSenderTaskMapping != null) {
            botSenderTaskMappingMapper.deleteBotSenderTaskMappingById(botSenderTaskMapping.getId());
        }
    }

    public BaseModel<PageModel<BotRobotSenderMappingDTO>> getBotSenderBotRobotList(BotRobotSenderMappingQuery query) {
        Asserts.notNull(query.getSenderId(), "参数异常");
        BotSender botSender = botSenderMapper.getValidBotSenderById(query.getSenderId());
        Asserts.notNull(botSender, "参数异常");
        int total = botRobotSenderMappingMapper.countBotRobotSenderMappingByCondition(query.setStatus(0));
        List<BotRobotSenderMapping> mappingList = botRobotSenderMappingMapper.getBotRobotSenderMappingByCondition(query);
        List<BotRobotSenderMappingDTO> list = mappingList.stream().map(mapping -> {
            BotRobot botRobot = botRobotCacheManager.getValidBotRobotById(mapping.getBotId());

            BotRobotSenderMappingDTO result = new BotRobotSenderMappingDTO();
            result.setBotId(botRobot.getId());
            result.setSenderName(botSender.getName());
            result.setBotName(botRobot.getName());
            result.setType(botRobot.getType());
            result.setListenIndex(mapping.getListenIndex());
            result.setSendIndex(mapping.getSendIndex());
            return result;
        }).collect(Collectors.toList());
        return PageModel.of(total, query.getPageSize(), query.getCurrent(), list);
    }

    public void updateBotSenderBotRobotIndex(UpdateBotRobotSenderMappingIndexRequest request) {
        Long senderId = request.getSenderId();
        List<Long> botIdList = request.getBotIdList();
        String indexType = request.getIndexType();
        Asserts.notNull(senderId, "参数异常");
        Asserts.notEmpty(botIdList, "参数异常");
        Asserts.notNull(indexType, "参数异常");
        for (int index = 0; index < botIdList.size(); index++) {
            Long botId = botIdList.get(index);
            BotRobotSenderMapping mapping = botRobotSenderMappingMapper.getBotRobotSenderMappingBySenderIdAndBotId(senderId, botId);
            if ("listenIndex".equals(indexType)) {
                botRobotSenderMappingMapper.updateBotRobotSenderMappingSelective(new BotRobotSenderMapping().setId(mapping.getId()).setListenIndex(index));
            } else if ("sendIndex".equals(indexType)) {
                botRobotSenderMappingMapper.updateBotRobotSenderMappingSelective(new BotRobotSenderMapping().setId(mapping.getId()).setSendIndex(index));
            } else {
                throw new AssertException("参数异常");
            }
        }
    }

    public List<Resource> listBotSenderResource(BotAdmin botAdmin) {
        BotSenderQuery botSenderQuery = new BotSenderQuery().setStatus(0);
        BotRoleAdminMapping adminMapping = botRoleAdminMappingMapper.getBotRoleAdminMappingByAdminIdAndRoleId(botAdmin.getId(), BotRoleConstant.adminRole);
        if (adminMapping == null) {
            botSenderQuery.setAdminId(botAdmin.getId());
        }
        List<BotSender> list = botSenderMapper.listBotSender(botSenderQuery);
        return list.stream().map(botSender -> new Resource(botSender.getId(), botSender.getName())).collect(Collectors.toList());
    }
}
