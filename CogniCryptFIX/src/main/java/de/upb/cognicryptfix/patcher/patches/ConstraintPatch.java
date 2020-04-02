package de.upb.cognicryptfix.patcher.patches;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import boomerang.callgraph.ObservableICFG;
import boomerang.jimple.Statement;
import boomerang.jimple.Val;
import crypto.analysis.AnalysisSeedWithSpecification;
import crypto.analysis.errors.ConstraintError;
import crypto.extractparameter.ExtractedValue;
import crypto.interfaces.ISLConstraint;
import crypto.rules.CrySLComparisonConstraint;
import crypto.rules.CrySLSplitter;
import crypto.rules.CrySLValueConstraint;
import de.upb.cognicryptfix.analysis.CryptoAnalysis;
import de.upb.cognicryptfix.crysl.CrySLEntity;
import de.upb.cognicryptfix.crysl.CrySLMethodCall;
import de.upb.cognicryptfix.crysl.CrySLVariable;
import de.upb.cognicryptfix.crysl.pool.CrySLEntityPool;
import de.upb.cognicryptfix.exception.NotExpectedUnitException;
import de.upb.cognicryptfix.exception.NotSupportedConstraintException;
import de.upb.cognicryptfix.generator.jimple.JimpleUtils;
import de.upb.cognicryptfix.utils.BoomerangUtils;
import de.upb.cognicryptfix.utils.Utils;
import soot.Body;
import soot.Local;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.InvokeExpr;
import sync.pds.solver.nodes.Node;

/**
 * @author Andre Sonntag
 * @date 21.04.2019
 */
public class ConstraintPatch extends AbstractPatch {

	private ConstraintError error;
	private ISLConstraint constraint;
	private AnalysisSeedWithSpecification seed;

	private CrySLEntity entity;
	private Body body;
	private ObservableICFG<Unit, SootMethod> icfg;
	private Unit patchUnit;

	/**
	 * Creates a new {@link ConstraintPatch} object that represents the patch for
	 * the {@link ConstraintError} argument. Currently the patch supports the repair
	 * of {@link CrySLComparisonConstraint} and {@link CrySLValueConstraint}.
	 * 
	 * @param error the error with a violated {@link CrySLComparisonConstraint} or
	 *              {@link CrySLValueConstraint}
	 */
	public ConstraintPatch(ConstraintError error) {
		this.error = error;
		this.constraint = error.getBrokenConstraint();
		this.seed = (AnalysisSeedWithSpecification) error.getObjectLocation();
		this.entity = CrySLEntityPool.getInstance().getEntityByClassName(error.getRule().getClassName());
		this.body = error.getErrorLocation().getMethod().getActiveBody();
		this.icfg = CryptoAnalysis.staticScanner.icfg();
		this.patchUnit = null;
	}

	@Override
	public Body applyPatch() throws NotSupportedConstraintException, NotExpectedUnitException {
		Unit errorCallUnit = error.getErrorLocation().getUnit().get();
		Statement errorStatement = error.getCallSiteWithExtractedValue().getCallSite().stmt();
		int errorParamIndex = error.getCallSiteWithExtractedValue().getCallSite().getIndex();

		if (JimpleUtils.containsInvokeExpr(errorCallUnit)) {
			InvokeExpr invokeExpr = JimpleUtils.getInvokeExpr(errorCallUnit);
			SootMethod invokedMethod = invokeExpr.getMethod();
			Local errorParamLocal = (Local) invokeExpr.getArg(errorParamIndex);
			CrySLMethodCall call = entity.getFSM().getCrySLMethodCallBySootMethod(invokedMethod);
			CrySLVariable paramCrySLVar = call.getCallParameters().get(errorParamIndex);
			Value expectedValue = null;

			if (constraint instanceof CrySLComparisonConstraint) {
				expectedValue = paramCrySLVar.getValue();
			} else if (constraint instanceof CrySLValueConstraint) {
				CrySLValueConstraint valueConstraint = (CrySLValueConstraint) constraint;
				CrySLSplitter splitter = valueConstraint.getVar().getSplitter();
				List<String> valueRange = valueConstraint.getValueRange();

				if (splitter == null) {
					expectedValue = paramCrySLVar.getValue();
				} else {
					String usedValue = Utils.extractValueAsString(seed, valueConstraint.getVar().getVarName()).keySet()
							.iterator().next().toString();
					String concatenatedValue = usedValue + splitter.getSplitter() + valueRange.get(0);
					expectedValue = JimpleUtils.generateConstantValue(concatenatedValue);
				}
			} else {
				throw new NotSupportedConstraintException("ConstraintPatch supports just the repair of CrySLValueConstraint & CrySLComparisonConstraint.");
			}

			AssignStmt assignStmt = getParameterAssignStmt(icfg, errorStatement, errorCallUnit, errorParamLocal);
			if (assignStmt != null) {
				assignStmt.setRightOp(expectedValue);
				patchUnit = assignStmt;
			} else {
				throw new NotExpectedUnitException("No AssignStmt for errorParamLocal.");
			}
		} else {
			throw new NotExpectedUnitException("error.getErrorLocation() doesn't contain an InvokeExpr.");
		}
		return body;
	}

	private AssignStmt getParameterAssignStmt(ObservableICFG<Unit, SootMethod> icfg, Statement errorStatement, Unit errorCallUnit, Local errorParamLocal) throws NotExpectedUnitException {
		List<ExtractedValue> extractedValues = BoomerangUtils.runBommerang(icfg, errorParamLocal, errorStatement,
				errorCallUnit);
		AssignStmt assignStmt = getLastAssignStmt(extractedValues.get(0), errorParamLocal);
		return assignStmt;
	}

	private AssignStmt getLastAssignStmt(ExtractedValue ev, Local errorParamLocal) throws NotExpectedUnitException {
		Set<Unit> unsortedUnitSet = Sets.newHashSet();
		List<Unit> sortedUnitList = Lists.newArrayList();
		AssignStmt retAssignStmt = null;

		
		for (Node<Statement, Val> node : ev.getDataFlowPath()) {
			unsortedUnitSet.add(node.stmt().getUnit().get());
		}

		for (Unit u : body.getUnits()) {
			if (unsortedUnitSet.contains(u)) {
				sortedUnitList.add(u);
			}
		}

		Collections.reverse(sortedUnitList);
		for (Unit u : sortedUnitList) {
			if (u instanceof AssignStmt) {
				AssignStmt assignStmt = (AssignStmt) u;
				if (assignStmt.getLeftOp() == errorParamLocal) {
					retAssignStmt = assignStmt;
					break;
				}
			}
		}
		
		if(retAssignStmt != null) {
			return retAssignStmt;
		} else {
			throw new NotExpectedUnitException("Local doesn't contain any assignment! used Local: "+errorParamLocal.toString()+" sortedDataFlow: "+sortedUnitList.toString());
		}
	}

	@Override
	public String toPatchString() {
		String constraintClass = "";
		if (constraint instanceof CrySLComparisonConstraint) {
			constraintClass = "CrySLComparisonConstraint";
		} else if (constraint instanceof CrySLValueConstraint) {
			constraintClass = "CrySLValueConstraint";
		}

		StringBuilder builder = new StringBuilder();
		builder.append(
				"\n------------------->--------------------ConstraintPatch--------------------->------------------\n");
		builder.append("Class: \t\t" + error.getErrorLocation().getMethod().getDeclaringClass().toString() + "\n");
		builder.append("Method: \t" + error.getErrorLocation().getMethod().getSignature() + "\n");
		builder.append("Error: \t\t" + error.getClass().getSimpleName() + "\n");
		builder.append("CrySLRule: \t" + entity.getRule().getClassName() + "\n");
		builder.append("Constraint: \t" + constraintClass + "\n");
		builder.append("Message: \t" + error.toErrorMarkerString() + "\n");
		builder.append("Patch: \t\t" + patchUnit);
		builder.append("\n");
		builder.append(
				"-----------------------------------------------------------------------------------------------\n");
		return builder.toString();
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ConstraintPatch [error=");
		builder.append(error.toErrorMarkerString());
		builder.append(", constraint=");
		builder.append(constraint);
		builder.append(", entity=");
		builder.append(entity.getRule().getClassName());
		builder.append("]");
		return builder.toString();
	}
	
	
	
}
