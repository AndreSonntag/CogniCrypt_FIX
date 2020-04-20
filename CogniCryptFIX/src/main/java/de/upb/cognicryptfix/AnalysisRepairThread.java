package de.upb.cognicryptfix;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.upb.cognicryptfix.analysis.CryptoAnalysis;
import de.upb.cognicryptfix.analysis.CryptoAnalysisListener;
import de.upb.cognicryptfix.analysis.JimpleToClassAnalysis;
import de.upb.cognicryptfix.analysis.JimpleToDavaAnalysis;
import de.upb.cognicryptfix.decompiler.JavaDecompiler;
import de.upb.cognicryptfix.utils.MavenProject;
import de.upb.cognicryptfix.utils.Utils;
import soot.options.Options;

public class AnalysisRepairThread extends Thread {

	private static final Logger logger = LogManager.getLogger(AnalysisRepairThread.class.getSimpleName());
	private MavenProject mp;
	private CryptoAnalysisListener listener;
	private boolean succ = false;
	
	public AnalysisRepairThread(MavenProject mp, CryptoAnalysisListener listener) {
		this.mp = mp;
		this.listener = listener;
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
			this.succ = JimpleToDavaAnalysis.getInstance().runSoot(Constants.JIMPLE_OUTPUT_PATH, Constants.DAVA_OUTPUT_PATH, Options.output_format_dava);
			this.succ = JimpleToClassAnalysis.getInstance().runSoot(Constants.JIMPLE_OUTPUT_PATH, Constants.CLASS_OUTPUT_PATH, Options.output_format_class);

//			JavaDecompiler jd = JavaDecompiler.getInstance();
//			try {
//				jd.decompile(Constants.CLASS_OUTPUT_PATH);
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
			Utils.copyDavaFilesInProject(Constants.DAVA_OUTPUT_PATH, Constants.DAVA_OUTPUT_COPY_PROJECT);
		} else {
			logger.error("");
		}
	}
}
