package de.upb.cognicryptfix.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

import boomerang.Query;
import crypto.analysis.AnalysisSeedWithSpecification;
import crypto.analysis.ClassSpecification;
import crypto.extractparameter.CallSiteWithExtractedValue;
import crypto.extractparameter.CallSiteWithParamIndex;
import crypto.extractparameter.ExtractedValue;
import crypto.interfaces.ISLConstraint;
import crypto.rules.CrySLComparisonConstraint;
import crypto.rules.CrySLConstraint;
import crypto.rules.CrySLMethod;
import crypto.rules.CrySLPredicate;
import crypto.rules.CrySLRule;
import crypto.rules.CrySLValueConstraint;
import crypto.rules.TransitionEdge;
import crypto.rules.CrySLArithmeticConstraint.ArithOp;
import crypto.rules.CrySLComparisonConstraint.CompOp;
import de.upb.cognicryptfix.analysis.CryptoAnalysis;
import de.upb.cognicryptfix.crysl.CrySLEntity;
import de.upb.cognicryptfix.crysl.CrySLMethodCall;
import soot.IntType;
import soot.Scene;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.Constant;
import soot.jimple.IntConstant;
import soot.jimple.LongConstant;
import soot.jimple.Stmt;
import soot.jimple.StringConstant;

/**
 * @author Andre Sonntag
 * @created 23.04.2019
 */
public class Utils {

	private static final Logger logger = LogManager.getLogger(Utils.class);
	
	//TODO: move to SootJimpleUtils
	public static Type getType(CrySLRule rule, String var) {
		ArrayList<TransitionEdge> transitions = new ArrayList<TransitionEdge>(
				rule.getUsagePattern().getAllTransitions());
		for (TransitionEdge transition : transitions) {
			List<CrySLMethod> methods = transition.getLabel();
			for (CrySLMethod method : methods) {
				List<Entry<String, String>> parameters = method.getParameters();
				for (Entry<String, String> parameter : parameters) {
					if (parameter.getKey().equals(var)) {
						return Scene.v().getTypeUnsafe(parameter.getValue());
					}
				}

				Entry<String, String> ret = method.getRetObject();
				if (ret.getKey().equals(var)) {
					return Scene.v().getTypeUnsafe(ret.getValue());
				}
			}
		}
		return null;
	}
	
//	public static AnalysisSeedWithSpecification createSeed(CrySLRule rule, SootMethod method) {
//
//		AnalysisSeedWithSpecification ret = null;
//
//		if (method == null || !method.hasActiveBody() || !method.getDeclaringClass().isApplicationClass()) {
//			throw new RuntimeException("upppsss");
//		}
//
//		ClassSpecification spec = new ClassSpecification(rule, CryptoAnalysis.staticScanner);
//		spec.invokesForbiddenMethod(method);
//
//		if (spec.getRule().getClassName().equals("javax.crypto.SecretKey")) {
//			throw new RuntimeException("upppsss");
//		}
//		for (Query seed : spec.getInitialSeeds(method)) {
//			ret = CryptoAnalysis.staticScanner.getOrCreateSeedWithSpec(
//					new AnalysisSeedWithSpecification(CryptoAnalysis.staticScanner, seed.stmt(), seed.var(), spec));
//		}
//		return ret;
//	}

	public static MavenProject createAndCompile(String mavenProjectPath) {
		MavenProject mi = new MavenProject(mavenProjectPath);
		mi.compile();
		return mi;
	}

	public static Map<String, CallSiteWithExtractedValue> extractValueAsString(AnalysisSeedWithSpecification seed,
			String varName) {

		Multimap<CallSiteWithParamIndex, ExtractedValue> parsAndVals = seed.getParameterAnalysis().getCollectedValues();
		Map<String, CallSiteWithExtractedValue> varVal = Maps.newHashMap();
		for (CallSiteWithParamIndex wrappedCallSite : parsAndVals.keySet()) {
			final Stmt callSite = wrappedCallSite.stmt().getUnit().get();

			for (ExtractedValue wrappedAllocSite : parsAndVals.get(wrappedCallSite)) {
				final Stmt allocSite = wrappedAllocSite.stmt().getUnit().get();

				if (wrappedCallSite.getVarName().equals(varName)) {
					if (callSite.equals(allocSite)) {
						varVal.put(
								retrieveConstantFromValue(callSite.getInvokeExpr().getArg(wrappedCallSite.getIndex())),
								new CallSiteWithExtractedValue(wrappedCallSite, wrappedAllocSite));
					} else if (allocSite instanceof AssignStmt) {
						if (wrappedAllocSite.getValue() instanceof Constant) {
							varVal.put(retrieveConstantFromValue(wrappedAllocSite.getValue()),
									new CallSiteWithExtractedValue(wrappedCallSite, wrappedAllocSite));
						}
					}
				}
			}
		}

		return varVal;
	}

	private static String retrieveConstantFromValue(Value val) {
		if (val instanceof StringConstant) {
			return ((StringConstant) val).value;
		} else if (val instanceof IntConstant || val.getType() instanceof IntType) {
			return val.toString();
		} else if (val instanceof LongConstant) {
			return val.toString().replaceAll("L", "");
		} else {
			return "";
		}
	}

	/**
	 * This method checks if a Collection is null or empty
	 * 
	 * @param c
	 * @return
	 */
	public static boolean isNullOrEmpty(final Collection<?> c) {
		return c == null || c.isEmpty();
	}
	
	public static List<Unit> summarizeUnitLists(Collection<List<Unit>> unitLists){
		List<Unit> summaryList = Lists.newArrayList();
		for (List<Unit> l : unitLists) {
			summaryList.addAll(l);
		}
		return summaryList;
	}
	
	public static String getAppropriateVarName(CrySLRule rule) {
		
		String[] splitClassName = rule.getClassName().split("\\.");
		String className = splitClassName[splitClassName.length-1];
		String variableName = "gen";

		for (int i = 0; i < className.length(); i++) {
			char c = className.charAt(i);
			variableName += Character.isUpperCase(c) ? c : ""; 
		}
		return variableName;
	}
	
	public static void printCallPath(CrySLRule producer, List<CrySLMethodCall> path) {

		StringBuilder builder = new StringBuilder();
		builder.append("****" + producer.getClassName() +"\n");
		for (CrySLMethodCall call : path) {
			builder.append("- " + call.getSootMethod().getSignature() + "\n");
		}

		System.out.println(builder.toString());
	}
	
	public static String resolveComparsionOperator(CompOp op) {
		switch (op) {
		case l:
			return "<";
		case le:
			return "<=";
		case g:
			return ">";
		case ge:
			return ">=";
		case neq:
			return "!=";
		default:
			return "=";
		}
	}
	
	public static String resolveArithmeticOperator(ArithOp op) {
		switch (op) {
		case p:
			return "+";
		case n:
			return "-";
		case m:
			return ">";
		default:
			return "something went wrong";
		}
	}

}
