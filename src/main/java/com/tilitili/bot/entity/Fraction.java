package com.tilitili.bot.entity;

public class Fraction {
	// 分子
	private final int molecule;
	// 分母
	private final int denominator;

	public Fraction(int molecule, int denominator) {
		int gcd = this.gcd(molecule, denominator);
		this.molecule = molecule / gcd;
		this.denominator = denominator / gcd;
	}

	public Fraction add(Fraction other) {
		return new Fraction(molecule * other.denominator + denominator * other.molecule, denominator * other.denominator);
	}

//	public Fraction sub()

	private int gcd(int a, int b) {
		a = Math.abs(a);
		b = Math.abs(b);
		if (b == 0) {
			return a;
		}
		return _gcd(a, b);
	}

	private int _gcd(int a, int b) {
		return a % b == 0? b : _gcd(b, a % b);
	}
}
