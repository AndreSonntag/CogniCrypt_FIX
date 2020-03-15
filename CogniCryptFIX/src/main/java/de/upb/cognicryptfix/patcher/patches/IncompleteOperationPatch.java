package de.upb.cognicryptfix.patcher.patches;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.Lists;

import crypto.analysis.errors.IncompleteOperationError;
import crypto.rules.CrySLRule;
import de.upb.cognicryptfix.crysl.CrySLEntityPool;
import de.upb.cognicryptfix.crysl.CrySLMethodCall;
import de.upb.cognicryptfix.crysl.fsm.call.CrySLCallFSM;
import de.upb.cognicryptfix.generator.JimpleCodeGenerator;
import de.upb.cognicryptfix.generator.jimple.JimpleUtils;
import de.upb.cognicryptfix.generator.jimple.rulebased.JimpleCodeGeneratorByRule;
import soot.Body;
import soot.Local;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.InvokeExpr;

public class IncompleteOperationPatch extends AbstractPatch {
 
	private static final Logger logger = LogManager.getLogger(IncompleteOperationPatch.class.getSimpleName());
	private IncompleteOperationError error;
	private CrySLRule violatedCrySLRule;
	private String brokenJimpleCode;
	private String patchValue;
	private JimpleCodeGenerator codeGenerator;
	private JimpleCodeGeneratorByRule extendedCodeGenerator;
	
	public IncompleteOperationPatch(IncompleteOperationError error) {
		this.error = error;
		this.codeGenerator = JimpleCodeGenerator.getInstance(error.getErrorLocation().getMethod().getActiveBody());
		this.extendedCodeGenerator = JimpleCodeGeneratorByRule.getInstance(error.getErrorLocation().getMethod().getActiveBody());
		setErrorInformation();
	}

	@Override
	public Body getPatch() {
		Body methodBody = error.getErrorLocation().getMethod().getActiveBody();
		Collection<SootMethod> expectedCalls = error.getExpectedMethodCalls();
		Unit activeUnit = error.getErrorLocation().getUnit().get();
		generateCalls(methodBody, activeUnit, expectedCalls);
		return methodBody;
	}
	
	public void generateCalls(Body body, Unit activeUnit, Collection<SootMethod> expectedCalls) {
		
		InvokeExpr invokeExpr = JimpleUtils.getInvokeExpr(activeUnit);
		Local invokeVarLocal = JimpleUtils.getInvokeLocal(activeUnit);
		CrySLEntityPool pool = CrySLEntityPool.getInstance();
		CrySLCallFSM fsm = pool.getEntityByClassName(violatedCrySLRule.getClassName()).getFSM();
		ArrayList<LinkedList<CrySLMethodCall>> paths = Lists.newArrayList();
		
		if(invokeExpr != null) {
			paths = fsm.calcFewestUserInteractionRefTypeGenPathsFrom(invokeExpr.getMethod());
		} else {
			paths = fsm.calcFewestUserInteractionRefTypeGenPathsFrom_(expectedCalls.iterator().next());
		}

		LinkedList<CrySLMethodCall> path = paths.get(0);
		
		for(CrySLMethodCall call : path) {	//loop for all following calls	
			Map<Local, List<Unit>> generatedUnits = extendedCodeGenerator.generateCallWithParameter(invokeVarLocal, call, true);
			List<Unit> units = Lists.newArrayList();
			for (List<Unit> l : generatedUnits.values()) {
				units.addAll(l);
			}
		
			if(!units.isEmpty()) {
				body.getUnits().insertAfter(units, activeUnit);
			}
			activeUnit = units.get(units.size()-1);
		}
		
		System.out.println("--------------------- "+violatedCrySLRule.getClassName()+" -----------------------------");
		System.out.println(body.toString());
	}
	
	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return null;
	}

	private void setErrorInformation() {
		this.violatedCrySLRule = error.getRule();
//		this.brokenJimpleCode = error.getErrorLocation().getUnit().get().getInvokeExpr().toString();

	}
}
