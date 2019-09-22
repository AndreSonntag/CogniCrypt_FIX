package de.upb.cognicryptfix.analysis;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.Lists;

import boomerang.callgraph.ObservableDynamicICFG;
import boomerang.callgraph.ObservableICFG;
import boomerang.preanalysis.BoomerangPretransformer;
import crypto.analysis.CrySLAnalysisListener;
import crypto.analysis.CryptoScanner;
import crypto.rules.CryptSLRule;
import de.upb.cognicryptfix.Constants;
import de.upb.cognicryptfix.utils.CrySLReaderUtils;
import de.upb.cognicryptfix.utils.MavenProject;
import soot.G;
import soot.PackManager;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootMethod;
import soot.Transform;
import soot.Unit;
import soot.options.Options;

/**
 * @author Andre Sonntag
 * @date 21.09.2019
 */
public class CryptoAnalysis {
	
	private static final Logger logger = LogManager.getLogger(CryptoAnalysis.class.getSimpleName());
	private static CG DEFAULT_CALL_GRAPH = CG.CHA;

	public static enum CG {
		CHA, SPARK_LIBRARY, SPARK
	}

	private static SceneTransformer createAnalysisTransformer(final CrySLAnalysisListener listener) {
		return new SceneTransformer() {

			@Override
			protected void internalTransform(final String phaseName, final Map<String, String> options) {
				BoomerangPretransformer.v().apply();
				final ObservableDynamicICFG icfg = new ObservableDynamicICFG(true);
				CryptoScanner scanner = new CryptoScanner() {

					@Override
					public ObservableICFG<Unit, SootMethod> icfg() {
						return icfg;
					}


				};
				scanner.getAnalysisListener().addReportListener(listener);
				scanner.scan(CrySLReaderUtils.readRulesFromBinaryFiles(Constants.serializedCrySLRulePath));
			}
		};
	}
	
	public static boolean runSoot(final MavenProject project, final CrySLAnalysisListener listener) {
		G.reset();
		setSootOptions(project);
		registerTransformers(listener);
		try {
			runSoot();
		}
		catch (final Exception t) {
			logger.error(t);
			return false;
		}
		return true;
	}
	
	private static void runSoot() {
		Scene.v().loadNecessaryClasses();
		PackManager.v().getPack("cg").apply();
		PackManager.v().getPack("wjtp").apply();
	}

	private static void setSootOptions(final MavenProject project) {
		Options.v().set_soot_classpath(getSootClasspath(project));
		Options.v().set_process_dir(Lists.newArrayList(project.getBuildDirectory()));
		Options.v().set_keep_line_number(true);
		Options.v().set_prepend_classpath(true);
		Options.v().set_allow_phantom_refs(true);
		Options.v().set_whole_program(true);
		Options.v().set_no_bodies_for_excluded(true);
		Options.v().set_include(getIncludeList());
		Options.v().set_exclude(getExcludeList());
		Scene.v().loadNecessaryClasses();

		switch (DEFAULT_CALL_GRAPH.ordinal()) {
		case 2:
			Options.v().setPhaseOption("cg.spark", "on");
			Options.v().setPhaseOption("cg", "all-reachable:true,library:any-subtype");
			break;
		case 0:
		default:
			Options.v().setPhaseOption("cg.cha", "on");
			Options.v().setPhaseOption("cg", "all-reachable:true");
		}

		Options.v().setPhaseOption("jb", "use-original-names:true");
		Options.v().set_output_format(Options.output_format_none);
		logger.info("initializing soot");

	}

	private static List<String> getIncludeList() {
		final List<String> includeList = new LinkedList<String>();
		includeList.add("java.lang.AbstractStringBuilder");
		includeList.add("java.lang.Boolean");
		includeList.add("java.lang.Byte");
		includeList.add("java.lang.Class");
		includeList.add("java.lang.Integer");
		includeList.add("java.lang.Long");
		includeList.add("java.lang.Object");
		includeList.add("java.lang.String");
		includeList.add("java.lang.StringCoding");
		includeList.add("java.lang.StringIndexOutOfBoundsException");
		return includeList;
	}

	private static List<String> getExcludeList() {
		final List<String> excludeList = new LinkedList<String>();
		for (final CryptSLRule r : CrySLReaderUtils.readRulesFromBinaryFiles(Constants.serializedCrySLRulePath)) {
			excludeList.add(crypto.Utils.getFullyQualifiedName(r));
		}
		return excludeList;
	}

	private static void registerTransformers(final CrySLAnalysisListener listener) {
		PackManager.v().getPack("wjtp").add(new Transform("wjtp.ifds", createAnalysisTransformer(listener)));
	}
	
	private static String getSootClasspath(final MavenProject project) {
		String sootClasspath = project.getBuildDirectory()+(project.getFullClassPath().equals("") ? "": File.pathSeparator+ project.getFullClassPath());
		return sootClasspath;
	}
}
