package de.upb.cognicryptfix.patcher.patches;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.base.Stopwatch;
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
import de.upb.cognicryptfix.exception.jimple.NotExpectedUnitException;
import de.upb.cognicryptfix.exception.patch.NotSupportedErrorException;
import de.upb.cognicryptfix.exception.patch.RepairException;
import de.upb.cognicryptfix.generator.jimple.JimpleUtils;
import de.upb.cognicryptfix.scheduler.ErrorScheduler;
import de.upb.cognicryptfix.utils.BoomerangUtils;
import de.upb.cognicryptfix.utils.Utils;
import soot.Body;
import soot.Local;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.Constant;
import soot.jimple.InvokeExpr;
import sync.pds.solver.nodes.Node;

/**
 * @author Andre Sonntag
 * @date 21.04.2019
 */
public class ConstraintPatch extends AbstractPatch {

	private static final Logger LOGGER = LogManager.getLogger(ConstraintPatch.class);

	private ConstraintError error;
	private ISLConstraint constraint;
	private AnalysisSeedWithSpecification seed;

	private CrySLEntity entity;
	private Body body;
	private String patch;

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
		this.patch = "";
	}

	@Override
	public Body applyPatch() throws RepairException {	
		Unit errorCallUnit = error.getErrorLocation().getUnit().get();
		int errorParamIndex = error.getCallSiteWithExtractedValue().getCallSite().getIndex();
		ExtractedValue ev = error.getCallSiteWithExtractedValue().getVal();
	
		if (JimpleUtils.containsInvokeExpr(errorCallUnit)) {
			InvokeExpr invokeExpr = JimpleUtils.getInvokeExpr(errorCallUnit);
			SootMethod invokedMethod = invokeExpr.getMethod();
			CrySLMethodCall call = entity.getFSM().getCrySLMethodCallBySootMethod(invokedMethod);
			CrySLVariable paramCrySLVar = call.getCallParameters().get(errorParamIndex);
			Value expectedValue = null;
						
			if (constraint instanceof CrySLComparisonConstraint) {
				CrySLComparisonConstraint compConstraint = (CrySLComparisonConstraint) constraint;
				
				//TODO: check for constraints with variables 
				
				expectedValue = paramCrySLVar.getValue();
			} else if (constraint instanceof CrySLValueConstraint) {
				CrySLValueConstraint valueConstraint = (CrySLValueConstraint) constraint;
				CrySLSplitter splitter = valueConstraint.getVar().getSplitter();
				List<String> valueRange = valueConstraint.getValueRange();
				String concatenatedValue = "";
				if (splitter == null) {
					expectedValue = JimpleUtils.generateConstantValue(paramCrySLVar.getType(), valueRange.get(0));
				} else {
					String usedValue = Utils.extractValueAsString(seed, valueConstraint.getVar().getVarName()).keySet().iterator().next().toString();
					long countSplitter = usedValue.chars().filter(ch -> ch == splitter.getSplitter().toCharArray()[0]).count();
					
					
					if(splitter.getIndex() == 0) {
						if(valueConstraint.getValueRange().contains(usedValue)) {
							concatenatedValue = usedValue + splitter.getSplitter() + valueRange.get(0);
						} else {
							concatenatedValue = valueConstraint.getValueRange().get(0);
						}
					} else if(countSplitter < splitter.getIndex()) { //AES || AES/ECB
						concatenatedValue = usedValue + splitter.getSplitter() + valueRange.get(0);
					} else if(countSplitter == splitter.getIndex()) {
						concatenatedValue = usedValue.substring(0, usedValue.lastIndexOf(splitter.getSplitter())+1)+valueRange.get(0);
					} else if(countSplitter > splitter.getIndex()) {
						
						int index = -1;
						List<Integer> splitterPosition = Lists.newArrayList();
						while((index = usedValue.indexOf(splitter.getSplitter(), index + 1)) >= 0) {
							splitterPosition.add(index);
						}
						
						String errorValue = usedValue.substring(splitterPosition.get(splitter.getIndex()-1)+1, splitterPosition.get(splitter.getIndex()));
						
						concatenatedValue = usedValue.replace(errorValue, valueRange.get(0));
					}

					expectedValue = JimpleUtils.generateConstantValue(concatenatedValue);
				}
			} else {
				throw new NotSupportedErrorException("ConstraintPatch supports just the repair of CrySLValueConstraint & CrySLComparisonConstraint.");
			}

			if(invokeExpr.getArg(errorParamIndex) instanceof Local) {
				Local errorParamLocal = (Local) invokeExpr.getArg(errorParamIndex);
				AssignStmt assignStmt = getLastAssignStmtOfLocal(ev, errorParamLocal);
				if (assignStmt != null) {
					assignStmt.setRightOp(expectedValue);
					patch += assignStmt.toString();
				} else {
					throw new NotExpectedUnitException("No AssignStmt found for "+errorParamLocal.toString());
				}
			} else  {
				invokeExpr.setArg(errorParamIndex, expectedValue);
				patch += errorCallUnit.toString();
			}
		} else {
			throw new NotExpectedUnitException("ConstraintPatch ErrorLocation doesn't contain an InvokeExpr");
		}	
		return body;
	}

	private AssignStmt getLastAssignStmtOfLocal(ExtractedValue ev, Local errorParamLocal) throws NotExpectedUnitException {
		Constant value = (Constant) ev.getValue();
		AssignStmt ret = null;

		for (Node<Statement, Val> node : ev.getDataFlowPath()) {
			
			if(node.stmt().getUnit().get() instanceof AssignStmt) {
				AssignStmt assignStmt = (AssignStmt) node.stmt().getUnit().get();
				if(assignStmt.getRightOp() instanceof Constant) {
					if(assignStmt.getRightOp() == value) {
						ret = assignStmt;
						patch += node.fact().m().getSignature()+" -> ";
						body = node.fact().m().getActiveBody();
						break;
					}
				}
			}	
		}

		if (ret != null) {
			return ret;
		} else {
			throw new NotExpectedUnitException("Local doesn't contain any assignment! Local: "+ errorParamLocal.toString());
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
		builder.append("\n__________[ConstraintPatch]__________\n");
		builder.append("Repaired in Round: \t" + ErrorScheduler.round+ "\n");
		builder.append("Class: \t" + error.getErrorLocation().getMethod().getDeclaringClass().toString() + "\n");
		builder.append("Method: \t" + error.getErrorLocation().getMethod().getSignature() + "\n");
		builder.append("Error: \t" + error.getClass().getSimpleName() + "\n");
		builder.append("CrySLRule: \t" + entity.getRule().getClassName() + "\n");
		builder.append("Constraint: \t" + constraintClass + "\n");
		builder.append("Message: \t" + error.toErrorMarkerString() + "\n");
		builder.append("Patch: \t" + patch);
		builder.append("\n");
		builder.append("__________________________________________");

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
