package de.upb.cognicryptfix.patcher.patches;

import java.util.ArrayList;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import boomerang.BackwardQuery;
import boomerang.Boomerang;
import boomerang.ForwardQuery;
import boomerang.callgraph.ObservableICFG;
import boomerang.jimple.AllocVal;
import boomerang.jimple.Statement;
import boomerang.jimple.Val;
import boomerang.results.BackwardBoomerangResults;
import boomerang.seedfactory.SeedFactory;
import crypto.analysis.errors.ConstraintError;
import crypto.boomerang.CogniCryptIntAndStringBoomerangOptions;
import crypto.extractparameter.ExtractedValue;
import crypto.rules.CrySLRule;
import de.upb.cognicryptfix.analysis.CryptoAnalysis;
import de.upb.cognicryptfix.extractor.constraints.IConstraint;
import de.upb.cognicryptfix.utils.BoomerangUtils;
import soot.Body;
import soot.Local;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.IntConstant;
import soot.jimple.StringConstant;
import soot.jimple.internal.JAssignStmt;
import wpds.impl.Weight.NoWeight;


/**
 * TODO: find a good description
 * @author Andre Sonntag
 * @date 21.04.2019
 */
public class PrimitiveConstraintPatch extends AbstractPatch {

	private static final Logger logger = LogManager.getLogger(PrimitiveConstraintPatch.class.getSimpleName());

	private final ConstraintError error;
	private final IConstraint constraint;
	private CrySLRule violatedCrySLRule;
	private String crySLVarName;
	private String jimpleVarName;
	private String jimpleVarValue;
	private String brokenJimpleCode;
	private int brokenVarIndex;
	private String patchValue;
	private ObservableICFG<Unit, SootMethod> icfg;

	public PrimitiveConstraintPatch(ConstraintError error, IConstraint constraint) {
		this.error = error;
		this.constraint = constraint;
		this.icfg = CryptoAnalysis.staticScanner.icfg();
		setErrorInformation();
	}
	
	private void setErrorInformation(){
		this.violatedCrySLRule = error.getRule();
		this.crySLVarName = error.getCallSiteWithExtractedValue().getCallSite().getVarName();
		this.brokenVarIndex = error.getCallSiteWithExtractedValue().getCallSite().getIndex();
		this.brokenJimpleCode = error.getErrorLocation().getUnit().get().getInvokeExpr().toString();
		this.jimpleVarName = error.getErrorLocation().getUnit().get().getInvokeExpr().getArgs().get(brokenVarIndex).toString();
		this.jimpleVarValue = getRealValue();
		this.patchValue = getExpectedConstraintValue();
	}
	
	@Override
	public Body getPatch() {
		Body methodBody = error.getErrorLocation().getMethod().getActiveBody();
		Unit errorCall = error.getErrorLocation().getUnit().get();
		Statement statement = error.getCallSiteWithExtractedValue().getCallSite().stmt();		
		Value parameter = error.getErrorLocation().getUnit().get().getInvokeExpr().getArg(brokenVarIndex);
		ExtractedValue ev = BoomerangUtils.bommerangPointsToAnalysis(icfg, (Local) parameter, statement, errorCall).get(0);
		JAssignStmt var = (JAssignStmt) ev.stmt().getUnit().get();
		
		
		// TODO: re-engineer the return types of the Values
		if(StringUtils.isNumeric(patchValue)) {		
			var.rightBox.setValue(IntConstant.v(Integer.parseInt(patchValue)));
		}
		else {
			var.rightBox.setValue(StringConstant.v(patchValue));
		}		
		//TODO: is boolean, char, etc.
		return methodBody;
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
	
	@Override
	public String toString() {
		StringBuilder strBuilder = new StringBuilder();
		strBuilder.append("\nPrimitiveConstraintPatch [\n");
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
}
