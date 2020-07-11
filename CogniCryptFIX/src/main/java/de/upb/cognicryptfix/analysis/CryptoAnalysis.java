package de.upb.cognicryptfix.analysis;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import boomerang.callgraph.ObservableDynamicICFG;
import boomerang.callgraph.ObservableICFG;
import boomerang.preanalysis.BoomerangPretransformer;
import crypto.analysis.CrySLAnalysisListener;
import crypto.analysis.CryptoScanner;
import crypto.rules.CrySLRule;
import de.upb.cognicryptfix.Constants;
import de.upb.cognicryptfix.crysl.CrySLReaderUtils;
import de.upb.cognicryptfix.generator.jimple.JimpleLocalGenerator;
import de.upb.cognicryptfix.utils.MavenProject;
import de.upb.cognicryptfix.utils.Utils;
import soot.Body;
import soot.EntryPoints;
import soot.G;
import soot.Local;
import soot.PackManager;
import soot.RefType;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootClass;
import soot.SootMethod;
import soot.Transform;
import soot.Unit;
import soot.javaToJimple.LocalGenerator;
import soot.options.Options;
import soot.util.Chain;

/**
 * @author Andre Sonntag
 * @date 21.09.2019
 */
public class CryptoAnalysis {

	private static final Logger LOGGER = LogManager.getLogger(CryptoAnalysis.class);
	private static CryptoAnalysis instance;

	private CG DEFAULT_CALL_GRAPH = CG.CHA;
	public static List<CrySLRule> rules = Lists.newArrayList();
	public static List<String> unsolvedClasses = Lists.newArrayList();
	public static CryptoScanner staticScanner = null;
	public static int round = 1;

	private static enum CG {
		CHA, SPARK_LIBRARY, SPARK
	}

	private CryptoAnalysis() {
		rules = getCrySLRules();
	}

	public static CryptoAnalysis getInstance() {
		if (CryptoAnalysis.instance == null) {
			CryptoAnalysis.instance = new CryptoAnalysis();
		}
		return CryptoAnalysis.instance;
	}

	private SceneTransformer createCryptoAnalysisTransformer(final CrySLAnalysisListener listener) {
		return new SceneTransformer() {

			@Override
			protected void internalTransform(final String phaseName, final Map<String, String> options) {
				
				for(SootClass sc : Scene.v().getApplicationClasses()) {
					
					if(sc.getPackageName().contains("java.lang")) {
						continue;
					}
					
					for(SootMethod sm : sc.getMethods()) {
						Body b = null;
						try {
							b = sm.retrieveActiveBody();
						} catch(java.lang.RuntimeException r){
							continue;
						}
						
						JimpleLocalGenerator lg = new JimpleLocalGenerator(b);
						Chain<Local> locals = sm.getActiveBody().getLocals();
						for(Local lo : locals) {
							if(lo.getName().contains("#")) {
								lo.setName(lg.findName(lo.getName().replace("#", "")));
							}
						}
					}
				}
				
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
				LOGGER.info("Run CogniCrypt_SAST Analysis round: " + round++);
				scanner.scan(rules);
			}
		};
	}

	public boolean runSoot(String inputPath, String outputPath, int outputFormat, CrySLAnalysisListener listener) {
		Utils.deleteIncludeListFiles(outputPath, getIncludeList());
		G.reset();
		setSootOptions(inputPath, outputPath, outputFormat);
		registerTransformer(listener);

		try {
			runSoot();
		} catch (final Exception t) {
			t.printStackTrace();
			LOGGER.error(t);
			Utils.deleteIncludeListFiles(outputPath, getIncludeList());
			return false;
		}
		Utils.deleteIncludeListFiles(outputPath, getIncludeList());
		return true;
	}

	public boolean runSoot(MavenProject project, String outputPath, int outputFormat, CrySLAnalysisListener listener) {
		Utils.deleteAllFiles(outputPath);
		setSootOptions(project, outputPath, outputFormat);
		registerTransformer(listener);

		try {
			runSoot();
		} catch (final Exception t) {
			t.printStackTrace();
			LOGGER.error(t);
			Utils.deleteIncludeListFiles(outputPath, getIncludeList());
			return false;
		}
		Utils.deleteIncludeListFiles(outputPath, getIncludeList());
		return true;
	}

	private void runSoot() {
		Scene.v().loadNecessaryClasses();
		PackManager.v().getPack("cg").apply();
		PackManager.v().getPack("wjtp").apply();
		PackManager.v().writeOutput();
	}



	private void setSootOptions(String inputPath, String outputPath, int outputFormat) {
		G.reset();
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

		// avoid the deletion of assigments
//		Options.v().setPhaseOption("jb.dae", "enabled:false");	
//		Options.v().setPhaseOption("jb.cp-ule", "enabled:false");
		
		Options.v().set_src_prec(Options.src_prec_jimple);
		Options.v().set_output_format(outputFormat);
		Options.v().set_force_overwrite(true);
		Options.v().set_output_dir(outputPath);
		Options.v().set_no_bodies_for_excluded(true);
		Options.v().set_allow_phantom_refs(true);
		Options.v().set_whole_program(true);
		Options.v().set_keep_line_number(true);
		Options.v().set_prepend_classpath(true);
		Options.v().set_soot_classpath(inputPath + File.pathSeparator + Constants.JCE_PATH);
		Options.v().set_process_dir(Lists.newArrayList(inputPath));
		Options.v().set_include(getIncludeList());
		Options.v().set_exclude(getExcludeList());
		Options.v().set_full_resolver(true);
		addUnsolvedClasses();
		Scene.v().loadNecessaryClasses();
		Scene.v().setEntryPoints(getEntryPoints());
	}
	
	private void setSootOptions(MavenProject project, String outputPath, int outputFormat) {
		G.reset();
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
		
		// avoid the deletion of assigments
//		Options.v().setPhaseOption("jb.dae", "enabled:false");	
//		Options.v().setPhaseOption("jb.cp-ule", "enabled:false");

		Options.v().set_src_prec(Options.src_prec_class);
		Options.v().set_output_format(outputFormat);
		Options.v().set_force_overwrite(true);
		Options.v().set_output_dir(outputPath);
		Options.v().set_no_bodies_for_excluded(true);
		Options.v().set_allow_phantom_refs(true);
		Options.v().set_whole_program(true);
		Options.v().set_keep_line_number(true);
		Options.v().set_prepend_classpath(true);
		Options.v().set_soot_classpath(getSootClasspath(project) + File.pathSeparator + Constants.JCE_PATH);
		Options.v().set_process_dir(Lists.newArrayList(project.getBuildDirectory()));
		Options.v().set_include(getIncludeList());
		Options.v().set_exclude(getExcludeList());
		Options.v().set_full_resolver(true);
		Options.v().set_validate(true);
		addUnsolvedClasses();
		Scene.v().loadNecessaryClasses();
		Scene.v().setEntryPoints(getEntryPoints());
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
		for (final CrySLRule r : rules) {
			excludeList.add(r.getClassName());
		}
		return excludeList;
	}

	private void addUnsolvedClasses() {
		unsolvedClasses = findUnsolvedClasses(false);
		unsolvedClasses.add("java.io.Serializable");
		unsolvedClasses.add("java.io.FileReader");
		unsolvedClasses.add("java.io.BufferedReader");
		unsolvedClasses.add("javax.net.ssl.KeyManager");
		unsolvedClasses.add("javax.net.ssl.KeyManager");
		unsolvedClasses.add("javax.net.ssl.TrustManager");

		for (String s : unsolvedClasses) {
			Scene.v().forceResolve(s, SootClass.HIERARCHY);
		}
	}
	
	private List<String> findUnsolvedClasses(boolean print) {
		Chain<SootClass> resolvedClasses = Scene.v().getClasses();
		Set<String> resolvedClassNames = Sets.newHashSet();
		for (SootClass clazz : resolvedClasses) {
			resolvedClassNames.add(clazz.getName());
		}

		List<String> unsolved = Lists.newArrayList();
		List<String> solved = Lists.newArrayList();
		for (final CrySLRule r : rules) {
			if (!resolvedClassNames.contains(r.getClassName())) {
				unsolved.add(r.getClassName());
			} else {
				solved.add(r.getClassName());
			}

//			//TODO: refactor
//			for(Entry<String, String> e :  r.getObjects()) {
//				if (!resolvedClasses.contains(e.getKey()) && !solved.contains(e.getKey())) {
//					unsolved.add(e.getKey());
//				} else {
//					solved.add(e.getKey());
//				}
//			}
		}

		if (print) {
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

	private void registerTransformer(final CrySLAnalysisListener listener) {
		PackManager.v().getPack("wjtp").add(new Transform("wjtp.ifds", createCryptoAnalysisTransformer(listener)));
	}
	
	private String getSootClasspath(final MavenProject project) {
		String sootClasspath = project.getBuildDirectory()
				+ (project.getFullClassPath().equals("") ? "" : File.pathSeparator + project.getFullClassPath());
		return sootClasspath;
	}

	public static CryptoScanner getCryptoScanner() {
		return staticScanner;
	}

	public static List<CrySLRule> getCrySLRules() {
		if (rules.isEmpty()) {
			rules = CrySLReaderUtils.readRulesFromSourceFiles(Constants.CRYSL_RULE_PATH);
		}
		return rules;
	}
}
