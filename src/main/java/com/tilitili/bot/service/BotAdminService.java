package com.tilitili.bot.service;

import com.tilitili.bot.entity.BotUserVO;
import com.tilitili.bot.entity.request.BotAdminRequest;
import com.tilitili.common.constant.BotRoleConstant;
import com.tilitili.common.constant.BotSenderConstant;
import com.tilitili.common.constant.BotUserConstant;
import com.tilitili.common.entity.BotAdminCode;
import com.tilitili.common.entity.BotRoleUserMapping;
import com.tilitili.common.entity.dto.BotUserDTO;
import com.tilitili.common.entity.query.BotRoleUserMappingQuery;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.manager.BotUserManager;
import com.tilitili.common.manager.QywechatManager;
import com.tilitili.common.manager.SendMessageManager;
import com.tilitili.common.mapper.mysql.BotAdminCodeMapper;
import com.tilitili.common.mapper.mysql.BotRoleUserMappingMapper;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.RedisCache;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Service
public class BotAdminService {
    private static final String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final String emailCodeKey = "emailCode-";
    private static final String emailCodeLockKey = "emailCodeLock-";
    private static final String sendMessageFromEmail = "Jadlokin_Scarlet@tilitili.club";
    private static final String testEmail = "w545459363@126.com";
    private final RedisCache redisCache;
    private final BotAdminCodeMapper botAdminCodeMapper;
    private final SendMessageManager sendMessageManager;
    private final QywechatManager qywechatManager;
    private final BotUserManager botUserManager;
    private final BotRoleUserMappingMapper botRoleUserMappingMapper;
    @Value("${spring.profiles.active}")
    private String active;

    public BotAdminService(BotAdminCodeMapper botAdminCodeMapper, RedisCache redisCache, SendMessageManager sendMessageManager, QywechatManager qywechatManager, BotUserManager botUserManager, BotRoleUserMappingMapper botRoleUserMappingMapper) {
        this.botAdminCodeMapper = botAdminCodeMapper;
        this.redisCache = redisCache;
        this.sendMessageManager = sendMessageManager;
        this.qywechatManager = qywechatManager;
        this.botUserManager = botUserManager;
        this.botRoleUserMappingMapper = botRoleUserMappingMapper;
    }

    public BotUserVO login(BotAdminRequest request) {
        String email = request.getEmail();
        String password = request.getPassword();
        Asserts.notBlank(email, "用户名不能为空");
        Asserts.notBlank(password, "密码不能为空");

        BotUserDTO botUser = botUserManager.getValidBotUserByExternalIdWithParent(BotUserConstant.USER_TYPE_WEB, email);
        Asserts.notNull(botUser, "账号不存在");

        String md5Password = password.length() == 32? password: md5Password(password);
        Asserts.checkEquals(botUser.getPassword(), md5Password, "密码错误");

        return this.getBotUserWithIsAdmin(botUser);
    }

    public void register(BotAdminRequest request) {
        String code = request.getCode();
        String email = request.getEmail();
        String emailCode = request.getEmailCode();
        String password = request.getPassword();
        String masterQQ = request.getMaster();
        this.checkRegisterCode(code);
        BotUserDTO botUser = this.checkMasterQQ(masterQQ);
        this.checkRegisterEmail(email);
        this.checkRegisterEmailCode(email, emailCode);
        this.checkRegisterPassword(password);

        botAdminCodeMapper.updateBotAdminCodeStatusSafe(code, 0, -1, botUser.getId());

        String md5Password = this.md5Password(password);
        botUserManager.addOrUpdateBotUser(null, botUser.setEmail(email).setPassword(md5Password));
        botRoleUserMappingMapper.addBotRoleUserMappingSelective(new BotRoleUserMapping().setUserId(botUser.getId()).setRoleId(BotRoleConstant.defaultRole));
    }

    public void sendEmailCode(BotAdminRequest request) {
        String email = request.getEmail();
        this.checkRegisterCode(request.getCode());
        this.checkMasterQQ(request.getMaster());
        this.checkRegisterEmail(email);
        this.generateAndSendEmail(email);
    }

    public void checkEmailCode(BotAdminRequest request) {
        String email = request.getEmail();
        String emailCode = request.getEmailCode();
        this.checkRegisterCode(request.getCode());
        this.checkMasterQQ(request.getMaster());
        this.checkRegisterEmail(email);
        this.checkRegisterEmailCode(email, emailCode);
    }

    public void checkRegisterCode(String code) {
        BotAdminCode adminCode = botAdminCodeMapper.getBotAdminCodeByCode(code);
        Asserts.notNull(adminCode, "邀请码有误");
        Asserts.checkEquals(adminCode.getStatus(), 0, "邀请码有误");
    }

    private void checkRegisterUsername(String username) {
        Asserts.notBlank(username, "请填写用户名");
        Asserts.isTrue(Pattern.matches("\\w{1,20}", username), "用户名格式不正确");
    }

    private void checkRegisterPassword(String password) {
        Asserts.notBlank(password, "请填写密码");
        Asserts.isTrue(password.length() < 30, "密码过长");
    }

    private void checkRegisterEmail(String email) {
        Asserts.notBlank(email, "请填写邮箱");
        Asserts.isTrue(Pattern.matches(".{1,64}@.{1,255}", email), "邮件格式有误");
        BotUserDTO dbUser = botUserManager.getValidBotUserByExternalIdWithParent(null, BotUserConstant.USER_TYPE_WEB, email);
        Asserts.checkNull(dbUser, "该邮箱已被注册");
    }

    private void checkRegisterEmailCode(String email, String emailCode) {
        String cacheEmailCode = (String) redisCache.getValue(emailCodeKey + email);
        Asserts.checkEquals(emailCode, cacheEmailCode, "验证码错误");
    }

    public BotUserDTO checkMasterQQ(String masterQQ) {
        BotUserDTO botUser = botUserManager.getValidBotUserByExternalIdWithParent(BotUserConstant.USER_TYPE_QQ, masterQQ);
        Asserts.notNull(botUser, "找不到用户");
        int adminMappingCnt = botRoleUserMappingMapper.countBotRoleUserMappingByCondition(new BotRoleUserMappingQuery().setUserId(botUser.getId()));
        Asserts.checkEquals(adminMappingCnt, 0, "该扣扣已被注册");
        return botUser;
    }

    private void generateAndSendEmail(String email) {
        String emailCode = this.generateRandomCode();

        Asserts.isTrue(redisCache.setNotExist(emailCodeLockKey + email, "yes", 1, TimeUnit.MINUTES), "收不到验证码请联系管理员");

        redisCache.setValue(emailCodeKey + email, emailCode);
        if (!"pro".equals(active) && testEmail.equals(email)) {
            sendMessageManager.sendMessage(BotMessage.simpleTextMessage("验证码："+emailCode).setSenderId(BotSenderConstant.MASTER_SENDER_ID));
        } else {
            SimpleMailMessage simpleMailMessage = new SimpleMailMessage();
            simpleMailMessage.setFrom(sendMessageFromEmail);
            simpleMailMessage.setTo(email);
            simpleMailMessage.setSubject("验证你的bot邮箱");
            simpleMailMessage.setText("验证码：" + emailCode);
            qywechatManager.sendMailMessage(simpleMailMessage);
        }
    }

    private String md5Password(String password) {
        return DigestUtils.md5DigestAsHex(password.getBytes());
    }

    private String generateRandomCode() {
        StringBuilder sb = new StringBuilder(6);

        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < 6; i++) {
            int randomIndex = random.nextInt(chars.length());
            char randomChar = chars.charAt(randomIndex);
            sb.append(randomChar);
        }

        return sb.toString();
    }

    public BotUserVO getBotUserWithIsAdmin(Long userId) {
        BotUserDTO botUser = botUserManager.getValidBotUserByIdWithParent(userId);
        return getBotUserWithIsAdmin(botUser);
    }

    public BotUserVO getBotUserWithIsAdmin(BotUserDTO botUser) {
        if (botUser == null) {
            return null;
        }
        int adminMappingCnt = botRoleUserMappingMapper.countBotRoleUserMappingByCondition(new BotRoleUserMappingQuery().setUserId(botUser.getId()));
        return new BotUserVO(botUser, adminMappingCnt > 0);
    }


    public void registerQuick(BotAdminRequest request) {
        String email = request.getEmail();
        String emailCode = request.getEmailCode();
        String password = request.getPassword();
        this.checkRegisterEmail(email);
        this.checkRegisterEmailCode(email, emailCode);
        this.checkRegisterPassword(password);

        String md5Password = this.md5Password(password);
        BotUserDTO newUser = new BotUserDTO(BotUserConstant.USER_TYPE_WEB, email).setName(email).setPassword(md5Password);
        botUserManager.addBotUserSelective(newUser);
    }

    public void sendEmailCodeQuick(BotAdminRequest request) {
        String email = request.getEmail();
        this.checkRegisterEmail(email);
        this.generateAndSendEmail(email);
    }

    public void checkEmailCodeQuick(BotAdminRequest request) {
        String email = request.getEmail();
        String emailCode = request.getEmailCode();
        this.checkRegisterEmail(email);
        this.checkRegisterEmailCode(email, emailCode);
    }
}
