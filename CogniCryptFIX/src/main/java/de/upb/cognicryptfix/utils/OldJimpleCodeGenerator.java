package de.upb.cognicryptfix.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.text.StyleConstants.CharacterConstants;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import de.upb.cognicryptfix.generator.jimple.JimpleUtils;
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
import soot.jimple.AddExpr;
import soot.jimple.ArrayRef;
import soot.jimple.AssignStmt;
import soot.jimple.ClassConstant;
import soot.jimple.Constant;
import soot.jimple.DoubleConstant;
import soot.jimple.EqExpr;
import soot.jimple.GotoStmt;
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
@Deprecated
public class OldJimpleCodeGenerator {

//	Aggregator.v().transform(body);
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
		return generateFreshLocal(body, OldJimpleCodeGenerator.getParameterArrayType(type, 1));
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
		Local l = generateFreshLocal(body, OldJimpleCodeGenerator.getParameterArrayType(type, 1));
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

		if (method.isStatic()) {
			invokeExpr = Jimple.v().newStaticInvokeExpr(ref, Arrays.asList(args));
		} else if (method.isConstructor()) {
			invokeExpr = Jimple.v().newSpecialInvokeExpr(var, ref, Arrays.asList(args));
		} else {
			invokeExpr = Jimple.v().newVirtualInvokeExpr(var, ref, Arrays.asList(args));
		}
		// DynamicInvokeExpr not supported
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
		} else if (object instanceof Character) {
			return StringConstant.v(object+"");
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

		if (CollectionUtils.isEmpty(paramNames)) {
			namesAvailable = false;
		} 
		if (CollectionUtils.isEmpty(paramValues)) {
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
			Local primeLocal = OldJimpleCodeGenerator.generateFreshLocal(body, primType, name);
			Unit valueAssign = OldJimpleCodeGenerator.generateAssignStmt(primeLocal,
					OldJimpleCodeGenerator.generateConstantValue(value));

			List<Unit> units = Lists.newArrayList();
			units.add(valueAssign);
			generatedUnits.put(primeLocal, units);
			return generatedUnits;
		} else if (type instanceof ArrayType) {
			ArrayType arrayType = (ArrayType) type;
			Local arrayLocal = OldJimpleCodeGenerator.genereateFreshArrayLocal(body, arrayType.baseType, name, 1);
			Unit arrayAssign = OldJimpleCodeGenerator.generateArrayAssignStmt(arrayLocal, arrayType.baseType, 1);

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
				clazz = getImplementedInterface(clazz);
				initMethod = getBestInitializationMethod(clazz);		
			} else {
				initMethod = getBestInitializationMethod(clazz);
			}

			List<Type> parameterTypes = initMethod.getParameterTypes();
			HashMap<Value, List<Unit>> generatedParameterUnits = Maps.newHashMap();
			if (!CollectionUtils.isEmpty(parameterTypes)) {
				for (Type parameterType : parameterTypes) {
					generatedParameterUnits.putAll(generateParameterUnit(body, "", parameterType, 1));
				}
			}

			Local refTypeLocal = OldJimpleCodeGenerator.generateFreshLocal(body, refType, name);
			Value[] parameterLocals = generatedParameterUnits.keySet().toArray(new Value[0]);

			if (initMethod.isStatic()) {
				Unit staticInvoke = OldJimpleCodeGenerator.generateInvokeStmt(refTypeLocal, initMethod, parameterLocals);
				Unit assign = OldJimpleCodeGenerator.generateAssignStmt(refTypeLocal,
						staticInvoke.getUseBoxes().get(0).getValue());

				List<Unit> units = Lists.newArrayList();
				units.add(assign);
				generatedUnits.put(refTypeLocal, units);
			} else {
				Local temp = OldJimpleCodeGenerator.generateFreshLocal(body, refType);
				Unit typeAssign = OldJimpleCodeGenerator.generateRefTypeAssignStmt(temp, refType);
				Unit invoke = OldJimpleCodeGenerator.generateInvokeStmt(temp, initMethod, parameterLocals);
				Unit assign = OldJimpleCodeGenerator.generateAssignStmt(refTypeLocal, temp);

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
		ArrayList<Unit> bodyUnits = Lists.newArrayList(body.getUnits());
		
		for (Trap trap : traps) {
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
		if(CollectionUtils.isEmpty(exceptions)) {
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

	private static SootMethod getBestInitializationMethod(SootClass clazz) {
		List<SootMethod> initMethods = Lists.newArrayList();
		for (SootMethod method : clazz.getMethods()) {
			if (method.isConstructor() || method.getName().contains("getInstance")) {
				initMethods.add(method);
			}
		}
		Collections.sort(initMethods, new InitializationMethodSorter());
		return initMethods.get(0);
	}
	
	private static SootClass getImplementedInterface(SootClass clazz) {
		HashMap<SootMethod,SootClass> implementationMap = Maps.newHashMap();
		
		List<SootClass> implementations = new ArrayList(hierarchy.getAllImplementersOfInterface(clazz));
		for(SootClass implementation : implementations) {
			implementationMap.put(getBestInitializationMethod(implementation),implementation);
		}
		
		List<SootMethod> initMethods = Lists.newArrayList(implementationMap.keySet());
		Collections.sort(initMethods, new InitializationMethodSorter());
		
		return implementationMap.get(initMethods);
	}

	private SootClass getSubclass(SootClass clazz) {
		return null;
	}
	
	public static HashMap<Local, List<Unit>> generateArrayUnits(Body body, Type contentType, List<Value> contentValues) {
		
		HashMap<Local, List<Unit>> generatedUnits = Maps.newLinkedHashMap();
		Local arrayLocal = genereateFreshArrayLocal(body, contentType, contentValues.size());
		AssignStmt arrayAssign = (AssignStmt) generateArrayAssignStmt(arrayLocal, arrayLocal.getType(), contentValues.size());
		generatedUnits.put(arrayLocal, Lists.newArrayList());
		generatedUnits.get(arrayLocal).add(arrayAssign);
		 
		for (int i = 0; i < contentValues.size(); i++) {
			Value index = IntConstant.v(i);
			ArrayRef leftSide = Jimple.v().newArrayRef(arrayLocal, index);
			Value rightSide = OldJimpleCodeGenerator.generateArrayUnit(body, contentValues.get(i), generatedUnits.get(arrayLocal));
			Unit parameterInArray = Jimple.v().newAssignStmt(leftSide, rightSide);
			generatedUnits.get(arrayLocal).add(generateAssignStmt(leftSide, rightSide));
		}
		return generatedUnits;
	}


	public static Value generateArrayUnit(Body body, Value value, List<Unit> generated) {
		if (value.getType() instanceof PrimType) {

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
			return value;
	}

	
	/*-
  	Scanner s = new Scanner(new File(HardCodedTest.class.getResource("Key.txt").getPath()));
	int[] a = new int[10];
	int counter = 0;
	while (s.hasNext()) {
		a[counter++] = s.nextInt();
	}
	s.close();
 */
//private Map<Local, List<Unit>> createScanner(SootClass clazz, Local l, String fileName) {
//
//	Type localType = l.getType();
//	if (localType instanceof ArrayType) {
//		localType = ((ArrayType) localType).baseType;
//	}
//
//	Map<Local, List<Unit>> generatedUnits = Maps.newLinkedHashMap();
//	List<Unit> generatedUnitList = Lists.newArrayList();
//
//	// $stack9 = class "Lde/upb/cognicryptfix/test/hardcoded/HardCodedTest;"
//	RefType clazzRefType = RefType.v("java.lang.Class");
//	SootClass clazzClass = clazzRefType.getSootClass();
//	Local clazzLocal = localGenerator.generateFreshLocal(clazzRefType);
//	ClassConstant clazzConstant = ClassConstant.v("L" + clazz.getName().replace('.', '/'));
//	Unit clazzAssignment = assignGenerator.generateAssignStmt(clazzLocal, clazzConstant);
//
//	// varReplacer0 = "Key.txt";
//	Local filePathLocal = localGenerator.generateFreshLocal(Scene.v().getType("java.lang.String"), "varReplacer");
//	StringConstant filePathValue = StringConstant.v(fileName);
//	Unit filePathAssignment = assignGenerator.generateAssignStmt(filePathLocal, filePathValue);
//
//	// $stack10 = virtualinvoke $stack9.<java.lang.Class: java.net.URL
//	// getResource(java.lang.String)>(varReplacer0);
//	RefType urlRefType = RefType.v("java.net.URL");
//	SootClass urlClass = urlRefType.getSootClass();
//	Local urlLocal = localGenerator.generateFreshLocal(urlRefType, "varReplacer");
//	SootMethod getResourceMethod = clazzClass.getMethod("java.net.URL getResource(java.lang.String)");
//	Unit getResourceCall = callGenerator.generateCallUnits(clazzLocal, urlLocal, getResourceMethod, filePathLocal)
//			.values().iterator().next().get(0);
//
//	// $stack11 = virtualinvoke $stack10.<java.net.URL: java.lang.String
//	// getPath()>();
//	Local stringPathLocal = localGenerator.generateFreshLocal(Scene.v().getType("java.lang.String"), "varReplacer");
//	SootMethod getPathMethod = urlClass.getMethod("java.lang.String getPath()");
//	Unit getPathCall = callGenerator.generateCallUnits(urlLocal, stringPathLocal, getPathMethod).values().iterator()
//			.next().get(0);
//
//	// specialinvoke $stack8.<java.io.File: void
//	// <init>(java.lang.String)>($stack11);
//	RefType fileRefType = RefType.v("java.io.File");
//	SootClass fileClass = fileRefType.getSootClass();
//	Local fileLocal = localGenerator.generateFreshLocal(fileRefType, "genFile");
//	SootMethod fileConstructor = fileClass.getMethod("void <init>(java.lang.String)");
//	Map<Local, List<Unit>> fileConstructorCall = callGenerator.generateCallUnits(fileLocal, null, fileConstructor,
//			stringPathLocal);
//
//	// specialinvoke $stack7.<java.util.Scanner: void
//	// <init>(java.io.File)>($stack8);
//	RefType scannerRefType = RefType.v("java.util.Scanner");
//	SootClass scannerClass = scannerRefType.getSootClass();
//	Local scannerLocal = localGenerator.generateFreshLocal(scannerRefType, "scanner");
//	SootMethod scannerConstructor = scannerClass.getMethod("void <init>(java.io.File)");
//	Map<Local, List<Unit>> scannerConstructorCall = callGenerator.generateCallUnits(scannerLocal, null,
//			scannerConstructor, fileLocal);
//
//	// counter = 0;
//	Local counterLocal = localGenerator.generateFreshLocal(IntType.v(), "counter");
//	Unit counterAssignment = assignGenerator.generateAssignStmt(counterLocal, IntConstant.v(0));
//
//	// $stack12 = virtualinvoke s.<java.util.Scanner: boolean hasNext()>();
//	Local hasNextBoolean = localGenerator.generateFreshLocal(BooleanType.v());
//	SootMethod hasNextMethod = scannerClass.getMethod("boolean hasNext()");
//	Unit hasNextCall = callGenerator.generateCallUnits(scannerLocal, hasNextBoolean, hasNextMethod).values()
//			.iterator().next().get(0);
//
//	// virtualinvoke s.<java.util.Scanner: void close()>();
//	SootMethod closeMethod = scannerClass.getMethod("void close()");
//	Unit closeMethodCall = callGenerator.generateCallUnits(scannerLocal, null, closeMethod).values().iterator()
//			.next().get(0);
//
//	// if $stack12 == 0 goto label2; goto close()
//	EqExpr clause = Jimple.v().newEqExpr(hasNextBoolean, IntConstant.v(0));
//	Unit ifStmt = Jimple.v().newIfStmt(clause, closeMethodCall);
//
//	// $stack16 = counter;
//	Local arrayIndex = localGenerator.generateFreshLocal(IntType.v());
//	Unit indexAssignment = assignGenerator.generateAssignStmt(arrayIndex, counterLocal);
//
//	// counter = counter + 1;
//	AddExpr add = Jimple.v().newAddExpr(counterLocal, IntConstant.v(1));
//	Unit increasingCounterAssigment = assignGenerator.generateAssignStmt(counterLocal, add);
//
//	// $stack17 = virtualinvoke s.<java.util.Scanner: int nextInt()>();
//	Local nextValueLocal = localGenerator.generateFreshLocal(localType);
//	List<Unit> nextCall = createNextCall(scannerClass, scannerLocal, nextValueLocal);
//
//	// a[$stack16] = $stack17;
//	ArrayRef leftSide = Jimple.v().newArrayRef(l, arrayIndex);
//	Unit arrayIndexAssignment = assignGenerator.generateAssignStmt(leftSide, nextValueLocal);
//
//	// goto label1;
//	GotoStmt gotoStmt = Jimple.v().newGotoStmt(hasNextCall);
//
//	generatedUnitList.add(clazzAssignment);
//	generatedUnitList.add(filePathAssignment);
//	generatedUnitList.add(getResourceCall);
//	generatedUnitList.add(getPathCall);
//	generatedUnitList.addAll(fileConstructorCall.values().iterator().next());
//	generatedUnitList.addAll(scannerConstructorCall.values().iterator().next());
//	generatedUnitList.add(counterAssignment);
//	generatedUnitList.add(hasNextCall);
//	generatedUnitList.add(ifStmt);
//	generatedUnitList.add(indexAssignment);
//	generatedUnitList.add(increasingCounterAssigment);
//	generatedUnitList.addAll(nextCall);
//	generatedUnitList.add(arrayIndexAssignment);
//	generatedUnitList.add(gotoStmt);
//	generatedUnitList.add(closeMethodCall);
//
//	generatedUnits.put(l, generatedUnitList);
//	return generatedUnits;
//}

//private List<Unit> createNextCall(SootClass scannerClass, Local scannerLocal, Local retLocal) {
//	Type type = retLocal.getType();
//	String methodString = "";
//	List<Unit> generatedUnitList = Lists.newArrayList();
//
//	if (JimpleUtils.equals(type, Scene.v().getType("java.lang.String"))
//			|| JimpleUtils.equals(type, Scene.v().getType("char"))) {
//		methodString = "java.lang.String next()";
//	} else {
//		methodString = type.toString() + " next" + StringUtils.capitalize(type.toString() + "()");
//	}
//
//	SootMethod nextMethod = scannerClass.getMethod(methodString);
//
//	if (JimpleUtils.equals(type, Scene.v().getType("char"))) {
//		RefType stringRefType = RefType.v("java.lang.String");
//		SootClass stringClass = stringRefType.getSootClass();
//		Local tempString = localGenerator.generateFreshLocal(stringRefType);
//		Unit nextCall = callGenerator.generateCallUnits(scannerLocal, tempString, nextMethod).values().iterator()
//				.next().get(0);
//
//		Local index = localGenerator.generateFreshLocal(IntType.v());
//		Unit indexAssignment = assignGenerator.generateAssignStmt(index, IntConstant.v(0));
//		SootMethod charAtMethod = stringClass.getMethod("char charAt(int)");
//		Unit charAtCall = callGenerator.generateCallUnits(tempString, retLocal, charAtMethod, index).values()
//				.iterator().next().get(0);//
//
//		generatedUnitList.add(nextCall);
//		generatedUnitList.add(indexAssignment);
//		generatedUnitList.add(charAtCall);
//	} else {
//		Unit nextCall = callGenerator.generateCallUnits(scannerLocal, retLocal, nextMethod).values().iterator()
//				.next().get(0);
//		generatedUnitList.add(nextCall);
//
//	}
//	return generatedUnitList;
//}

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
