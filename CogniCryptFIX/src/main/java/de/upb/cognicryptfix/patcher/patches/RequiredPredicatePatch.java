package de.upb.cognicryptfix.patcher.patches;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.AbstractMap.SimpleEntry;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.Lists;

import crypto.analysis.errors.RequiredPredicateError;
import crypto.rules.CrySLObject;
import de.upb.cognicryptfix.Constants;
import de.upb.cognicryptfix.crysl.CrySLEntity;
import de.upb.cognicryptfix.crysl.CrySLMethodCall;
import de.upb.cognicryptfix.crysl.CrySLPredicate;
import de.upb.cognicryptfix.crysl.CrySLPredicateFilter;
import de.upb.cognicryptfix.crysl.CrySLVariable;
import de.upb.cognicryptfix.crysl.pool.CrySLEntityPool;
import de.upb.cognicryptfix.exception.jimple.NotExpectedUnitException;
import de.upb.cognicryptfix.exception.patch.RepairException;
import de.upb.cognicryptfix.generator.JimpleCodeGeneratorByRule;
import de.upb.cognicryptfix.generator.jimple.JimpleUtils;
import de.upb.cognicryptfix.scheduler.ErrorScheduler;
import de.upb.cognicryptfix.utils.Utils;
import soot.Body;
import soot.Local;
import soot.Unit;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;

public class RequiredPredicatePatch extends AbstractPatch{
	
	private static final Logger LOGGER = LogManager.getLogger(RequiredPredicatePatch.class);
	
	private RequiredPredicateError error;
	
	private CrySLEntityPool pool;
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
		this.pool = CrySLEntityPool.getInstance();
		this.entity = pool.getEntityByClassName(error.getRule().getClassName());
		this.body = error.getErrorLocation().getMethod().getActiveBody();
		this.generator = new JimpleCodeGeneratorByRule(error.getErrorLocation().getMethod().getActiveBody());
	}
	
	@Override
	public Body applyPatch() throws RepairException{
		Unit errorUnit = error.getErrorLocation().getUnit().get();

		if(JimpleUtils.containsInvokeExpr(errorUnit)) {
			InvokeExpr errorInvokeExpr = JimpleUtils.getInvokeExpr(errorUnit);
			Value errorValue = error.getExtractedValues().getCallSite().fact().value();
			Local usageLocal = null;  
			if(errorValue instanceof Local) {
				usageLocal = (Local) errorValue;
			}

			CrySLMethodCall call = entity.getFSM().getCrySLMethodCallBySootMethod(errorInvokeExpr.getMethod());
			CrySLVariable variable = call.getCallParameters().get(error.getExtractedValues().getCallSite().getIndex());
			List<CrySLPredicate> reqPredicatesForVariable = entity.getRequiredPredicateForVariableByType(variable);
			List<CrySLPredicate> matchingPredicates = Lists.newArrayList();
			CrySLPredicate contradictedPredicate = null;
			for(CrySLPredicate predicate : reqPredicatesForVariable) {
				if(predicate.getPredicate_().getPredName().equals(error.getContradictedPredicate().getPredName())) {
					matchingPredicates.add(predicate);
				}
			}			
			if(matchingPredicates.size() > 1) {
				Entry<CrySLPredicate, LinkedList<CrySLMethodCall>> predicateEntry = CrySLPredicateFilter.applyPredicateCriteriaFilters(matchingPredicates);
				contradictedPredicate = predicateEntry.getKey();
			} else if (matchingPredicates.size() == 1){
				contradictedPredicate = matchingPredicates.get(0);
			} else {
				throw new RepairException("No Predicate Object found for contradicted predicate: "+error.getContradictedPredicate());
			}

			Map<Local, List<Unit>> generatedPredicateUnits = generator.generatePredicateForVariable(contradictedPredicate, usageLocal);
			Local[] parameterLocals = generatedPredicateUnits.keySet().toArray(new Local[0]);
			List<Unit> generatedUnits = Utils.summarizeUnitLists(generatedPredicateUnits.values());	
			errorInvokeExpr.setArg(error.getExtractedValues().getCallSite().getIndex(), parameterLocals[0]);
			
			
			if(errorUnit instanceof InvokeStmt) {
				InvokeStmt errorInvokeStmt = (InvokeStmt) errorUnit;
				errorInvokeStmt.setInvokeExpr(errorInvokeExpr);
			} else if(errorUnit instanceof AssignStmt) {
				AssignStmt errorAssignStmt = (AssignStmt) errorUnit;
				errorAssignStmt.setRightOp(errorInvokeExpr);
			} else {
				throw new NotExpectedUnitException("errorUnit isn't an InvokeStmt or AssignStmt");
			}
					
		if(!generatedUnits.isEmpty()) {

			List<Unit> afterPredicateUnits = generatedUnits.stream()                
			              .filter(unit -> unit.getTag(Constants.AFTER_PREDICATE_TAG) != null)
			               .collect(Collectors.toList());
				 
			if(!afterPredicateUnits.isEmpty()) {
				generatedUnits.removeAll(afterPredicateUnits); 
				body.getUnits().insertAfter(afterPredicateUnits, errorUnit);
			} 
			body.getUnits().insertBefore(generatedUnits, errorUnit);
		} 
		
		}else {
			throw new NotExpectedUnitException("RequiredPredicatePatch ErrorLocation doesn't contain an InvokeExpr");
		}
		return body;
	}
	
	@Override
	public String toPatchString() {
		StringBuilder builder = new StringBuilder();
		builder.append("\n__________[RequiredPredicatePatch]__________\n");
		builder.append("Repaired in Round: \t" + ErrorScheduler.round+ "\n");
		builder.append("Class: \t" + error.getErrorLocation().getMethod().getDeclaringClass().toString() + "\n");
		builder.append("Method: \t" + error.getErrorLocation().getMethod().getSignature() + "\n");
		builder.append("Error: \t" + error.getClass().getSimpleName() + "\n");
		builder.append("CrySLRule: \t" + entity.getRule().getClassName() + "\n");
		builder.append("Message: \t" + error.toErrorMarkerString() + "\n");
		builder.append("Patch: \t");
		builder.append("generate "+error.getContradictedPredicate().getPredName());
		builder.append("\n");
		builder.append("__________________________________________");
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
