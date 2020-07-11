package de.upb.cognicryptfix.patcher.patches;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import crypto.analysis.errors.ForbiddenMethodError;
import de.upb.cognicryptfix.Constants;
import de.upb.cognicryptfix.crysl.CrySLEntity;
import de.upb.cognicryptfix.crysl.CrySLMethodCall;
import de.upb.cognicryptfix.crysl.pool.CrySLEntityPool;
import de.upb.cognicryptfix.exception.generation.GenerationException;
import de.upb.cognicryptfix.exception.patch.RepairException;
import de.upb.cognicryptfix.exception.path.NoCallFoundException;
import de.upb.cognicryptfix.exception.path.PathException;
import de.upb.cognicryptfix.generator.JimpleCodeGeneratorByRule;
import de.upb.cognicryptfix.generator.jimple.JimpleUtils;
import de.upb.cognicryptfix.scheduler.ErrorScheduler;
import de.upb.cognicryptfix.utils.InitializationMethodSorter;
import de.upb.cognicryptfix.utils.Utils;
import soot.Body;
import soot.Local;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.ValueBox;
import soot.jimple.AssignStmt;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.internal.JimpleLocalBox;

/**
 * 
 * @author Andre Sonntag
 * @date 18.03.2020
 *
 */
public class ForbiddenMethodPatch extends AbstractPatch {

	private static final Logger LOGGER = LogManager.getLogger(ForbiddenMethodPatch.class);
	private ForbiddenMethodError error;
	
	private CrySLEntity entity;
	private Body body;
	private JimpleCodeGeneratorByRule generator;
	private Unit patch;

	/**
	 * Creates a new {@link ForbiddenMethodPatch} object that represents
	 * the patch for the {@link ForbiddenMethodError} argument. 
	 * 
	 * @param error the error
	 */
	public ForbiddenMethodPatch(ForbiddenMethodError error) {
		this.error = error;
		this.entity = CrySLEntityPool.getInstance().getEntityByClassName(error.getRule().getClassName());
		this.body = error.getErrorLocation().getMethod().getActiveBody();
		this.generator = new JimpleCodeGeneratorByRule(error.getErrorLocation().getMethod().getActiveBody());
		this.patch = null;
	}

	@Override
	public Body applyPatch() throws RepairException{
		Unit forbiddenUnit = error.getErrorLocation().getUnit().get();
		SootMethod forbiddenMethod = error.getCalledMethod();
		ArrayList<SootMethod> alternativeMethods = new ArrayList<>(error.getAlternatives());

		if (CollectionUtils.isEmpty(alternativeMethods)) {
			body.getUnits().remove(forbiddenUnit);
		} else {
			Collections.sort(alternativeMethods, new InitializationMethodSorter());
			replaceForbiddenMethod(forbiddenUnit, forbiddenMethod, alternativeMethods.get(0));
		}
		generator.removeUnnecessaryTryCatchBlock();
		return body;
	}

	private void replaceForbiddenMethod(Unit forbiddenUnit, SootMethod forbiddenMethod, SootMethod alternativeMethod) throws GenerationException, PathException {

		Map<Local, List<Unit>> generatedCallUnits = Maps.newLinkedHashMap();
		Map<Local, List<Unit>> generatedParameterUnits = Maps.newLinkedHashMap();
		CrySLMethodCall call = entity.getFSM().getCrySLMethodCallBySootMethod(alternativeMethod);
		
		Map<Integer, Local> parameterMatches = Maps.newHashMap();
		parameterMatches = calcParameterMatches(forbiddenUnit, alternativeMethod); 
		
		if(MapUtils.isEmpty(parameterMatches)) {
			generatedParameterUnits = generator.generateParameters(call);
		} else {
			generatedParameterUnits = generator.generateParameters(call, parameterMatches);
		}
		
		Local invokeLocal = JimpleUtils.getInvokeLocal(forbiddenUnit);		
		Local[] parameterLocals = generatedParameterUnits.keySet().toArray(new Local[0]);
		generatedCallUnits = generator.generateCall(invokeLocal, call, true, parameterLocals);

		List<Unit> parameterUnitList = Utils.summarizeUnitLists(generatedParameterUnits.values());
		List<Unit> callUnitList = Utils.summarizeUnitLists(generatedCallUnits.values());
		
		insertParameterUnits(forbiddenUnit, parameterUnitList);
		insertAlternativeCallUnits(forbiddenMethod, forbiddenUnit, alternativeMethod, callUnitList);
	}

	private void insertParameterUnits(Unit forbiddenUnit, List<Unit> parameterUnits) {
		if (!parameterUnits.isEmpty()) {
			
			List<Unit> afterPredicateUnits = parameterUnits.stream()                
		                .filter(unit -> unit.getTag(Constants.AFTER_PREDICATE_TAG) != null)
		                .collect(Collectors.toList());
			 
			if(!afterPredicateUnits.isEmpty()) {
				parameterUnits.removeAll(afterPredicateUnits); 
				body.getUnits().insertAfter(afterPredicateUnits, forbiddenUnit);
			} 
			
			body.getUnits().insertBefore(parameterUnits, forbiddenUnit);
		}
	}

	private void insertAlternativeCallUnits(SootMethod forbiddenMethod, Unit forbiddenUnit,
			SootMethod alternativeMethod, List<Unit> alternativeUnits) {
		InvokeExpr alternativeInvokeExpr = null;
		for (Unit u : alternativeUnits) {
			if (JimpleUtils.containsInvokeExpr(u)) {
				alternativeInvokeExpr = JimpleUtils.getInvokeExpr(u);
				break;
			}
		}

		for (ValueBox box : alternativeInvokeExpr.getUseBoxes()) {
			if (box instanceof JimpleLocalBox) {
				box.setValue(JimpleUtils.getInvokeLocal(forbiddenUnit));
				break;
			}
		}

		if (alternativeMethod.isConstructor()) {
			if (forbiddenMethod.isConstructor()) {
				if (forbiddenUnit instanceof AssignStmt) {
					AssignStmt assign = (AssignStmt) forbiddenUnit;
					assign.getRightOpBox().setValue((Value) alternativeInvokeExpr);
				} else {
					InvokeStmt invoke = (InvokeStmt) forbiddenUnit;
					invoke.setInvokeExpr(alternativeInvokeExpr);
				}
			} else {
				body.getUnits().insertBefore(alternativeUnits, forbiddenUnit);
				body.getUnits().remove(forbiddenUnit);
			}
		} else {
			if (forbiddenUnit instanceof AssignStmt) {
				AssignStmt assign = (AssignStmt) forbiddenUnit;
				assign.getRightOpBox().setValue((Value) alternativeInvokeExpr);

			} else {
				InvokeStmt invoke = (InvokeStmt) forbiddenUnit;
				invoke.setInvokeExpr(alternativeInvokeExpr);
			}
		}
		patch = forbiddenUnit;
	}

	private Map<Integer, Local> calcParameterMatches(Unit forbiddenUnit, SootMethod alternativeMethod){
		Map<Integer, Local> parameterMatchMap = Maps.newHashMap();
		
		if(JimpleUtils.containsInvokeExpr(forbiddenUnit)) {
			InvokeExpr errorInvokeExpr = JimpleUtils.getInvokeExpr(forbiddenUnit);
			
			for(Value arg : errorInvokeExpr.getArgs()) {
				int i = 0;
				for(Type alterParams : alternativeMethod.getParameterTypes()) {
					if(JimpleUtils.equals(arg.getType(), alterParams)) {
						if(!parameterMatchMap.containsKey(i)) {
							if(arg instanceof Local) {
								parameterMatchMap.put(i, (Local) arg);
							}  
						} else {
							continue;
						}
					}
					i++;
				}
			}
		}
		return parameterMatchMap;
	}
	
	
	@Override
	public String toPatchString() {
		StringBuilder builder = new StringBuilder();
		builder.append("\n__________[ForbiddenMethodPatch]__________\n");
		builder.append("Repaired in Round: \t" + ErrorScheduler.round+ "\n");
		builder.append("Class: \t"+error.getErrorLocation().getMethod().getDeclaringClass().toString()+"\n");
		builder.append("Method: \t"+error.getErrorLocation().getMethod().getSignature()+"\n");
		builder.append("Error: \t"+error.getClass().getSimpleName()+"\n");
		builder.append("CrySLRule: \t"+entity.getRule().getClassName()+"\n");
		builder.append("Message: \t"+error.toErrorMarkerString()+"\n");
		builder.append("Patch: \t");
		if(patch == null) {
			builder.append("Forbidden call removed");
		} else {
			builder.append(patch);
		}
		builder.append("\n");
		builder.append("__________________________________________");
		return builder.toString();
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ForbiddenMethodPatch [error=");
		builder.append(error.toErrorMarkerString());
		builder.append(", entity=");
		builder.append(entity.getRule().getClassName());
		builder.append("]");
		return builder.toString();
	}
}
