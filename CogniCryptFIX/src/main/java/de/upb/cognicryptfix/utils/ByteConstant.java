package de.upb.cognicryptfix.utils;

import soot.ByteType;
import soot.Type;
import soot.jimple.ArithmeticConstant;
import soot.jimple.NumericConstant;
import soot.util.Switch;

public class ByteConstant extends ArithmeticConstant {

	public final int value;

	protected ByteConstant(int value) {
		this.value = value;
	}

	public static ByteConstant v(int value) {
		return new ByteConstant(value);
	}

	public boolean equals(Object c) {
		return c instanceof ByteConstant && ((ByteConstant) c).value == value;
	}

	public int hashCode() {
		return value;
	}

	// PTC 1999/06/28
	public NumericConstant add(NumericConstant c) {
		if (!(c instanceof ByteConstant)) {
			throw new IllegalArgumentException("ByteConstant expected");
		}
		return ByteConstant.v(this.value + ((ByteConstant) c).value);
	}

	public NumericConstant subtract(NumericConstant c) {
		if (!(c instanceof ByteConstant)) {
			throw new IllegalArgumentException("ByteConstant expected");
		}
		return ByteConstant.v(this.value - ((ByteConstant) c).value);
	}

	public NumericConstant multiply(NumericConstant c) {
		if (!(c instanceof ByteConstant)) {
			throw new IllegalArgumentException("ByteConstant expected");
		}
		return ByteConstant.v(this.value * ((ByteConstant) c).value);
	}

	public NumericConstant divide(NumericConstant c) {
		if (!(c instanceof ByteConstant)) {
			throw new IllegalArgumentException("ByteConstant expected");
		}
		return ByteConstant.v(this.value / ((ByteConstant) c).value);
	}

	public NumericConstant remainder(NumericConstant c) {
		if (!(c instanceof ByteConstant)) {
			throw new IllegalArgumentException("ByteConstant expected");
		}
		return ByteConstant.v(this.value % ((ByteConstant) c).value);
	}

	public NumericConstant equalEqual(NumericConstant c) {
		if (!(c instanceof ByteConstant)) {
			throw new IllegalArgumentException("ByteConstant expected");
		}
		return ByteConstant.v((this.value == ((ByteConstant) c).value) ? 1 : 0);
	}

	public NumericConstant notEqual(NumericConstant c) {
		if (!(c instanceof ByteConstant)) {
			throw new IllegalArgumentException("ByteConstant expected");
		}
		return ByteConstant.v((this.value != ((ByteConstant) c).value) ? 1 : 0);
	}

	public NumericConstant lessThan(NumericConstant c) {
		if (!(c instanceof ByteConstant)) {
			throw new IllegalArgumentException("ByteConstant expected");
		}
		return ByteConstant.v((this.value < ((ByteConstant) c).value) ? 1 : 0);
	}

	public NumericConstant lessThanOrEqual(NumericConstant c) {
		if (!(c instanceof ByteConstant)) {
			throw new IllegalArgumentException("ByteConstant expected");
		}
		return ByteConstant.v((this.value <= ((ByteConstant) c).value) ? 1 : 0);
	}

	public NumericConstant greaterThan(NumericConstant c) {
		if (!(c instanceof ByteConstant)) {
			throw new IllegalArgumentException("ByteConstant expected");
		}
		return ByteConstant.v((this.value > ((ByteConstant) c).value) ? 1 : 0);
	}

	public NumericConstant greaterThanOrEqual(NumericConstant c) {
		if (!(c instanceof ByteConstant)) {
			throw new IllegalArgumentException("ByteConstant expected");
		}
		return ByteConstant.v((this.value >= ((ByteConstant) c).value) ? 1 : 0);
	}

	public NumericConstant negate() {
		return ByteConstant.v(-(this.value));
	}

	public ArithmeticConstant and(ArithmeticConstant c) {
		if (!(c instanceof ByteConstant)) {
			throw new IllegalArgumentException("ByteConstant expected");
		}
		return ByteConstant.v(this.value & ((ByteConstant) c).value);
	}

	public ArithmeticConstant or(ArithmeticConstant c) {
		if (!(c instanceof ByteConstant)) {
			throw new IllegalArgumentException("ByteConstant expected");
		}
		return ByteConstant.v(this.value | ((ByteConstant) c).value);
	}

	public ArithmeticConstant xor(ArithmeticConstant c) {
		if (!(c instanceof ByteConstant)) {
			throw new IllegalArgumentException("ByteConstant expected");
		}
		return ByteConstant.v(this.value ^ ((ByteConstant) c).value);
	}

	public ArithmeticConstant shiftLeft(ArithmeticConstant c) {
		if (!(c instanceof ByteConstant)) {
			throw new IllegalArgumentException("ByteConstant expected");
		}
		return ByteConstant.v(this.value << ((ByteConstant) c).value);
	}

	public ArithmeticConstant shiftRight(ArithmeticConstant c) {
		if (!(c instanceof ByteConstant)) {
			throw new IllegalArgumentException("ByteConstant expected");
		}
		return ByteConstant.v(this.value >> ((ByteConstant) c).value);
	}

	public ArithmeticConstant unsignedShiftRight(ArithmeticConstant c) {
		if (!(c instanceof ByteConstant)) {
			throw new IllegalArgumentException("ByteConstant expected");
		}
		return ByteConstant.v(this.value >>> ((ByteConstant) c).value);
	}

	public String toString() {
		return new Byte((byte)value).toString();
	}

	public Type getType() {
		return ByteType.v();
	}

	public void apply(Switch sw) {
//		((ConstantSwitch) sw).caseByteConstant(this);
	}

}
