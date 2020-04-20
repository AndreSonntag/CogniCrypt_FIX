package de.upb.cognicryptfix.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

import crypto.analysis.AnalysisSeedWithSpecification;
import crypto.analysis.errors.AbstractError;
import crypto.analysis.errors.ConstraintError;
import crypto.analysis.errors.ForbiddenMethodError;
import crypto.analysis.errors.IncompleteOperationError;
import crypto.analysis.errors.NeverTypeOfError;
import crypto.analysis.errors.RequiredPredicateError;
import crypto.analysis.errors.TypestateError;
import crypto.extractparameter.CallSiteWithExtractedValue;
import crypto.extractparameter.CallSiteWithParamIndex;
import crypto.extractparameter.ExtractedValue;
import crypto.rules.CrySLArithmeticConstraint.ArithOp;
import crypto.rules.CrySLComparisonConstraint.CompOp;
import crypto.rules.CrySLMethod;
import crypto.rules.CrySLRule;
import crypto.rules.TransitionEdge;
import de.upb.cognicryptfix.crysl.CrySLMethodCall;
import de.upb.cognicryptfix.generator.jimple.JimpleUtils;
import soot.IntType;
import soot.Local;
import soot.Scene;
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

	private static final Logger LOGGER = LogManager.getLogger(Utils.class);

	// TODO: move to SootJimpleUtils
	public static Type getType(CrySLRule rule, String var) {
		ArrayList<TransitionEdge> transitions = new ArrayList<TransitionEdge>(
				rule.getUsagePattern().getAllTransitions());
		for (TransitionEdge transition : transitions) {
			List<CrySLMethod> methods = transition.getLabel();
			for (CrySLMethod method : methods) {
				List<Entry<String, String>> parameters = method.getParameters();
				for (Entry<String, String> parameter : parameters) {
					if (parameter.getKey().equals(var)) {
						Type retType = null;
						try {
							retType = Scene.v().getType(parameter.getValue());
						} catch (java.lang.RuntimeException e) {
							retType = Scene.v().getTypeUnsafe(parameter.getValue());
						}
						return retType;
					}
				}

				Entry<String, String> ret = method.getRetObject();
				if (ret.getKey().equals(var)) {
					Type retType = null;
					try {
						retType = Scene.v().getType(ret.getValue());
					} catch (java.lang.RuntimeException e) {
						retType = Scene.v().getTypeUnsafe(ret.getValue());
					}

					return retType;
				}
			}
		}
		return null;
	}

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

	public static List<Unit> summarizeUnitLists(Collection<List<Unit>> unitLists) {
		List<Unit> summaryList = Lists.newArrayList();
		for (List<Unit> l : unitLists) {
			summaryList.addAll(l);
		}
		return summaryList;
	}

	public static String getAppropriateVarName(CrySLRule rule) {

		String[] splitClassName = rule.getClassName().split("\\.");
		String className = splitClassName[splitClassName.length - 1];
		String variableName = "gen";

		for (int i = 0; i < className.length(); i++) {
			char c = className.charAt(i);
			variableName += Character.isUpperCase(c) ? c : "";
		}
		return variableName;
	}

	public static void printCallPath(CrySLRule producer, List<CrySLMethodCall> path) {

		StringBuilder builder = new StringBuilder();
		builder.append("****" + producer.getClassName() + "\n");
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

	public static String printErrorInformation(AbstractError error) {
		String errorType = "";
		String errorRule = error.getRule().getClassName();
		String errorClass = error.getErrorLocation().getMethod().getDeclaringClass().toString();
		String errorOuterMethod = error.getErrorLocation().getMethod().getSignature();
		String errorMessage = error.toErrorMarkerString();

		if (error instanceof ConstraintError) {
			errorType = "ConstraintError";
		} else if (error instanceof ForbiddenMethodError) {
			errorType = "ForbiddenMethodError";
		} else if (error instanceof NeverTypeOfError) {
			errorType = "NeverTypeOfError";
		} else if (error instanceof TypestateError) {
			errorType = "TypestateError";
		} else if (error instanceof IncompleteOperationError) {
			errorType = "IncompleteOperationError";
		} else if (error instanceof RequiredPredicateError) {
			errorType = "RequiredPredicateError";
		}

		StringBuilder builder = new StringBuilder();
		builder.append(
				"\n_________________________________________CogniCrypt_SAST_________________________________________\n");
		builder.append("Detected: \t" + errorType + "\n");
		builder.append("CrySLRule: \t" + errorRule + "\n");
		builder.append("Class: \t\t" + errorClass + "\n");
		builder.append("Method: \t" + errorOuterMethod + "\n");
		builder.append("Message: \t" + errorMessage + "\n");
		builder.append(
				"_________________________________________________________________________________________________\n");
		return builder.toString();

	}

	public static void deleteIncludeListFiles(String path, List<String> filesToDelete) {
		File[] files = new File(path).listFiles();
		for (File f : files) {
			String name = com.google.common.io.Files.getNameWithoutExtension(f.getName());
			if (filesToDelete.contains(name)) {
				f.delete();
			}
		}
	}

	public static void deleteAllFiles(String path) {
		File[] files = new File(path).listFiles();
		for (File f : files) {	
			if(f.isDirectory()) {
				try {
					FileUtils.deleteDirectory(f);
				} catch (IOException e) {
					e.printStackTrace();
				}
			} else {
				f.delete();
			}
		}
	}
	
	public static void copyDavaFilesInProject(String davaOutputPath, String projectPath) {
		LOGGER.info("Copy Dava Files in Project: "+projectPath);
		davaOutputPath += "\\dava\\src";
		projectPath += "\\src";
		File sourceFolder = new File(davaOutputPath);
		File targetFolder = new File(projectPath);
		try {
			FileUtils.copyDirectory(sourceFolder, targetFolder);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static String findFileName(String name) {
		StringBuilder sb = new StringBuilder(name);
		
		Path filePath = Paths.get(name+".txt");
		if (java.nio.file.Files.exists(filePath)) {
			if (Character.isDigit(name.charAt(name.length() - 1))) {
				int firstNumber = Integer.parseInt(name.charAt(name.length() - 1) + "");
				if (firstNumber == 9) {
					if (Character.isDigit(name.charAt(name.length() - 2))) {
						int secondNumber = Integer.parseInt(name.charAt(name.length() - 2) + "");
						sb.replace(name.length() - 2, name.length() - 2, secondNumber + 1 + "");
						sb.replace(name.length() - 1, name.length() - 1, 0 + "");
						name = sb.toString();
					} else {
						sb.replace(name.length() - 1, name.length() - 1, "10");
						name = sb.toString();
					}
				} else {
					sb.replace(name.length()-1, name.length(), firstNumber + 1 + "");
					name = sb.toString();
				}
			} else {
				name = findFileName(name+"_"+1);
			}
		}
		filePath = Paths.get(name+".txt");
		if (java.nio.file.Files.exists(filePath)) {
			name = findFileName(name);
		} 
		
		return name;
	}


}
