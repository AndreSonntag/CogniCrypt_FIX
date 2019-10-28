package de.upb.cognicryptfix.patcher.patches;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.Maps;

import boomerang.BackwardQuery;
import boomerang.Boomerang;
import boomerang.DefaultBoomerangOptions;
import boomerang.ForwardQuery;
import boomerang.callgraph.ObservableICFG;
import boomerang.jimple.Statement;
import boomerang.jimple.Val;
import boomerang.results.AbstractBoomerangResults;
import boomerang.results.BackwardBoomerangResults;
import boomerang.seedfactory.SeedFactory;
import crypto.analysis.AnalysisSeedWithSpecification;
import crypto.analysis.errors.NeverTypeOfError;
import crypto.rules.CryptSLRule;
import de.upb.cognicryptfix.HeadlessRepairer;
import de.upb.cognicryptfix.analysis.CryptoAnalysis;
import de.upb.cognicryptfix.extractor.constraints.IConstraint;
import de.upb.cognicryptfix.utils.JimpleUtils;
import de.upb.cognicryptfix.utils.Utils;
import soot.ArrayType;
import soot.Body;
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
import soot.javaToJimple.LocalGenerator;
import soot.jimple.ArrayRef;
import soot.jimple.IntConstant;
import soot.jimple.Jimple;
import soot.jimple.NewArrayExpr;
import soot.jimple.Stmt;
import soot.jimple.StringConstant;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.internal.JVirtualInvokeExpr;
import soot.jimple.internal.JimpleLocal;
import soot.jimple.internal.JimpleLocalBox;
import soot.jimple.toolkits.pointer.LocalMustAliasAnalysis;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.SimpleLocalDefs;
import soot.toolkits.scalar.SimpleLocalUses;
import soot.util.Chain;
import wpds.impl.Weight.NoWeight;

public class NeverTypeOfPatch extends AbstractPatch {
	private static final Logger logger = LogManager.getLogger(NeverTypeOfPatch.class.getSimpleName());
	public final static HashMap<Type, Type> predefinedTypeReplacement;
	static {
		predefinedTypeReplacement = new HashMap<>();
		predefinedTypeReplacement.put(Scene.v().getType("java.lang.String"), Scene.v().getType("char[]"));
	}

	private NeverTypeOfError error;
	private IConstraint predCon;
	private CryptSLRule violatedCrySLRule;
	private String crySLVarName;
	private String jimpleVarName;
	private String jimpleVarValue;
	private String jimpleVarType;
	private String brokenJimpleCode;
	private int brokenVarIndex;
	private String patchValue;

	public NeverTypeOfPatch(NeverTypeOfError error, IConstraint predCon) {
		this.error = error;
		this.predCon = predCon;
		setErrorInformation();
		logger.info(toString());
		System.out.println();
	}

	public Body replaceStringByCharArray(Body methodBody) {

		Unit toCharArray = error.getCallSiteWithExtractedValue().getVal().stmt().getUnit().get();

		List<Value> parameter = new ArrayList();
		parameter.add(StringConstant.v("h"));
		parameter.add(StringConstant.v("a"));
		parameter.add(StringConstant.v("l"));
		parameter.add(StringConstant.v("l"));
		parameter.add(StringConstant.v("o"));

		HashMap<Value, List<Unit>> generatedArrayMap = JimpleUtils.generateParameterArray(parameter, methodBody);
		Value arrayRef = generatedArrayMap.keySet().iterator().next();
		List<Unit> generatedArrayUnits = generatedArrayMap.get(arrayRef);

		methodBody.getUnits().insertBefore(generatedArrayUnits, toCharArray);
		Unit call = error.getCallSiteWithExtractedValue().getVal().stmt().getUnit().get();
		if (call instanceof JAssignStmt) {
			JAssignStmt stmt = (JAssignStmt) call;
			stmt.setRightOp(arrayRef);
		}
		System.out.println();

		return methodBody;
	}

	@Override
	public Body getPatch() {
		Body methodBody = error.getErrorLocation().getMethod().getActiveBody();
		UnitGraph uGraph = new ExceptionalUnitGraph(methodBody);
		Unit toCharArray = error.getCallSiteWithExtractedValue().getVal().stmt().getUnit().get();
		
		ValueBox local = null;
		JVirtualInvokeExpr stmt = null;
		List<ValueBox> useBoxes = toCharArray.getUseBoxes();
		for (ValueBox box : useBoxes) {
			if(box.getValue() instanceof JimpleLocal) {
				local = box;
			}
			else if(box.getValue() instanceof JVirtualInvokeExpr) {
				stmt = (JVirtualInvokeExpr) box.getValue();
			}
		}
		System.out.println();

		
	runBoomerang(error.getCallSiteWithExtractedValue().getVal().stmt(), error.getErrorLocation().getMethod(), local.getValue(), CryptoAnalysis.staticScanner.icfg());
		
		

		try {
			switch (predCon.getUsedValue()) {
			case "java.lang.String":
				replaceStringByCharArray(methodBody);
				break;
			default:
				break;
			}
		} catch (Exception e) {
			logger.error(e);
		}

		return methodBody;
	}

	private void setErrorInformation() {
		this.violatedCrySLRule = error.getRule();
		this.crySLVarName = error.getCallSiteWithExtractedValue().getCallSite().getVarName();
		this.brokenVarIndex = error.getCallSiteWithExtractedValue().getCallSite().getIndex();
		this.brokenJimpleCode = error.getErrorLocation().getUnit().get().getInvokeExpr().toString();
		this.jimpleVarName = error.getErrorLocation().getUnit().get().getInvokeExpr().getArgs().get(brokenVarIndex)
				.toString();
		this.jimpleVarType = ""; // error.getErrorLocation().getUnit().get().getInvokeExpr().getArgs().get(brokenVarIndex).getType().toString();
		// this.jimpleVarValue = getRealValue();
		// this.patchValue = getExpectedConstraintValue();
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
		// strBuilder.append("jimpleVarValue = " + jimpleVarValue + "\n");
		// strBuilder.append("new patch value for jimple Variable "+ jimpleVarName +" =
		// "+ patchValue + "\n");
		return strBuilder.toString();
	}

	public String runBoomerang(Statement stmt, SootMethod sootMethod, Value value, ObservableICFG<Unit, SootMethod> observableDynamicICFG) {

		Boomerang solver = new Boomerang(new DefaultBoomerangOptions() {
			public boolean onTheFlyCallGraph() {
				return false;
			};
		}) {
			@Override
			public ObservableICFG<Unit, SootMethod> icfg() {
				return observableDynamicICFG;
			}

			@Override
			public SeedFactory<NoWeight> getSeedFactory() {
				return null;
			}
		};
		Map<ForwardQuery, AbstractBoomerangResults<NoWeight>.Context> map = Maps.newHashMap();

		BackwardQuery query = new BackwardQuery(stmt,new Val(value, sootMethod));
		BackwardBoomerangResults<NoWeight> backwardQueryResults = solver.solve(query);
		map.putAll(backwardQueryResults.getAllocationSites());

		
//		for (Unit pred : observableDynamicICFG.getPredsOf(stmt)) {
//			BackwardQuery query = new BackwardQuery(stmt,val);
////			BackwardQuery query = new BackwardQuery(new Statement((Stmt) pred, sootMethod), new Val(val, sootMethod));
//			BackwardBoomerangResults<NoWeight> backwardQueryResults = solver.solve(query);
//			map.putAll(backwardQueryResults.getAllocationSites());
//		}

		if (map.size() == 1) {
		
			
		} else if (map.size() > 1) {

		} else {

		}
		return "";
	}

}
