package de.upb.cognicryptfix.generator.jimple;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.collections4.CollectionUtils;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import de.upb.cognicryptfix.exception.generation.NoInterfaceImplementerException;
import soot.ArrayType;
import soot.Body;
import soot.Local;
import soot.PrimType;
import soot.RefType;
import soot.Scene;
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

	private JimpleLocalGenerator localGenerator;
	private JimpleAssignGenerator assignGenerator;
	private JimpleCallGenerator callGenerator;
	
	public JimpleParameterGenerator(Body body) {
		this.localGenerator = new JimpleLocalGenerator(body);
		this.assignGenerator = new JimpleAssignGenerator();
		this.callGenerator = new JimpleCallGenerator(body);
	}
	
	public Map<Local, List<Unit>> generateParameterUnits(SootMethod method, List<String> names, List<Object> values) throws NoInterfaceImplementerException {

		Map<Local, List<Unit>> generatedUnits = Maps.newLinkedHashMap();
		List<Type> types = method.getParameterTypes();
		boolean namesAvailable = true;
		boolean valuesAvailable = true;

		if (CollectionUtils.isEmpty(names)) {
			namesAvailable = false;
		}
		if (CollectionUtils.isEmpty(values)) {
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
				generatedUnits.putAll(generateParameterUnit(types.get(i), names.get(i), null));
			}
		} else {
			for (Type parameterType : types) {
				generatedUnits.putAll(generateParameterUnit(parameterType, "", null));
			}
		}

		return generatedUnits;
	}

	private Map<Local, List<Unit>> generateParameterUnit(Type type, String name, Object value) throws NoInterfaceImplementerException {
		Map<Local, List<Unit>> generatedUnits = Maps.newLinkedHashMap();

		if (type instanceof PrimType || JimpleUtils.equals(type, Scene.v().getType("java.lang.String"))) {
			Type primType = type;
			Local primeLocal = localGenerator.generateFreshLocal(primType, name);
			Value primeValue = value == null ? JimpleUtils.generateDummyConstantValue(primType) : JimpleUtils.generateConstantValue(value);
			Unit primeAssignStmt = assignGenerator.generateAssignStmt(primeLocal, primeValue);
			List<Unit> primeUnits = Lists.newArrayList();
			primeUnits.add(primeAssignStmt);
			generatedUnits.put(primeLocal, primeUnits);
			
		} else if (type instanceof ArrayType) {
			ArrayType arrayType = (ArrayType) type;
			Local arrayLocal = localGenerator.genereateFreshArrayLocal(arrayType, name, 1);
			Unit arrayAssign = assignGenerator.generateArrayAssignStmt(arrayLocal, 16);
			List<Unit> arrayUnits = Lists.newArrayList();
			arrayUnits.add(arrayAssign);
			generatedUnits.put(arrayLocal, arrayUnits);

		} else if (type instanceof RefType) {
			RefType refType = (RefType) type;
			SootClass refTypeClazz = refType.getSootClass();
			Entry<SootClass, SootMethod> init = null;
			SootClass initClazz = null;
			SootMethod initMethod = null;
			
			init = JimpleUtils.getImplementingClassAndInitMethod(refTypeClazz);
			if(init != null) {
				initClazz = init.getKey();
				initMethod = init.getValue();
			} else {
				initClazz = Scene.v().getSootClass("java.lang.Object");
				initMethod = initClazz.getMethodByName("");
			}
				
			Local initLocal = localGenerator.generateFreshLocal(initClazz.getType(), name);
		
			generatedUnits.put(initLocal, Lists.newArrayList());
			Map<Local, List<Unit>> generatedParameterUnits = generateParameterUnits(initMethod, null , null);		
			for(List<Unit> l : generatedParameterUnits.values()) {
				generatedUnits.get(initLocal).addAll(l);	
			}
			
			Local[] parameterLocals = generatedParameterUnits.keySet().toArray(new Local[0]);
			Map<Local, List<Unit>> generatedCallUnits = callGenerator.generateCallUnits(initLocal, initMethod, parameterLocals);
			for(List<Unit> l : generatedCallUnits.values()) {
				generatedUnits.get(initLocal).addAll(l);	
			}	
		}
		return generatedUnits;
	}

}
