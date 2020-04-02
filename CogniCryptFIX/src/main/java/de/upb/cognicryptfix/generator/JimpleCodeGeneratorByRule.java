package de.upb.cognicryptfix.generator;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Maps;

import de.upb.cognicryptfix.crysl.CrySLEntity;
import de.upb.cognicryptfix.crysl.CrySLMethodCall;
import de.upb.cognicryptfix.crysl.CrySLPredicate;
import de.upb.cognicryptfix.crysl.CrySLVariable;
import de.upb.cognicryptfix.crysl.pool.CrySLEntityPool;
import de.upb.cognicryptfix.exception.NoEnsuredPredicateException;
import de.upb.cognicryptfix.generator.jimple.JimpleArrayGenerator;
import de.upb.cognicryptfix.generator.jimple.JimpleCallGenerator;
import de.upb.cognicryptfix.generator.jimple.JimpleLocalGenerator;
import de.upb.cognicryptfix.generator.jimple.JimpleTrapGenerator;
import de.upb.cognicryptfix.generator.jimple.JimpleUtils;
import de.upb.cognicryptfix.generator.jimple.rulebased.JimpleParameterGeneratorByRule;
import de.upb.cognicryptfix.generator.jimple.rulebased.JimplePredicateGenerator;
import soot.Body;
import soot.Local;
import soot.Type;
import soot.Unit;
import soot.Value;

public class JimpleCodeGeneratorByRule {

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

	public Map<Local, List<Unit>> generateParameters(CrySLMethodCall call) {
		return parameterGenerator.generateParameterUnits(call);
	}

	public Map<Local, List<Unit>> generateCall(Local invokingLocal, CrySLMethodCall call, boolean generateReturn,
			Local... parameterLocals) {
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
		Map<Local, List<Unit>> generatedUnits = Maps.newLinkedHashMap();
		Map<Local, List<Unit>> generatedParameterUnits = Maps.newLinkedHashMap();
		Map<Local, List<Unit>> generatedCallUnits = Maps.newLinkedHashMap();

		generatedParameterUnits = parameterGenerator.generateParameterUnits(call);
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

	public Map<Local, List<Unit>> generatePredicateForVariable(CrySLEntity entity, CrySLVariable variable, Local usageLocal) {
		return predicateGenerator.generatePredicateUnits(entity, variable, usageLocal);
	}
}
