package com.tilitili.bot.service.mirai.calendar;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageHandle;
import com.tilitili.common.emnus.SendTypeEmum;
import com.tilitili.common.entity.BotCalendar;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.mapper.mysql.BotCalendarMapper;
import com.tilitili.common.utils.AESUtils;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import static com.tilitili.common.utils.DateUtils.setDayOfWeekToCalendar;
import static com.tilitili.common.utils.StringUtils.convertCnNumber;
import static org.apache.logging.log4j.util.Strings.isBlank;
import static org.apache.logging.log4j.util.Strings.isNotBlank;

@Slf4j
@Component
public class CalendarHandle extends ExceptionRespMessageHandle {
    private final BotCalendarMapper botCalendarMapper;

    @Autowired
    public CalendarHandle(BotCalendarMapper botCalendarMapper) {
        this.botCalendarMapper = botCalendarMapper;
    }

	@Override
    public BotMessage handleMessage(BotMessageAction messageAction) {
        String body = messageAction.getBody();
        BotMessage botMessage = messageAction.getBotMessage();
        List<Long> atList = messageAction.getAtList();
        String sendType = botMessage.getSendType();
        Long qq = botMessage.getQq();
        Long group = botMessage.getGroup();
        Long guildId = botMessage.getGuildId();
        Long channelId = botMessage.getChannelId();

        Asserts.notBlank(body, "格式错啦(正文)");
        body = convertCnNumber(body);
        // TODO: 晚上六点五十叫我
        List<String> pattenList = StringUtils.extractList("^(明天|今天|后天|大后天|周(?:\\d|日)|下周(?:\\d|日)|下下周(?:\\d|日)|\\d+号|\\d+天后)?" +
                "(凌晨|早上|早晨|今早|上午|中午|白天|下午|晚上|今晚)?" +
                "(\\d+点||\\d+小时后)?" +
                "(半|1刻|3刻|\\d+时|\\d+分|\\d+分钟后)?" +
                "(叫我|提醒)" +
                "(.*)", body);
        Asserts.isTrue(pattenList.size() > 1, "格式错啦()");
        String day = pattenList.get(0);
        String a = pattenList.get(1);
        String hour = pattenList.get(2);
        String minute = pattenList.get(3);
        String somebody = pattenList.get(4);
        String something = pattenList.get(5);

        Asserts.notBlank(somebody, "怪怪的");

        log.info("day={} a={} hour={} minute={} somebody={} something={}", day, a, hour, minute, somebody, something);
        boolean isAtOther = Objects.equals(somebody, "提醒");
        List<Long> respAtList = new ArrayList<>();
        if (isAtOther) {
            Asserts.notEmpty(atList, "提醒的话就要at要提醒的人哦");
            respAtList.addAll(atList);
        } else if (sendType.equals(SendTypeEmum.GROUP_MESSAGE_STR)) {
            Asserts.notNull(qq, "怪怪的");
            respAtList.add(qq);
        }

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        if (isNotBlank(day)) {
            setDayToCalendar(calendar, day);
        }
        if (isNotBlank(a) || isNotBlank(hour)) {
            setHourToCalendar(calendar, a, hour);
        } else if (isBlank(minute)) {
            calendar.set(Calendar.HOUR_OF_DAY, 14);
        }
        if (isNotBlank(minute)) {
            setMinuteToCalendar(calendar, minute);
        } else {
            calendar.set(Calendar.MINUTE, 0);
        }
        calendar.set(Calendar.SECOND, 0);

        String atListStr = respAtList.isEmpty()? null: respAtList.stream().map(String::valueOf).collect(Collectors.joining(","));
        BotCalendar botCalendar = new BotCalendar().setSendTime(calendar.getTime()).setText(AESUtils.encrypt(something)).setSendGroup(group).setSendQq(qq).setSendType(sendType).setSendGuild(guildId).setSendChannel(channelId).setAtList(atListStr);
        botCalendarMapper.addBotCalendarSelective(botCalendar);

        SimpleDateFormat sdf = new SimpleDateFormat("MM月dd日 HH时mm分");
        String reply = String.format("知道啦！%s（%s），日程码%s。", body.replaceAll("我", "你").replace("提醒", isAtOther?"提醒他们":"提醒"), sdf.format(calendar.getTime()), botCalendar.getId());
        return BotMessage.simpleTextMessage(reply);
    }
    // (明天|今天|后天|大后天|周(?\d|日)|下周(?\d|日)|下下周(?\d|日)|\d+号)
    private void setDayToCalendar(Calendar calendar, String day) {
        int dayOfWeek = day.contains("周")? (Objects.equals(day.split("周")[1], "日") ? 7: Integer.parseInt(day.split("周")[1])): 0;
        switch (day.replaceAll("\\d|日", "")) {
            case "今天": break;
            case "明天": calendar.add(Calendar.DAY_OF_MONTH, 1); break;
            case "后天": calendar.add(Calendar.DAY_OF_MONTH, 2); break;
            case "大后天": calendar.add(Calendar.DAY_OF_MONTH, 3); break;
            case "周": setDayOfWeekToCalendar(calendar, dayOfWeek); break;
            case "下周": calendar.add(Calendar.WEEK_OF_MONTH, 1); setDayOfWeekToCalendar(calendar, dayOfWeek); break;
            case "下下周": calendar.add(Calendar.WEEK_OF_MONTH, 2); setDayOfWeekToCalendar(calendar, dayOfWeek); break;
            case "号": calendar.set(Calendar.DAY_OF_MONTH, Integer.parseInt(day.split("号")[0])); break;
            case "天后": calendar.add(Calendar.DAY_OF_MONTH, Integer.parseInt(day.split("天后")[0])); break;
            default: break;
        }
    }

    // (凌晨|早上|上午|中午|下午|晚上)?(\d+点||d+小时后)?
    private void setHourToCalendar(Calendar calendar, String a, String hourStr) {
        if (hourStr.contains("小时后")) {
            calendar.set(Calendar.HOUR_OF_DAY, calendar.get(Calendar.HOUR_OF_DAY) + Integer.parseInt(hourStr.split("小时后")[0]));
            return;
        }

        boolean hasHour = isNotBlank(hourStr);
        int hour = hasHour? Integer.parseInt(hourStr.split("点")[0]) : 0;
        switch (a) {
            case "凌晨": calendar.set(Calendar.HOUR_OF_DAY, hasHour? hour: 4); break;
            case "早上": case "上午": case "早晨": case "今早": calendar.set(Calendar.HOUR_OF_DAY, hasHour? hour: 10); break;
            case "中午": case "白天": calendar.set(Calendar.HOUR_OF_DAY, hasHour? hour : 14); break;
            case "下午": calendar.set(Calendar.HOUR_OF_DAY, hasHour? (12 + hour % 12) : 16); break;
            case "晚上": case "今晚": calendar.set(Calendar.HOUR_OF_DAY, hasHour? (12 + hour % 12) : 20); break;
            default: break;
        }
    }

    // (半|1刻|3刻|\d+时)?
    private void setMinuteToCalendar(Calendar calendar, String minute) {
        switch (minute.replaceAll("\\d+", "")) {
            case "半": calendar.set(Calendar.MINUTE, 30); break;
            case "刻": calendar.set(Calendar.MINUTE, 15 * Integer.parseInt(minute.split("刻")[0])); break;
            case "分": case "时": calendar.set(Calendar.MINUTE, Integer.parseInt(minute.split("[时分]")[0])); break;
            case "分钟后": calendar.add(Calendar.MINUTE, Integer.parseInt(minute.split("分钟后")[0])); break;
        }
    }

}
