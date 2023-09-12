package com.tilitili.bot.util;

import com.tilitili.bot.StartApplication;
import com.tilitili.bot.entity.ExcelResult;
import com.tilitili.bot.entity.FishConfigDTO;
import com.tilitili.common.utils.Gsons;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;

@Slf4j
@SpringBootTest(classes = StartApplication.class)
public class ExcelUtilTest {

	@Test
	public void getListFromExcel() {
		File file = new File("/Users/admin/Downloads/钓鱼奖励配置.xlsx");
		ExcelResult<FishConfigDTO> excelResult = ExcelUtil.getListFromExcel(file, FishConfigDTO.class);
		System.out.println(Gsons.toJson(excelResult));
	}
}