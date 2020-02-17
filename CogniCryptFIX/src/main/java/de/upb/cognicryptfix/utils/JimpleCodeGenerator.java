package de.upb.cognicryptfix.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import soot.ArrayType;
import soot.Body;
import soot.BooleanType;
import soot.ByteType;
import soot.CharType;
import soot.DoubleType;
import soot.FastHierarchy;
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
import soot.Trap;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.javaToJimple.LocalGenerator;
import soot.jimple.ArrayRef;
import soot.jimple.AssignStmt;
import soot.jimple.Constant;
import soot.jimple.DoubleConstant;
import soot.jimple.IdentityStmt;
import soot.jimple.IntConstant;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.Jimple;
import soot.jimple.LongConstant;
import soot.jimple.NewArrayExpr;
import soot.jimple.NullConstant;
import soot.jimple.StaticInvokeExpr;
import soot.jimple.StringConstant;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.util.Chain;

public class JimpleCodeGenerator {

	private static FastHierarchy hierarchy = Scene.v().getOrMakeFastHierarchy();

	/**
	 * <p>
	 * Generates a fresh {@link Local} variable.
	 * </p>
	 * 
	 * @param body The body in which the variable is generated.
	 * @param type Type of the variable.
	 * @return Returns a fresh generated {@link Local} variable.
	 */
	public static Local generateFreshLocal(Body body, Type type) {
		LocalGenerator lg = new LocalGenerator(body);
		return lg.generateLocal(type);
	}

	/**
	 * <p>
	 * Generates a fresh {@link Local} variable with a name.
	 * </p>
	 * 
	 * @param body The body in which the variable is generated.
	 * @param type Type of the variable.
	 * @param name Name of the variable.
	 * @return Returns a fresh generated {@link Local} variable.
	 */
	public static Local generateFreshLocal(Body body, Type type, String name) {
		int i = 1;
		Local existingLocaL = getLocalByName(body, name);
		if (existingLocaL != null) {
			while (true) {
				Local tempLocal = getLocalByName(body, name + "#" + i);
				if (tempLocal != null) {
					i++;
				} else {
					name = name + "#" + i;
					break;
				}
			}
		}

		Local l = generateFreshLocal(body, type);
		if (!name.isEmpty())
			l.setName(name);
		return l;
	}

	/**
	 * <p>
	 * Generates a fresh {@link Local} Array variable.
	 * </p>
	 * 
	 * @param body      The body in which the variable is generated.
	 * @param type      Type of the variable.
	 * @param dimension Dimension of the array.
	 * @return Returns a fresh generated {@link Local} array variable.
	 */
	public static Local genereateFreshArrayLocal(Body body, Type type, int dimension) {
		return generateFreshLocal(body, JimpleCodeGenerator.getParameterArrayType(type, 1));
	}

	/**
	 * <p>
	 * Generates a fresh {@link Local} Array variable with a name.
	 * </p>
	 * 
	 * @param body      The body in which the variable is generated.
	 * @param type      Type of the variable.
	 * @param dimension Dimension of the array.
	 * @param name      Name of the variable.
	 * @return Returns a fresh generated {@link Local} Array variable.
	 */
	public static Local genereateFreshArrayLocal(Body body, Type type, String name, int dimension) {
		Local l = generateFreshLocal(body, JimpleCodeGenerator.getParameterArrayType(type, 1));
		if (!name.isEmpty())
			l.setName(name);
		return l;
	}

	/**
	 * <p>
	 * Looks for a {@link Local} variable in a {@link Body}. <code>null</code> is
	 * returned if the variable is not existing.
	 * </p>
	 * 
	 * @param body The body in which the variable should be available.
	 * @param name Name of the variable.
	 * @return Returns the {@link Local} variable or <code>null</code> if the
	 *         variable is not existing.
	 */
	public static Local getLocalByName(Body body, String name) {
		Local local = body.getLocals().stream().filter(x -> name.equals(x.getName())).findAny().orElse(null);
		return local;
	}

	/**
	 * <p>
	 * Converts a {@link Type} to an Array type
	 * </p>
	 * 
	 * @param type      The type of the Array. i.e. char
	 * @param dimension Dimension of the Array.
	 * @return Returns Array type.
	 */
	private static Type getParameterArrayType(Type type, int dimension) {
		Type parameterArray = ArrayType.v(type, 1);
		return parameterArray;
	}

	/**
	 * <p>
	 * Generates a {@link InvokeStmt} as {@link Unit} object.
	 * </p>
	 * <p>
	 * i.e. specialinvoke $r0.&lt;javax.crypto.spec.PBEKeySpec: void
	 * &lt;init&gt;(char[])&gt;();
	 * </p>
	 * 
	 * @param var    The variable that executes the method call.
	 * @param method The method to be called.
	 * @param args   Arguments for the method call.
	 * @return Returns the new generated invocation statement.
	 */
	public static Unit generateInvokeStmt(Local var, SootMethod method, Value... args) {
		SootMethodRef ref = method.makeRef();
		InvokeExpr invokeExpr = null;

		// TODO: virtual invoke normal calls, special invoke for construcotr
		if (method.isStatic()) {
			invokeExpr = Jimple.v().newStaticInvokeExpr(ref, Arrays.asList(args));
		} else if (method.isConstructor()) {
			invokeExpr = Jimple.v().newSpecialInvokeExpr(var, ref, Arrays.asList(args));
		} else {
			invokeExpr = Jimple.v().newVirtualInvokeExpr(var, ref, Arrays.asList(args));
		}

		InvokeStmt invoke = Jimple.v().newInvokeStmt(invokeExpr);
		return invoke;
	}

	/**
	 * <p>
	 * Generates a {@link AssignStmt} as {@link Unit} object between a {@link Local}
	 * variable and a {@link RefType}.
	 * </p>
	 * <p>
	 * i.e. $r0 = new javax.crypto.spec.PBEKeySpec;
	 * </p>
	 * 
	 * @param var  The variable that gets assigned.
	 * @param type RefType of the call.
	 * @return Returns the new generated assignment statement.
	 */
	public static Unit generateRefTypeAssignStmt(Local var, RefType type) {
		Unit typeAssignStmt = Jimple.v().newAssignStmt(var, Jimple.v().newNewExpr(type));
		return typeAssignStmt;
	}

	/**
	 * <p>
	 * Generates a {@link AssignStmt} as {@link Unit} object between two
	 * {@link Value} objects.
	 * </p>
	 * <p>
	 * i.e. // myVar = $r0;
	 * </p>
	 * 
	 * @param left  The left value
	 * @param right The right value
	 * @return Returns the new generated assignment statement.
	 */
	public static Unit generateAssignStmt(Value left, Value right) {
		Unit assignStmt = Jimple.v().newAssignStmt(left, right);
		return assignStmt;
	}

	/**
	 * <p>
	 * Generates a {@link AssignStmt} as {@link Unit} object between a {@link Local}
	 * array variable and the array {@link RefType}.
	 * </p>
	 * 
	 * @param arrayLocal     The array variable that gets assigned by the array
	 *                       type.
	 * @param localType      Type of the array.
	 * @param arrayDimension Dimension of the array.
	 * @return Returns the new generated assignment statement.
	 */
	public static Unit generateArrayAssignStmt(Local arrayLocal, Type localType, int arrayDimension) {
		NewArrayExpr arrayExpr = Jimple.v().newNewArrayExpr(localType, IntConstant.v(arrayDimension));
		Unit assignStmt = Jimple.v().newAssignStmt(arrayLocal, arrayExpr);
		return assignStmt;
	}

	/**
	 * <p>
	 * Converts a primitive data type into a Jimple {@link Constant}
	 * </p>
	 * 
	 * @param object Object to be converted.
	 * @return Returns the corresponding {@link Constant}.
	 */
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
		} else if (object instanceof Short) {
			return LongConstant.v(((Short) object).shortValue());
		} else if (object instanceof String) {
			return StringConstant.v((String) object);
		} else if (object instanceof Double) {
			return DoubleConstant.v(((Double) object).doubleValue());
		} else {
			throw new RuntimeException("unrecognized constant value = " + object);
		}
	}
	
	
	

	public static HashMap<Local, List<Unit>> generateParameterUnits(Body body, SootMethod method,
			List<String> paramNames, List<Object> paramValues) {

		boolean namesAvailable = true;
		boolean valuesAvailable = true;

		if (Utils.isNullOrEmpty(paramNames)) {
			namesAvailable = false;
		} 
		if (Utils.isNullOrEmpty(paramValues)) {
			valuesAvailable = false;
		}

		LinkedHashMap <Local, List<Unit>> generatedUnits = Maps.newLinkedHashMap();
		List<Type> parameterTypes = method.getParameterTypes();

		if (namesAvailable && valuesAvailable) {
			for (int i = 0; i < parameterTypes.size(); i++) {
				generatedUnits.putAll(
						generateParameterUnit(body, paramNames.get(i), parameterTypes.get(i), paramValues.get(i)));
			}
		} else if (!namesAvailable && valuesAvailable) {
			for (int i = 0; i < parameterTypes.size(); i++) {
				generatedUnits.putAll(generateParameterUnit(body, "", parameterTypes.get(i), paramValues.get(i)));
			}
		} else if (namesAvailable && !valuesAvailable) {
			
			for (int i = 0; i < parameterTypes.size(); i++) {
				generatedUnits.putAll(generateParameterUnit(body, paramNames.get(i), parameterTypes.get(i), 1));
			}
		} else {
			for (Type parameterType : parameterTypes) {
				generatedUnits.putAll(generateParameterUnit(body, "", parameterType, 1));
			}
		}

		return generatedUnits;
	}

	public static HashMap<Local, List<Unit>> generateParameterUnits(Body body, SootMethod method) {
		return generateParameterUnits(body, method, null, null);
	}

	private static HashMap<Local, List<Unit>> generateParameterUnit(Body body, String name, Type type, Object value) {
		HashMap<Local, List<Unit>> generatedUnits = Maps.newHashMap();

		if (type instanceof PrimType) {
			PrimType primType = (PrimType) type;
			Local primeLocal = JimpleCodeGenerator.generateFreshLocal(body, primType, name);
			Unit valueAssign = JimpleCodeGenerator.generateAssignStmt(primeLocal,
					JimpleCodeGenerator.generateConstantValue(value));

			List<Unit> units = Lists.newArrayList();
			units.add(valueAssign);
			generatedUnits.put(primeLocal, units);
			return generatedUnits;
		} else if (type instanceof ArrayType) {
			ArrayType arrayType = (ArrayType) type;
			Local arrayLocal = JimpleCodeGenerator.genereateFreshArrayLocal(body, arrayType.baseType, name, 1);
			Unit arrayAssign = JimpleCodeGenerator.generateArrayAssignStmt(arrayLocal, arrayType.baseType, 1);

			List<Unit> units = Lists.newArrayList();
			units.add(arrayAssign);
			generatedUnits.put(arrayLocal, units);
			return generatedUnits;
		} else if (type instanceof RefType) {
			RefType refType = (RefType) type;
			SootClass clazz = refType.getSootClass();
			SootMethod initMethod = null;

			// look for the beste method to inialize the object
			if (clazz.isAbstract()) {

			} else if (clazz.isInterface()) {

			} else {
				initMethod = Utils.getBestInitializationMethod(clazz);
			}

			List<Type> parameterTypes = initMethod.getParameterTypes();
			HashMap<Value, List<Unit>> generatedParameterUnits = Maps.newHashMap();
			if (!Utils.isNullOrEmpty(parameterTypes)) {
				for (Type parameterType : parameterTypes) {
					generatedParameterUnits.putAll(generateParameterUnit(body, "", parameterType, 1));
				}
			}

			Local refTypeLocal = JimpleCodeGenerator.generateFreshLocal(body, refType, name);
			Value[] parameterLocals = generatedParameterUnits.keySet().toArray(new Value[0]);

			if (initMethod.isStatic()) {
				Unit staticInvoke = JimpleCodeGenerator.generateInvokeStmt(refTypeLocal, initMethod, parameterLocals);
				Unit assign = JimpleCodeGenerator.generateAssignStmt(refTypeLocal,
						staticInvoke.getUseBoxes().get(0).getValue());

				List<Unit> units = Lists.newArrayList();
				units.add(assign);
				generatedUnits.put(refTypeLocal, units);
			} else {
				Local temp = JimpleCodeGenerator.generateFreshLocal(body, refType);
				Unit typeAssign = JimpleCodeGenerator.generateRefTypeAssignStmt(temp, refType);
				Unit invoke = JimpleCodeGenerator.generateInvokeStmt(temp, initMethod, parameterLocals);
				Unit assign = JimpleCodeGenerator.generateAssignStmt(refTypeLocal, temp);

				List<Unit> units = Lists.newArrayList();
				units.add(typeAssign);
				units.add(invoke);
				units.add(assign);
				generatedUnits.put(refTypeLocal, units);
			}
		}
		return generatedUnits;
	}

	private static void removeCaughtExceptionsByThrown(Body body, List<SootClass> exceptions) {
		List<SootClass> throwsExceptions = body.getMethod().getExceptions();
		// remove exception that caught by "thrown"
		exceptions.removeAll(throwsExceptions);

		// remove subclasses of caught exceptions by "thrown"
		for (SootClass throwsException : throwsExceptions) {
			for (SootClass exception : new ArrayList<>(exceptions)) { // iterating over a copy
				if (hierarchy.isSubclass(exception, throwsException)) {
					exceptions.remove(exception);
				}
			}
		}
	}

	private static void removeCaughtExceptionsByCatch(Body body, List<SootClass> exceptions, List<Unit> tryUnits) {
		
		Chain<Trap> traps = body.getTraps();
		ArrayList bodyUnits = new ArrayList(body.getUnits());
		
		for (Trap trap : traps) {
			//TODO: check if it is necessary!
			List<Unit> trapTryUnits = bodyUnits.subList(bodyUnits.indexOf(trap.getBeginUnit()), bodyUnits.indexOf(trap.getEndUnit()));
			SootClass trapCatchException = trap.getException();
			if (trapTryUnits.containsAll(tryUnits)) {
				for (SootClass exception : new ArrayList<>(exceptions)) {
					if (exception.equals(trapCatchException) || hierarchy.isSubclass(exception, trapCatchException)) {
						exceptions.remove(exception);
					}
				}
			}
		}
	}

	public static void generateTraps(Body body, SootMethod method, List<Unit> tryUnits) {
		List<SootClass> exceptions = method.getExceptions();
		
		// remove caught exceptions
		removeCaughtExceptionsByThrown(body, exceptions);
		removeCaughtExceptionsByCatch(body, exceptions, tryUnits);
		if(Utils.isNullOrEmpty(exceptions)) {
			return;
		}
		
		//Try block
		Unit startTryBlockUnit = tryUnits.get(0);
		Unit endTryBlockUnit = tryUnits.get(tryUnits.size() - 1);
		
		Unit nullAssign = null;
		if (startTryBlockUnit instanceof AssignStmt) {
			AssignStmt assign = (AssignStmt) startTryBlockUnit;
			Value variable = assign.getLeftOpBox().getValue();
			nullAssign = generateAssignStmt(variable, generateConstantValue(null));
			body.getUnits().insertBefore(nullAssign, body.getUnits().getPredOf(startTryBlockUnit)); // add null initialization
		} 
		Unit endTryBlockUnitSucc = Jimple.v().newGotoStmt(body.getUnits().getSuccOf(endTryBlockUnit));
		
		//Catch block;
		for(SootClass exception : exceptions) {
			
			SootClass thrwoableClass = Scene.v().getSootClass("java.lang.Throwable");
			RefType thrwoableRefType = RefType.v(thrwoableClass.getName());
			Local thrwoableTempLocal = generateFreshLocal(body, thrwoableRefType);
			
			Unit caughtStmt = Jimple.v().newIdentityStmt(thrwoableTempLocal, Jimple.v().newCaughtExceptionRef());

			Local thrwoableELocal = generateFreshLocal(body, thrwoableRefType, "e");
			Unit localAssign = generateAssignStmt(thrwoableELocal, thrwoableTempLocal);

			RefType exceptionRefType = RefType.v(exception.getName());
			Local exceptionTempLocal = generateFreshLocal(body, exceptionRefType);
			Value castExpr = Jimple.v().newCastExpr(thrwoableELocal, exceptionRefType);
			
			Unit castAssign = generateAssignStmt(exceptionTempLocal, castExpr);
			SootMethod mPrintStackTrace = thrwoableClass.getMethod("void printStackTrace()");
			Unit printStackTraceInvokeStmt = generateInvokeStmt(exceptionTempLocal, mPrintStackTrace);
			
			List<Unit> catchBlock = new ArrayList<Unit>();
			catchBlock.add(endTryBlockUnitSucc);
			catchBlock.add(caughtStmt);
			catchBlock.add(localAssign);
			catchBlock.add(castAssign);
			catchBlock.add(printStackTraceInvokeStmt);
			
			insertRightBeforeNoRedirect(body, catchBlock, body.getUnits().getSuccOf(endTryBlockUnit));
			body.getTraps().add(Jimple.v().newTrap(exception, startTryBlockUnit, endTryBlockUnitSucc, caughtStmt));
		}		
	}
	
	public static void insertRightBeforeNoRedirect(Body body, List<Unit> catchBlock, Unit s) {
		assert !(s instanceof IdentityStmt);
		for (Unit stmt : catchBlock)
			body.getUnits().insertBeforeNoRedirect(stmt, s);
	}

	
	

	
	
	
	
	
	
	
	
	
	
	
	
	
	
	private SootClass getImplementedInterface(SootClass clazz) {

		return null;
	}

	private SootClass getSubclass(SootClass clazz) {
		return null;
	}

	
	
	
	
	// TODO:
	public static HashMap<Value, List<Unit>> generateParameterArray(Body body, List<Value> parameterList) {
		List<Unit> generated = new ArrayList<Unit>();
		NewArrayExpr arrayExpr = Jimple.v().newNewArrayExpr(CharType.v(), IntConstant.v(parameterList.size()));

		Value newArrayLocal = JimpleCodeGenerator.generateFreshLocal(body,
				JimpleCodeGenerator.getParameterArrayType(CharType.v(), 1));
		Unit newAssignStmt = Jimple.v().newAssignStmt(newArrayLocal, arrayExpr);
		generated.add(newAssignStmt);

		for (int i = 0; i < parameterList.size(); i++) {
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

	// TODO:
	public static Value generateCorrectObject(Body body, Value value, List<Unit> generated) {
		if (value.getType() instanceof PrimType) {
			// in case of a primitive type, we use boxing (I know it is not nice, but it
			// works...) in order to use the Object type
			if (value.getType() instanceof BooleanType) {
				Local booleanLocal = generateFreshLocal(body, RefType.v("java.lang.Boolean"));

				SootClass sootClass = Scene.v().getSootClass("java.lang.Boolean");
				SootMethod valueOfMethod = sootClass.getMethod("java.lang.Boolean valueOf(boolean)");
				StaticInvokeExpr staticInvokeExpr = Jimple.v().newStaticInvokeExpr(valueOfMethod.makeRef(), value);

				Unit newAssignStmt = Jimple.v().newAssignStmt(booleanLocal, staticInvokeExpr);
				generated.add(newAssignStmt);

				return booleanLocal;
			} else if (value.getType() instanceof ByteType) {
				Local byteLocal = generateFreshLocal(body, RefType.v("java.lang.Byte"));

				SootClass sootClass = Scene.v().getSootClass("java.lang.Byte");
				SootMethod valueOfMethod = sootClass.getMethod("java.lang.Byte valueOf(byte)");
				StaticInvokeExpr staticInvokeExpr = Jimple.v().newStaticInvokeExpr(valueOfMethod.makeRef(), value);

				Unit newAssignStmt = Jimple.v().newAssignStmt(byteLocal, staticInvokeExpr);
				generated.add(newAssignStmt);

				return byteLocal;
			} else if (value.getType() instanceof CharType) {
				Local characterLocal = generateFreshLocal(body, RefType.v("java.lang.Character"));

				SootClass sootClass = Scene.v().getSootClass("java.lang.Character");
				SootMethod valueOfMethod = sootClass.getMethod("java.lang.Character valueOf(char)");
				StaticInvokeExpr staticInvokeExpr = Jimple.v().newStaticInvokeExpr(valueOfMethod.makeRef(), value);

				Unit newAssignStmt = Jimple.v().newAssignStmt(characterLocal, staticInvokeExpr);
				generated.add(newAssignStmt);

				return characterLocal;
			} else if (value.getType() instanceof DoubleType) {
				Local doubleLocal = generateFreshLocal(body, RefType.v("java.lang.Double"));

				SootClass sootClass = Scene.v().getSootClass("java.lang.Double");
				SootMethod valueOfMethod = sootClass.getMethod("java.lang.Double valueOf(double)");

				StaticInvokeExpr staticInvokeExpr = Jimple.v().newStaticInvokeExpr(valueOfMethod.makeRef(), value);

				Unit newAssignStmt = Jimple.v().newAssignStmt(doubleLocal, staticInvokeExpr);
				generated.add(newAssignStmt);

				return doubleLocal;
			} else if (value.getType() instanceof FloatType) {
				Local floatLocal = generateFreshLocal(body, RefType.v("java.lang.Float"));

				SootClass sootClass = Scene.v().getSootClass("java.lang.Float");
				SootMethod valueOfMethod = sootClass.getMethod("java.lang.Float valueOf(float)");
				StaticInvokeExpr staticInvokeExpr = Jimple.v().newStaticInvokeExpr(valueOfMethod.makeRef(), value);

				Unit newAssignStmt = Jimple.v().newAssignStmt(floatLocal, staticInvokeExpr);
				generated.add(newAssignStmt);

				return floatLocal;
			} else if (value.getType() instanceof IntType) {
				Local integerLocal = generateFreshLocal(body, RefType.v("java.lang.Integer"));

				SootClass sootClass = Scene.v().getSootClass("java.lang.Integer");
				SootMethod valueOfMethod = sootClass.getMethod("java.lang.Integer valueOf(int)");
				StaticInvokeExpr staticInvokeExpr = Jimple.v().newStaticInvokeExpr(valueOfMethod.makeRef(), value);

				Unit newAssignStmt = Jimple.v().newAssignStmt(integerLocal, staticInvokeExpr);
				generated.add(newAssignStmt);

				return integerLocal;
			} else if (value.getType() instanceof LongType) {
				Local longLocal = generateFreshLocal(body, RefType.v("java.lang.Long"));

				SootClass sootClass = Scene.v().getSootClass("java.lang.Long");
				SootMethod valueOfMethod = sootClass.getMethod("java.lang.Long valueOf(long)");
				StaticInvokeExpr staticInvokeExpr = Jimple.v().newStaticInvokeExpr(valueOfMethod.makeRef(), value);

				Unit newAssignStmt = Jimple.v().newAssignStmt(longLocal, staticInvokeExpr);
				generated.add(newAssignStmt);

				return longLocal;
			} else if (value.getType() instanceof ShortType) {
				Local shortLocal = generateFreshLocal(body, RefType.v("java.lang.Short"));

				SootClass sootClass = Scene.v().getSootClass("java.lang.Short");
				SootMethod valueOfMethod = sootClass.getMethod("java.lang.Short valueOf(short)");
				StaticInvokeExpr staticInvokeExpr = Jimple.v().newStaticInvokeExpr(valueOfMethod.makeRef(), value);

				Unit newAssignStmt = Jimple.v().newAssignStmt(shortLocal, staticInvokeExpr);
				generated.add(newAssignStmt);

				return shortLocal;
			} else
				throw new RuntimeException("Ooops, something went all wonky!");
		} else
			// just return the value, there is nothing to box
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
