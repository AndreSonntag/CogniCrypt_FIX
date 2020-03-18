package de.upb.cognicryptfix.generator;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import de.upb.cognicryptfix.crysl.pool.CrySLEntityPool;
import de.upb.cognicryptfix.generator.jimple.JimpleArrayGenerator;
import de.upb.cognicryptfix.generator.jimple.JimpleCallGenerator;
import de.upb.cognicryptfix.generator.jimple.JimpleInvokeGenerator;
import de.upb.cognicryptfix.generator.jimple.JimpleParameterGenerator;
import de.upb.cognicryptfix.generator.jimple.JimpleTrapGenerator;
import soot.Body;
import soot.Local;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.Value;

/**
 * @author Andre Sonntag
 * @date 19.02.2020
 */
public class JimpleCodeGenerator {

	private static JimpleCodeGenerator instance;
	private Body body;
	private JimpleCallGenerator callGenerator;
	private JimpleParameterGenerator parameterGenerator;
	private JimpleTrapGenerator trapGenerator;
	private JimpleInvokeGenerator invokeGenerator;
	private JimpleArrayGenerator arrayGenerator;
	private CrySLEntityPool fsm;
	
	private JimpleCodeGenerator(Body body) {
		this.body = body;
		this.callGenerator = new JimpleCallGenerator(body);
		this.invokeGenerator = new JimpleInvokeGenerator();
		this.parameterGenerator = new JimpleParameterGenerator(body);
		this.trapGenerator = new JimpleTrapGenerator(body);
		this.arrayGenerator = new JimpleArrayGenerator(body);
	}

	public static JimpleCodeGenerator getInstance(Body body) {
		if (JimpleCodeGenerator.instance == null) {
			JimpleCodeGenerator.instance = new JimpleCodeGenerator(body);
		}
		return JimpleCodeGenerator.instance;
	}
	

	//currently it is here not possible to use the returned object :/
	public Map<Local, List<Unit>> generateCall(Local var, SootMethod method, Local... parameterLocals){
		return callGenerator.generateCallUnits(var, method, parameterLocals);
	}
		
	public Map<Local, List<Unit>> generateParametersForCall(SootMethod method, List<String> parameterNames, List<Object> parameterValues){
		return parameterGenerator.generateParameterUnits(method, parameterNames, parameterValues);
	}
	
	public void generateTryCatch(SootMethod method, List<Unit> tryUnits) {
		trapGenerator.generateTraps(method, tryUnits);
	}
		
	public Map<Local, List<Unit>> generateArray(Type type, List<Value> values){
		return arrayGenerator.generateArrayUnits(type, values);
	}

}
