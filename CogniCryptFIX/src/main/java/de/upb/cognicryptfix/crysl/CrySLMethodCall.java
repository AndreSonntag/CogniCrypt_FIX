package de.upb.cognicryptfix.crysl;

import java.util.List;
import java.util.Map.Entry;

import com.google.common.collect.Lists;

import crypto.rules.CrySLMethod;
import crypto.rules.CrySLRule;
import de.upb.cognicryptfix.utils.Utils;
import soot.RefType;
import soot.Scene;
import soot.SootMethod;

/**
 * TODO: documentation
 * 
 * @author Andre Sonntag
 * @date 10.03.2020
 */
public class CrySLMethodCall {

	private CrySLRule rule;
	private SootMethod sootMethod;
	private CrySLMethod crySLMethod;
	private CrySLVariableConstraintPool pool;
	private List<CrySLVariable> callParameters;
	private CrySLVariable callReturn;
	private CrySLMethodCallCriteria callCriteria;
	private int requiredUserInteractions;
	private int requiredRefTypeGenerations;
	private boolean initCall;

	public CrySLMethodCall(CrySLRule rule, SootMethod sootMethod, CrySLMethod cryslMethod, boolean initCall, CrySLVariableConstraintPool pool) {
		this.rule = rule;
		this.sootMethod = sootMethod;
		this.crySLMethod = cryslMethod;
		this.initCall = initCall;
		this.pool = pool;
		this.callParameters = resolveCallParameters();	
		this.callReturn = resolveCallReturn();
		this.requiredUserInteractions = countUserIteractions();
		this.requiredRefTypeGenerations = countRefTypeParamterGeneration();
		this.callCriteria = new CrySLMethodCallCriteria(requiredUserInteractions,requiredRefTypeGenerations);
	}
	
	private int countRefTypeParamterGeneration() {
		int countRequiredRefTypeGenerations = 0;
		
		for (CrySLVariable parameter : callParameters) {
			if (parameter.getType() instanceof RefType && parameter.getType() != Scene.v().getType("java.lang.String")) {
				countRequiredRefTypeGenerations++;
			}
		}

		return countRequiredRefTypeGenerations;
	}

	private int countUserIteractions() {
		int countRequiredUserInteractions = 0;
		for (CrySLVariable parameter : callParameters) {
			if (parameter.getVariable().equals("_")) {
				countRequiredUserInteractions += 2;
			} else {
				if(pool.getVariableConstraint(parameter) == null ) {
					countRequiredUserInteractions++;
				}
			}
		}
		return countRequiredUserInteractions < 0 ? 0 : countRequiredUserInteractions;
	}
	
	private List<CrySLVariable> resolveCallParameters() {
		List<CrySLVariable> solvedParameters = Lists.newArrayList();
		List<Entry<String, String>> unsolvedParameters = crySLMethod.getParameters();
		for (int i = 0; i < unsolvedParameters.size(); i++) {
			solvedParameters.add(pool.getVariableByName(unsolvedParameters.get(i).getKey()));
		}
		return solvedParameters;
	}
	
	private CrySLVariable resolveCallReturn() {		
		if(initCall) {
			return pool.getVariableByName(Utils.getAppropriateVarName(rule));
		} else {
			return pool.getVariableByName(crySLMethod.getRetObject().getKey());
		}
	}

	public SootMethod getSootMethod() {
		return sootMethod;
	}

	public CrySLMethod getCrySLMethod() {
		return crySLMethod;
	}

	public List<CrySLVariable> getCallParameters() {
		return callParameters;
	}

	public CrySLVariable getCallReturn() {
		return callReturn;
	}

	public CrySLMethodCallCriteria getCallCriteria() {
		return callCriteria;
	}
	
	public boolean isInitCall() {
		return initCall;
	}
	
	public CrySLRule getRule() {
		return rule;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("CrySLMethodCall [rule=");
		builder.append(rule.getClassName());
		builder.append(", sootMethod=");
		builder.append(sootMethod.getSignature());
		builder.append(", crySLMethod=");
		builder.append(crySLMethod.getMethodName());
		builder.append(", callParameters=");
		builder.append(callParameters);
		builder.append(", callReturn=");
		builder.append(callReturn);
		builder.append(", callCriteria=");
		builder.append(callCriteria);
		builder.append("]");
		return builder.toString();
	}

}
