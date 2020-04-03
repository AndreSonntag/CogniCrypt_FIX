package de.upb.cognicryptfix.analysis;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.io.Files;

import boomerang.callgraph.ObservableDynamicICFG;
import boomerang.callgraph.ObservableICFG;
import boomerang.preanalysis.BoomerangPretransformer;
import crypto.analysis.CrySLAnalysisListener;
import crypto.analysis.CryptoScanner;
import crypto.rules.CrySLRule;
import de.upb.cognicryptfix.Constants;
import de.upb.cognicryptfix.utils.MavenProject;
import soot.EntryPoints;
import soot.G;
import soot.PackManager;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootClass;
import soot.SootMethod;
import soot.Transform;
import soot.Unit;
import soot.options.Options;
import soot.util.Chain;

/**
 * @author Andre Sonntag
 * @date 21.09.2019
 */
public class CryptoAnalysis {
	
	private static final Logger logger = LogManager.getLogger(CryptoAnalysis.class.getSimpleName());
	private CG DEFAULT_CALL_GRAPH = CG.CHA;
	private List<CrySLRule> rules;
	public static CryptoScanner staticScanner = null;
	public static int run;
	
	
	public static enum CG {
		CHA, SPARK_LIBRARY, SPARK
	}
	
	public CryptoAnalysis(List<CrySLRule> rules) {
		this.rules = rules;
		this.run = 0;
	}

	private SceneTransformer createAnalysisTransformer(final CrySLAnalysisListener listener) {
		return new SceneTransformer() {

			@Override
			protected void internalTransform(final String phaseName, final Map<String, String> options) {
				BoomerangPretransformer.v().reset();
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
	
	public boolean runSoot(final String jimpleOutputPath, final CrySLAnalysisListener listener) {
		CryptoAnalysis.run++;
		logger.info("CRYPTOANALYSIS RUN: "+run+" ##################################################################");
		deleteIncludeListJimpleFiles();
		G.reset();
		setSootOptions(jimpleOutputPath);
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
	
	
	public boolean runSoot(final MavenProject project, final CrySLAnalysisListener listener) {
		logger.info("CRYPTOANALYSIS RUN: "+run+" ##################################################################");
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
	    PackManager.v().writeOutput();
	}
	
	private void deleteIncludeListJimpleFiles() {
		List<String> includeList = getIncludeList();
		File[] files = new File(Constants.jimpleOutputPath).listFiles();
		for(File f : files) {
			String name = Files.getNameWithoutExtension(f.getName());
			if(includeList.contains(name)) {
				f.delete();
			}
		}
	}
	
	private void setSootOptions(String jimpleOutputPath) {
		G.v().reset();
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
		Options.v().set_src_prec(Options.src_prec_jimple);
		Options.v().set_output_format(Options.output_format_jimple);
		Options.v().set_force_overwrite(true);
		Options.v().set_output_dir(pathToJimpleOutput());
		Options.v().set_no_bodies_for_excluded(true);
		Options.v().set_allow_phantom_refs(true);
		Options.v().set_whole_program(true);
		Options.v().set_keep_line_number(true);
		Options.v().set_prepend_classpath(true);
		Options.v().set_soot_classpath(jimpleOutputPath+ File.pathSeparator + pathToJCE());
		Options.v().set_process_dir(Lists.newArrayList(jimpleOutputPath));
		Options.v().set_include(getIncludeList());
		Options.v().set_exclude(getExcludeList());
		Options.v().set_full_resolver(true);
		addUnsolvedClasses();
		Scene.v().loadNecessaryClasses();
		Scene.v().setEntryPoints(getEntryPoints());
		logger.info("initializing soot for Jimple files");
	}
	
	private void setSootOptions(final MavenProject project) {
		G.v().reset();
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
		Options.v().set_output_format(Options.output_format_jimple);
		Options.v().set_force_overwrite(true);
		Options.v().set_output_dir(pathToJimpleOutput());
		Options.v().set_no_bodies_for_excluded(true);
		Options.v().set_allow_phantom_refs(true);
		Options.v().set_whole_program(true);
		Options.v().set_keep_line_number(true);
		Options.v().set_prepend_classpath(true);
		Options.v().set_soot_classpath(getSootClasspath(project)+ File.pathSeparator + pathToJCE());
		Options.v().set_process_dir(Lists.newArrayList(project.getBuildDirectory()));
		Options.v().set_include(getIncludeList());
		Options.v().set_exclude(getExcludeList());
		Options.v().set_full_resolver(true);
		Options.v().set_validate(true);
		addUnsolvedClasses();
		Scene.v().loadNecessaryClasses();
		Scene.v().setEntryPoints(getEntryPoints());
		logger.info("initializing soot for Maven project");
	}

	private List<SootMethod> getEntryPoints() {
		List<SootMethod> entryPoints = Lists.newArrayList();
		entryPoints.addAll(EntryPoints.v().application());
		entryPoints.addAll(EntryPoints.v().methodsOfApplicationClasses());
		return entryPoints;
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
		int i = 0;
		for (final CrySLRule r : rules) {
			excludeList.add(r.getClassName());
		}
		return excludeList;
	}
	
	private void addUnsolvedClasses() {
		List<String> unsolvedClasses = findUnsolvedClasses(false);
		for(String s : unsolvedClasses) {
			Scene.v().forceResolve(s, SootClass.SIGNATURES);
		}
	}
	
	private List<String> findUnsolvedClasses(boolean print) {
		Chain<SootClass> resolvedClasses = Scene.v().getClasses();
		Set<String> resolvedClassNames = Sets.newHashSet();
		for(SootClass clazz : resolvedClasses) {
			resolvedClassNames.add(clazz.getName());
			if(clazz.getPackageName().contains("de.upb")) {
				System.out.println(clazz.getName());
			}
		}
		
		List<String> unsolved = Lists.newArrayList();
		List<String> solved = Lists.newArrayList();
		for (final CrySLRule r : rules) {
			if(!resolvedClassNames.contains(r.getClassName())) {
				unsolved.add(r.getClassName());
			} else {
				solved.add(r.getClassName());
			}
		}
		
		if(print) {
			int i = 0;
			for (String us : unsolved) {
				System.out.println(i + ". " + us + " couldn't solved.");
				i++;
			}
			System.out.println("---------------------------------------");
			int j = 0;
			for (String s : solved) {
				System.out.println(j + ". " + s + " solved.");
				j++;
			}
		}
		return unsolved;
	}

	private void registerTransformers(final CrySLAnalysisListener listener) {
		PackManager.v().getPack("wjtp").add(new Transform("wjtp.ifds", createAnalysisTransformer(listener)));
	}
	
	private String getSootClasspath(final MavenProject project) {
		String sootClasspath = project.getBuildDirectory()+(project.getFullClassPath().equals("") ? "": File.pathSeparator+ project.getFullClassPath());
		System.out.println(sootClasspath);
		return sootClasspath;
	}
	
	private String pathToJCE() {
		// When whole program mode is disabled, the classpath misses jce.jar
		return System.getProperty("java.home") + File.separator + "lib" + File.separator + "jce.jar";
	}
	
	private String pathToJimpleOutput() {
		File outputFolderFile = new File(Constants.jimpleOutputPath);
		return outputFolderFile.getAbsolutePath();
	}
	
	public static CryptoScanner getCryptoScanner() {
		return staticScanner;
	}
}
