package de.upb.cognicryptfix.analysis;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.Lists;
import com.google.common.io.Files;

import de.upb.cognicryptfix.Constants;
import de.upb.cognicryptfix.decompiler.JavaDecompiler;
import de.upb.cognicryptfix.utils.Utils;
import soot.Body;
import soot.BodyTransformer;
import soot.EntryPoints;
import soot.G;
import soot.PackManager;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootClass;
import soot.SootMethod;
import soot.SourceLocator;
import soot.Transform;
import soot.baf.BafASMBackend;
import soot.baf.BafBody;
import soot.baf.toolkits.base.StoreChainOptimizer;
import soot.jimple.JimpleToBafContext;
import soot.options.Options;
import soot.toolkits.scalar.UnusedLocalEliminator;
import soot.util.Chain;

public class JimpleToClassAnalysis {

	private static final Logger LOGGER = LogManager.getLogger(JimpleToClassAnalysis.class);
	private static JimpleToClassAnalysis instance;
	private CG DEFAULT_CALL_GRAPH = CG.CHA;
	private JavaDecompiler decompiler;

	private static enum CG {
		CHA, SPARK_LIBRARY, SPARK
	}

	private JimpleToClassAnalysis() {
		this.decompiler = JavaDecompiler.getInstance();
	}

	public static JimpleToClassAnalysis getInstance() {
		if (JimpleToClassAnalysis.instance == null) {
			JimpleToClassAnalysis.instance = new JimpleToClassAnalysis();
		}
		return JimpleToClassAnalysis.instance;
	}

	private SceneTransformer createSceneTransformer() {
		return new SceneTransformer() {
			@Override
			protected void internalTransform(String phaseName, Map<String, String> options) {				
//			
//				Chain<SootClass> appClasses = Scene.v().getApplicationClasses();
//				for (SootClass appClass : appClasses) {
//					try {
//						LOGGER.debug("Transformed class: " + appClass.getName());
//						int java_version = Options.v().java_version();
//						String fileName = SourceLocator.v().getFileNameFor(appClass, Options.output_format_jimple);
//						fileName = Files.getNameWithoutExtension(fileName)+".class";						
//						OutputStream streamOut = new FileOutputStream(fileName);
//						BafASMBackend backend = new BafASMBackend(appClass, java_version);
//						backend.generateClassFile(streamOut);
//						streamOut.close();
//						decompiler.decompile(fileName);
//					} catch (IOException e) {
//						e.printStackTrace();
//					}
//				}
			}
		};
	}

	private BodyTransformer createBodyTransformer() {
		return new BodyTransformer() {

			@Override
			protected void internalTransform(Body b, String phaseName, Map<String, String> options) {}

		};
	}

	public boolean runSoot(String inputPath, String outputPath, int outputFormat) {
		Utils.deleteIncludeListFiles(outputPath, getIncludeList());
		setSootOptions(inputPath, outputPath, outputFormat);

		Utils.deleteAllFiles(outputPath);
		registerJimpleToClassTransformer();

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
		PackManager.v().runPacks();
		PackManager.v().writeOutput();
	}

	private void setSootOptions(String inputPath, String outputPath, int outputFormat) {
		G.reset();
		Options.v().setPhaseOption("jb", "use-original-names:true");
//		Options.v().setPhaseOption("bb.lso", "enabled:false"); // load-store optimizer (needed for jgf-moldyn)
//		Options.v().setPhaseOption("bb.pho", "enabled:true"); // peep-hole-optimizer
//		Options.v().setPhaseOption("bb.ule", "enabled:true"); // unused-local eliminator
//		Options.v().setPhaseOption("bb.lp", "enabled:false"); // locals packer
		Options.v().write_local_annotations();
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
		Options.v().set_full_resolver(true);
		Scene.v().forceResolve("java.util.Scanner", SootClass.SIGNATURES);
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

	private void registerJimpleToClassTransformer() {
		PackManager.v().getPack("jb").add(new Transform("jb.body", createBodyTransformer()));
		PackManager.v().getPack("bb").add(new Transform("bb.body", createBodyTransformer()));
	}
}
