package de.upb.cognicryptfix.generator.jimple;

import java.math.BigInteger;
import java.util.AbstractMap.SimpleEntry;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import de.upb.cognicryptfix.Constants;
import de.upb.cognicryptfix.exception.NoImplementerException;
import de.upb.cognicryptfix.utils.ByteConstant;
import de.upb.cognicryptfix.utils.InitializationMethodSorter;
import soot.Body;
import soot.BooleanType;
import soot.ByteType;
import soot.DoubleType;
import soot.FastHierarchy;
import soot.FloatType;
import soot.Hierarchy;
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

	public static FastHierarchy getFastHierarchy() {
		return Scene.v().getOrMakeFastHierarchy();
	}

	public static Hierarchy getHierarchy() {
		return Scene.v().getActiveHierarchy();
	}

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
				if (invokeExpr.getMethod().isStatic()) {
					invokeLocal = (Local) assignStmt.getLeftOp();
				} else {
					invokeLocal = (Local) invokeExpr.getUseBoxes().stream()
							.filter(useBox -> useBox instanceof JimpleLocalBox).map(ValueBox::getValue).findAny()
							.orElse(null);
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

	public static boolean containsInvokeExpr(Unit u) {

		if (u instanceof AssignStmt) {
			AssignStmt assignStmt = (AssignStmt) u;
			if (assignStmt.containsInvokeExpr()) {
				return true;
			}
		} else if (u instanceof InvokeStmt) {
			return true;
		}

		return false;
	}

	public static InvokeExpr getInvokeExpr(Unit u) {
		InvokeExpr invokeExpr = null;

		if (!containsInvokeExpr(u)) {
			return invokeExpr;
		}

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

	public static Constant generateDummyConstantValue(Type type) {

		Type objType = Scene.v().getType("java.lang.Object");
		Type charType = Scene.v().getType("java.lang.Character");

		if (type instanceof PrimType || type == Scene.v().getType("java.lang.String")) {
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
				return generateConstantValue((byte) 1);
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
		if (type == Scene.v().getType("java.lang.String")) {
			return generateConstantValue(object);
		} else if (type == Scene.v().getType("java.lang.String[]")) {
			return generateConstantValue(object);
		} else if (type == BooleanType.v()) {
			return generateConstantValue(Boolean.parseBoolean(object));
		} else if (type == IntType.v()) {
			return generateConstantValue(Integer.parseInt(object));
		} else if (type == DoubleType.v()) {
			return generateConstantValue(Double.parseDouble(object));
		} else if (type == LongType.v()) {
			return generateConstantValue(Long.parseLong(object));
		} else if (type == ShortType.v()) {
			return generateConstantValue(Short.parseShort(object));
		} else if (type == FloatType.v()) {
			return generateConstantValue(Float.parseFloat(object));
		} else if (type == ByteType.v()) {
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

		if (type == toCheck) {
			return true;
		} else if (getFastHierarchy().getAllSubinterfaces(superClass).contains(subClass)
				|| getFastHierarchy().getSubclassesOf(superClass).contains(subClass)
				|| getFastHierarchy().getAllImplementersOfInterface(superClass).contains(subClass)) {
			return true;
		} else {
			return false;
		}
	}

	public static boolean isHighestSuperCryptoInterfaceOrAbstractClass(Type type) {
		
		if (type instanceof RefType) {
			RefType refType = (RefType) type;
			SootClass refTypeClazz = refType.getSootClass();

			if (refTypeClazz.isInterface()) {
				List<SootClass> superInterfaces = getHierarchy().getSuperinterfacesOf(refTypeClazz);
				for (SootClass superInterface : superInterfaces) {
					String packagePath = superInterface.getPackageName();
					if (packagePath.contains("java.security") || packagePath.contains("javax.crypto")) {
						return false;
					} 
				}
				return true;
			} else if (refTypeClazz.isAbstract()) {
				List<SootClass> superAbstractClasses = getHierarchy().getSuperclassesOf(refTypeClazz);
				for (SootClass superClasses : superAbstractClasses) {
					String packagePath = superClasses.getPackageName();
					if (packagePath.contains("java.security") || packagePath.contains("javax.crypto")) {
						return false;
					} 
				}
				return true;
			} else {
				return false;
			}
		} else {
			return false;
		}
	}

	public static Entry<SootClass, SootMethod> getImplementingClassAndInitMethod(SootClass clazz)
			throws NoImplementerException {

		if (clazz.isConcrete()) {
			SootMethod init = getBestInitializationMethod(clazz);
			return new SimpleEntry(clazz, init);
		} else if (clazz.isInterface() || clazz.isAbstract()) {
			List<SootClass> clazzCandidates = getImplementedInterfaceOrAbstractClass(clazz);

			if (!clazzCandidates.isEmpty()) {
				Map<SootClass, SootMethod> classInitMethodMap = Maps.newHashMap();

				for (SootClass clazzCandidate : clazzCandidates) {
					SootMethod initMethod = getBestInitializationMethod(clazzCandidate);
					if (initMethod != null) {
						classInitMethodMap.put(clazzCandidate, initMethod);
					}
				}

				List<SootMethod> initMethods = Lists.newArrayList(classInitMethodMap.values());
				Collections.sort(initMethods, new InitializationMethodSorter());
				SootClass winner = null;

				for (SootClass clazzCandidate : classInitMethodMap.keySet()) {
					if (classInitMethodMap.get(clazzCandidate) == initMethods.get(0)) {
						winner = clazzCandidate;
						break;
					}
				}
				return new SimpleEntry<SootClass, SootMethod>(winner, classInitMethodMap.get(winner));
			} else {				
				SootMethod init = getBestInitializationMethod(clazz);
				if(init != null) {
					return new SimpleEntry<SootClass, SootMethod>(clazz, init);
				} else {
					throw new NoImplementerException(clazz.getName());
				}
			}
		} else {
			throw new NoImplementerException(clazz.getName());
		}
	}

	private static List<SootClass> getImplementedInterfaceOrAbstractClass(SootClass clazz) {
		List<SootClass> concreteImplementatons = Lists.newArrayList();
		List<SootClass> implementations = Lists.newArrayList(getFastHierarchy().getAllImplementersOfInterface(clazz));

		for (SootClass implementation : implementations) {
			if (implementation.isConcrete()) {
				concreteImplementatons.add(implementation);
			}
		}
		implementations = Lists.newArrayList(getFastHierarchy().getSubclassesOf(clazz));

		for (SootClass implementation : implementations) {
			if (implementation.isConcrete()) {
				concreteImplementatons.add(implementation);
			}
		}
		return concreteImplementatons;
	}

	private static SootMethod getBestInitializationMethod(SootClass clazz) {

		List<SootMethod> initMethods = Lists.newArrayList();
		for (SootMethod method : clazz.getMethods()) {
			if ((method.isConstructor() || method.getName().contains("getInstance")) && method.isPublic() && !method.isAbstract()) {
				boolean useOfNotSupportedType = false;
				for (Type parameterType : method.getParameterTypes()) {
					if (Constants.notSupportedParameterTypes.contains(parameterType.toQuotedString())) {
						useOfNotSupportedType = true;
					}
				}
				if (!useOfNotSupportedType) {
					initMethods.add(method);
				}
			}
		}
		Collections.sort(initMethods, new InitializationMethodSorter());
		return initMethods.isEmpty() ? null : initMethods.get(0);
	}

}
