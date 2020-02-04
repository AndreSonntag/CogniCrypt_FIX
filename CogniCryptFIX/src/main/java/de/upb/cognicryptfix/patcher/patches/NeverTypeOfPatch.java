package de.upb.cognicryptfix.patcher.patches;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import boomerang.callgraph.ObservableICFG;
import boomerang.jimple.Statement;
import crypto.analysis.errors.NeverTypeOfError;
import crypto.extractparameter.ExtractedValue;
import crypto.rules.CrySLRule;
import de.upb.cognicryptfix.analysis.CryptoAnalysis;
import de.upb.cognicryptfix.extractor.constraints.IConstraint;
import de.upb.cognicryptfix.utils.BoomerangUtils;
import de.upb.cognicryptfix.utils.JimpleCodeGenerator;
import soot.Body;
import soot.Local;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.ValueBox;
import soot.jimple.StringConstant;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.internal.JimpleLocal;

public class NeverTypeOfPatch extends AbstractPatch {
	private static final Logger logger = LogManager.getLogger(NeverTypeOfPatch.class.getSimpleName());	
	private NeverTypeOfError error;
	private IConstraint predCon;
	private CrySLRule violatedCrySLRule;
	private String crySLVarName;
	private String jimpleVarName;
	private String jimpleVarValue;
	private String jimpleVarType;
	private String brokenJimpleCode;
	private int brokenVarIndex;
	private String patchValue;
	private ObservableICFG<Unit, SootMethod> icfg;

	public NeverTypeOfPatch(NeverTypeOfError error, IConstraint predCon) {
		this.error = error;
		this.predCon = predCon;
		this.icfg = CryptoAnalysis.staticScanner.icfg();
		setErrorInformation();
	}
	
	public HashMap<Value, List<Unit>> generateCharArrayForValue(String value) {
		List<Value> parameter = new ArrayList<Value>();
		//remove the quotes
		for(char c : value.substring(1, value.length()-1).toCharArray()) {
			parameter.add(StringConstant.v(c+""));
		}	
		return JimpleCodeGenerator.generateParameterArray(error.getErrorLocation().getMethod().getActiveBody(), parameter);
	}

	private void setErrorInformation() {
		this.violatedCrySLRule = error.getRule();
		this.crySLVarName = error.getCallSiteWithExtractedValue().getCallSite().getVarName();
		this.brokenVarIndex = error.getCallSiteWithExtractedValue().getCallSite().getIndex();
		this.brokenJimpleCode = error.getErrorLocation().getUnit().get().getInvokeExpr().toString();
		this.jimpleVarName = error.getErrorLocation().getUnit().get().getInvokeExpr().getArgs().get(brokenVarIndex).toString();
		try {
			this.jimpleVarType = predCon.getUsedValue();
			 this.jimpleVarValue = predCon.getExpectedConstraintValue();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public String toString() {
		StringBuilder strBuilder = new StringBuilder();
		strBuilder.append("\nNeverTypeOfPatch [\n");
		strBuilder.append("error = " + error.toString() + "\n");
		strBuilder.append("violatedCrySLRule = " + violatedCrySLRule.getClassName() + "\n");
		strBuilder.append("crySLVarName = " + crySLVarName + "\n");
		strBuilder.append("brokenJimpleCode = " + brokenJimpleCode + "\n");
		strBuilder.append("brokenVarIndex = " + brokenVarIndex + "\n");
		strBuilder.append("jimpleVarName = " + jimpleVarName + "\n");
		strBuilder.append("jimpleVarType = " + jimpleVarType + "\n");
		strBuilder.append("jimpleVarValue = " + jimpleVarValue + "\n");
		strBuilder.append("new type for jimple Variable "+ jimpleVarName +" ="+ patchValue + "\n");
		return strBuilder.toString();
	}
	
	private void setRightOpOfAssignStmt(Unit assignStmt, Local newValue) {
		
 		if (assignStmt instanceof JAssignStmt) {	
			JAssignStmt stmt = (JAssignStmt) assignStmt;
			stmt.setRightOp(newValue);
		}else {
			logger.error("This shouldn't happen!");
		}
	}
	
	private void handleStringDataType(Body body) {
		/*
		 * Workaround for Boomerang:
		 * TODO: describe workaround
		 */
		Unit toCharArrayAssignment = error.getCallSiteWithExtractedValue().getVal().stmt().getUnit().get();
		List<ValueBox> useBoxes = toCharArrayAssignment.getUseBoxes();
		for (ValueBox box : useBoxes) {
			if(box.getValue() instanceof JimpleLocal) {
				setRightOpOfAssignStmt(toCharArrayAssignment, JimpleCodeGenerator.getLocalByName(body, box.getValue().toString()));
			}
		}
				
		HashMap<Value, List<Unit>> generatedArrayMap = generateCharArrayForValue(getVariableValueAsString());		
		Value arrayRef = generatedArrayMap.keySet().iterator().next();
		patchValue = arrayRef.toString();
		List<Unit> generatedArrayUnits = generatedArrayMap.get(arrayRef);		
		body.getUnits().insertBefore(generatedArrayUnits, toCharArrayAssignment);
		setRightOpOfAssignStmt(toCharArrayAssignment,(Local) arrayRef);
	}
	
	@Override
	public Body getPatch() {
		Body body = error.getErrorLocation().getMethod().getActiveBody();				
		//TODO: needs a more generic solution: maybe single and array data types, also consider casts please
		//Analysis does not detect casts
		try {
			switch (predCon.getUsedValue()) {
			case "java.lang.String":
				handleStringDataType(body);
				break;
			default:
				break;
			}
		} catch (Exception e) {
			logger.error(e);
		}

		return body;
	}
	
	private String getVariableValueAsString() {
		Unit errorCall = error.getErrorLocation().getUnit().get();
		Statement statement = error.getCallSiteWithExtractedValue().getCallSite().stmt();		
		Value parameter = error.getErrorLocation().getUnit().get().getInvokeExpr().getArg(brokenVarIndex);
		ExtractedValue ev = BoomerangUtils.bommerangPointsToAnalysis(icfg, (Local) parameter, statement, errorCall);
		return ev.getValue().toString();
	}
	
	
	private void handleSingleDataType(Body body) {

	}
	
	private void handleArrayDataType(Body body) {
		
	}






}
