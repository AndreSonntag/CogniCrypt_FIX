package de.upb.cognicryptfix.generator.jimple;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import de.upb.cognicryptfix.utils.ByteConstant;
import de.upb.cognicryptfix.utils.InitializationMethodSorter;
import soot.Body;
import soot.FastHierarchy;
import soot.Local;
import soot.PrimType;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Type;
import soot.jimple.Constant;
import soot.jimple.DoubleConstant;
import soot.jimple.IntConstant;
import soot.jimple.LongConstant;
import soot.jimple.NullConstant;
import soot.jimple.StringConstant;

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
	
	public static boolean isNumericPrimType(Type type) {
		
		Type intType = Scene.v().getType("java.lang.Integer");
		Type doubleType = Scene.v().getType("java.lang.Double");
		Type longType = Scene.v().getType("java.lang.Long");
		Type shortType = Scene.v().getType("java.lang.Short");
		Type floatType = Scene.v().getType("java.lang.Float"); 
		
		if(type instanceof PrimType) {
			if(type == intType || type == doubleType || type == longType || type == shortType || type == floatType) {
				return true;
			} else {
				return false;
			}
		}
		else {
			return false;
		}		
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

		if(type instanceof PrimType) {
			if (type == boolType) {
				return generateConstantValue(true);
			} else if (type == intType) {
				return generateConstantValue(1);
			} else if (type == doubleType) {
				return generateConstantValue(1.0);
			} else if (type == longType) {
				return generateConstantValue(1L);
			} else if (type == shortType) {
				return generateConstantValue(1);
			} else if (type == charType) {
				return generateConstantValue('c');
			} else if (type == stringType) {
				return generateConstantValue("");
			} else {
				throw new RuntimeException("error");
			}		
		}
		return null; 
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
		} else if (object instanceof Byte) {
			return ByteConstant.v(((Byte) object).byteValue());
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
	
	
	//TODO: not good enough
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

	
}
