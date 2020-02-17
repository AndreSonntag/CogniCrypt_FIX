package de.upb.cognicryptfix.utils;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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
import crypto.rules.CrySLPredicate;
import crypto.rules.CrySLRule;
import crypto.rules.CrySLValueConstraint;
import de.upb.cognicryptfix.analysis.CryptoAnalysis;
import soot.IntType;
import soot.SootClass;
import soot.SootMethod;
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

	public static AnalysisSeedWithSpecification createSeed(CrySLRule rule ,SootMethod method) {

		AnalysisSeedWithSpecification ret = null;
		
		if (method == null || !method.hasActiveBody() || !method.getDeclaringClass().isApplicationClass()) {
    		throw new RuntimeException("upppsss");
		}
		
		ClassSpecification spec = new ClassSpecification(rule, CryptoAnalysis.staticScanner);
		spec.invokesForbiddenMethod(method);
		
		if (spec.getRule().getClassName().equals("javax.crypto.SecretKey")) {
    		throw new RuntimeException("upppsss");
		}
		for (Query seed : spec.getInitialSeeds(method)) {
			ret = CryptoAnalysis.staticScanner.getOrCreateSeedWithSpec(new AnalysisSeedWithSpecification(CryptoAnalysis.staticScanner, seed.stmt(), seed.var(), spec));
		}
		return ret;
	}

	public static MavenProject createAndCompile(String mavenProjectPath) {
		MavenProject mi = new MavenProject(mavenProjectPath);
		mi.compile();
		return mi;
	}

	public static String constraintToString(ISLConstraint constraint) {
		StringBuilder builder = new StringBuilder();

		if (constraint instanceof CrySLConstraint) {
			CrySLConstraint con = (CrySLConstraint) constraint;
			builder.append("\n" + con.getClass().getSimpleName() + "[\n");
			builder.append("left = " + con.getLeft() + "\n");
			builder.append("right = " + con.getRight() + "\n");
			builder.append("op = " + con.getOperator() + "\n");

		} else if (constraint instanceof CrySLValueConstraint) {
			CrySLValueConstraint con = (CrySLValueConstraint) constraint;
			builder.append("\n" + con.getClass().getSimpleName() + "[\n");
			builder.append(con.toString() + "]\n");

		} else if (constraint instanceof CrySLPredicate) {
			CrySLPredicate con = (CrySLPredicate) constraint;
			builder.append("\n" + con.getClass().getSimpleName() + "[\n");
			builder.append(con.toString() + "]\n");

		} else if (constraint instanceof CrySLComparisonConstraint) {
			CrySLComparisonConstraint con = (CrySLComparisonConstraint) constraint;
			builder.append("\n" + con.getClass().getSimpleName() + "[\n");
			builder.append(con.toString() + "]\n");
		}

		return builder.toString();

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
	
	/**
	 * 
	 * @param clazz
	 * @return
	 */
	public static SootMethod getBestInitializationMethod(SootClass clazz) {
		List<SootMethod> initMethods = Lists.newArrayList();
		for (SootMethod method : clazz.getMethods()) {
			if (method.isConstructor() || method.getName().contains("getInstance")) {
				initMethods.add(method);
			}
		}
		
		Collections.sort(initMethods, new InitializationMethodSorter());
		return initMethods.get(0);		//TODO modify back!!!!
	}

}
