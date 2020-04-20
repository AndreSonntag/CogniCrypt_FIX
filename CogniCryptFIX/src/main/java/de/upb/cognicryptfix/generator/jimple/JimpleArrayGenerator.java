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

	private JimpleLocalGenerator localGenerator;
	private JimpleAssignGenerator assignGenerator;
	
	public JimpleArrayGenerator(Body body) {
		this.localGenerator = new JimpleLocalGenerator(body);
		this.assignGenerator = new JimpleAssignGenerator();
	}
	
	
	public HashMap<Local, List<Unit>> generateArrayUnits(Local arrayLocal, List<Value> values) {
		
		HashMap<Local, List<Unit>> generatedUnits = Maps.newLinkedHashMap();
		Unit arrayAssign = assignGenerator.generateArrayAssignStmt(arrayLocal, values.size());	
		generatedUnits.put(arrayLocal, Lists.newArrayList());
		generatedUnits.get(arrayLocal).add(arrayAssign);
		 
		for (int i = 0; i < values.size(); i++) {
			Value index = IntConstant.v(i);
			ArrayRef leftSide = Jimple.v().newArrayRef(arrayLocal, index);
			Unit parameterToArrayAssign = assignGenerator.generateAssignStmt(leftSide, values.get(i));
			generatedUnits.get(arrayLocal).add(parameterToArrayAssign);
		}
		return generatedUnits;
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
			Unit parameterToArrayAssign = assignGenerator.generateAssignStmt(leftSide, values.get(i));
			generatedUnits.get(arrayLocal).add(parameterToArrayAssign);
		}
		return generatedUnits;
	}
}
