package de.upb.cognicryptfix.patcher.patches;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.Lists;

import boomerang.callgraph.ObservableICFG;
import crypto.analysis.errors.ForbiddenMethodError;
import crypto.rules.CrySLForbiddenMethod;
import crypto.rules.CrySLMethod;
import crypto.rules.CrySLRule;
import de.upb.cognicryptfix.analysis.CryptoAnalysis;
import de.upb.cognicryptfix.utils.JimpleCodeGenerator;
import de.upb.cognicryptfix.utils.Utils;
import soot.ArrayType;
import soot.Body;
import soot.Local;
import soot.PrimType;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.ValueBox;
import soot.jimple.InvokeExpr;
import soot.jimple.internal.JInvokeStmt;
import soot.jimple.internal.JimpleLocalBox;

public class ForbiddenMethodPatch extends AbstractPatch{
	private static final Logger logger = LogManager.getLogger(ForbiddenMethodPatch.class.getSimpleName());	
	private ForbiddenMethodError error;
	private CrySLRule violatedCrySLRule;
	private String brokenJimpleCode;
	private int brokenVarIndex;
	private ObservableICFG<Unit, SootMethod> icfg;

	public ForbiddenMethodPatch(ForbiddenMethodError error) {
		this.error = error;
		this.icfg = CryptoAnalysis.staticScanner.icfg();
		setErrorInformation();
	}

	@Override
	public Body getPatch() {
		Body methodBody = error.getErrorLocation().getMethod().getActiveBody();
		Unit forbiddenInvokeStmt = error.getErrorLocation().getUnit().get();
		SootMethod forbiddenMethod = error.getCalledMethod();
		
		ArrayList<SootMethod> secureAlternatives = new ArrayList<>(error.getAlternatives());
 		
		if(Utils.isNullOrEmpty(secureAlternatives)){
 			removeCall(methodBody, forbiddenInvokeStmt);
 		}
 		else {
 			replaceCall(methodBody, forbiddenMethod, forbiddenInvokeStmt, secureAlternatives);
 		}
		
		return null;
	}
	
	
	private void removeCall(Body body, Unit removableCall) {	
		body.getUnits().remove(removableCall);
	}
	
	
	private void replaceCall(Body body, SootMethod forbiddenMethod, Unit forbiddenInvokeStmt, ArrayList<SootMethod> alternatives) {
		
		if(forbiddenInvokeStmt instanceof JInvokeStmt) {
			JInvokeStmt call = (JInvokeStmt) forbiddenInvokeStmt;
			InvokeExpr invoke = call.getInvokeExpr();
			List<Value> params = invoke.getArgs();			

			Value fMethodCallVariableName = invoke.getUseBoxes().stream()
		    .filter(useBox -> useBox instanceof JimpleLocalBox)
		    .map(ValueBox::getValue)
		    .findAny()                                     
            .orElse(null); 
		    
			Local fMethodCallVariable = JimpleCodeGenerator.getLocalByName(body, fMethodCallVariableName.toString());
			JInvokeStmt alternativeJInvokeStmt = (JInvokeStmt) JimpleCodeGenerator.generateJInvokeStmt(fMethodCallVariable, alternatives.get(0));
			call.setInvokeExpr(alternativeJInvokeStmt.getInvokeExpr());
			
			generateAlternativeCallParameters(body, forbiddenMethod, alternatives.get(0));
			
		}
	}
	
	private void generateAlternativeCallParameters(Body body, SootMethod forbiddenMethod, SootMethod alternative) {

		List<Entry<String,String>> parameterNames = extractParameterVarNameFromRule(forbiddenMethod, alternative);
		List<Type> parameterTypes = alternative.getParameterTypes();

		Iterator<Entry<String,String>> parameterNameIterator = parameterNames.iterator();
        Iterator<Type> parameterTypeIterator = parameterTypes.iterator();
        
        while(parameterNameIterator.hasNext() && parameterTypeIterator.hasNext()) {
        	String varName = parameterNameIterator.next().getKey();
        	Type varType = parameterTypeIterator.next();
    		List<Unit> generatedUnits = Lists.newArrayList();

        	if(varType instanceof ArrayType) {
        		ArrayType arrayType = (ArrayType) varType;
        		Local arrayLocal = JimpleCodeGenerator.genereateFreshArrayLocal(body, arrayType.baseType, varName, 1);
        		Unit u = JimpleCodeGenerator.generateArrayAssignStmt(arrayLocal, arrayType.baseType, 1);
        		System.out.println(u);
        	}
        	else if(varType instanceof PrimType) {
        		PrimType primType = (PrimType) varType;
        		Local local = JimpleCodeGenerator.generateFreshLocal(body, primType, varName);
        		Unit u = JimpleCodeGenerator.generateAssignStmt(local, JimpleCodeGenerator.generateConstantValue(1));
        	}
        }
		
		System.out.println();
		
		
	}
	
	private List<Entry<String,String>> extractParameterVarNameFromRule(SootMethod forbiddenSootMethod, SootMethod alternativeSootMethod) {
						
		List<CrySLForbiddenMethod> forbiddenCrySLMethods = violatedCrySLRule.getForbiddenMethods();
		for(CrySLForbiddenMethod forbiddenCrySLMethod : forbiddenCrySLMethods) {
			CrySLMethod innerCrySLMethod = forbiddenCrySLMethod.getMethod();
							
			if(isMethodNameMatch(innerCrySLMethod,forbiddenSootMethod)) {
			
				List<Entry<String, String>> innerCrySLMethodParamaters = innerCrySLMethod.getParameters();
				List<String> crySLMethodParameterTypes = Lists.newArrayList();
				for(Entry<String, String> e : innerCrySLMethodParamaters) {
					crySLMethodParameterTypes.add(e.getValue());
				}
				
				List<Type> forbiddenSootMethodParameters = forbiddenSootMethod.getParameterTypes();
				List<String> forbiddenSootMethodParameterTypes = Lists.newArrayList();
				for(Type t : forbiddenSootMethodParameters) {
					forbiddenSootMethodParameterTypes.add(t.toString());
				}
				
				if(isParameterMatch(crySLMethodParameterTypes, forbiddenSootMethodParameterTypes)) {
					return forbiddenCrySLMethod.getAlternatives().get(0).getParameters();
				}
			}
			
		}
		return null;
	}
	
	private boolean isMethodNameMatch(CrySLMethod crySLMethod, SootMethod sootMethod) {
		if(isCrySLMethodConstructor(crySLMethod) && sootMethod.isConstructor()) {
			if(StringUtils.substringBeforeLast(crySLMethod.getMethodName(), ".").equals(sootMethod.getDeclaringClass().toString())) {
				return true;
			}
			else {
				return false;
			}
		}
		else if(crySLMethod.getMethodName().equals(sootMethod.getDeclaringClass().toString()+"."+sootMethod.getName())) {
			return true;
		}
		else {
			return false;
		}
	}
	
	private boolean isParameterMatch(List<String> crySLMethodParameterTypes, List<String> sootMethodParameterTypes) {
		return crySLMethodParameterTypes.equals(sootMethodParameterTypes);
	}
	
	private boolean isCrySLMethodConstructor(CrySLMethod method) {
		String shortClassName = StringUtils.substringAfterLast(violatedCrySLRule.getClassName(), ".");			
		return shortClassName.equals(method.getShortMethodName());
	}

	@Override
	public String toString() {

		return null;
	}
	
	private void setErrorInformation() {
		this.violatedCrySLRule = error.getRule();
		this.brokenJimpleCode = error.getErrorLocation().getUnit().get().getInvokeExpr().toString();
	}
}
