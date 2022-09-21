package com.tilitili.bot.util;

import com.tilitili.bot.annotation.ExcelProperty;
import com.tilitili.common.exception.AssertException;
import com.tilitili.common.utils.Asserts;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class ExcelUtil {

	public static <T> List<T> getListFromExcel(File file, Class<T> clazz) {
		try {
			Map<String, Field> fieldMap = new HashMap<>();
			for (Field field : clazz.getFields()) {
				field.setAccessible(true);
				ExcelProperty annotation = field.getAnnotation(ExcelProperty.class);
				fieldMap.put(annotation.value(), field);
			}
			List<List<String>> data = readExcelFile(file);
			log.info("data={}", data);
			Asserts.notEmpty(data, "啊嘞，怎么是空的。");
			List<String> headList = data.get(0);
			Map<Integer, Field> indexFieldMap = new HashMap<>();
			for (int i = 0, headListSize = headList.size(); i < headListSize; i++) {
				String headName = headList.get(i);
				indexFieldMap.put(i, fieldMap.get(headName));
			}
			List<List<String>> rowList = data.subList(1, data.size());
			Asserts.notEmpty(rowList, "啊嘞，怎么是空的。");
			for (List<String> item : rowList) {
				T newObject = clazz.getConstructor().newInstance();
				for (int i = 0; i < item.size(); i++) {
					String cell = item.get(i);
//					switch (indexFieldMap.get(i).)
//					indexFieldMap.get(i).set(newObject, );
				}
			}
			return null;
		} catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
			throw new AssertException("啊嘞，不太对劲。");
		}
	}

	private static List<List<String>> readExcelFile(File file) {
		List<List<String>> data = new ArrayList<>();
		try (FileInputStream stream = new FileInputStream(file); Workbook workbook = new XSSFWorkbook(stream);) {
			Sheet sheet = workbook.getSheetAt(0);

			for (Row row : sheet) {
				List<String> item = new ArrayList<>();
				data.add(item);
				for (Cell cell : row) {
					switch (cell.getCellType()) {
						case STRING: cell.getRichStringCellValue().getString(); break;
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
			}
			return data;
		} catch (Exception e) {
			log.error("读取excel文件异常");
			throw new AssertException("啊嘞，文件看不懂。");
		}
	}
}
