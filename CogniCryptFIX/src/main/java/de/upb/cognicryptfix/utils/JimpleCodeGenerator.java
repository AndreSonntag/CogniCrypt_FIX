package de.upb.cognicryptfix.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import soot.ArrayType;
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
import soot.SootMethodRef;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.ValueBox;
import soot.javaToJimple.LocalGenerator;
import soot.jimple.ArrayRef;
import soot.jimple.AssignStmt;
import soot.jimple.Constant;
import soot.jimple.DoubleConstant;
import soot.jimple.IntConstant;
import soot.jimple.InvokeStmt;
import soot.jimple.Jimple;
import soot.jimple.LongConstant;
import soot.jimple.NewArrayExpr;
import soot.jimple.NullConstant;
import soot.jimple.SpecialInvokeExpr;
import soot.jimple.StaticInvokeExpr;
import soot.jimple.StringConstant;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.internal.JInvokeStmt;

public class JimpleCodeGenerator {

		
	/**
	 * <p>Generates a fresh {@link Local} variable from a certain {@link Type} to the {@link Body}.</p>
	 * @param body The body in which the variable is generated.
	 * @param type Type of the variable.
	 * @return Returns a fresh generated variable.
	 */
	public static Local generateFreshLocal(Body body, Type type) {
		LocalGenerator lg = new LocalGenerator(body);
		return lg.generateLocal(type);
	}

	/**
	 * <p>Generates a fresh {@link Local} variable from a certain {@link Type} to the {@link Body}.</p>
	 * 
	 * @param body The body in which the variable is generated.
	 * @param type Type of the variable.
	 * @param name Name of the variable.
	 * @return Returns a fresh generated variable.
	 */
	public static Local generateFreshLocal(Body body, Type type, String name) {
		Local l = generateFreshLocal(body, type);
		l.setName(name);
		return l;
	}
	
	
	public static Local genereateFreshArrayLocal(Body body, Type type, String name, int dimension) {
		Local l =  JimpleCodeGenerator.generateFreshLocal(body,JimpleCodeGenerator.getParameterArrayType(type,1));
		l.setName(name);
		return l;
	}

	
	public static Local genereateFreshArrayLocal(Body body, Type type, int dimension) {
		return JimpleCodeGenerator.generateFreshLocal(body,JimpleCodeGenerator.getParameterArrayType(type,1));
	}
	

	/**
	 * <p>Looks for a {@link Local} variable in a {@link Body}. <code>null</code> is returned if the variable is not existing.</p>
	 * @param body The body in which the variable should be available.
	 * @param name Name of the variable.
	 * @return Returns the {@link Local} variable or <code>null</code> if the variable is not existing.
	 */
	public static Local getLocalByName(Body body, String name) {
		Local local = body.getLocals().stream()
			    .filter(x -> name.equals(x.getName()))
			    .findAny()                                     
	            .orElse(null); 
		return local;
	}
	
	/**
	 * <p>Generates a {@link JInvokeStmt} of a {@link Local}.</p>
	 * <p>i.e. specialinvoke $r0.&lt;javax.crypto.spec.PBEKeySpec: void &lt;init&gt;(char[])&gt;();</p>
	 * @param var The variable that executes the method call.
	 * @param method The method to be called
	 * @param args Arguments for the method call
	 * @return Returns the {@link JInvokeStmt}
	 */
	public static Unit generateJInvokeStmt(Local var, SootMethod method, Local... args) {
		SootMethodRef ref = method.makeRef();
		SpecialInvokeExpr invokeExpr = Jimple.v().newSpecialInvokeExpr(var, ref, Arrays.asList(args));
		InvokeStmt invoke =  Jimple.v().newInvokeStmt(invokeExpr);
		return invoke;
	}
	
	/**
	 * <p>Generates a {@link AssignStmt} for the {@link RefType} of a {@link Local} variable.</p>
	 * <p>i.e. $r0 = new javax.crypto.spec.PBEKeySpec;</p>
	 * @param var The variable that gets assigned.
	 * @param type Referenz type of the variable.
	 * @return Returns the {@link AssignStmt} for the variable.
	 */
	public static Unit generateTypeAssignStmt(Local var, RefType type) {
		Unit typeAssignStmt = Jimple.v().newAssignStmt(var, Jimple.v().newNewExpr(type));
		return typeAssignStmt;
	}

	/**
	 * <p>Generates a {@link AssignStmt} for two {@link Local} variables.</p>
	 * <p>i.e. // myVar = $r0;</p> 
	 * @param left The left variable
	 * @param right The right variable
	 * @return Returns the {@link AssignStmt} for the two variables.
	 */
	public static Unit generateAssignStmt(Local left, Local right) {
		Unit assignStmt = Jimple.v().newAssignStmt(left, right);
		return assignStmt;
	}
	
	public static Unit generateAssignStmt(Local var, Value val) {
		Unit assignStmt = Jimple.v().newAssignStmt(var, val);
		return assignStmt;
	}
	
	/**
	 * <p>Returns the {@link Type} for a Array</p>
	 * @param type The type of the Array. i.e. char
	 * @param dimension Dimension of the Array
	 * @return Returns Array type.
	 */
	private static Type getParameterArrayType(Type type, int dimension){
		Type parameterArray = ArrayType.v(type, 1);
		return parameterArray;
	}
	
	
	public static Unit generateArrayAssignStmt(Local arrayLocal, Type localType, int arrayDimension) {
		NewArrayExpr arrayExpr = Jimple.v().newNewArrayExpr(localType, IntConstant.v(arrayDimension));
		Unit assignStmt = Jimple.v().newAssignStmt(arrayLocal, arrayExpr);
		return assignStmt;
	}
	

    public static Constant generateConstantValue(Object object) {
        if (object == null) {
            return NullConstant.v();
        } else if (object instanceof Boolean) {
            Boolean flag = (Boolean) object;
            if (flag.booleanValue()) {
                return IntConstant.v(1);
            } else {
                return IntConstant.v(0);
            }
        } else if (object instanceof Integer) {
            return IntConstant.v(((Integer) object).intValue());
        } else if (object instanceof Long) {
            return LongConstant.v(((Long) object).longValue());
        } else if (object instanceof String) {
            return StringConstant.v((String) object);
        } else if (object instanceof Double) {
            return DoubleConstant.v(((Double) object).doubleValue());
        } else {
            throw new RuntimeException("unrecognized constant value = "
                    + object);
        }
    }
	
	//TODO:
	public static HashMap<Value, List<Unit>> generateParameterArray(Body body, List<Value> parameterList){
		List<Unit> generated = new ArrayList<Unit>();
		NewArrayExpr arrayExpr = Jimple.v().newNewArrayExpr(CharType.v(), IntConstant.v(parameterList.size()));
		
		Value newArrayLocal = JimpleCodeGenerator.generateFreshLocal(body, JimpleCodeGenerator.getParameterArrayType(CharType.v(),1));
		Unit newAssignStmt = Jimple.v().newAssignStmt(newArrayLocal, arrayExpr);
		generated.add(newAssignStmt);
		
		for(int i = 0; i < parameterList.size(); i++){
			Value index = IntConstant.v(i);
			ArrayRef leftSide = Jimple.v().newArrayRef(newArrayLocal, index);
			Value rightSide = JimpleCodeGenerator.generateCorrectObject(body, parameterList.get(i), generated);
			
			Unit parameterInArray = Jimple.v().newAssignStmt(leftSide, rightSide);
			generated.add(parameterInArray);
		}

		HashMap<Value, List<Unit>> units = new HashMap<Value, List<Unit>>();
		units.put(newArrayLocal, generated);
		return units;
	}
	
	//TODO:
	public static Value generateCorrectObject(Body body, Value value, List<Unit> generated){
		if(value.getType() instanceof PrimType){
			//in case of a primitive type, we use boxing (I know it is not nice, but it works...) in order to use the Object type
			if(value.getType() instanceof BooleanType){
				Local booleanLocal = generateFreshLocal(body, RefType.v("java.lang.Boolean"));
				
				SootClass sootClass = Scene.v().getSootClass("java.lang.Boolean");
				SootMethod valueOfMethod = sootClass.getMethod("java.lang.Boolean valueOf(boolean)");
				StaticInvokeExpr staticInvokeExpr = Jimple.v().newStaticInvokeExpr(valueOfMethod.makeRef(), value);
				
				Unit newAssignStmt = Jimple.v().newAssignStmt(booleanLocal, staticInvokeExpr);
				generated.add(newAssignStmt);
				
				return booleanLocal;
			}
			else if(value.getType() instanceof ByteType){
				Local byteLocal = generateFreshLocal(body, RefType.v("java.lang.Byte"));
				
				SootClass sootClass = Scene.v().getSootClass("java.lang.Byte");
				SootMethod valueOfMethod = sootClass.getMethod("java.lang.Byte valueOf(byte)");
				StaticInvokeExpr staticInvokeExpr = Jimple.v().newStaticInvokeExpr(valueOfMethod.makeRef(), value);
				
				Unit newAssignStmt = Jimple.v().newAssignStmt(byteLocal, staticInvokeExpr);
				generated.add(newAssignStmt);
				
				return byteLocal;
			}
			else if(value.getType() instanceof CharType){
				Local characterLocal = generateFreshLocal(body, RefType.v("java.lang.Character"));
				
				SootClass sootClass = Scene.v().getSootClass("java.lang.Character");
				SootMethod valueOfMethod = sootClass.getMethod("java.lang.Character valueOf(char)");
				StaticInvokeExpr staticInvokeExpr = Jimple.v().newStaticInvokeExpr(valueOfMethod.makeRef(), value);
				
				Unit newAssignStmt = Jimple.v().newAssignStmt(characterLocal, staticInvokeExpr);
				generated.add(newAssignStmt); 
				
				return characterLocal;
			}
			else if(value.getType() instanceof DoubleType){
				Local doubleLocal = generateFreshLocal(body, RefType.v("java.lang.Double"));
				
				SootClass sootClass = Scene.v().getSootClass("java.lang.Double");
				SootMethod valueOfMethod = sootClass.getMethod("java.lang.Double valueOf(double)");
																
				StaticInvokeExpr staticInvokeExpr = Jimple.v().newStaticInvokeExpr(valueOfMethod.makeRef(), value);
				
				Unit newAssignStmt = Jimple.v().newAssignStmt(doubleLocal, staticInvokeExpr);
				generated.add(newAssignStmt); 
				
				return doubleLocal;
			}
			else if(value.getType() instanceof FloatType){
				Local floatLocal = generateFreshLocal(body, RefType.v("java.lang.Float"));
				
				SootClass sootClass = Scene.v().getSootClass("java.lang.Float");
				SootMethod valueOfMethod = sootClass.getMethod("java.lang.Float valueOf(float)");
				StaticInvokeExpr staticInvokeExpr = Jimple.v().newStaticInvokeExpr(valueOfMethod.makeRef(), value);
				
				Unit newAssignStmt = Jimple.v().newAssignStmt(floatLocal, staticInvokeExpr);
				generated.add(newAssignStmt); 
				
				return floatLocal;
			}
			else if(value.getType() instanceof IntType){
				Local integerLocal = generateFreshLocal(body, RefType.v("java.lang.Integer"));
				
				SootClass sootClass = Scene.v().getSootClass("java.lang.Integer");
				SootMethod valueOfMethod = sootClass.getMethod("java.lang.Integer valueOf(int)");
				StaticInvokeExpr staticInvokeExpr = Jimple.v().newStaticInvokeExpr(valueOfMethod.makeRef(), value);
				
				Unit newAssignStmt = Jimple.v().newAssignStmt(integerLocal, staticInvokeExpr);
				generated.add(newAssignStmt); 
				
				return integerLocal;
			}
			else if(value.getType() instanceof LongType){
				Local longLocal = generateFreshLocal(body, RefType.v("java.lang.Long"));
				
				SootClass sootClass = Scene.v().getSootClass("java.lang.Long");
				SootMethod valueOfMethod = sootClass.getMethod("java.lang.Long valueOf(long)");
				StaticInvokeExpr staticInvokeExpr = Jimple.v().newStaticInvokeExpr(valueOfMethod.makeRef(), value);
				
				Unit newAssignStmt = Jimple.v().newAssignStmt(longLocal, staticInvokeExpr);
				generated.add(newAssignStmt); 
				
				return longLocal;
			}
			else if(value.getType() instanceof ShortType){
				Local shortLocal = generateFreshLocal(body, RefType.v("java.lang.Short"));
				
				SootClass sootClass = Scene.v().getSootClass("java.lang.Short");
				SootMethod valueOfMethod = sootClass.getMethod("java.lang.Short valueOf(short)");
				StaticInvokeExpr staticInvokeExpr = Jimple.v().newStaticInvokeExpr(valueOfMethod.makeRef(), value);
				
				Unit newAssignStmt = Jimple.v().newAssignStmt(shortLocal, staticInvokeExpr);
				generated.add(newAssignStmt); 
				
				return shortLocal;
			}
			else
				throw new RuntimeException("Ooops, something went all wonky!");
		}
		else
			//just return the value, there is nothing to box
			return value;
	}
	
	
	
//	public static void exampleMethodCallGeneration(Body body) {
//
//		// javax.crypto.spec.PBEKeySpec $r0;
//		RefType localRefType = RefType.v("javax.crypto.spec.PBEKeySpec");
//		Local tempInvokeVar = JimpleCodeGenerator.generateFreshLocal(body, localRefType);
//
//		// $r0 = new javax.crypto.spec.PBEKeySpec;
//		Unit typeAssign = JimpleCodeGenerator.generateTypeAssignStmt(tempInvokeVar, localRefType);
//
//		// specialinvoke $r0.<javax.crypto.spec.PBEKeySpec: void <init>(char[])>()
//		SootMethod pBEKeySpec = Scene.v().getMethod("<javax.crypto.spec.PBEKeySpec: void <init>(char[])>");
//		Unit invoke = JimpleCodeGenerator.generateInvokeStmt(tempInvokeVar, pBEKeySpec);
//
//		// javax.crypto.spec.PBEKeySpec myVar;
//		Local myNamedVar = JimpleCodeGenerator.generateFreshLocal(body, RefType.v("javax.crypto.spec.PBEKeySpec"), "myVar");
//
//		// myVar = $r0;
//		Unit assign = JimpleCodeGenerator.generateAssignStmt(myNamedVar, tempInvokeVar);
//
//		ArrayList<Unit> generationChain = new ArrayList<Unit>();
//		generationChain.add(typeAssign);
//		generationChain.add(invoke);
//		generationChain.add(assign);
//		body.getUnits().addAll(generationChain);
//	}
	
	
}
