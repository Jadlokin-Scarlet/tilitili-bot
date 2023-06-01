package com.tilitili.bot.service;

import com.tilitili.bot.entity.request.BotAdminRequest;
import com.tilitili.common.constant.BotRoleConstant;
import com.tilitili.common.constant.BotSenderConstant;
import com.tilitili.common.entity.BotAdmin;
import com.tilitili.common.entity.BotAdminCode;
import com.tilitili.common.entity.BotRoleMapping;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.manager.BotAdminCodeManager;
import com.tilitili.common.manager.SendMessageManager;
import com.tilitili.common.mapper.mysql.BotAdminCodeMapper;
import com.tilitili.common.mapper.mysql.BotAdminMapper;
import com.tilitili.common.mapper.mysql.BotRoleMappingMapper;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.RedisCache;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Service
public class BotAdminService {
    private static final String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final String emailCodeKey = "emailCode-";
    private static final String emailCodeLockKey = "emailCodeLock-";
    private static final String sendMessageFromEmail = "Jadlokin_Scarlet@tilitili.club";
    private static final String testEmail = "w5454593633@126.com";
    private final RedisCache redisCache;
    private final BotAdminMapper botAdminMapper;
    private final JavaMailSender javaMailSender;
    private final BotAdminCodeMapper botAdminCodeMapper;
    private final BotAdminCodeManager botAdminCodeManager;
    private final SendMessageManager sendMessageManager;
    private final BotRoleMappingMapper botRoleMappingMapper;

    public BotAdminService(BotAdminMapper botAdminMapper, JavaMailSender javaMailSender, BotAdminCodeManager botAdminCodeManager, BotAdminCodeMapper botAdminCodeMapper, RedisCache redisCache, SendMessageManager sendMessageManager, BotRoleMappingMapper botRoleMappingMapper) {
        this.botAdminMapper = botAdminMapper;
        this.javaMailSender = javaMailSender;
        this.botAdminCodeManager = botAdminCodeManager;
        this.botAdminCodeMapper = botAdminCodeMapper;
        this.redisCache = redisCache;
        this.sendMessageManager = sendMessageManager;
        this.botRoleMappingMapper = botRoleMappingMapper;
    }

    public BotAdmin login(BotAdminRequest request) {
        String username = request.getUsername();
        String password = request.getPassword();
        Asserts.notBlank(username, "用户名不能为空");
        Asserts.notBlank(password, "密码不能为空");

        BotAdmin botAdmin = botAdminMapper.getBotAdminByUsername(username);
        if (botAdmin == null) {
            botAdmin = botAdminMapper.getBotAdminByEmail(username);
        }
        Asserts.notNull(botAdmin, "账号不存在");

        String md5Password = md5Password(password);
        Asserts.checkEquals(botAdmin.getPassword(), md5Password, "密码错误");

        return botAdmin;
    }

    public void register(BotAdminRequest request) {
        String code = request.getCode();
        String email = request.getEmail();
        String emailCode = request.getEmailCode();
        String username = request.getUsername();
        String password = request.getPassword();
        this.checkRegisterCode(code);
        this.checkRegisterEmail(email);
        this.checkRegisterEmailCode(email, emailCode);
        this.checkRegisterUsername(username);
        this.checkRegisterPassword(password);

        botAdminCodeMapper.updateBotAdminCodeStatusSafe(code, 0, -1);

        String md5Password = this.md5Password(password);
        BotAdmin newAdminBot = new BotAdmin().setCode(code).setEmail(email).setUsername(username).setPassword(md5Password).setStatus(0);
        botAdminMapper.addBotAdminSelective(newAdminBot);
        botRoleMappingMapper.addBotRoleMappingSelective(new BotRoleMapping().setAdminId(newAdminBot.getId()).setRoleId(BotRoleConstant.defaultRole));
    }

    public void sendEmailCode(BotAdminRequest request) {
        String email = request.getEmail();
        this.checkRegisterCode(request.getCode());
        this.checkRegisterEmail(email);
        this.generateAndSendEmail(email);
    }

    public void checkEmailCode(BotAdminRequest request) {
        String email = request.getEmail();
        String emailCode = request.getEmailCode();
        this.checkRegisterCode(request.getCode());
        this.checkRegisterEmail(email);
        this.checkRegisterEmailCode(email, emailCode);
    }

    public void checkRegisterCode(String code) {
        List<String> codeList = botAdminCodeManager.listCodeListCache();
        Asserts.isTrue(codeList.contains(code), "邀请码有误");
        BotAdminCode adminCode = botAdminCodeMapper.getBotAdminCodeByCode(code);
        Asserts.checkEquals(adminCode.getStatus(), 0, "邀请码已失效");
    }

    private void checkRegisterUsername(String username) {
        Asserts.notBlank(username, "请填写用户名");
        Asserts.isTrue(Pattern.matches("\\w{1,20}", username), "用户名格式不正确");
        BotAdmin dbAdmin = botAdminMapper.getBotAdminByUsername(username);
        Asserts.checkNull(dbAdmin, "该用户名已被注册");
    }

    private void checkRegisterPassword(String password) {
        Asserts.notBlank(password, "请填写密码");
    }

    private void checkRegisterEmail(String email) {
        Asserts.notBlank(email, "请填写邮箱");
        Asserts.isTrue(Pattern.matches(".{1,64}@.{1,255}", email), "邮件格式有误");
        BotAdmin dbAdmin = botAdminMapper.getBotAdminByEmail(email);
        Asserts.checkNull(dbAdmin, "该邮箱已被注册");
    }

    private void checkRegisterEmailCode(String email, String emailCode) {
        String cacheEmailCode = (String) redisCache.getValue(emailCodeKey + email);
        Asserts.checkEquals(emailCode, cacheEmailCode, "验证码错误");
    }

    private void generateAndSendEmail(String email) {
        String emailCode = this.generateRandomCode();

        Asserts.isTrue(redisCache.setNotExist(emailCodeLockKey + email, "yes", 1, TimeUnit.MINUTES), "收不到验证码请联系管理员");

        redisCache.setValue(emailCodeKey + email, emailCode);
        if (testEmail.equals(email)) {
            sendMessageManager.sendMessage(BotMessage.simpleTextMessage("验证码："+emailCode).setSenderId(BotSenderConstant.MASTER_SENDER_ID));
        } else {
            SimpleMailMessage simpleMailMessage = new SimpleMailMessage();
            simpleMailMessage.setFrom(sendMessageFromEmail);
            simpleMailMessage.setTo(email);
            simpleMailMessage.setSubject("验证你的bot邮箱");
            simpleMailMessage.setText("验证码：" + emailCode);
            javaMailSender.send(simpleMailMessage);
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
}
