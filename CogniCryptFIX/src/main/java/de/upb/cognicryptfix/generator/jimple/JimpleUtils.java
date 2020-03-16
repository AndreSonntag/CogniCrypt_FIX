package de.upb.cognicryptfix.generator.jimple;

import java.util.Collections;
import java.util.List;

import com.google.common.collect.Lists;

import de.upb.cognicryptfix.utils.ByteConstant;
import de.upb.cognicryptfix.utils.InitializationMethodSorter;
import soot.Body;
import soot.BooleanType;
import soot.ByteType;
import soot.DoubleType;
import soot.FastHierarchy;
import soot.FloatType;
import soot.IntType;
import soot.Local;
import soot.LongType;
import soot.PrimType;
import soot.Scene;
import soot.ShortType;
import soot.SootClass;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.ValueBox;
import soot.jimple.AssignStmt;
import soot.jimple.Constant;
import soot.jimple.DoubleConstant;
import soot.jimple.IntConstant;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.LongConstant;
import soot.jimple.NullConstant;
import soot.jimple.StringConstant;
import soot.jimple.internal.JimpleLocalBox;

/**
 * @author Andre Sonntag
 * @date 19.02.2020
 */
public class JimpleUtils {

	private static FastHierarchy hierarchy = Scene.v().getOrMakeFastHierarchy();

	public static FastHierarchy getHierarchy() {
		return hierarchy;
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

	public static Local getInvokeLocal(Unit u) {
		Local invokeLocal = null;

		if (u instanceof AssignStmt) {
			AssignStmt assignStmt = (AssignStmt) u;
			if (assignStmt.containsInvokeExpr()) {
				InvokeExpr invokeExpr = (InvokeExpr) assignStmt.getRightOpBox().getValue();
				if (invokeExpr.getMethod().getName().equals("getInstance")) {
					invokeLocal = (Local) assignStmt.getLeftOp();
				}
			} else {
				invokeLocal = (Local) assignStmt.getLeftOp();
			}
		} else if (u instanceof InvokeStmt) {
			InvokeStmt invokeStmt = (InvokeStmt) u;
			InvokeExpr invokeExpr = invokeStmt.getInvokeExpr();
			invokeLocal = (Local) invokeExpr.getUseBoxes().stream().filter(useBox -> useBox instanceof JimpleLocalBox)
					.map(ValueBox::getValue).findAny().orElse(null);

		}
		return invokeLocal;
	}

	public static InvokeExpr getInvokeExpr(Unit u) {
		InvokeExpr invokeExpr = null;

		if (u instanceof AssignStmt) {
			AssignStmt assignStmt = (AssignStmt) u;
			if (assignStmt.containsInvokeExpr()) {
				invokeExpr = (InvokeExpr) assignStmt.getRightOpBox().getValue();
			}
		} else if (u instanceof InvokeStmt) {
			InvokeStmt invokeStmt = (InvokeStmt) u;
			invokeExpr = invokeStmt.getInvokeExpr();
		}

		return invokeExpr;
	}

	public static Constant generateDummyValueForType(Type type) {

		Type objType = Scene.v().getType("java.lang.Object");
		Type boolType = Scene.v().getType("java.lang.Boolean");
		Type intType = Scene.v().getType("java.lang.Integer");
		Type doubleType = Scene.v().getType("java.lang.Double");
		Type longType = Scene.v().getType("java.lang.Long");
		Type shortType = Scene.v().getType("java.lang.Short");
		Type stringType = Scene.v().getType("java.lang.String");
		Type charType = Scene.v().getType("java.lang.Character");
		Type byteType = Scene.v().getType("java.lang.Byte");

		if (type instanceof PrimType) {

			if (type == objType) {
				return generateConstantValue(new Object());
			} else if (type == BooleanType.v()) {
				return generateConstantValue(true);
			} else if (type == IntType.v()) {
				return generateConstantValue(1);
			} else if (type == DoubleType.v()) {
				return generateConstantValue(1.0);
			} else if (type == LongType.v()) {
				return generateConstantValue(1L);
			} else if (type == ShortType.v()) {
				return generateConstantValue(1);
			} else if (type == ByteType.v()) {
				return generateConstantValue((byte) 10);
			} else if (type == charType) {
				return generateConstantValue('c');
			} else if (type == Scene.v().getType("java.lang.String")) {
				return generateConstantValue("");
			} else {
				throw new RuntimeException("error");
			}
		}
		return null;
	}
	
	public static Constant generateConstantValue(Type type, String object) {
		if(type == Scene.v().getType("java.lang.String")) {
			return generateConstantValue(object);
		}else if(type == Scene.v().getType("java.lang.String[]")) {
				return generateConstantValue(object);
		}else if (type == IntType.v()) {
			return generateConstantValue(Integer.parseInt(object));
		} else if(type == DoubleType.v()) {
			return generateConstantValue(Double.parseDouble(object));
		} else if(type == LongType.v()) {
			return generateConstantValue(Long.parseLong(object));
		} else if(type == ShortType.v()) {
			return generateConstantValue(Short.parseShort(object));
		} else if(type == FloatType.v()) {
			return generateConstantValue(Float.parseFloat(object));
		} else if(type == ByteType.v()) {
			return generateConstantValue(Byte.parseByte(object));	
		} else {
			throw new RuntimeException("unrecognized constant value = " + object);
		}
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
		} else if (object instanceof Byte) {
			return ByteConstant.v(((Byte) object).byteValue());
		} else if (object instanceof Long) {
			return LongConstant.v(((Long) object).longValue());
		} else if (object instanceof Short) {
			return LongConstant.v(((Short) object).shortValue());
		} else if (object instanceof Character) {
			return StringConstant.v(object + "");
		} else if (object instanceof String) {
			return StringConstant.v((String) object);
		} else if (object instanceof Double) {
			return DoubleConstant.v(((Double) object).doubleValue());
		} else {
			throw new RuntimeException("unrecognized constant value = " + object);
		}
	}

	public static boolean isEqualOrSubClassOrSubInterface(Type type, Type toCheck) {

		SootClass superClass = Scene.v().loadClassAndSupport(type.toQuotedString());
		SootClass subClass = Scene.v().loadClassAndSupport(toCheck.toQuotedString());

		if(type == toCheck) {
			return true;
		} else if (hierarchy.getAllSubinterfaces(superClass).contains(subClass)
				|| hierarchy.getSubclassesOf(superClass).contains(subClass)
				|| hierarchy.getAllImplementersOfInterface(superClass).contains(subClass)) {
			return true;
		} else {
			return false;
		}
	}

	public static SootMethod getBestInitializationMethod(SootClass clazz) {
		List<SootMethod> initMethods = Lists.newArrayList();
		for (SootMethod method : clazz.getMethods()) {
			if (method.isConstructor() || method.getName().contains("getInstance")) {
				initMethods.add(method);
			}
		}
		Collections.sort(initMethods, new InitializationMethodSorter());
		return initMethods.get(0);
	}

//	// TODO: not good enough
//	public static SootClass getImplementedInterface(SootClass clazz) {
//		HashMap<SootMethod, SootClass> implementationMap = Maps.newHashMap();
//
//		List<SootClass> implementations = new ArrayList(hierarchy.getAllImplementersOfInterface(clazz));
//		for (SootClass implementation : implementations) {
//			implementationMap.put(getBestInitializationMethod(implementation), implementation);
//		}
//
//		List<SootMethod> initMethods = Lists.newArrayList(implementationMap.keySet());
//		Collections.sort(initMethods, new InitializationMethodSorter());
//
//		return implementationMap.get(initMethods);
//	}
//
//	// TODO: not good enough
//	public static SootClass getSubClass(SootClass clazz) {
//		HashMap<SootMethod, SootClass> subclassMap = Maps.newHashMap();
//		List<SootClass> subclasses = new ArrayList(hierarchy.getSubclassesOf(clazz));
//		for (SootClass subclass : subclasses) {
//			subclassMap.put(getBestInitializationMethod(subclass), subclass);
//		}
//
//		List<SootMethod> initMethods = Lists.newArrayList(subclassMap.keySet());
//		Collections.sort(initMethods, new InitializationMethodSorter());
//
//		return subclassMap.get(initMethods);
//	}

}
