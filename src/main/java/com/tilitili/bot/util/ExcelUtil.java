package com.tilitili.bot.util;

import com.tilitili.bot.annotation.ExcelProperty;
import com.tilitili.bot.entity.ExcelResult;
import com.tilitili.common.exception.AssertException;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class ExcelUtil {

	public static <T> ExcelResult<T> getListFromExcel(File file, Class<T> clazz) {
		try {
			Map<String, Field> fieldMap = new HashMap<>();
			for (Field field : clazz.getDeclaredFields()) {
				field.setAccessible(true);
				ExcelProperty annotation = field.getAnnotation(ExcelProperty.class);
				fieldMap.put(annotation.value(), field);
			}
			ExcelResult<T> result = readExcelFile(file);
			List<List<List<String>>> data = result.getData();
			log.info("data={}", data);
			Asserts.notEmpty(data, "啊嘞，怎么是空的。");
			List<List<String>> firstPage = data.get(0);
			Asserts.notEmpty(firstPage, "啊嘞，怎么是空的。");
			List<String> headList = firstPage.get(0);
			Map<Integer, Field> indexFieldMap = new HashMap<>();
			for (int i = 0, headListSize = headList.size(); i < headListSize; i++) {
				String headName = headList.get(i);
				Field field = fieldMap.get(headName);
				if (field == null) {
					throw new AssertException("啊嘞，表头好像不对("+headName+")");
				}
				indexFieldMap.put(i, field);
			}
			List<List<String>> rowList = firstPage.subList(1, firstPage.size());
			Asserts.notEmpty(rowList, "啊嘞，怎么是空的。");
			List<T> resultList = new ArrayList<>();
			for (List<String> item : rowList) {
				if (item.isEmpty() || item.stream().allMatch(StringUtils::isBlank)) {
					continue;
				}
				T newObject = clazz.getConstructor().newInstance();
				for (int i = 0; i < item.size(); i++) {
					String valueStr = item.get(i).trim();
					Field field = indexFieldMap.get(i);

					if (Modifier.isFinal(field.getModifiers())) {
						continue;//final不做处理
					}
					Class<?> typeClass = field.getType();
					Constructor<?> con = typeClass.getConstructor(valueStr.getClass());//获取有参构造函数
					field.set(newObject, con.newInstance(valueStr));
				}
				resultList.add(newObject);
			}
			result.setResultList(resultList);

			if (data.size() > 1) {
				List<List<String>> configPage = data.get(1);
				Map<String, String> paramMap = new HashMap<>();
				for (List<String> config : configPage) {
					if (config.size() < 2) {
						continue;
					}
					paramMap.put(config.get(0), config.get(1));
				}
				result.setParamMap(paramMap);
			}
			return result;
		} catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
			throw new AssertException("啊嘞，不太对劲。", e);
		}
	}

	private static <T> ExcelResult<T> readExcelFile(File file) {
		ExcelResult<T> result = new ExcelResult<>();
		try (FileInputStream stream = new FileInputStream(file); Workbook workbook = new XSSFWorkbook(stream);) {
			List<List<List<String>>> data = new ArrayList<>();
			for (Sheet sheet : workbook) {
				List<List<String>> page = new ArrayList<>();
				for (Row row : sheet) {
					List<String> item = new ArrayList<>();
					for (Cell cell : row) {
						switch (cell.getCellType()) {
							case STRING: item.add(cell.getRichStringCellValue().getString()); break;
							case NUMERIC: {
								if (DateUtil.isCellDateFormatted(cell)) {
									item.add(cell.getDateCellValue() + "");
								} else {
									item.add(cell.getNumericCellValue() + "");
								}
								break;
							}
							case BOOLEAN: item.add(cell.getBooleanCellValue() + ""); break;
							case FORMULA: item.add(cell.getCellFormula() + ""); break;
							default: item.add("");
						}
					}
					page.add(item);
				}
				data.add(page);
			}
			result.setData(data);
			return result;
		} catch (Exception e) {
			log.error("读取excel文件异常");
			throw new AssertException("啊嘞，文件看不懂。", e);
		}
	}
}
