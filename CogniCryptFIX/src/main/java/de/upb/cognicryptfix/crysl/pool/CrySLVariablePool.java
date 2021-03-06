package de.upb.cognicryptfix.crysl.pool;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import crypto.interfaces.ISLConstraint;
import crypto.rules.CrySLArithmeticConstraint;
import crypto.rules.CrySLComparisonConstraint;
import crypto.rules.CrySLConstraint;
import crypto.rules.CrySLObject;
import crypto.rules.CrySLRule;
import crypto.rules.CrySLValueConstraint;
import de.upb.cognicryptfix.crysl.CrySLVariable;
import de.upb.cognicryptfix.generator.jimple.JimpleUtils;
import de.upb.cognicryptfix.utils.ArithmeticConstraintSolver;
import de.upb.cognicryptfix.utils.Pair;
import de.upb.cognicryptfix.utils.Utils;
import soot.RefType;
import soot.Scene;
import soot.Value;
import soot.jimple.NullConstant;

public class CrySLVariablePool {

	private CrySLRule rule;
	private List<CrySLVariable> variables;
	private List<ISLConstraint> constraints;
	private Map<String, CrySLVariable> nameVariableMap;
	private Map<CrySLVariable, List<ISLConstraint>> variableISLConstraintMap;
	private Map<CrySLVariable, ISLConstraint> variableActiveConstraintMap;	

	public CrySLVariablePool(CrySLRule rule) {
		this.rule = rule;
		this.variables = Lists.newArrayList();
		this.constraints = rule.getConstraints();
		this.nameVariableMap = Maps.newHashMap();
		this.variableISLConstraintMap = Maps.newHashMap();
		this.variableActiveConstraintMap = Maps.newHashMap();
		init();
	}

	private void init() {
		for (Entry<String, String> object : rule.getObjects()) {
			CrySLVariable variable = new CrySLVariable(object.getValue(), Utils.getType(rule, object.getValue()));
			variables.add(variable);
			nameVariableMap.put(variable.getName(), variable);
			variableISLConstraintMap.put(variable, Lists.newArrayList());
			variableActiveConstraintMap.put(variable, null);
		}
		
		CrySLVariable thisVar = new CrySLVariable("this", Scene.v().getType(rule.getClassName()));
		variables.add(thisVar);
		nameVariableMap.put(thisVar.getName(), thisVar);
		variableISLConstraintMap.put(thisVar, Lists.newArrayList());
		variableActiveConstraintMap.put(thisVar, null);

		CrySLVariable initVar = new CrySLVariable(Utils.getAppropriateVarName(rule),
				Scene.v().getType(rule.getClassName()));
		variables.add(initVar);
		nameVariableMap.put(initVar.getName(), initVar);
		variableISLConstraintMap.put(initVar, Lists.newArrayList());
		variableActiveConstraintMap.put(initVar, null);

		CrySLVariable voidReturnVar = new CrySLVariable("void", NullConstant.v().getType());
		variables.add(voidReturnVar);
		nameVariableMap.put(voidReturnVar.getName(), voidReturnVar);
		variableISLConstraintMap.put(voidReturnVar, Lists.newArrayList());
		variableActiveConstraintMap.put(voidReturnVar, null);

		CrySLVariable substitutedVar = new CrySLVariable("_", Utils.getType(rule, "_"));
		variables.add(substitutedVar);
		nameVariableMap.put(substitutedVar.getName(), substitutedVar);
		variableISLConstraintMap.put(substitutedVar, Lists.newArrayList());
		variableActiveConstraintMap.put(substitutedVar, null);

		computeVariableConstraintMapping();
		setValues();
	}
	

	private void computeVariableConstraintMapping() {

		List<CrySLVariable> variableSetByImpliesCrySLConstraint = Lists.newArrayList();

		for (ISLConstraint constraint : rule.getConstraints()) {
			if (constraint instanceof CrySLValueConstraint) {
				CrySLValueConstraint valCon = (CrySLValueConstraint) constraint;

				if (variableActiveConstraintMap.get(getVariableByName(valCon.getVarName())) == null) {
					variableActiveConstraintMap.put(getVariableByName(valCon.getVarName()), valCon);
				}

			} else if (constraint instanceof CrySLComparisonConstraint) {
				CrySLComparisonConstraint compCon = (CrySLComparisonConstraint) constraint;

				if (compCon.getLeft().getLeft() instanceof CrySLObject
						&& compCon.getLeft().getRight() instanceof CrySLObject
						&& compCon.getRight().getLeft() instanceof CrySLObject) {
					CrySLObject leftLeft = (CrySLObject) compCon.getLeft().getLeft();
					CrySLObject leftRight = (CrySLObject) compCon.getLeft().getRight();
					CrySLObject rightLeft = (CrySLObject) compCon.getRight().getLeft();

					// i.e. iterationCount + 0 > 10000 + 0;
					if (!StringUtils.isNumeric(leftLeft.getVarName()) && StringUtils.isNumeric(leftRight.getVarName())
							&& StringUtils.isNumeric(rightLeft.getVarName())) {

						if (variableActiveConstraintMap.get(getVariableByName(leftLeft.getVarName())) == null) {
							variableActiveConstraintMap.put(getVariableByName(leftLeft.getVarName()), compCon);
						}

					}
				}
			} else if (constraint instanceof CrySLConstraint) {
				CrySLConstraint crySLCon = (CrySLConstraint) constraint;

				if (crySLCon.getLeft() instanceof CrySLValueConstraint
						&& crySLCon.getRight() instanceof CrySLValueConstraint) {
					CrySLValueConstraint leftValueCon = (CrySLValueConstraint) crySLCon.getLeft();
					CrySLValueConstraint rightValueCon = (CrySLValueConstraint) crySLCon.getRight();
					CrySLVariable leftVariable = getVariableByName(leftValueCon.getVarName());
					CrySLVariable rightVariable = getVariableByName(rightValueCon.getVarName());

					if (!leftVariable.equals(rightVariable) && crySLCon.getOperator().name().equals("implies")) {
						
						if (variableSetByImpliesCrySLConstraint.contains(leftVariable)
								&& variableSetByImpliesCrySLConstraint.contains(rightVariable)) {

						} else if (variableSetByImpliesCrySLConstraint.contains(leftVariable)) {

						} else if (variableSetByImpliesCrySLConstraint.contains(rightVariable)) {
							if (variableActiveConstraintMap
									.get(getVariableByName(leftValueCon.getVarName())) == null) {
								variableActiveConstraintMap.put(getVariableByName(leftValueCon.getVarName()),
										leftValueCon);
							}
						} else {
							variableSetByImpliesCrySLConstraint.add(leftVariable);
							variableSetByImpliesCrySLConstraint.add(rightVariable);
							variableActiveConstraintMap.put(getVariableByName(leftValueCon.getVarName()),
									leftValueCon);
							variableActiveConstraintMap.put(getVariableByName(rightValueCon.getVarName()),
									rightValueCon);
						}
					} else {
						if (!variableSetByImpliesCrySLConstraint.contains(leftVariable)) {
							if (variableActiveConstraintMap
									.get(getVariableByName(leftValueCon.getVarName())) == null) {
								variableActiveConstraintMap.put(getVariableByName(leftValueCon.getVarName()),
										leftValueCon);
							}
						}
					}

				} else if (crySLCon.getLeft() instanceof CrySLValueConstraint
						&& !(crySLCon.getRight() instanceof CrySLValueConstraint)) {
					CrySLValueConstraint leftValueCon = (CrySLValueConstraint) crySLCon.getLeft();
					CrySLVariable leftVariable = getVariableByName(leftValueCon.getVarName());

					if (!variableSetByImpliesCrySLConstraint.contains(leftVariable)) {
						if (variableActiveConstraintMap.get(leftVariable) == null) {
							variableActiveConstraintMap.put(leftVariable, leftValueCon);
						}
					}
				}
			}
		}
	}

	private void setValues() {

		for (CrySLVariable variable : variableActiveConstraintMap.keySet()) {
			if (variableActiveConstraintMap.get(variable) == null) {
				continue;
			} else if(variable.getType() instanceof RefType && variable.getType() != Scene.v().getType("java.lang.String")) {
				continue;
			} else if(variable.getType() == null) {
				throw new RuntimeException(rule.getClassName()+" variable: "+variable.toString()+" Type is null.");
			}
			
			ISLConstraint activeConstraint = variableActiveConstraintMap.get(variable);
			if (activeConstraint instanceof CrySLComparisonConstraint) {
				CrySLComparisonConstraint compCon = (CrySLComparisonConstraint) activeConstraint;
				String operator = Utils.resolveComparsionOperator(compCon.getOperator());

				ArithmeticConstraintSolver aritConPlus = new ArithmeticConstraintSolver(CrySLArithmeticConstraint.ArithOp.p,
						new Pair<String, String>("_", compCon.getRight().getLeft().getName()),
						new Pair<String, String>("_", "1"));
				ArithmeticConstraintSolver aritConMinus = new ArithmeticConstraintSolver(CrySLArithmeticConstraint.ArithOp.m,
						new Pair<String, String>("_", compCon.getRight().getLeft().getName()),
						new Pair<String, String>("_", "1"));
				Value parameterValue = JimpleUtils.generateConstantValue(variable.getType(), compCon.getRight().getLeft().getName());

				switch (operator) {
				case "<":
					variable.setValue(
							JimpleUtils.generateConstantValue(variable.getType(), aritConMinus.resolveConstraint()));
					break;
				case ">":
					variable.setValue(
							JimpleUtils.generateConstantValue(variable.getType(), aritConPlus.resolveConstraint()));
					break;
				case "<=":
					variable.setValue(parameterValue);
					break;
				case ">=":
					variable.setValue(parameterValue);
					break;
				case "!=":
					variable.setValue(
							JimpleUtils.generateConstantValue(variable.getType(), aritConPlus.resolveConstraint()));
					break;
				case "=":
					variable.setValue(parameterValue);
					break;
				}
//				System.out.println(rule.getClassName() + " " + variable.getVariable() + " " + variable.getType() + " "
//						+ variable.getValue());

			} else if (activeConstraint instanceof CrySLValueConstraint) {
				CrySLValueConstraint valueCon = (CrySLValueConstraint) activeConstraint;
				variable.setValue(JimpleUtils.generateConstantValue(variable.getType(), valueCon.getValueRange().get(0)));
//				System.out.println(rule.getClassName() + " " + variable.getVariable() + " " + variable.getType() + " "
//						+ variable.getValue());
			}

		}
	}

	public CrySLVariable getVariableByName(String name) {
		return nameVariableMap.get(name);
	}

	public ISLConstraint getVariableConstraint(CrySLVariable variable) {
		return variableActiveConstraintMap.get(variable);
	}

	public List<CrySLVariable> getVariables() {
		return variables;
	}

	public List<ISLConstraint> getConstraints() {
		return constraints;
	}
}
