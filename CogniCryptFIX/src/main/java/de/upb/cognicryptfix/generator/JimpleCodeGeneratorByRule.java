package de.upb.cognicryptfix.generator;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.Maps;

import de.upb.cognicryptfix.crysl.CrySLEntity;
import de.upb.cognicryptfix.crysl.CrySLMethodCall;
import de.upb.cognicryptfix.crysl.CrySLVariable;
import de.upb.cognicryptfix.crysl.pool.CrySLEntityPool;
import de.upb.cognicryptfix.exception.generation.GenerationException;
import de.upb.cognicryptfix.exception.generation.crysl.CrySLGenerationException;
import de.upb.cognicryptfix.exception.path.PathException;
import de.upb.cognicryptfix.generator.jimple.JimpleArrayGenerator;
import de.upb.cognicryptfix.generator.jimple.JimpleCallGenerator;
import de.upb.cognicryptfix.generator.jimple.JimpleLocalGenerator;
import de.upb.cognicryptfix.generator.jimple.JimpleTrapGenerator;
import de.upb.cognicryptfix.generator.jimple.rulebased.JimpleParameterGeneratorByRule;
import de.upb.cognicryptfix.generator.jimple.rulebased.JimplePredicateGenerator;
import soot.Body;
import soot.Local;
import soot.Type;
import soot.Unit;
import soot.Value;

public class JimpleCodeGeneratorByRule {

	private static final Logger LOGGER = LogManager.getLogger(JimpleCodeGeneratorByRule.class);
	private CrySLEntityPool pool;
	private JimpleLocalGenerator localGenerator;
	private JimpleCallGenerator callGenerator;
	private JimpleParameterGeneratorByRule parameterGenerator;
	private JimplePredicateGenerator predicateGenerator;
	private JimpleTrapGenerator trapGenerator;
	private JimpleArrayGenerator arrayGenerator;

	public JimpleCodeGeneratorByRule(Body body) {
		this.pool = CrySLEntityPool.getInstance();
		this.localGenerator = new JimpleLocalGenerator(body);
		this.callGenerator = new JimpleCallGenerator(body);
		this.predicateGenerator = new JimplePredicateGenerator(body, this);
		this.parameterGenerator = new JimpleParameterGeneratorByRule(body, pool, predicateGenerator);
		this.trapGenerator = new JimpleTrapGenerator(body);
		this.arrayGenerator = new JimpleArrayGenerator(body);
	}
	
	
	public Map<Local, List<Unit>> generateParameters(CrySLMethodCall call, Map<Integer, Local> alreadyGeneratedParameter) {
		Map<Local, List<Unit>> generatedUnits = Maps.newHashMap();
		try {
			generatedUnits = parameterGenerator.generateParameterUnits(call, alreadyGeneratedParameter);
		} catch (GenerationException | PathException e) {
			LOGGER.error("Error happened during the parameter generation",e);
		} 
		return generatedUnits;
	}

	public Map<Local, List<Unit>> generateParameters(CrySLMethodCall call) {
		Map<Local, List<Unit>> generatedUnits = Maps.newHashMap();
		try {
			generatedUnits = parameterGenerator.generateParameterUnits(call);
		} catch (GenerationException | PathException e) {
			LOGGER.error("Error happened during the parameter generation",e);
		} 
		return generatedUnits;
	}

	public Map<Local, List<Unit>> generateCall(Local invokingLocal, CrySLMethodCall call, boolean generateReturn, Local... parameterLocals) {
		Map<Local, List<Unit>> generatedCallUnits = Maps.newLinkedHashMap();

		if (call.isInitCall()) {
			generatedCallUnits = callGenerator.generateCallUnits(invokingLocal, call.getSootMethod(), parameterLocals);
		} else if (generateReturn && !call.getCallReturn().getType().toString().equals("void")) {
			CrySLVariable callReturn = call.getCallReturn();
			Local returnLocal = localGenerator.generateFreshLocal(callReturn.getType(), callReturn.getName());
			generatedCallUnits = callGenerator.generateCallUnits(invokingLocal, returnLocal, call.getSootMethod(),
					parameterLocals);
		} else {
			generatedCallUnits = callGenerator.generateCallUnits(invokingLocal, call.getSootMethod(), parameterLocals);
		}

		return generatedCallUnits;
	}

	public Map<Local, List<Unit>> generateCallWithParameter(Local invokingLocal, CrySLMethodCall call, boolean generateReturn){
		LOGGER.debug("Call to generate: "+call);
		Map<Local, List<Unit>> generatedUnits = Maps.newLinkedHashMap();
		Map<Local, List<Unit>> generatedParameterUnits = Maps.newLinkedHashMap();
		Map<Local, List<Unit>> generatedCallUnits = Maps.newLinkedHashMap();
		
		try {
			generatedParameterUnits = parameterGenerator.generateParameterUnits(call);
		} catch (GenerationException | PathException e) {
			LOGGER.error("Error happened during the parameter generation",e);
		} 
		
		Local[] parameterLocals = generatedParameterUnits.keySet().toArray(new Local[0]);
		generatedCallUnits = generateCall(invokingLocal, call, generateReturn, parameterLocals);

		generatedUnits.putAll(generatedParameterUnits);
		generatedUnits.putAll(generatedCallUnits);
		return generatedUnits;
	}
	
	public void generateTryCatchBlock(List<Unit> units) {
		trapGenerator.generateTrap(units);
	}
	
	public void generateTryCatchBlock(Unit unit) {
		trapGenerator.generateTrap(unit);
	}
	
	public void removeUnnecessaryTryCatchBlock() {
		trapGenerator.removeTrapsWithOnlyGotoStatement();
	}
	
	public Map<Local, List<Unit>> generateArray(Type type, List<Value> values){
		return arrayGenerator.generateArrayUnits(type, values);
	}
	
	public Map<Local, List<Unit>> generateArray(Local arrayLocal, List<Value> values){
		return arrayGenerator.generateArrayUnits(arrayLocal, values);
	}
	
	public Local generateVariable(Type type) {
		return localGenerator.generateFreshLocal(type);
	}

	public Map<Local, List<Unit>> generatePredicateForVariable(CrySLEntity entity, CrySLVariable variable, Local usageLocal) {
		Map<Local, List<Unit>> generatedUnits = Maps.newHashMap();
		
		try {
			generatedUnits = predicateGenerator.generatePredicateUnits(entity, variable, usageLocal);
		} catch (CrySLGenerationException | PathException e) {
			LOGGER.error("Error happened during the predicate generation", e);
		}
		
		return generatedUnits;
	}
}
