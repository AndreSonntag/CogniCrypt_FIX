package de.upb.cognicryptfix.patcher.patches;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.management.RuntimeErrorException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.primitives.Primitives;
import com.sun.javafx.binding.LongConstant;

import boomerang.callgraph.ObservableICFG;
import boomerang.jimple.Statement;
import crypto.analysis.errors.NeverTypeOfError;
import crypto.extractparameter.ExtractedValue;
import crypto.rules.CrySLRule;
import de.upb.cognicryptfix.analysis.CryptoAnalysis;
import de.upb.cognicryptfix.extractor.constraints.IConstraint;
import de.upb.cognicryptfix.generator.JimpleCodeGenerator;
import de.upb.cognicryptfix.generator.jimple.JimpleCallGenerator;
import de.upb.cognicryptfix.generator.jimple.JimpleUtils;
import de.upb.cognicryptfix.utils.BoomerangUtils;
import de.upb.cognicryptfix.utils.Utils;
import javassist.compiler.ast.DoubleConst;
import soot.ArrayType;
import soot.Body;
import soot.ByteType;
import soot.CharType;
import soot.Local;
import soot.PrimType;
import soot.RefType;
import soot.Scene;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.ValueBox;
import soot.jimple.AssignStmt;
import soot.jimple.IntConstant;
import soot.jimple.InvokeExpr;
import soot.jimple.Jimple;
import soot.jimple.StaticInvokeExpr;
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
	private JimpleCodeGenerator codeGenerator;

	public NeverTypeOfPatch(NeverTypeOfError error, IConstraint predCon) {
		this.error = error;
		this.predCon = predCon;
		this.icfg = CryptoAnalysis.staticScanner.icfg();
		this.codeGenerator = JimpleCodeGenerator.getInstance(error.getErrorLocation().getMethod().getActiveBody());
		setErrorInformation();
	}

	@Override
	public Body getPatch() {
		Body body = error.getErrorLocation().getMethod().getActiveBody();

		try {
			Type usedType = Scene.v().getType(predCon.getUsedValue());
			Type expectedType = Scene.v().getType(predCon.getExpectedConstraintValue());

			switch (predCon.getUsedValue()) {
			case "java.lang.String":
				replaceStringType(body, usedType, expectedType); // char[] or byte[]
				break;
			default:
				break;
			}
		} catch (Exception e) {
			logger.error(e);
		}

		return body;
	}

	private void replaceStringType(Body body, Type usedType, Type expectedType) {
//		Test purposes
//		Type byteType = Scene.v().getType("byte[]");
//		expectedType = byteType;
		
		HashMap<Local, List<Unit>> generatedUnits = Maps.newLinkedHashMap();
		AssignStmt wrapperAssignStmt = (AssignStmt) error.getCallSiteWithExtractedValue().getVal().stmt().getUnit()
				.get();
		InvokeExpr wrapperInvokeExpr = (InvokeExpr) wrapperAssignStmt.getRightOpBox().getValue();
		Local stringLocal = (Local) wrapperInvokeExpr.getUseBoxes().get(0).getValue();

		/*-
		 * Boomerang workaround: replace the wrapperInvokeExpr by the local which calls
		 * the wrapper call to get the original String value by Boomerang
		 */
		wrapperAssignStmt.setRightOp(stringLocal);
		ArrayList<Value> valueList = extractValueListViaBoomerang();

		if (usedType == Scene.v().getType("java.lang.String") && expectedType instanceof ArrayType) {
			generatedUnits = generateUnitsForArrayType(valueList, usedType, (ArrayType) expectedType);
			Local arrayLocal = generatedUnits.keySet().iterator().next();
			Unit arrayAssign = generatedUnits.values().iterator().next().get(0);
			patchValue = arrayLocal.toString() + " -> " + arrayAssign.toString();
			wrapperAssignStmt.getRightOpBox().setValue(arrayLocal);
			body.getUnits().insertBefore(generatedUnits.get(arrayLocal), wrapperAssignStmt);
//			System.out.println();
		} else if (expectedType instanceof PrimType) {
		} else if (expectedType == Scene.v().getType("java.lang.String")) {
		}
	}

	private HashMap<Local, List<Unit>> generateUnitsForArrayType(List<Value> values, Type usedType,
			ArrayType expectedArrayType) {
		HashMap<Local, List<Unit>> generatedUnits = Maps.newHashMap();
		Type stringType = Scene.v().getType("java.lang.String");
		Value value = values.get(0);
		List<Value> constants = Lists.newArrayList();

		if (usedType instanceof RefType) {
			if (usedType == stringType) {
				String valueString = ((StringConstant) value).value;				
				
				if (expectedArrayType.baseType instanceof CharType) {
					for (char c : valueString.toCharArray()) {
						constants.add(JimpleUtils.generateConstantValue(c + ""));
					}
				} else if (expectedArrayType.baseType instanceof ByteType) {
					for (char c : valueString.toCharArray()) {
						byte b = (byte) c;
						constants.add(JimpleUtils.generateConstantValue(b));
					}
				}
			}
		}

		generatedUnits = codeGenerator.generateArray(expectedArrayType.baseType, constants);
		return generatedUnits;
	}
	
	private ArrayList<Value> extractValueListViaBoomerang() {
		Unit errorUnit = error.getErrorLocation().getUnit().get();
		Statement statement = error.getCallSiteWithExtractedValue().getCallSite().stmt();
		Local parameter = (Local) error.getErrorLocation().getUnit().get().getInvokeExpr().getArg(brokenVarIndex);
		ArrayList<ExtractedValue> evList = BoomerangUtils.bommerangPointsToAnalysis(icfg, parameter, statement,
				errorUnit);
		ArrayList<Value> valueList = Lists.newArrayList();
		for (ExtractedValue ev : evList) {
			valueList.add(ev.getValue());
		}
		return valueList;
	}

	private void setErrorInformation() {
		this.violatedCrySLRule = error.getRule();
		this.crySLVarName = error.getCallSiteWithExtractedValue().getCallSite().getVarName();
		this.brokenVarIndex = error.getCallSiteWithExtractedValue().getCallSite().getIndex();
		this.brokenJimpleCode = error.getErrorLocation().getUnit().get().getInvokeExpr().toString();
		this.jimpleVarName = error.getErrorLocation().getUnit().get().getInvokeExpr().getArgs().get(brokenVarIndex)
				.toString();
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
		strBuilder.append("new type for jimple Variable " + jimpleVarName + " =" + patchValue + "\n");
		return strBuilder.toString();
	}

// General approach!
//	private HashMap<Local, List<Unit>> generateUnitsForPrimType(List<Value> values, Type usedType,
//			PrimType expectedType) {
//
//		HashMap<Local, List<Unit>> generatedUnits = Maps.newHashMap();
//		Type stringType = Scene.v().getType("java.lang.String");
//		Value value = values.get(0);
//
//		if (usedType instanceof PrimType) {
//			if (JimpleUtils.isNumericPrimType(usedType) && JimpleUtils.isNumericPrimType(expectedType)) { // i.e.
//																											// Integer
//				// to Double
//			}
//		} else if (usedType instanceof RefType) {
//			if (usedType == stringType && JimpleUtils.isNumericPrimType(expectedType)) { // i.e. String to Integer
//
//			}
//		} else if (usedType instanceof ArrayType) {
//			throw new RuntimeException("Convertring from ArrayType to PrimeType is not supported"); // i.e. byte[] to
//																									// Integer
//		}
//		return null;
//	}

}
