package com.tilitili.bot.entity;

public class CalculationObject {
	private CalculationObject left;
	private String operate;
	private CalculationObject right;

	public CalculationObject(String initStr) {
		int addIndex = initStr.indexOf("+");

//		if (initStr.contains("(")) {
//
//		} else if (addIndex != -1) {
//			operate = "+";
//			left = addIndex==0? 0: new CalculationObject(initStr.substring(0, addIndex));
//			right = new CalculationObject(initStr.substring(addIndex + 1));
//		} else if ()
	}
}
