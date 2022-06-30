package com.tilitili.bot.entity;

import com.tilitili.common.exception.AssertException;
import com.tilitili.common.utils.Asserts;
import org.apache.commons.lang3.math.NumberUtils;

public class CalculateObject {
	private CalculateObject left;
	private String operate;
	private CalculateObject right;
	private int value;

	public CalculateObject(String initStr) {
		int addIndex = -1;
		int subIndex = -1;
		int mulIndex = -1;
		int divIndex = -1;

		int level = 0;
		int levelCnt = 0;
		char[] charArray = initStr.toCharArray();
		for (int index = 0, charArrayLength = charArray.length; index < charArrayLength; index++) {
			char item = charArray[index];
			if (item == '(') level++;
			else if (item == ')') level--;
			else if (item == '+' && level == 0) addIndex = index;
			else if (item == '-' && level == 0) subIndex = index;
			else if (item == '*' && level == 0) mulIndex = index;
			else if (item == '/' && level == 0) divIndex = index;

			if (level == 0) levelCnt++;
		}

		if (initStr.contains("(") && levelCnt == 1) {
			left = new CalculateObject(initStr.substring(1, initStr.length() - 1));
			operate = "+";
			right = new CalculateObject("0");
			return;
		}

		if (addIndex != -1 || subIndex != -1) {
			Asserts.notEquals(addIndex, 0, "这个加号的左值是什么");
			Asserts.notEquals(addIndex + 1, initStr.length(), "这个加号的右值是什么");
			Asserts.notEquals(subIndex, 0, "这个减号的左值是什么");
			Asserts.notEquals(subIndex + 1, initStr.length(), "这个减号的右值是什么");

			operate = addIndex > subIndex? "+": "-";
			int operateIndex = Math.max(addIndex, subIndex);
			left = new CalculateObject(initStr.substring(0, operateIndex));
			right = new CalculateObject(initStr.substring(operateIndex + 1));
			return;
		}

		if (mulIndex != -1 || divIndex != -1) {
			Asserts.notEquals(mulIndex, 0, "这个乘号的左值是什么");
			Asserts.notEquals(mulIndex + 1, initStr.length(), "这个乘号的右值是什么");
			Asserts.notEquals(divIndex, 0, "这个除号的左值是什么");
			Asserts.notEquals(divIndex + 1, initStr.length(), "这个除号的右值是什么");

			operate = mulIndex > divIndex? "*": "/";
			int operateIndex = Math.max(mulIndex, divIndex);
			left = new CalculateObject(initStr.substring(0, operateIndex));
			right = new CalculateObject(initStr.substring(operateIndex + 1));
			return;
		}

		Asserts.isTrue(NumberUtils.isDigits(initStr), "啊嘞，这个好像不对劲[%s]", initStr);

		value = Integer.parseInt(initStr);
	}

	public int getResult() {
		if (left != null && right != null && operate != null) {
			int leftResult = left.getResult();
			int rightResult = right.getResult();
			switch (operate) {
				case "+": return leftResult + rightResult;
				case "-": return leftResult - rightResult;
				case "*": return leftResult * rightResult;
				case "/": return rightResult == 0? 0: leftResult / rightResult;
				default: throw new AssertException("好像哪里不对劲");
			}
		} else {
			return value;
		}
	}

	public String toString() {
		if (left != null && right != null && operate != null) {
			String leftResult = left.toString();
			String rightResult = right.toString();
			switch (operate) {
				case "+": return "(" + leftResult + "+" + rightResult + ")";
				case "-": return "(" + leftResult + "-" + rightResult + ")";
				case "*": return "(" + leftResult + "*" + rightResult + ")";
				case "/": return "(" + leftResult + "/" + rightResult + ")";
				default: throw new AssertException("好像哪里不对劲");
			}
		} else {
			return String.valueOf(value);
		}
	}
}
