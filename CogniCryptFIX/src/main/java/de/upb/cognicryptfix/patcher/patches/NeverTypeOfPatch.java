package de.upb.cognicryptfix.patcher.patches;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.activation.UnsupportedDataTypeException;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import boomerang.callgraph.ObservableICFG;
import boomerang.jimple.Statement;
import boomerang.jimple.Val;
import crypto.analysis.errors.NeverTypeOfError;
import crypto.extractparameter.ExtractedValue;
import crypto.interfaces.ISLConstraint;
import crypto.rules.CrySLPredicate;
import de.upb.cognicryptfix.Constants;
import de.upb.cognicryptfix.analysis.CryptoAnalysis;
import de.upb.cognicryptfix.crysl.CrySLEntity;
import de.upb.cognicryptfix.crysl.CrySLVariable;
import de.upb.cognicryptfix.crysl.pool.CrySLEntityPool;
import de.upb.cognicryptfix.exception.jimple.NotExpectedUnitException;
import de.upb.cognicryptfix.exception.jimple.NotSupportedUnitTypeException;
import de.upb.cognicryptfix.exception.patch.NotSupportedErrorException;
import de.upb.cognicryptfix.exception.patch.RepairException;
import de.upb.cognicryptfix.generator.JimpleCodeGeneratorByRule;
import de.upb.cognicryptfix.generator.jimple.JimpleUtils;
import de.upb.cognicryptfix.scheduler.ErrorScheduler;
import de.upb.cognicryptfix.utils.BoomerangUtils;
import de.upb.cognicryptfix.utils.Utils;
import soot.ArrayType;
import soot.Body;
import soot.ByteType;
import soot.CharType;
import soot.Local;
import soot.Scene;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.Constant;
import soot.jimple.InvokeExpr;
import soot.jimple.StringConstant;
import sync.pds.solver.nodes.Node;

public class NeverTypeOfPatch extends AbstractPatch {

	private static final Logger LOGGER = LogManager.getLogger(NeverTypeOfPatch.class);
	private NeverTypeOfError error;
	private ISLConstraint constraint;

	private CrySLEntity entity;
	private Body body;
	private JimpleCodeGeneratorByRule generator;
	private ObservableICFG<Unit, SootMethod> icfg;
	private List<Unit> patch;

	private List<Integer> supportedTypes;

	
	//Datatype comes from a parameter, or method call
	/**
	 * Creates a new {@link NeverTypeOfPatch} object that represents the patch for
	 * the {@link NeverTypeOfError} argument.
	 * 
	 * @param error the error
	 */
	public NeverTypeOfPatch(NeverTypeOfError error) {
		this.error = error;
		this.constraint = error.getBrokenConstraint();
		this.entity = CrySLEntityPool.getInstance().getEntityByClassName(error.getRule().getClassName());
		this.body = error.getErrorLocation().getMethod().getActiveBody();
		this.generator = new JimpleCodeGeneratorByRule(body);
		this.icfg = CryptoAnalysis.staticScanner.icfg();
		this.patch = Lists.newArrayList();
		this.supportedTypes = Lists.newArrayList(Scene.v().getType("char[]").getNumber(),
				Scene.v().getType("byte[]").getNumber());
	}

	@Override
	public Body applyPatch() throws RepairException {

		CrySLPredicate pred = (CrySLPredicate) constraint;
		if (pred.getPredName().equals("neverTypeOf")) {
			Type forbiddenType = Scene.v().getType(pred.getParameters().get(1).toString());
			String variableName = pred.getParameters().get(0).getName();
			CrySLVariable variable = entity.getVariableByName(variableName);
			Type targetType = variable.getType();

			if (JimpleUtils.equals(forbiddenType, Scene.v().getType("java.lang.String"))) {
				replaceStringType(targetType);
			} else {
				throw new NotSupportedUnitTypeException(
						"NeverTypeOfPatch supports just the wrapping of java.lang.String! used Type: "
								+ forbiddenType.toQuotedString());
			}
		} else {
			throw new NotSupportedErrorException(
					"CrySLPredicate doesn't contain neverTypeOf! used predicate: " + pred.getPredName());
		}
	

	return body;

	}

	private void replaceStringType(Type targetType) throws NotExpectedUnitException, NotSupportedUnitTypeException {

		if (!supportedTypes.contains(targetType.getNumber())) {
			throw new NotSupportedUnitTypeException("The target type must by char[] or byte[]");
		}

		Map<Local, List<Unit>> generatedUnits = Maps.newLinkedHashMap();
		Unit wrapperUnit = error.getCallSiteWithExtractedValue().getVal().stmt().getUnit().get();

		if (wrapperUnit instanceof AssignStmt && JimpleUtils.containsInvokeExpr(wrapperUnit)) {
			AssignStmt wrapperAssignStmt = (AssignStmt) error.getCallSiteWithExtractedValue().getVal().stmt().getUnit().get();
			InvokeExpr wrapperInvokeExpr = (InvokeExpr) wrapperAssignStmt.getRightOpBox().getValue();
			Local wrapperInputLocal = (Local) wrapperInvokeExpr.getUseBoxes().get(0).getValue();

			/*-
			 * Boomerang workaround: replace the wrapperInvokeExpr by the local which calls
			 * the wrapper call to get the original String value by Boomerang
			 */
			Value originalValue = wrapperAssignStmt.getRightOp();
			wrapperAssignStmt.setRightOp(wrapperInputLocal);
			AssignStmt stringAssignStmt = getAllocationAssignStmt(error.getCallSiteWithExtractedValue().getCallSite().stmt(), error.getErrorLocation().getUnit().get(), wrapperInputLocal);
			
			if(stringAssignStmt == null) {
				wrapperAssignStmt.setRightOp(originalValue);
				throw new NotExpectedUnitException("Parameter value comes from a stream/database/other external source!");
			}
			
			generatedUnits = generateArrayUnitsForStringType((Local) wrapperAssignStmt.getLeftOp(),(ArrayType) targetType, (StringConstant) stringAssignStmt.getRightOp());
			body.getUnits().insertAfter(Utils.summarizeUnitLists(generatedUnits.values()), wrapperAssignStmt);
			body.getUnits().remove(stringAssignStmt);
			body.getUnits().remove(wrapperAssignStmt);
			patch.addAll(Utils.summarizeUnitLists(generatedUnits.values()));

		} else {
			throw new NotExpectedUnitException(
					"CallSiteValue doesn't contain an AssignStmt with an InvokeExpr! Unit: " + wrapperUnit);
		}
	}

	private Map<Local, List<Unit>> generateArrayUnitsForStringType(Local arrayLocal, ArrayType targetType,
			StringConstant value) {

		Map<Local, List<Unit>> generatedUnits = Maps.newHashMap();
		List<Value> constants = Lists.newArrayList();
		String valueString = ((StringConstant) value).value;

		if (JimpleUtils.equals(targetType, Scene.v().getType("char[]")) ) {
			for (char c : valueString.toCharArray()) {
				constants.add(JimpleUtils.generateConstantValue((int) c));
			}
		} else if (JimpleUtils.equals(targetType, Scene.v().getType("byte[]")) ) {
			for (char c : valueString.toCharArray()) {
				byte b = (byte) c;
				constants.add(JimpleUtils.generateConstantValue(b));
			}
		}
		generatedUnits = generator.generateArray(arrayLocal, constants);
		return generatedUnits;
	}

	private AssignStmt getAllocationAssignStmt(Statement errorStatement, Unit errorCallUnit, Local errorParamLocal) throws NotExpectedUnitException {
		List<ExtractedValue> extractedValues = BoomerangUtils.runBommerang(icfg, errorParamLocal, errorStatement, errorCallUnit);
		
		if(CollectionUtils.isEmpty(extractedValues)) {
			return null;
		}
		
		List<ExtractedValue> evList = extractedValues;
		for(ExtractedValue ev : evList) {
			if(ev.stmt().getMethod().getSignature().equals(error.getErrorLocation().getMethod().getSignature())) {
				AssignStmt extracted = getAssignStmt(ev, errorParamLocal);
				if(extracted != null) {
					return extracted;
				}
			} else {
				
			}		
		}
				
		return null;
	}

	private AssignStmt getAssignStmt(ExtractedValue ev, Local errorParamLocal) throws NotExpectedUnitException {

		if(!(ev.getValue() instanceof Constant)) {
			return null;
		}
		
		Constant value = (Constant) ev.getValue();
		AssignStmt ret = null;
		for (Node<Statement, Val> node : ev.getDataFlowPath()) {
			if(node.stmt().getUnit().get() instanceof AssignStmt) {
				AssignStmt assignStmt = (AssignStmt) node.stmt().getUnit().get();
				if(assignStmt.getRightOp() instanceof StringConstant) {
					if(assignStmt.getRightOp() == value) {
						ret = assignStmt;
						break;
					}
				}
			}
		}

		if (ret != null) {
			return ret;
		} else {
			throw new NotExpectedUnitException("Local doesn't contain a String assignment! used Local: "
					+ errorParamLocal.toString() + " dataFlow: " + ev.getDataFlowPath().toString());
		}
	}
	
	@Override
	public String toPatchString() {
		StringBuilder builder = new StringBuilder();
		builder.append("\n__________[NeverTypeOfPatch]__________\n");
		builder.append("Repaired in Round: \t" + ErrorScheduler.round+ "\n");
		builder.append("Class: \t" + error.getErrorLocation().getMethod().getDeclaringClass().toString() + "\n");
		builder.append("Method: \t" + error.getErrorLocation().getMethod().getSignature() + "\n");
		builder.append("Error: \t" + error.getClass().getSimpleName() + "\n");
		builder.append("CrySLRule: \t" + entity.getRule().getClassName() + "\n");
		builder.append("Constraint: \t" + "NeverTypeOf" + "\n");
		builder.append("Message: \t" + error.toErrorMarkerString() + "\n");
		builder.append("Patch: \t" + patch.toString());
		builder.append("\n");
		builder.append("__________________________________________");
		return builder.toString();
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("NeverTypeOfPatch [error=");
		builder.append(error.toErrorMarkerString());
		builder.append(", constraint=");
		builder.append(constraint);
		builder.append(", entity=");
		builder.append(entity.getRule().getClassName());
		builder.append("]");
		return builder.toString();
	}
}
