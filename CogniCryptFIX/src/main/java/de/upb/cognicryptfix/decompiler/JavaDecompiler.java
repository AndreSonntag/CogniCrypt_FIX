package de.upb.cognicryptfix.decompiler;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jd.core.v1.ClassFileToJavaSourceDecompiler;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import de.upb.cognicryptfix.decompiler.jd.Loader;
import de.upb.cognicryptfix.decompiler.jd.Printer;


public class JavaDecompiler implements IDecompiler{

	private static final Logger LOGGER = LogManager.getLogger(JavaDecompiler.class);
	private static JavaDecompiler instance;
	private Set<String> classesToPrint;
	


	public void setClassesToPrint(Set<String> classesToPrint) {
		this.classesToPrint = classesToPrint;
	}

	private JavaDecompiler() {
		classesToPrint = Sets.newHashSet();
	}

	public static JavaDecompiler getInstance() {
		if (JavaDecompiler.instance == null) {
			JavaDecompiler.instance = new JavaDecompiler();
		}
		return JavaDecompiler.instance;
	}
	
	
	
	public void decompile(String path) throws IOException {
		File root = new File(path);
		Files.walk(root.toPath())
	      .filter(p -> !Files.isDirectory(p))
	      .forEach(p -> decompileFile(p.toString()));
	}
	
	private void decompileFile(String pathToClassFile) {
		
		if(!classesToPrint.contains(pathToClassFile)) {
			return;
		}
		
		Printer p = new Printer();
		try {
			ClassFileToJavaSourceDecompiler decompiler = new ClassFileToJavaSourceDecompiler();
			decompiler.decompile(new Loader(), p, pathToClassFile);
		} catch (Exception e) {
			LOGGER.error("Something went wrong during the decompile process of "+pathToClassFile);
			e.printStackTrace();
		}
		
		String source = p.toString();
		
		LOGGER.debug(source);
	}
}
