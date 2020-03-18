package de.upb.cognicryptfix.crysl;

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
import de.upb.cognicryptfix.extractor.constraints.ArithmeticConstraint;
import de.upb.cognicryptfix.extractor.constraints.ComparisonConstraint;
import de.upb.cognicryptfix.generator.jimple.JimpleUtils;
import de.upb.cognicryptfix.utils.Pair;
import de.upb.cognicryptfix.utils.Utils;
import soot.Scene;
import soot.Value;
import soot.jimple.NullConstant;

public class CrySLVariableConstraintPool {

	private CrySLRule rule;
	private List<CrySLVariable> variables;
	private List<ISLConstraint> constraints;
	private Map<String, CrySLVariable> nameVariableMap;
	private Map<CrySLVariable, List<ISLConstraint>> variableISLConstraintMap;
	private Map<CrySLVariable, ISLConstraint> variableActiveConstraintMapping;	//TODO refactor

	public CrySLVariableConstraintPool(CrySLRule rule) {
		this.rule = rule;
		this.variables = Lists.newArrayList();
		this.constraints = rule.getConstraints();
		this.nameVariableMap = Maps.newHashMap();
		this.variableISLConstraintMap = Maps.newHashMap();
		this.variableActiveConstraintMapping = Maps.newHashMap();
		init();
	}

	private void init() {
		for (Entry<String, String> object : rule.getObjects()) {
			CrySLVariable variable = new CrySLVariable(object.getValue(), Utils.getType(rule, object.getValue()));
			variables.add(variable);
			nameVariableMap.put(variable.getVariable(), variable);
			variableISLConstraintMap.put(variable, Lists.newArrayList());
			variableActiveConstraintMapping.put(variable, null);
		}

		CrySLVariable initVar = new CrySLVariable(Utils.getAppropriateVarName(rule),
				Scene.v().getType(rule.getClassName()));
		variables.add(initVar);
		nameVariableMap.put(initVar.getVariable(), initVar);
		variableISLConstraintMap.put(initVar, Lists.newArrayList());
		variableActiveConstraintMapping.put(initVar, null);

		CrySLVariable voidReturnVar = new CrySLVariable("void", NullConstant.v().getType());
		variables.add(voidReturnVar);
		nameVariableMap.put(voidReturnVar.getVariable(), voidReturnVar);
		variableISLConstraintMap.put(voidReturnVar, Lists.newArrayList());
		variableActiveConstraintMapping.put(voidReturnVar, null);

		CrySLVariable substitutedVar = new CrySLVariable("_", Utils.getType(rule, "_"));
		variables.add(substitutedVar);
		nameVariableMap.put(substitutedVar.getVariable(), substitutedVar);
		variableISLConstraintMap.put(substitutedVar, Lists.newArrayList());
		variableActiveConstraintMapping.put(substitutedVar, null);

		computeVariableConstraintMapping();
		setValues();
	}
	

	private void computeVariableConstraintMapping() {

		List<CrySLVariable> variableSetByImpliesCrySLConstraint = Lists.newArrayList();

		for (ISLConstraint constraint : rule.getConstraints()) {
			if (constraint instanceof CrySLValueConstraint) {
				CrySLValueConstraint valCon = (CrySLValueConstraint) constraint;

				if (variableActiveConstraintMapping.get(getVariableByName(valCon.getVarName())) == null) {
					variableActiveConstraintMapping.put(getVariableByName(valCon.getVarName()), valCon);
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

						if (variableActiveConstraintMapping.get(getVariableByName(leftLeft.getVarName())) == null) {
							variableActiveConstraintMapping.put(getVariableByName(leftLeft.getVarName()), compCon);
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
							if (variableActiveConstraintMapping
									.get(getVariableByName(leftValueCon.getVarName())) == null) {
								variableActiveConstraintMapping.put(getVariableByName(leftValueCon.getVarName()),
										leftValueCon);
							}
						} else {
							variableSetByImpliesCrySLConstraint.add(leftVariable);
							variableSetByImpliesCrySLConstraint.add(rightVariable);
							variableActiveConstraintMapping.put(getVariableByName(leftValueCon.getVarName()),
									leftValueCon);
							variableActiveConstraintMapping.put(getVariableByName(rightValueCon.getVarName()),
									rightValueCon);
						}
					} else {
						if (!variableSetByImpliesCrySLConstraint.contains(leftVariable)) {
							if (variableActiveConstraintMapping
									.get(getVariableByName(leftValueCon.getVarName())) == null) {
								variableActiveConstraintMapping.put(getVariableByName(leftValueCon.getVarName()),
										leftValueCon);
							}
						}
					}

				} else if (crySLCon.getLeft() instanceof CrySLValueConstraint
						&& !(crySLCon.getRight() instanceof CrySLValueConstraint)) {
					CrySLValueConstraint leftValueCon = (CrySLValueConstraint) crySLCon.getLeft();
					CrySLVariable leftVariable = getVariableByName(leftValueCon.getVarName());

					if (!variableSetByImpliesCrySLConstraint.contains(leftVariable)) {
						if (variableActiveConstraintMapping.get(leftVariable) == null) {
							variableActiveConstraintMapping.put(leftVariable, leftValueCon);
						}
					}
				}
			}
		}
	}

	private void setValues() {

		for (CrySLVariable variable : variableActiveConstraintMapping.keySet()) {
			if (variableActiveConstraintMapping.get(variable) == null) {
				continue;
			}
			ISLConstraint activeConstraint = variableActiveConstraintMapping.get(variable);

			if (activeConstraint instanceof CrySLComparisonConstraint) {
				CrySLComparisonConstraint compCon = (CrySLComparisonConstraint) activeConstraint;
				String operator = ComparisonConstraint.resolveComparsionOperator(compCon.getOperator());

				ArithmeticConstraint aritConPlus = new ArithmeticConstraint(CrySLArithmeticConstraint.ArithOp.p,
						new Pair<String, String>("_", compCon.getRight().getLeft().getName()),
						new Pair<String, String>("_", "1"));
				ArithmeticConstraint aritConMinus = new ArithmeticConstraint(CrySLArithmeticConstraint.ArithOp.m,
						new Pair<String, String>("_", compCon.getRight().getLeft().getName()),
						new Pair<String, String>("_", "1"));
				Value parameterValue = JimpleUtils.generateConstantValue(variable.getType(),
						compCon.getRight().getLeft().getName());

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
				System.out.println(rule.getClassName() + " " + variable.getVariable() + " " + variable.getType() + " "
						+ variable.getValue());

			} else if (activeConstraint instanceof CrySLValueConstraint) {
				CrySLValueConstraint valueCon = (CrySLValueConstraint) activeConstraint;
				variable.setValue(
						JimpleUtils.generateConstantValue(variable.getType(), valueCon.getValueRange().get(0)));
				System.out.println(rule.getClassName() + " " + variable.getVariable() + " " + variable.getType() + " "
						+ variable.getValue());
			}

		}
	}

	public CrySLVariable getVariableByName(String name) {
		return nameVariableMap.get(name);
	}

	public ISLConstraint getVariableConstraint(CrySLVariable variable) {
		return variableActiveConstraintMapping.get(variable);
	}

	public List<CrySLVariable> getVariables() {
		return variables;
	}

	public List<ISLConstraint> getConstraints() {
		return constraints;
	}
}
