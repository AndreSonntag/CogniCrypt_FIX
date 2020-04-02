package de.upb.cognicryptfix.crysl.test;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import crypto.rules.CrySLRule;
import de.upb.cognicryptfix.crysl.CrySLVariable;
import de.upb.cognicryptfix.utils.Utils;
import soot.Scene;
import soot.jimple.NullConstant;

public class VariablePool {

//	private CrySLRule rule;
//	private List<CrySLVariable> variables;
//	private Map<String, CrySLVariable> nameVariableMap;
//
//	public VariablePool(CrySLRule rule) {
//		super();
//		this.rule = rule;
//		this.variables = Lists.newArrayList();
//		this.nameVariableMap = Maps.newHashMap();
//		initCrySLVariables();
//	}
//
//	private void initCrySLVariables() {
//		for (Entry<String, String> object : rule.getObjects()) {
//			CrySLVariable variable = new CrySLVariable(object.getValue(), Utils.getType(rule, object.getValue()));
//			variables.add(variable);
//			nameVariableMap.put(variable.getVariable(), variable);
//
//		}
//		
//		CrySLVariable initVariable = new CrySLVariable(Utils.getAppropriateVarName(rule), Scene.v().getType(rule.getClassName()));
//		variables.add(initVariable);
//		nameVariableMap.put(initVariable.getVariable(), initVariable);
//		
//		CrySLVariable voidReturnVariable = new CrySLVariable("void", NullConstant.v().getType());
//		variables.add(voidReturnVariable);
//		nameVariableMap.put(voidReturnVariable.getVariable(), voidReturnVariable);
//
//		CrySLVariable substitutedVariable = new CrySLVariable("_", Utils.getType(rule, "_"));
//		variables.add(substitutedVariable);
//		nameVariableMap.put(substitutedVariable.getVariable(), substitutedVariable);
//
//	}
//
//	public List<CrySLVariable> getVariables() {
//		return variables;
//	}
//	
//	public CrySLVariable getVariableByName(String name) {
//		return nameVariableMap.get(name);
//	}
//
//	public Map<String, CrySLVariable> getNameVariableMap() {
//		return nameVariableMap;
//	}
}
