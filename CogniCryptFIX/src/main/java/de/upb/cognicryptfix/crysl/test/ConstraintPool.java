package de.upb.cognicryptfix.crysl.test;

import java.util.List;
import java.util.Map;

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
import soot.Value;

public class ConstraintPool {

//	private CrySLRule rule;
//	private VariablePool varPool;
//	private List<ISLConstraint> constraints;
//	private Map<CrySLVariable, ISLConstraint> variableActiveConstraintMap;	
//	
//	public ConstraintPool(CrySLRule rule, VariablePool varPool) {
//		super();
//		this.rule = rule;
//		this.varPool = varPool;
//		this.constraints = rule.getConstraints();
//		this.variableActiveConstraintMap = Maps.newHashMap();
//		initVariableActiveConstraintMap();
//		setValuesForVariablesWithActiveConstraint();
//	}
//	
//	public List<ISLConstraint> getConstraints() {
//		return constraints;
//	}
//
//	private void initVariableActiveConstraintMap() {
//		List<CrySLVariable> variableSetByImpliesCrySLConstraint = Lists.newArrayList();
//
//		for (ISLConstraint constraint : rule.getConstraints()) {
//			if (constraint instanceof CrySLValueConstraint) {
//				CrySLValueConstraint valCon = (CrySLValueConstraint) constraint;
//
//				if (variableActiveConstraintMap.get(varPool.getVariableByName(valCon.getVarName())) == null) {
//					variableActiveConstraintMap.put(varPool.getVariableByName(valCon.getVarName()), valCon);
//				}
//
//			} else if (constraint instanceof CrySLComparisonConstraint) {
//				CrySLComparisonConstraint compCon = (CrySLComparisonConstraint) constraint;
//
//				if (compCon.getLeft().getLeft() instanceof CrySLObject
//						&& compCon.getLeft().getRight() instanceof CrySLObject
//						&& compCon.getRight().getLeft() instanceof CrySLObject) {
//					CrySLObject leftLeft = (CrySLObject) compCon.getLeft().getLeft();
//					CrySLObject leftRight = (CrySLObject) compCon.getLeft().getRight();
//					CrySLObject rightLeft = (CrySLObject) compCon.getRight().getLeft();
//
//					// i.e. iterationCount + 0 > 10000 + 0;
//					if (!StringUtils.isNumeric(leftLeft.getVarName()) && StringUtils.isNumeric(leftRight.getVarName())
//							&& StringUtils.isNumeric(rightLeft.getVarName())) {
//
//						if (variableActiveConstraintMap.get(varPool.getVariableByName(leftLeft.getVarName())) == null) {
//							variableActiveConstraintMap.put(varPool.getVariableByName(leftLeft.getVarName()), compCon);
//						}
//
//					}
//				}
//			} else if (constraint instanceof CrySLConstraint) {
//				CrySLConstraint crySLCon = (CrySLConstraint) constraint;
//
//				if (crySLCon.getLeft() instanceof CrySLValueConstraint
//						&& crySLCon.getRight() instanceof CrySLValueConstraint) {
//					CrySLValueConstraint leftValueCon = (CrySLValueConstraint) crySLCon.getLeft();
//					CrySLValueConstraint rightValueCon = (CrySLValueConstraint) crySLCon.getRight();
//					CrySLVariable leftVariable = varPool.getVariableByName(leftValueCon.getVarName());
//					CrySLVariable rightVariable = varPool.getVariableByName(rightValueCon.getVarName());
//
//					if (!leftVariable.equals(rightVariable) && crySLCon.getOperator().name().equals("implies")) {
//						
//						if (variableSetByImpliesCrySLConstraint.contains(leftVariable)
//								&& variableSetByImpliesCrySLConstraint.contains(rightVariable)) {
//
//						} else if (variableSetByImpliesCrySLConstraint.contains(leftVariable)) {
//
//						} else if (variableSetByImpliesCrySLConstraint.contains(rightVariable)) {
//							if (variableActiveConstraintMap
//									.get(varPool.getVariableByName(leftValueCon.getVarName())) == null) {
//								variableActiveConstraintMap.put(varPool.getVariableByName(leftValueCon.getVarName()),
//										leftValueCon);
//							}
//						} else {
//							variableSetByImpliesCrySLConstraint.add(leftVariable);
//							variableSetByImpliesCrySLConstraint.add(rightVariable);
//							variableActiveConstraintMap.put(varPool.getVariableByName(leftValueCon.getVarName()),
//									leftValueCon);
//							variableActiveConstraintMap.put(varPool.getVariableByName(rightValueCon.getVarName()),
//									rightValueCon);
//						}
//					} else {
//						if (!variableSetByImpliesCrySLConstraint.contains(leftVariable)) {
//							if (variableActiveConstraintMap
//									.get(varPool.getVariableByName(leftValueCon.getVarName())) == null) {
//								variableActiveConstraintMap.put(varPool.getVariableByName(leftValueCon.getVarName()),
//										leftValueCon);
//							}
//						}
//					}
//
//				} else if (crySLCon.getLeft() instanceof CrySLValueConstraint
//						&& !(crySLCon.getRight() instanceof CrySLValueConstraint)) {
//					CrySLValueConstraint leftValueCon = (CrySLValueConstraint) crySLCon.getLeft();
//					CrySLVariable leftVariable = varPool.getVariableByName(leftValueCon.getVarName());
//
//					if (!variableSetByImpliesCrySLConstraint.contains(leftVariable)) {
//						if (variableActiveConstraintMap.get(leftVariable) == null) {
//							variableActiveConstraintMap.put(leftVariable, leftValueCon);
//						}
//					}
//				}
//			}
//		}
//	}
//	
//	private void setValuesForVariablesWithActiveConstraint() {
//
//		for (CrySLVariable variable : variableActiveConstraintMap.keySet()) {
//			if (variableActiveConstraintMap.get(variable) == null) {
//				continue;
//			}
//			
//			ISLConstraint activeConstraint = variableActiveConstraintMap.get(variable);
////			variable.setActiveConstraint(activeConstraint);
//
//			if (activeConstraint instanceof CrySLComparisonConstraint) {
//				CrySLComparisonConstraint compCon = (CrySLComparisonConstraint) activeConstraint;
//				String operator = ComparisonConstraint.resolveComparsionOperator(compCon.getOperator());
//
//				ArithmeticConstraint aritConPlus = new ArithmeticConstraint(CrySLArithmeticConstraint.ArithOp.p,
//						new Pair<String, String>("_", compCon.getRight().getLeft().getName()),
//						new Pair<String, String>("_", "1"));
//				ArithmeticConstraint aritConMinus = new ArithmeticConstraint(CrySLArithmeticConstraint.ArithOp.m,
//						new Pair<String, String>("_", compCon.getRight().getLeft().getName()),
//						new Pair<String, String>("_", "1"));
//				Value parameterValue = JimpleUtils.generateConstantValue(variable.getType(),
//						compCon.getRight().getLeft().getName());
//
//				switch (operator) {
//				case "<":
//					variable.setValue(
//							JimpleUtils.generateConstantValue(variable.getType(), aritConMinus.resolveConstraint()));
//					break;
//				case ">":
//					variable.setValue(
//							JimpleUtils.generateConstantValue(variable.getType(), aritConPlus.resolveConstraint()));
//					break;
//				case "<=":
//					variable.setValue(parameterValue);
//					break;
//				case ">=":
//					variable.setValue(parameterValue);
//					break;
//				case "!=":
//					variable.setValue(
//							JimpleUtils.generateConstantValue(variable.getType(), aritConPlus.resolveConstraint()));
//					break;
//				case "=":
//					variable.setValue(parameterValue);
//					break;
//				}
//				System.out.println(rule.getClassName() + " " + variable.getVariable() + " " + variable.getType() + " "
//						+ variable.getValue());
//
//			} else if (activeConstraint instanceof CrySLValueConstraint) {
//				CrySLValueConstraint valueCon = (CrySLValueConstraint) activeConstraint;
//				variable.setValue(
//						JimpleUtils.generateConstantValue(variable.getType(), valueCon.getValueRange().get(0)));
//				System.out.println(rule.getClassName() + " " + variable.getVariable() + " " + variable.getType() + " "
//						+ variable.getValue());
//			}
//
//		}
//	}
//	
}
