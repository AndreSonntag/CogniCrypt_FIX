package de.upb.cognicryptfix.generator.jimple.rulebased;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Maps;

import de.upb.cognicryptfix.crysl.CrySLEntity;
import de.upb.cognicryptfix.crysl.CrySLEntityPool;
import de.upb.cognicryptfix.crysl.CrySLMethodCall;
import de.upb.cognicryptfix.crysl.CrySLVariable;
import de.upb.cognicryptfix.generator.jimple.JimpleCallGenerator;
import de.upb.cognicryptfix.generator.jimple.JimpleLocalGenerator;
import soot.ArrayType;
import soot.Body;
import soot.Local;
import soot.RefType;
import soot.SootMethod;
import soot.Type;
import soot.Unit;

public class JimpleCodeGeneratorByRule {

	private static JimpleCodeGeneratorByRule instance;
	private CrySLEntityPool pool;
	private JimpleLocalGenerator localGenerator;
	private JimpleCallGenerator callGenerator;
	private JimpleParameterGeneratorByRule parameterGenerator;
	private JimplePredicateGenerator predicateGenerator;

	private JimpleCodeGeneratorByRule(Body body) {
		this.pool = CrySLEntityPool.getInstance();
		this.localGenerator = new JimpleLocalGenerator(body);
		this.callGenerator = new JimpleCallGenerator(body);
		this.predicateGenerator = new JimplePredicateGenerator(this);
		this.parameterGenerator = new JimpleParameterGeneratorByRule(body, pool, predicateGenerator);
	}

	public static JimpleCodeGeneratorByRule getInstance(Body body) {
		if (JimpleCodeGeneratorByRule.instance == null) {
			JimpleCodeGeneratorByRule.instance = new JimpleCodeGeneratorByRule(body);
		}
		return JimpleCodeGeneratorByRule.instance;
	}

	/*- desired KeyGenerator code 
	  keyGen = staticinvoke <javax.crypto.KeyGenerator: javax.crypto.KeyGenerator getInstance(java.lang.String)>(varReplacer22);
	  key = virtualinvoke keyGen.<javax.crypto.KeyGenerator: javax.crypto.SecretKey generateKey()>();
	*/

	public Map<Local, List<Unit>> generateCallWithParameter(Local invokingLocal, CrySLMethodCall call, boolean generateReturn) {
		Map<Local, List<Unit>> generatedUnits = Maps.newLinkedHashMap();
		Map<Local, List<Unit>> generatedParameterUnits = Maps.newLinkedHashMap();
		Map<Local, List<Unit>> generatedCallUnits = Maps.newLinkedHashMap();

		generatedParameterUnits = parameterGenerator.generateParameterUnits(call);
		Local[] parameterLocals = generatedParameterUnits.keySet().toArray(new Local[0]);

		if (generateReturn && !call.getCallReturn().getType().toString().equals("void")) {
			CrySLVariable callReturn = call.getCallReturn();
			Local returnLocal = localGenerator.generateFreshLocal(callReturn.getType(), callReturn.getVariable());
			generatedCallUnits = callGenerator.generateCallUnits(invokingLocal, returnLocal, call.getSootMethod(),
					parameterLocals);
		} else {
			generatedCallUnits = callGenerator.generateCallUnits(invokingLocal, call.getSootMethod(), parameterLocals);
		}

		generatedUnits.putAll(generatedParameterUnits);
		generatedUnits.putAll(generatedCallUnits);
		return generatedUnits;
	}

	public Map<Local, List<Unit>> generateCallWithParameter(CrySLMethodCall method, boolean generateReturn) {
		RefType methodType = method.getSootMethod().getDeclaringClass().getType();
		Local invokingLocal = localGenerator.generateFreshLocal(methodType);
		return generateCallWithParameter(invokingLocal, method, generateReturn);
	}

}
