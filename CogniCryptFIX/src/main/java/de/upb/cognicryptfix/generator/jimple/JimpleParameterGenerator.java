package de.upb.cognicryptfix.generator.jimple;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import de.upb.cognicryptfix.utils.Utils;
import soot.ArrayType;
import soot.Body;
import soot.Local;
import soot.PrimType;
import soot.RefType;
import soot.SootClass;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.Value;

/**
 * @author Andre Sonntag
 * @date 19.02.2020
 */
public class JimpleParameterGenerator {

	private Body body;
	private JimpleLocalGenerator localGenerator;
	private JimpleAssignGenerator assignGenerator;
	private JimpleCallGenerator callGenerator;
	
	public JimpleParameterGenerator(Body body) {
		this.body = body;
		this.localGenerator = new JimpleLocalGenerator(body);
		this.assignGenerator = new JimpleAssignGenerator();
		this.callGenerator = new JimpleCallGenerator(body);
	}
	
	public HashMap<Local, List<Unit>> generateParameterUnits(SootMethod method, List<String> names,
			List<Object> values) {

		LinkedHashMap<Local, List<Unit>> generatedUnits = Maps.newLinkedHashMap();
		List<Type> types = method.getParameterTypes();
		boolean namesAvailable = true;
		boolean valuesAvailable = true;

		if (Utils.isNullOrEmpty(names)) {
			namesAvailable = false;
		}
		if (Utils.isNullOrEmpty(values)) {
			valuesAvailable = false;
		}

		if (namesAvailable && valuesAvailable) {
			for (int i = 0; i < types.size(); i++) {
				generatedUnits.putAll(generateParameterUnit(types.get(i), names.get(i), values.get(i)));
			}
		} else if (!namesAvailable && valuesAvailable) {
			for (int i = 0; i < types.size(); i++) {
				generatedUnits.putAll(generateParameterUnit(types.get(i), "", values.get(i)));
			}
		} else if (namesAvailable && !valuesAvailable) {
			for (int i = 0; i < types.size(); i++) {
				generatedUnits.putAll(generateParameterUnit(types.get(i), names.get(i), 1));
			}
		} else {
			for (Type parameterType : types) {
				generatedUnits.putAll(generateParameterUnit(parameterType, "", 1));
			}
		}

		return generatedUnits;
	}

	private HashMap<Local, List<Unit>> generateParameterUnit(Type type, String name, Object value) {
		HashMap<Local, List<Unit>> generatedUnits = Maps.newHashMap();

		if (type instanceof PrimType) {
			PrimType primType = (PrimType) type;
			Local primeLocal = localGenerator.generateFreshLocal(primType, name);
			Value primeValue = value == null ? JimpleUtils.generateDummyValueForType(primType) : JimpleUtils.generateConstantValue(value);
			Unit primeAssignStmt = assignGenerator.generateAssignStmt(primeLocal, primeValue);
			List<Unit> primeUnits = Lists.newArrayList();
			primeUnits.add(primeAssignStmt);
			generatedUnits.put(primeLocal, primeUnits);
			return generatedUnits;
			
		} else if (type instanceof ArrayType) {
			ArrayType arrayType = (ArrayType) type;
			//TODO: length and value of Array
			Local arrayLocal = localGenerator.genereateFreshArrayLocal(arrayType.baseType, name, 16);
			Unit arrayAssign = assignGenerator.generateArrayAssignStmt(arrayLocal, 16);
			List<Unit> arrayUnits = Lists.newArrayList();
			arrayUnits.add(arrayAssign);
			generatedUnits.put(arrayLocal, arrayUnits);
			return generatedUnits;
		} else if (type instanceof RefType) {
			
			RefType refType = (RefType) type;
			SootClass clazz = refType.getSootClass();
			SootMethod initMethod = JimpleUtils.getBestInitializationMethod(clazz);

			// TODO: check if clazz is abstract or an interface, if yes find the beste subclass!
//			if (clazz.isAbstract()) {
//
//			} else if (clazz.isInterface()) {
//				clazz = getImplementedInterface(clazz);
//				initMethod = getBestInitializationMethod(clazz);
//			} else {
//				initMethod = getBestInitializationMethod(clazz);
//			}

			List<Type> parameterTypes = initMethod.getParameterTypes();
			HashMap<Local, List<Unit>> generatedRefTypeParameterUnits = Maps.newHashMap();
			if (!Utils.isNullOrEmpty(parameterTypes)) {
				for (Type parameterType : parameterTypes) {
					generatedRefTypeParameterUnits.putAll(generateParameterUnit(parameterType, "" , JimpleUtils.generateDummyValueForType(parameterType)));
				}
			}

			Local refTypeLocal = localGenerator.generateFreshLocal(refType, name);
			Local[] RefTypeParameterLocals = generatedRefTypeParameterUnits.keySet().toArray(new Local[0]);

			if (initMethod.isStatic()) {	// getInstance()
				generatedUnits.putAll(callGenerator.generateCallUnits(refTypeLocal, initMethod, RefTypeParameterLocals));			
			} else {	// constructor
				generatedUnits.putAll(callGenerator.generateConstructorCallUnits(refTypeLocal, initMethod, RefTypeParameterLocals));			
			}
		}
		return generatedUnits;
	}

}
