package com.tilitili.bot.service;

import com.tilitili.common.constant.BotUserConstant;
import com.tilitili.common.entity.dto.BotUserDTO;
import com.tilitili.common.entity.query.BotUserQuery;
import com.tilitili.common.entity.view.BaseModel;
import com.tilitili.common.entity.view.PageModel;
import com.tilitili.common.manager.BotUserManager;
import com.tilitili.common.mapper.mysql.BotUserSenderMappingMapper;
import com.tilitili.common.utils.Asserts;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class BotUserService {
	private final BotUserManager botUserManager;
	private final BotUserSenderMappingMapper botUserSenderMappingMapper;

	public BotUserService(BotUserManager botUserManager, BotUserSenderMappingMapper botUserSenderMappingMapper) {
		this.botUserManager = botUserManager;
		this.botUserSenderMappingMapper = botUserSenderMappingMapper;
	}

	public BaseModel<PageModel<BotUserDTO>> listBotUser(BotUserQuery query) {
		int total = botUserManager.countBotUserByAdmin(query);
		List<BotUserDTO> userList = botUserManager.getBotUserByAdmin(query).stream().distinct().collect(Collectors.toList());
		return PageModel.of(total, query.getPageSize(), query.getCurrent(), userList);
	}

	public void changeStatus(Long adminId, BotUserDTO updBotUser) {
		Asserts.notNull(updBotUser.getId(), "参数异常");
		Asserts.notNull(updBotUser.getStatus(), "参数异常");
		Asserts.isTrue(botUserSenderMappingMapper.checkUserAndAdminBind(updBotUser.getId(), adminId) > 0, "参数异常");
		botUserManager.safeUpdateStatus(updBotUser.getId(), updBotUser.getStatus());
	}

	public void bindUser(Long adminId, Long userId, Long qq) {
		Asserts.notNull(userId, "参数异常");
		Asserts.notNull(qq, "参数异常");
		Asserts.isTrue(botUserSenderMappingMapper.checkUserAndAdminBind(userId, adminId) > 0, "参数异常");

		BotUserDTO subUser = botUserManager.getValidBotUserByIdWithParent(null, userId);
		BotUserDTO parentUser = botUserManager.getValidBotUserByExternalIdWithParent(qq, BotUserConstant.USER_TYPE_QQ);
		botUserManager.bindUser(subUser, parentUser);
	}
}
