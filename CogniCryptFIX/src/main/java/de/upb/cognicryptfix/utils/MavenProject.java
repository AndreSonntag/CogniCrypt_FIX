package de.upb.cognicryptfix.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;

import com.google.common.collect.Lists;

import de.upb.cognicryptfix.Constants;

public class MavenProject {
	private String pathToProjectRoot;
	private boolean compiled;
	private String fullProjectClassPath;

	public MavenProject(String pathToProjectRoot) {
		File file = new File(pathToProjectRoot);
		if (!file.exists())
			throw new RuntimeException("The path " + pathToProjectRoot + " does not exist!");
		this.pathToProjectRoot = new File(pathToProjectRoot).getAbsolutePath();
		
	}

	public void compile() {
		InvocationRequest request = new DefaultInvocationRequest();
		request.setPomFile(new File(pathToProjectRoot + File.separator + "pom.xml"));
		ArrayList<String> goals = Lists.newArrayList();
		goals.add("clean");
		goals.add("compile");
		request.setGoals(goals);

		Invoker invoker = new DefaultInvoker();
		try {
			invoker.execute(request);
		} catch (MavenInvocationException e) {
			throw new RuntimeException(
					"Was not able to invoke maven in path " + pathToProjectRoot + ". Does a pom.xml exist?");
		}
		compiled = true;
		computeClassPath();
	}

	private void computeClassPath() {
		InvocationRequest request = new DefaultInvocationRequest();
		request.setPomFile(new File(pathToProjectRoot + File.separator + "pom.xml"));
		ArrayList<String> goals = Lists.newArrayList();
		goals.add("dependency:build-classpath");
		goals.add("-Dmdep.outputFile=\"classPath.temp\"");
		request.setGoals(goals);
		Invoker invoker = new DefaultInvoker();
		try {
			invoker.execute(request);
		} catch (MavenInvocationException e) {
			throw new RuntimeException("Was not able to invoke maven to compute depenencies");
		}
		try {
			File classPathFile = new File(pathToProjectRoot + File.separator + "classPath.temp");
			fullProjectClassPath = IOUtils.toString(new FileInputStream(classPathFile), "utf-8");
			classPathFile.delete();
		} catch (IOException e) {
			throw new RuntimeException("Was not able to read in class path from file classPath.temp");
		}
	}

	public String getBuildDirectory() {
		if (!compiled) {
			throw new RuntimeException("You first have to compile the project. Use method compile()");
		}
		String buildPath = pathToProjectRoot + File.separator + "target" + File.separator + "classes";
		return buildPath;
	}

	public String getFullClassPath() {
		return fullProjectClassPath;
	}

	
	/**
	 * i.e.Crypto.One.Enc
	 * get(error.getErrorLocation().getMethod().getDeclaringClass().toString())
	 * @return path to Java file
	 */
	public HashMap<String, String> getProjectStructureHashMap() {
		
		HashMap<String, String> projectHashMap = new HashMap<String, String>();
		String projectSrcDirPath = pathToProjectRoot+Constants.FILE_SEPARATOR+"src"+Constants.FILE_SEPARATOR;
		File projectSrcDir = new File(projectSrcDirPath);
		Collection<File> files = FileUtils.listFiles(projectSrcDir, new SuffixFileFilter("java"), TrueFileFilter.INSTANCE);     
		for(File file : files){
			
			String key = file.getAbsolutePath().substring(projectSrcDirPath.length(),file.getAbsolutePath().length())
												   .replace(File.separator, ".")
												   .replaceAll(".java", "");
			projectHashMap.put(key, file.getAbsolutePath());			
		} 
		return projectHashMap;
	}

}
