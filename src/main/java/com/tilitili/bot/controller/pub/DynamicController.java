package com.tilitili.bot.controller.pub;

import com.tilitili.bot.controller.BaseController;
import com.tilitili.common.constant.SubscriptionConstant;
import com.tilitili.common.entity.Subscription;
import com.tilitili.common.entity.SubscriptionDynamic;
import com.tilitili.common.entity.SubscriptionUser;
import com.tilitili.common.entity.dto.SubscriptionDynamicDTO;
import com.tilitili.common.entity.query.SubscriptionQuery;
import com.tilitili.common.entity.view.BaseModel;
import com.tilitili.common.exception.AssertException;
import com.tilitili.common.manager.BilibiliManager;
import com.tilitili.common.manager.DynamicManager;
import com.tilitili.common.mapper.mysql.SubscriptionDynamicMapper;
import com.tilitili.common.mapper.mysql.SubscriptionMapper;
import com.tilitili.common.mapper.mysql.SubscriptionUserMapper;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.net.URI;
import java.util.List;

@Controller
@RequestMapping("/api/pub/dynamic")
public class DynamicController extends BaseController {
    private final SubscriptionDynamicMapper subscriptionDynamicMapper;
    private final SubscriptionUserMapper subscriptionUserMapper;
    private final DynamicManager dynamicManager;
    private final SubscriptionMapper subscriptionMapper;
    private final BilibiliManager bilibiliManager;

    public DynamicController(SubscriptionDynamicMapper subscriptionDynamicMapper, SubscriptionUserMapper subscriptionUserMapper, DynamicManager dynamicManager, SubscriptionMapper subscriptionMapper, BilibiliManager bilibiliManager) {
        this.subscriptionDynamicMapper = subscriptionDynamicMapper;
        this.subscriptionUserMapper = subscriptionUserMapper;
        this.dynamicManager = dynamicManager;
        this.subscriptionMapper = subscriptionMapper;
        this.bilibiliManager = bilibiliManager;
    }

    @ResponseBody
    @GetMapping("/{type}/{externalId}")
    public BaseModel<SubscriptionDynamicDTO> getTweetByExternalId(@PathVariable String externalId, @PathVariable Integer type) {
        Asserts.notNull(type, "参数有误");
        Asserts.notBlank(externalId, "参数有误");

        SubscriptionDynamic dynamic = subscriptionDynamicMapper.getSubscriptionDynamicByTypeAndExternalId(type, externalId);
        Asserts.notNull(dynamic, "参数有误");
        this.uploadImage(dynamic);
        SubscriptionDynamicDTO dynamicDTO = new SubscriptionDynamicDTO(dynamic);

        // can be null
        SubscriptionUser subscriptionUser = subscriptionUserMapper.getSubscriptionUserByTypeAndExternalId(type, dynamic.getExternalUserId());
        dynamicDTO.setUser(subscriptionUser);

        if (dynamic.getQuoteId() != null) {
            SubscriptionDynamic quoteDynamic = subscriptionDynamicMapper.getSubscriptionDynamicByTypeAndExternalId(type, dynamic.getQuoteId());
            this.uploadImage(quoteDynamic);
            SubscriptionDynamicDTO quoteDynamicDTO = new SubscriptionDynamicDTO(quoteDynamic);
            dynamicDTO.setQuote(quoteDynamicDTO);

            // can be null
            SubscriptionUser quoteUser = subscriptionUserMapper.getSubscriptionUserByTypeAndExternalId(type, quoteDynamic.getExternalUserId());
            dynamicDTO.getQuote().setUser(quoteUser);
        }

        return BaseModel.success(dynamicDTO);
    }

    @GetMapping("/jump/{type}/{externalId}")
    public ResponseEntity<Void> jumpToSource(@PathVariable String externalId, @PathVariable Integer type) {
        Asserts.notNull(type, "参数有误");
        Asserts.notBlank(externalId, "参数有误");
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(this.getJumpUrl(externalId, type)));
        return new ResponseEntity<>(headers, HttpStatus.SEE_OTHER);
    }

    private String getJumpUrl(String externalId, Integer type) {
        if (SubscriptionConstant.TYPE_BILIBILI_LIVE_ROOM == type) {
            List<Subscription> subscriptionList = subscriptionMapper.getSubscriptionByCondition(new SubscriptionQuery().setType(type).setValue(externalId).setStatus(0));
            Asserts.checkEquals(subscriptionList.size(), 1, "找不到关注");
            return bilibiliManager.getRoomUrlByRoomId(subscriptionList.get(0).getRoomId());
        }

        SubscriptionDynamic dynamic = subscriptionDynamicMapper.getSubscriptionDynamicByTypeAndExternalId(type, externalId);
        if (dynamic.getQuoteId() != null) {
            SubscriptionDynamic quoteDynamic = subscriptionDynamicMapper.getSubscriptionDynamicByTypeAndExternalId(type, dynamic.getQuoteId());
            if (StringUtils.isNotBlank(quoteDynamic.getShareUrl())) {
                return quoteDynamic.getShareUrl();
            }
        }
        if (StringUtils.isNotBlank(dynamic.getShareUrl())) {
            return dynamic.getShareUrl();
        }
        throw new AssertException("找不到关注");
    }

    private void uploadImage(SubscriptionDynamic dynamic) {
        dynamic.setShareImage(dynamicManager.uploadShareImage(dynamic));
        dynamic.setPicList(dynamicManager.uploadPicList(dynamic));
    }
}
