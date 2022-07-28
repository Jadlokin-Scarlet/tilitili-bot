package com.tilitili.bot.entity;

import com.google.common.collect.ImmutableMap;
import com.tilitili.common.exception.AssertException;
import com.tilitili.common.utils.Asserts;
import org.apache.commons.lang3.math.NumberUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class CalculateObject {
	private CalculateObject left;
	private String operate;
	private CalculateObject right;
	private Fraction value;

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

		value = new Fraction(Integer.parseInt(initStr));
	}

	public int getResult() throws AssertException {
		Fraction result = this._getResult();
		Asserts.isTrue(result.isInteger(), "好像便乘小数惹");
		return result.getInteger();
	}

	private Fraction _getResult() {
		if (left != null && right != null && operate != null) {
			Fraction leftResult = left._getResult();
			Fraction rightResult = right._getResult();
			switch (operate) {
				case "+": return leftResult.add(rightResult);
				case "-": return leftResult.sub(rightResult);
				case "*": return leftResult.mul(rightResult);
				case "/": return leftResult.div(rightResult);
				default: throw new AssertException("好像哪里不对劲");
			}
		} else {
			return value;
		}
	}

	public String toString() {
		return this._toString(null, null);
	}

	private static final String PARENT_OPERATE_TYPE_LEFT = "left";
	private static final String PARENT_OPERATE_TYPE_RIGHT = "right";
	private static final Map<String, Map<String, List<String>>> needProtectOperateMap = ImmutableMap.of(
			"+", ImmutableMap.of(
					PARENT_OPERATE_TYPE_LEFT, Arrays.asList("*", "/"),
					PARENT_OPERATE_TYPE_RIGHT, Arrays.asList("*", "/", "-")
			)
	);
	private static final List<String> addLeftNeedProtectOperate = Arrays.asList("*", "/");
	private static final List<String> addRightNeedProtectOperate = Arrays.asList("*", "/", "-");
	private String _toString(String parentOperate, String type) {
		if (left != null && right != null && operate != null) {
			String leftResult = left._toString(operate, PARENT_OPERATE_TYPE_LEFT);
			String rightResult = right._toString(operate, PARENT_OPERATE_TYPE_RIGHT);
			if (Objects.equals(leftResult, "0")) rightResult = right._toString(parentOperate, type);
			if (Objects.equals(rightResult, "0")) leftResult = left._toString(parentOperate, type);
			switch (operate) {
				case "+": {
					if (Objects.equals(leftResult, "0")) return rightResult;
					if (Objects.equals(rightResult, "0")) return leftResult;
					return this.concatResult(parentOperate, type, leftResult, operate, rightResult);
				}
				case "-": return "(" + leftResult + "-" + rightResult + ")";
				case "*": return this.concatResult(Objects.equals(parentOperate, "/") && Objects.equals(type, PARENT_OPERATE_TYPE_RIGHT), leftResult + "*" + rightResult);
				case "/": return leftResult + "/" + rightResult;
				default: throw new AssertException("好像哪里不对劲");
			}
		} else {
			return String.valueOf(value.getInteger());
		}
	}

	private String concatResult(String parentOperate, String type, String leftResult, String operate, String rightResult) {
		if (parentOperate != null && type != null && needProtectOperateMap.get(operate).get(type).contains(parentOperate)) {
			return "(" + leftResult + operate + rightResult + ")";
		} else {
				return leftResult + operate + rightResult;
		}
	}

	private String concatResult(boolean needProtect, String content) {
		return needProtect? "(" + content + ")": content;
	}
}
