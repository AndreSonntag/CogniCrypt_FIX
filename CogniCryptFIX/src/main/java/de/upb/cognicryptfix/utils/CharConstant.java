package de.upb.cognicryptfix.utils;

import soot.PrimType;
import soot.RefType;
import soot.Scene;
import soot.Type;
import soot.jimple.Constant;
import soot.jimple.ConstantSwitch;
import soot.jimple.StringConstant;
import soot.util.StringTools;
import soot.util.Switch;

public class CharConstant extends Constant{

	public final char value;

	  private CharConstant(int c) {
	    this.value = (char) c;
	  }

	  public static CharConstant v(int c) {
	    return new CharConstant(c);
	  }

	  // In this case, equals should be structural equality.
	  public boolean equals(Object c) {
	    return (c instanceof CharConstant && ((CharConstant) c).value == (this.value));
	  }
	  
	  public String toString() {
		   return value+"";
	  }
	  
	  public Type getType() {
	    return Scene.v().getType("char");
	  }

	@Override
	public void apply(Switch sw) {
		// TODO Auto-generated method stub
		
	}

}
