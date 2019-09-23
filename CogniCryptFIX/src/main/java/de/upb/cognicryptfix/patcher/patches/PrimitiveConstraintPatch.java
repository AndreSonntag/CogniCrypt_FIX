package de.upb.cognicryptfix.patcher.patches;

import java.util.Iterator;

import javax.script.ScriptException;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


import crypto.analysis.errors.ConstraintError;
import crypto.rules.CryptSLRule;
import de.upb.cognicryptfix.extractor.constraints.IConstraint;
import de.upb.cognicryptfix.extractor.constraints.ValueConstraint;
import soot.Body;
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
	private final IConstraint constraint;
	private CryptSLRule violatedCrySLRule;
	private String crySLVarName;
	private String jimpleVarName;
	private String jimpleVarValue;
	private String brokenJimpleCode;
	private int brokenVarIndex;
	private String patchValue;
	
	public PrimitiveConstraintPatch(ConstraintError error, IConstraint constraint) {
		this.error = error;
		this.constraint = constraint;
		setErrorInformation();
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
					
					//TODO: is boolean, char, etc.
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
	
	private void setErrorInformation(){
		this.violatedCrySLRule = error.getRule();
		this.crySLVarName = error.getCallSiteWithExtractedValue().getCallSite().getVarName();
		this.brokenVarIndex = error.getCallSiteWithExtractedValue().getCallSite().getIndex();
		this.brokenJimpleCode = error.getErrorLocation().getUnit().get().getInvokeExpr().toString();
		this.jimpleVarName = error.getErrorLocation().getUnit().get().getInvokeExpr().getArgs().get(brokenVarIndex).toString();
		this.jimpleVarValue = getRealValue();
		
		if(constraint instanceof ValueConstraint) {
			this.patchValue = getRealValue()+getExpectedConstraintValue();
		}
		else {
			this.patchValue = getExpectedConstraintValue();
		}
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

	
	private String getExpectedConstraintValue() {
		String expectedValue = "";
		try {
			expectedValue = constraint.getExpectedConstraintValue();
		} catch (Exception e) {
			logger.error(e);
		}
		return expectedValue;
	}
	
	private String getRealValue() {
		String expectedValue = "";
		try {
			expectedValue = constraint.getUsedValue();
		} catch (Exception e) {
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
