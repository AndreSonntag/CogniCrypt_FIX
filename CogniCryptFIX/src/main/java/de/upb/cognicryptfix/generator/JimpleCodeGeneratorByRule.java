package de.upb.cognicryptfix.generator.jimple.rulebased;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Maps;

import de.upb.cognicryptfix.crysl.CrySLMethodCall;
import de.upb.cognicryptfix.crysl.CrySLVariable;
import de.upb.cognicryptfix.crysl.pool.CrySLEntityPool;
import de.upb.cognicryptfix.generator.jimple.JimpleCallGenerator;
import de.upb.cognicryptfix.generator.jimple.JimpleLocalGenerator;
import de.upb.cognicryptfix.generator.jimple.JimpleUtils;
import soot.Body;
import soot.Local;
import soot.RefType;
import soot.Unit;

public class JimpleCodeGeneratorByRule {

	private Body body;
	private CrySLEntityPool pool;
	private JimpleLocalGenerator localGenerator;
	private JimpleCallGenerator callGenerator;
	private JimpleParameterGeneratorByRule parameterGenerator;
	private JimplePredicateGenerator predicateGenerator;

	public JimpleCodeGeneratorByRule(Body body) {
		this.body = body;
		this.pool = CrySLEntityPool.getInstance();
		this.localGenerator = new JimpleLocalGenerator(body);
		this.callGenerator = new JimpleCallGenerator(body);
		this.predicateGenerator = new JimplePredicateGenerator(body,this);
		this.parameterGenerator = new JimpleParameterGeneratorByRule(body, pool, predicateGenerator);
	}
	
	public Map<Local, List<Unit>> generateCallWithParameter(Local invokingLocal, CrySLMethodCall call, boolean generateReturn) {		
		Map<Local, List<Unit>> generatedUnits = Maps.newLinkedHashMap();
		Map<Local, List<Unit>> generatedParameterUnits = Maps.newLinkedHashMap();
		Map<Local, List<Unit>> generatedCallUnits = Maps.newLinkedHashMap();

		generatedParameterUnits = parameterGenerator.generateParameterUnits(call);
		Local[] parameterLocals = generatedParameterUnits.keySet().toArray(new Local[0]);

		
		if(call.isInitCall()) {
			generatedCallUnits = callGenerator.generateCallUnits(invokingLocal, call.getSootMethod(), parameterLocals);
		} else if(generateReturn && !call.getCallReturn().getType().toString().equals("void")) {
			CrySLVariable callReturn = call.getCallReturn();
			Local returnLocal = localGenerator.generateFreshLocal(callReturn.getType(), callReturn.getVariable());
			generatedCallUnits = callGenerator.generateCallUnits(invokingLocal, returnLocal, call.getSootMethod(), parameterLocals);
		} else {
			generatedCallUnits = callGenerator.generateCallUnits(invokingLocal, call.getSootMethod(), parameterLocals);
		}

		generatedUnits.putAll(generatedParameterUnits);
		generatedUnits.putAll(generatedCallUnits);
		return generatedUnits;
	}

	public Body getBody() {
		return body;
	}
}
