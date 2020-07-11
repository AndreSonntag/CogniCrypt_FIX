package de.upb.cognicryptfix.analysis;

import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.Lists;

import de.upb.cognicryptfix.Constants;
import de.upb.cognicryptfix.generator.jimple.JimpleTrapGenerator;
import de.upb.cognicryptfix.generator.jimple.JimpleUtils;
import de.upb.cognicryptfix.utils.Utils;
import soot.EntryPoints;
import soot.G;
import soot.PackManager;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootClass;
import soot.SootMethod;
import soot.Transform;
import soot.Unit;
import soot.UnitPatchingChain;
import soot.dava.Dava;
import soot.dava.DavaBody;
import soot.dava.DavaPrinter;
import soot.grimp.Grimp;
import soot.grimp.GrimpBody;
import soot.jimple.InvokeExpr;
import soot.jimple.JimpleBody;
import soot.options.Options;
import soot.util.Chain;

public class JimpleToDavaAnalysis {

	private static final Logger LOGGER = LogManager.getLogger(JimpleToDavaAnalysis.class);
	private static JimpleToDavaAnalysis instance;
	private CG DEFAULT_CALL_GRAPH = CG.CHA;

	private static enum CG {
		CHA, SPARK_LIBRARY, SPARK
	}

	private JimpleToDavaAnalysis() {
		
	}

	public static JimpleToDavaAnalysis getInstance() {
		if (JimpleToDavaAnalysis.instance == null) {
			JimpleToDavaAnalysis.instance = new JimpleToDavaAnalysis();
		}
		return JimpleToDavaAnalysis.instance;
	}

	private SceneTransformer createTransformer() {
		return new SceneTransformer() {
			@Override
			protected void internalTransform(String phaseName, Map<String, String> options) {
				LOGGER.info("Run Jimple to Java transformation!");
				Instant start = Instant.now();
				Chain<SootClass> appClasses = Scene.v().getApplicationClasses();
				for(SootClass appClass : appClasses) {
					LOGGER.debug("Class transformation: "+appClass.getName());
					if(appClass.getName().equals("com.mastercard.developer.interceptors.OkHttp2FieldLevelEncryptionInterceptor")) {
						System.out.println(1);
					}
					
					for(SootMethod appClassMethod : appClass.getMethods()) {
						LOGGER.debug("Method transformations: "+appClassMethod.getName());
						JimpleBody jimpleBody = (JimpleBody) appClassMethod.getActiveBody();
//						JimpleTrapGenerator trapGenerator = new JimpleTrapGenerator(jimpleBody);
//						Iterator it = jimpleBody.getUnits().snapshotIterator();		
//						while(it.hasNext()) {
//							Unit next = (Unit) it.next();
//							trapGenerator.generateTrap(next);
//						}
						
						GrimpBody grimpBody = Grimp.v().newBody(jimpleBody, "gb");
						DavaBody davaBody = Dava.v().newBody(grimpBody);
						appClassMethod.setActiveBody(davaBody);
					}
				}
				Instant finish = Instant.now();
				long timeElapsed = Duration.between(start, finish).toMillis();
				LOGGER.info("Transformation took "+timeElapsed+" milliseconds!");
			}
		};
	}
	
	public boolean runSoot(String inputPath, String outputPath, int outputFormat) {
		Utils.deleteIncludeListFiles(outputPath, getIncludeList());
		setSootOptions(inputPath, outputPath, outputFormat);
		
		Utils.deleteAllFiles(outputPath);
		registerJimpleToDavaTransformer();
		
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
		PackManager.v().getPack("wjop").apply();
//		PackManager.v().writeOutput();
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
		Options.v().set_full_resolver(true);
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
	
	private void addUnsolvedClasses() {
		for (String s : CryptoAnalysis.unsolvedClasses) {
			Scene.v().forceResolve(s, SootClass.SIGNATURES);
		}
	}
	
	private void registerJimpleToDavaTransformer() {
		PackManager.v().getPack("wjtp").add(new Transform("wjtp.getBody", createTransformer()));
	}
}
