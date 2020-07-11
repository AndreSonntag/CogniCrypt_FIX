package de.upb.cognicryptfix;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import crypto.analysis.errors.AbstractError;
import de.upb.cognicryptfix.analysis.CryptoAnalysis;
import de.upb.cognicryptfix.analysis.CryptoAnalysisListener;
import de.upb.cognicryptfix.analysis.JimpleToClassAnalysis;
import de.upb.cognicryptfix.analysis.JimpleToDavaAnalysis;
import de.upb.cognicryptfix.decompiler.JavaDecompiler;
import de.upb.cognicryptfix.scheduler.ErrorScheduler;
import de.upb.cognicryptfix.utils.MavenProject;
import de.upb.cognicryptfix.utils.Utils;
import soot.SourceLocator;
import soot.options.Options;

public class AnalysisRepairThread extends Thread {

	private static final Logger LOGGER = LogManager.getLogger(AnalysisRepairThread.class);
	private MavenProject mp;
	private ErrorScheduler scheduler;
	private CryptoAnalysisListener listener;
	private boolean succ = false;
		
	public AnalysisRepairThread(MavenProject mp) {
		this.mp = mp;
		this.scheduler = ErrorScheduler.getInstance();
		this.listener = new CryptoAnalysisListener(scheduler);
	}

	public boolean isSucc() {
		return this.succ;
	}

	@Override
	public void run() {
		if(mp != null) {
			this.succ = CryptoAnalysis.getInstance().runSoot(mp, Constants.JIMPLE_OUTPUT_PATH, Options.output_format_jimple, listener);
			while(listener.isUnderRepair()) {
				this.succ = CryptoAnalysis.getInstance().runSoot(Constants.JIMPLE_OUTPUT_PATH, Constants.JIMPLE_OUTPUT_PATH, Options.output_format_jimple, listener);
			}
			
			Set<String> errorClassPaths = Sets.newHashSet();
			Map<Class, Set<Entry<String, AbstractError>>> sastErrors = scheduler.getInitiallyErrors();
			for (Set<Entry<String, AbstractError>> set : sastErrors.values()) {
				if (!set.isEmpty()) {
					for (Entry<String, AbstractError> entry : set) {
						
						String path = SourceLocator.v().getFileNameFor(entry.getValue().getErrorLocation().getMethod().getDeclaringClass(), Options.output_format_class);
						path = path.replace("jimpleOutput", "classOutput");
						errorClassPaths.add(path);
					}
				}
			}
//			this.succ = JimpleToDavaAnalysis.getInstance().runSoot(Constants.JIMPLE_OUTPUT_PATH, Constants.DAVA_OUTPUT_PATH, Options.output_format_dava);
//			Utils.copyDavaFilesInProject(Constants.DAVA_OUTPUT_PATH, Constants.DAVA_OUTPUT_COPY_PROJECT);

			
//			this.succ = JimpleToClassAnalysis.getInstance().runSoot(Constants.JIMPLE_OUTPUT_PATH, Constants.CLASS_OUTPUT_PATH, Options.output_format_class);
//			JavaDecompiler jd = JavaDecompiler.getInstance();
//			jd.setClassesToPrint(errorClassPaths);
//			try {
//				jd.decompile(Constants.CLASS_OUTPUT_PATH);
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//	
			
//			Utils.copyDavaFilesInProject(Constants.DAVA_OUTPUT_PATH, Constants.DAVA_OUTPUT_COPY_PROJECT);
		} else {
			LOGGER.error("");
		}
	}

	
}
