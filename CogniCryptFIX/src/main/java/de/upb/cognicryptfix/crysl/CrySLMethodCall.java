package de.upb.cognicryptfix.crysl;

import java.util.List;
import java.util.Map.Entry;

import com.google.common.collect.Lists;

import crypto.rules.CrySLMethod;
import crypto.rules.CrySLRule;
import de.upb.cognicryptfix.crysl.pool.CrySLVariablePool;
import de.upb.cognicryptfix.utils.Utils;
import soot.SootMethod;

/**
 * @author Andre Sonntag
 * @date 10.03.2020
 */
public class CrySLMethodCall {

	private CrySLRule rule;
	private SootMethod sootMethod;
	private CrySLMethod crySLMethod;
	private CrySLVariablePool pool;
	private List<CrySLVariable> callParameters;
	private CrySLVariable callReturn;
	private boolean initCall;
	private boolean afterPredicateCall;

	public CrySLMethodCall(CrySLMethodCall copy) {
		this.rule = copy.rule;
		this.sootMethod = copy.sootMethod;
		this.crySLMethod = copy.crySLMethod;
		this.pool = copy.pool;
		this.callParameters = copy.callParameters;
		this.callReturn = copy.callReturn;
		this.initCall = copy.initCall;
		this.afterPredicateCall = copy.afterPredicateCall;
	}
	
	public CrySLMethodCall(CrySLRule rule, SootMethod sootMethod, CrySLMethod cryslMethod, CrySLVariablePool pool) {
		this.rule = rule;
		this.sootMethod = sootMethod;
		this.crySLMethod = cryslMethod;
		this.initCall = isInitCall();
		this.pool = pool;
		this.callParameters = resolveCallParameters();	
		this.callReturn = resolveCallReturn();
		this.afterPredicateCall = false;
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
	
	public int getParamterIndex(CrySLVariable paramter) {
		return callParameters.indexOf(paramter);
	}

	public CrySLVariable getCallReturn() {
		return callReturn;
	}

	public boolean isInitCall() {
		return sootMethod.isConstructor() || rule.getClassName().equals(sootMethod.getReturnType().toQuotedString()) ? true : false;
	}
	
	public CrySLRule getRule() {
		return rule;
	}
	
	public CrySLVariablePool getPool() {
		return pool;
	}
	
	public boolean isAfterPredicateCall() {
		return afterPredicateCall;
	}

	public void setAfterPredicateCall(boolean afterPredicateCall) {
		this.afterPredicateCall = afterPredicateCall;
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
//		builder.append(", callParameters=");
//		builder.append(callParameters);
//		builder.append(", callReturn=");
//		builder.append(callReturn);
//		builder.append(", callCriteria=");
//		builder.append(callCriteria);
		builder.append("]");
		return builder.toString();
	}

}
