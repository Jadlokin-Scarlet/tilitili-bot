package com.tilitili.bot.util;

import com.tilitili.bot.StartApplication;
import com.tilitili.bot.entity.ExcelResult;
import com.tilitili.bot.entity.RandomTalkDTO;
import com.tilitili.common.utils.Gsons;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(classes = StartApplication.class)
public class ExcelUtilTest {

	@Test
	public void getListFromExcel() {
		File file = new File("/Users/admin/Documents/随机对话模板-测试.xlsx");
		ExcelResult<RandomTalkDTO> excelResult = ExcelUtil.getListFromExcel(file, RandomTalkDTO.class);
		System.out.println(Gsons.toJson(excelResult));
	}
}