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
import crypto.rules.CrySLRule;
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
	private CG DEFAULT_CALL_GRAPH = CG.CHA;
	private List<CrySLRule> rules;
	public static CryptoScanner staticScanner = null;
	
	public static enum CG {
		CHA, SPARK_LIBRARY, SPARK
	}
	
	public CryptoAnalysis(List<CrySLRule> rules) {
		this.rules = rules;
	}

	private SceneTransformer createAnalysisTransformer(final CrySLAnalysisListener listener) {
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
				staticScanner = scanner;
				scanner.getAnalysisListener().addReportListener(listener);
				scanner.scan(rules);
			}
		};
	}
	
	
	public boolean runSoot(final MavenProject project, final CrySLAnalysisListener listener) {
		G.reset();
		setSootOptions(project);
		registerTransformers(listener);
		try {
			runSoot();
		}
		catch (final Exception t) {
			t.printStackTrace();
			logger.error(t);
			return false;
		}
		return true;
	}
	
	private void runSoot() {
		Scene.v().loadNecessaryClasses();
		PackManager.v().getPack("cg").apply();
		PackManager.v().getPack("wjtp").apply();
	}

	private void setSootOptions(final MavenProject project) {
//		Options.v().set_src_prec(3);	//input JimpleFiles! 
		Options.v().set_soot_classpath(getSootClasspath(project)+ File.pathSeparator + pathToJCE());

		Options.v().set_process_dir(Lists.newArrayList(project.getBuildDirectory()));
		
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
		Options.v().set_keep_line_number(true);
		Options.v().set_prepend_classpath(true);
		Options.v().set_output_format(Options.output_format_none);
		Options.v().set_allow_phantom_refs(true);
		Options.v().set_whole_program(true);
		Options.v().set_no_bodies_for_excluded(true);
		Options.v().set_include(getIncludeList());
		Options.v().set_exclude(getExcludeList());
		Options.v().set_full_resolver(true);
		loadAndSupportNotResolvedClasses();
		Scene.v().loadNecessaryClasses();
		logger.info("initializing soot");
	}
	
	private void loadAndSupportNotResolvedClasses() {
		for(CrySLRule rule : rules) {
			if(!Scene.v().containsClass(rule.getClassName())) {
				Scene.v().loadClassAndSupport(rule.getClassName());
			}
		}
	}
	
	private List<String> getIncludeList() {
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
	
	private List<String> getExcludeList() {
		final List<String> excludeList = new LinkedList<String>();
		for (final CrySLRule r : rules) {
			excludeList.add(crypto.Utils.getFullyQualifiedName(r));
		}
		return excludeList;
	}

	private void registerTransformers(final CrySLAnalysisListener listener) {
		PackManager.v().getPack("wjtp").add(new Transform("wjtp.ifds", createAnalysisTransformer(listener)));
	}
	
	private String getSootClasspath(final MavenProject project) {
		String sootClasspath = project.getBuildDirectory()+(project.getFullClassPath().equals("") ? "": File.pathSeparator+ project.getFullClassPath());
		return sootClasspath;
	}
	
	private String pathToJCE() {
		// When whole program mode is disabled, the classpath misses jce.jar
		return System.getProperty("java.home") + File.separator + "lib" + File.separator + "jce.jar";
	}
}
