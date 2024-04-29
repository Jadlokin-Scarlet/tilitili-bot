package com.tilitili.bot.service;

import com.tilitili.common.constant.BotUserConstant;
import com.tilitili.common.entity.dto.BotUserDTO;
import com.tilitili.common.entity.query.BotUserQuery;
import com.tilitili.common.entity.view.BaseModel;
import com.tilitili.common.entity.view.PageModel;
import com.tilitili.common.manager.BotRoleManager;
import com.tilitili.common.manager.BotUserManager;
import com.tilitili.common.mapper.mysql.BotUserSenderMappingMapper;
import com.tilitili.common.utils.Asserts;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class BotUserService {
	private final BotUserManager botUserManager;
	private final BotRoleManager botRoleManager;
	private final BotUserSenderMappingMapper botUserSenderMappingMapper;

	public BotUserService(BotUserManager botUserManager, BotUserSenderMappingMapper botUserSenderMappingMapper, BotRoleManager botRoleManager) {
		this.botUserManager = botUserManager;
		this.botUserSenderMappingMapper = botUserSenderMappingMapper;
		this.botRoleManager = botRoleManager;
	}

	public BaseModel<PageModel<BotUserDTO>> listBotUser(BotUserQuery query) {
		int total = botUserManager.countBotUserByAdmin(query);
		List<BotUserDTO> userDTOList = botUserManager.getBotUserByAdmin(query);
		List<BotUserDTO> userList = userDTOList.stream().distinct().collect(Collectors.toList());
		return PageModel.of(total, query.getPageSize(), query.getCurrent(), userList);
	}

	public void changeStatus(Long adminUserId, BotUserDTO updBotUser) {
		Asserts.notNull(updBotUser.getId(), "参数异常");
		Asserts.notNull(updBotUser.getStatus(), "参数异常");
		Asserts.isTrue(botUserSenderMappingMapper.checkUserAndAdminBind(updBotUser.getId(), adminUserId) > 0, "参数异常");
		botUserManager.safeUpdateStatus(updBotUser.getId(), updBotUser.getStatus());
	}

	public void bindUser(Long adminUserId, Long userId, String qq) {
		Asserts.notNull(qq, "参数异常");
		Asserts.notNull(userId, "参数异常");
		if (!botRoleManager.isAdmin(adminUserId)) {
			Asserts.isTrue(botUserSenderMappingMapper.checkUserAndAdminBind(userId, adminUserId) > 0, "参数异常");
		}

		BotUserDTO subUser = botUserManager.getValidBotUserByIdWithParent(null, userId);
		BotUserDTO parentUser = botUserManager.getValidBotUserByExternalIdWithParent(BotUserConstant.USER_TYPE_QQ, qq);
		botUserManager.bindUser(subUser, parentUser);
	}
}
