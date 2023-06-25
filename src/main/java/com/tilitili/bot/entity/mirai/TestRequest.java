package com.tilitili.bot.entity.mirai;

import com.tilitili.common.entity.dto.BaseDTO;
import lombok.Data;

import java.util.List;

@Data
public class TestRequest extends BaseDTO {
    List<String> title;
}
