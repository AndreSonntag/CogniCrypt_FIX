package de.upb.cognicryptfix.patcher.patches;

import java.io.IOException;
import java.util.Iterator;

import javax.script.ScriptException;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


import crypto.analysis.errors.ConstraintError;
import crypto.rules.CryptSLRule;
import de.upb.cognicryptfix.utils.PrimitiveConstraint;
import soot.Body;
import soot.Scene;
import soot.SootClass;
import soot.jimple.IntConstant;
import soot.jimple.StringConstant;
import soot.jimple.internal.JAssignStmt;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.UnitGraph;


/**
 * TODO: find a good description
 * @author Andre Sonntag
 * @date 21.04.2019
 */
public class PrimitiveConstraintPatch extends AbstractPatch {

	private static final Logger logger = LogManager.getLogger(PrimitiveConstraintPatch.class.getSimpleName());

	private final ConstraintError error;
	private final PrimitiveConstraint primCon;
	private CryptSLRule violatedCrySLRule;
	private String crySLVarName;
	private String jimpleVarName;
	private String jimpleVarValue;
	private String brokenJimpleCode;
	private int brokenVarIndex;
	private String patchValue;
	
	public PrimitiveConstraintPatch(ConstraintError error, PrimitiveConstraint primCon) {
		this.error = error;
		this.primCon = primCon;
		setErrorInformation();
		logger.info(toString());		
	}
	
	
	@Override
	public Body getPatch() {
		Body methodBody = error.getErrorLocation().getMethod().getActiveBody();
		UnitGraph uGraph = new ExceptionalUnitGraph(methodBody); 
		Iterator i = uGraph.iterator();
		while(i.hasNext()) {
			Object o = i.next();
			if(o instanceof JAssignStmt) {
				JAssignStmt var = (JAssignStmt) o;
				if(var.leftBox.getValue().toString().equals(jimpleVarName)) {
					
					if(StringUtils.isNumeric(patchValue)) {
						var.rightBox.setValue(IntConstant.v(Integer.parseInt(patchValue)));
					}
					else {
						var.rightBox.setValue(StringConstant.v(patchValue));
					}
					break;
				}	
			}
		}
		return methodBody;
	}
	
	/**
	 * <p>
	 * This method extract all required information
	 * </p>
	 * 
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	private void setErrorInformation(){
		this.violatedCrySLRule = error.getRule();
		this.crySLVarName = error.getCallSiteWithExtractedValue().getCallSite().getVarName();
		this.brokenVarIndex = error.getCallSiteWithExtractedValue().getCallSite().getIndex();
		this.brokenJimpleCode = error.getErrorLocation().getUnit().get().getInvokeExpr().toString();
		this.jimpleVarName = error.getErrorLocation().getUnit().get().getInvokeExpr().getArgs().get(brokenVarIndex).toString();
		this.jimpleVarValue = getRealValue(crySLVarName);
		this.patchValue = getExpectedConstraintValue(crySLVarName);
	}
	
	@Override
	public String toString() {
		StringBuilder strBuilder = new StringBuilder();
		strBuilder.append("\nConstraintPatch [\n");
		strBuilder.append("error = " + error.toString() + "\n");
		strBuilder.append("violatedCrySLRule = " + violatedCrySLRule.getClassName() + "\n");
		strBuilder.append("crySLVarName = " + crySLVarName + "\n");
		strBuilder.append("brokenJimpleCode = " + brokenJimpleCode + "\n");
		strBuilder.append("brokenVarIndex = " + brokenVarIndex + "\n");
		strBuilder.append("jimpleVarName = " + jimpleVarName + "\n");
		strBuilder.append("jimpleVarValue = " + jimpleVarValue + "\n");
		strBuilder.append("new patch value for jimple Variable "+ jimpleVarName +" = "+ patchValue + "\n");
		return strBuilder.toString();
	}

	
	private String getExpectedConstraintValue(String crySLVar) {
		String expectedValue = "";
		try {
			expectedValue = primCon.getExpectedValueOfVar(crySLVar);
		} catch (ScriptException e) {
			logger.error(e);
		}
		return expectedValue;
	}
	
	private String getRealValue(String crySLVar) {
		String expectedValue = "";
		try {
			expectedValue = primCon.getRealValueOfVar(crySLVar);
		} catch (ScriptException e) {
			logger.error(e);
		}
		return expectedValue;
	}
	
	public ConstraintError getError() {
		return error;
	}

	public CryptSLRule getViolatedCrySLRule() {
		return violatedCrySLRule;
	}

	public String getCrySLVarName() {
		return crySLVarName;
	}

	public String getJimpleVarName() {
		return jimpleVarName;
	}

	public String getBrokenJimpleCode() {
		return brokenJimpleCode;
	}

	public int getBrokenVarIndex() {
		return brokenVarIndex;
	}

	public String getJimpleVarValue() {
		return jimpleVarValue;
	}	
}
