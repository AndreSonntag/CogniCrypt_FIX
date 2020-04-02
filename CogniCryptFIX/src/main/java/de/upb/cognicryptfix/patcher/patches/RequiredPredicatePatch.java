package de.upb.cognicryptfix.patcher.patches;

import java.util.List;
import java.util.Map;

import crypto.analysis.errors.RequiredPredicateError;
import de.upb.cognicryptfix.crysl.CrySLEntity;
import de.upb.cognicryptfix.crysl.CrySLMethodCall;
import de.upb.cognicryptfix.crysl.CrySLVariable;
import de.upb.cognicryptfix.crysl.pool.CrySLEntityPool;
import de.upb.cognicryptfix.generator.JimpleCodeGeneratorByRule;
import de.upb.cognicryptfix.utils.Utils;
import soot.Body;
import soot.Local;
import soot.Unit;
import soot.jimple.InvokeStmt;

public class RequiredPredicatePatch extends AbstractPatch{
	
	private RequiredPredicateError error;
	
	private CrySLEntity entity;
	private Body body;
	private JimpleCodeGeneratorByRule generator;
	
	/**
	 * Creates a new {@link RequiredPredicatePatch} object that represents
	 * the patch for the {@link RequiredPredicateError} argument. 
	 * 
	 * @param error the error
	 */
	public RequiredPredicatePatch(RequiredPredicateError error) {
		this.error = error;
		this.entity = CrySLEntityPool.getInstance().getEntityByClassName(error.getRule().getClassName());
		this.body = error.getErrorLocation().getMethod().getActiveBody();
		this.generator = new JimpleCodeGeneratorByRule(error.getErrorLocation().getMethod().getActiveBody());
	}
	
	@Override
	public Body applyPatch() {
		InvokeStmt invokeStmt = (InvokeStmt) error.getErrorLocation().getUnit().get();
		Local usageLocal = (Local) error.getExtractedValues().getCallSite().fact().value();		
		CrySLMethodCall call = entity.getFSM().getCrySLMethodCallBySootMethod(invokeStmt.getInvokeExpr().getMethod());
		CrySLVariable variable = call.getCallParameters().get(error.getExtractedValues().getCallSite().getIndex());
		Map<Local, List<Unit>> generatedPredicateUnits = generator.generatePredicateForVariable(entity, variable, usageLocal);
		Local[] parameterLocals = generatedPredicateUnits.keySet().toArray(new Local[0]);
		List<Unit> generatedUnits = Utils.summarizeUnitLists(generatedPredicateUnits.values());	
		invokeStmt.getInvokeExpr().setArg(error.getExtractedValues().getCallSite().getIndex(), parameterLocals[0]);
		
		if(!generatedUnits.isEmpty()) {
			body.getUnits().insertBefore(generatedUnits, error.getErrorLocation().getUnit().get());
		}
		generator.generateTryCatchBlock(generatedUnits);		
		return body;
	}
	
	@Override
	public String toPatchString() {
		StringBuilder builder = new StringBuilder();
		builder.append(
				"\n------------------->--------------------RequiredPredicatePatch------------------>------------------\n");
		builder.append("Class: \t\t" + error.getErrorLocation().getMethod().getDeclaringClass().toString() + "\n");
		builder.append("Method: \t" + error.getErrorLocation().getMethod().getSignature() + "\n");
		builder.append("Error: \t\t" + error.getClass().getSimpleName() + "\n");
		builder.append("CrySLRule: \t" + entity.getRule().getClassName() + "\n");
		builder.append("Message: \t" + error.toErrorMarkerString() + "\n");
		builder.append("Patch:\t\t");
		builder.append("generate "+error.getContradictedPredicate().getPredName()+"\n");
		builder.append(
				"-----------------------------------------------------------------------------------------------\n");
		return builder.toString();
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("RequiredPredicatePatch [error=");
		builder.append(error);
		builder.append(", entity=");
		builder.append(entity.getRule().getClassName());
		builder.append("]");
		return builder.toString();
	}
}
