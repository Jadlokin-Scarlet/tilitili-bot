package com.tilitili.bot.entity;

import com.tilitili.common.entity.dto.BaseDTO;
import com.tilitili.common.utils.Asserts;

public class Fraction extends BaseDTO {
	// 分子
	private final int molecule;
	// 分母
	private final int denominator;

	public Fraction(int integer) {
		this(integer, 1);
	}

	public Fraction(int molecule, int denominator) {
		Asserts.notEquals(denominator, 0, "不能除0啦");
		int gcd = this.gcd(molecule, denominator);
		int abs = denominator < 0? -1: 1;
		this.molecule = molecule * abs / gcd;
		this.denominator = denominator * abs / gcd;
	}

	public Fraction add(Fraction other) {
		return new Fraction(molecule * other.denominator + denominator * other.molecule, denominator * other.denominator);
	}

	public Fraction sub(Fraction other) {
		return new Fraction(molecule * other.denominator - denominator * other.molecule, denominator * other.denominator);
	}

	public Fraction mul(Fraction other) {
		return new Fraction(molecule * other.molecule, denominator * other.denominator);
	}

	public Fraction div(Fraction other) {
		return new Fraction(molecule * other.denominator, denominator * other.molecule);
	}

	public Fraction negate() {
		return new Fraction(-molecule, denominator);
	}

	public boolean isInteger() {
		return denominator == 1;
	}

	public int getInteger() {
		Asserts.isTrue(this.isInteger(), "啊嘞，好像便乘小数了");
		return molecule;
	}

	private int gcd(int a, int b) {
		a = Math.abs(a);
		b = Math.abs(b);
		return _gcd(a, b);
	}

	private int _gcd(int a, int b) {
		return a % b == 0? b : _gcd(b, a % b);
	}
}
