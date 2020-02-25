package de.upb.cognicryptfix.generator.jimple;

import java.util.HashMap;
import java.util.List;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import soot.Body;
import soot.BooleanType;
import soot.ByteType;
import soot.CharType;
import soot.DoubleType;
import soot.FloatType;
import soot.IntType;
import soot.Local;
import soot.LongType;
import soot.PrimType;
import soot.RefType;
import soot.Scene;
import soot.ShortType;
import soot.SootClass;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.jimple.ArrayRef;
import soot.jimple.IntConstant;
import soot.jimple.Jimple;
import soot.jimple.StaticInvokeExpr;

/**
 * @author Andre Sonntag
 * @date 19.02.2020
 */
public class JimpleArrayGenerator {

	private Body body;
	private JimpleLocalGenerator localGenerator;
	private JimpleAssignGenerator assignGenerator;
	
	public JimpleArrayGenerator(Body body) {
		this.body = body;
		this.localGenerator = new JimpleLocalGenerator(body);
		this.assignGenerator = new JimpleAssignGenerator();
	}
	
	
	public HashMap<Local, List<Unit>> generateArrayUnits(Type type, List<Value> values) {
		
		HashMap<Local, List<Unit>> generatedUnits = Maps.newLinkedHashMap();
		
		Local arrayLocal = localGenerator.generateFreshArrayLocal(type, 1);
		Unit arrayAssign = assignGenerator.generateArrayAssignStmt(arrayLocal, values.size());
				
		generatedUnits.put(arrayLocal, Lists.newArrayList());
		generatedUnits.get(arrayLocal).add(arrayAssign);
		 
		for (int i = 0; i < values.size(); i++) {
			Value index = IntConstant.v(i);
			ArrayRef leftSide = Jimple.v().newArrayRef(arrayLocal, index);
			Value rightSide = generateArrayUnit(values.get(i), generatedUnits.get(arrayLocal));
			Unit parameterToArrayAssign = assignGenerator.generateAssignStmt(leftSide, rightSide);
			generatedUnits.get(arrayLocal).add(parameterToArrayAssign);
		}
		return generatedUnits;
	}


	private Value generateArrayUnit(Value value, List<Unit> generated) {
		if (value.getType() instanceof PrimType) {

			if (value.getType() instanceof BooleanType) {
				Local booleanLocal = localGenerator.generateFreshLocal(RefType.v("java.lang.Boolean"));
				SootClass sootClass = Scene.v().getSootClass("java.lang.Boolean");
				SootMethod valueOfMethod = sootClass.getMethod("java.lang.Boolean valueOf(boolean)");
				StaticInvokeExpr staticInvokeExpr = Jimple.v().newStaticInvokeExpr(valueOfMethod.makeRef(), value);

				Unit newAssignStmt = Jimple.v().newAssignStmt(booleanLocal, staticInvokeExpr);
				generated.add(newAssignStmt);
				return booleanLocal;
			} else if (value.getType() instanceof ByteType) {
				Local byteLocal = localGenerator.generateFreshLocal(RefType.v("java.lang.Byte"));
				SootClass sootClass = Scene.v().getSootClass("java.lang.Byte");
				SootMethod valueOfMethod = sootClass.getMethod("java.lang.Byte valueOf(byte)");
				StaticInvokeExpr staticInvokeExpr = Jimple.v().newStaticInvokeExpr(valueOfMethod.makeRef(), value);

				Unit newAssignStmt = Jimple.v().newAssignStmt(byteLocal, staticInvokeExpr);
				generated.add(newAssignStmt);

				return byteLocal;
			} else if (value.getType() instanceof CharType) {
				Local characterLocal =  localGenerator.generateFreshLocal(RefType.v("java.lang.Character"));
				SootClass sootClass = Scene.v().getSootClass("java.lang.Character");
				SootMethod valueOfMethod = sootClass.getMethod("java.lang.Character valueOf(char)");
				StaticInvokeExpr staticInvokeExpr = Jimple.v().newStaticInvokeExpr(valueOfMethod.makeRef(), value);

				Unit newAssignStmt = Jimple.v().newAssignStmt(characterLocal, staticInvokeExpr);
				generated.add(newAssignStmt);

				return characterLocal;
			} else if (value.getType() instanceof DoubleType) {
				Local doubleLocal =  localGenerator.generateFreshLocal(RefType.v("java.lang.Double"));
				SootClass sootClass = Scene.v().getSootClass("java.lang.Double");
				SootMethod valueOfMethod = sootClass.getMethod("java.lang.Double valueOf(double)");
				StaticInvokeExpr staticInvokeExpr = Jimple.v().newStaticInvokeExpr(valueOfMethod.makeRef(), value);

				Unit newAssignStmt = Jimple.v().newAssignStmt(doubleLocal, staticInvokeExpr);
				generated.add(newAssignStmt);

				return doubleLocal;
			} else if (value.getType() instanceof FloatType) {
				Local floatLocal =  localGenerator.generateFreshLocal(RefType.v("java.lang.Float"));
				SootClass sootClass = Scene.v().getSootClass("java.lang.Float");
				SootMethod valueOfMethod = sootClass.getMethod("java.lang.Float valueOf(float)");
				StaticInvokeExpr staticInvokeExpr = Jimple.v().newStaticInvokeExpr(valueOfMethod.makeRef(), value);

				Unit newAssignStmt = Jimple.v().newAssignStmt(floatLocal, staticInvokeExpr);
				generated.add(newAssignStmt);

				return floatLocal;
			} else if (value.getType() instanceof IntType) {
				Local integerLocal =  localGenerator.generateFreshLocal(RefType.v("java.lang.Integer"));
				SootClass sootClass = Scene.v().getSootClass("java.lang.Integer");
				SootMethod valueOfMethod = sootClass.getMethod("java.lang.Integer valueOf(int)");
				StaticInvokeExpr staticInvokeExpr = Jimple.v().newStaticInvokeExpr(valueOfMethod.makeRef(), value);

				Unit newAssignStmt = Jimple.v().newAssignStmt(integerLocal, staticInvokeExpr);
				generated.add(newAssignStmt);

				return integerLocal;
			} else if (value.getType() instanceof LongType) {
				Local longLocal =  localGenerator.generateFreshLocal(RefType.v("java.lang.Long"));
				SootClass sootClass = Scene.v().getSootClass("java.lang.Long");
				SootMethod valueOfMethod = sootClass.getMethod("java.lang.Long valueOf(long)");
				StaticInvokeExpr staticInvokeExpr = Jimple.v().newStaticInvokeExpr(valueOfMethod.makeRef(), value);

				Unit newAssignStmt = Jimple.v().newAssignStmt(longLocal, staticInvokeExpr);
				generated.add(newAssignStmt);

				return longLocal;
			} else if (value.getType() instanceof ShortType) {
				Local shortLocal =  localGenerator.generateFreshLocal(RefType.v("java.lang.Short"));
				SootClass sootClass = Scene.v().getSootClass("java.lang.Short");
				SootMethod valueOfMethod = sootClass.getMethod("java.lang.Short valueOf(short)");
				StaticInvokeExpr staticInvokeExpr = Jimple.v().newStaticInvokeExpr(valueOfMethod.makeRef(), value);

				Unit newAssignStmt = Jimple.v().newAssignStmt(shortLocal, staticInvokeExpr);
				generated.add(newAssignStmt);

				return shortLocal;
			} else
				throw new RuntimeException("Ooops, something went all wonky!");
		} else
			return value;
	}
}
