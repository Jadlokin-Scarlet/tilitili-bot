package com.tilitili.bot.controller.pub;

import com.tilitili.bot.controller.BaseController;
import com.tilitili.common.entity.SubscriptionDynamic;
import com.tilitili.common.entity.SubscriptionUser;
import com.tilitili.common.entity.dto.SubscriptionDynamicDTO;
import com.tilitili.common.entity.view.BaseModel;
import com.tilitili.common.manager.DynamicManager;
import com.tilitili.common.mapper.mysql.SubscriptionDynamicMapper;
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

@Controller
@RequestMapping("/api/pub/dynamic")
public class DynamicController extends BaseController {
    private final SubscriptionDynamicMapper subscriptionDynamicMapper;
    private final SubscriptionUserMapper subscriptionUserMapper;
    private final DynamicManager dynamicManager;

    public DynamicController(SubscriptionDynamicMapper subscriptionDynamicMapper, SubscriptionUserMapper subscriptionUserMapper, DynamicManager dynamicManager) {
        this.subscriptionDynamicMapper = subscriptionDynamicMapper;
        this.subscriptionUserMapper = subscriptionUserMapper;
        this.dynamicManager = dynamicManager;
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

        SubscriptionDynamic dynamic = subscriptionDynamicMapper.getSubscriptionDynamicByTypeAndExternalId(type, externalId);
        if (dynamic.getQuoteId() != null) {
            SubscriptionDynamic quoteDynamic = subscriptionDynamicMapper.getSubscriptionDynamicByTypeAndExternalId(type, dynamic.getQuoteId());
            if (StringUtils.isNotBlank(quoteDynamic.getShareUrl())) {
                headers.setLocation(URI.create(quoteDynamic.getShareUrl()));
            }
        }
        if (StringUtils.isNotBlank(dynamic.getShareUrl())) {
            headers.setLocation(URI.create(dynamic.getShareUrl()));
        }
        return new ResponseEntity<>(headers, HttpStatus.SEE_OTHER);
    }

    private void uploadImage(SubscriptionDynamic dynamic) {
        dynamic.setShareImage(dynamicManager.uploadShareImage(dynamic));
        dynamic.setPicList(dynamicManager.uploadPicList(dynamic));
    }
}
