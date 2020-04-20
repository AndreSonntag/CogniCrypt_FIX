package de.upb.cognicryptfix.patcher.patches;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap.SimpleEntry;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Files;

import crypto.analysis.errors.HardCodedError;
import crypto.extractparameter.ExtractedValue;
import de.upb.cognicryptfix.Constants;
import de.upb.cognicryptfix.crysl.CrySLEntity;
import de.upb.cognicryptfix.crysl.CrySLMethodCall;
import de.upb.cognicryptfix.crysl.pool.CrySLEntityPool;
import de.upb.cognicryptfix.exception.jimple.NotExpectedUnitException;
import de.upb.cognicryptfix.exception.patch.RepairException;
import de.upb.cognicryptfix.generator.jimple.JimpleAssignGenerator;
import de.upb.cognicryptfix.generator.jimple.JimpleCallGenerator;
import de.upb.cognicryptfix.generator.jimple.JimpleLocalGenerator;
import de.upb.cognicryptfix.generator.jimple.JimpleTrapGenerator;
import de.upb.cognicryptfix.generator.jimple.JimpleUtils;
import de.upb.cognicryptfix.utils.CharConstant;
import de.upb.cognicryptfix.utils.Utils;
import soot.ArrayType;
import soot.Body;
import soot.BooleanType;
import soot.CharType;
import soot.IntType;
import soot.Local;
import soot.PrimType;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.SourceLocator;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.jimple.AddExpr;
import soot.jimple.ArrayRef;
import soot.jimple.AssignStmt;
import soot.jimple.ClassConstant;
import soot.jimple.Constant;
import soot.jimple.EqExpr;
import soot.jimple.GeExpr;
import soot.jimple.GotoStmt;
import soot.jimple.IntConstant;
import soot.jimple.Jimple;
import soot.jimple.LengthExpr;
import soot.jimple.NewArrayExpr;
import soot.jimple.StringConstant;
import soot.jimple.ThisRef;
import soot.jimple.internal.JimpleLocal;
import soot.options.Options;

public class HardCodedPatch extends AbstractPatch {
	private static final Logger LOGGER = LogManager.getLogger(HardCodedPatch.class);

	private HardCodedError error;

	private CrySLEntity entity;
	private Body body;
	private JimpleTrapGenerator trapGenerator;
	private JimpleLocalGenerator localGenerator;
	private JimpleAssignGenerator assignGenerator;
	private JimpleCallGenerator callGenerator;
	private String patch;

	/**
	 * Creates a new {@link HardCodedPatch} object that represents the patch for the
	 * {@link HardCodedError} argument.
	 * 
	 * @param error the error
	 */
	public HardCodedPatch(HardCodedError error) {
		this.error = error;
		this.entity = CrySLEntityPool.getInstance().getEntityByClassName(error.getRule().getClassName());
		this.body = error.getErrorLocation().getMethod().getActiveBody();
		this.localGenerator = new JimpleLocalGenerator(body);
		this.assignGenerator = new JimpleAssignGenerator();
		this.callGenerator = new JimpleCallGenerator(body);
		this.trapGenerator = new JimpleTrapGenerator(body);
		this.patch = "";
	}

	@Override
	public Body applyPatch() throws RepairException {
		Unit errorUnit = error.getErrorLocation().getUnit().get();
		ExtractedValue ev = error.getCallSiteWithExtractedValue().getVal();
		Type errorLocalType = ev.getValue().getType();
		
		if (errorLocalType instanceof ArrayType) {
			Entry<List<Unit>, List<Value>> valueEntry = extraxtHardCodedArrayValue(ev, errorUnit);

			ArrayType errorLocalArrayType = (ArrayType) errorLocalType;
			AssignStmt arrayAssigment = (AssignStmt) valueEntry.getKey().get(0);
			Local arrayLocal = (Local) arrayAssigment.getLeftOp();

			for (int i = 1; i < valueEntry.getKey().size(); i++) {
				body.getUnits().remove(valueEntry.getKey().get(i));
			}

			String fileName = createFileWithContent(error.getErrorLocation().getMethod().getDeclaringClass(), error.getErrorLocation().getMethod(), errorLocalArrayType.baseType, valueEntry.getValue().stream().toArray(Value[]::new));
			Map<Local, List<Unit>> readerUnits = createReadContentFromFile(error.getErrorLocation().getMethod().getDeclaringClass(), arrayLocal, fileName);
			List<Unit> generatedUnits = Utils.summarizeUnitLists(readerUnits.values());
			body.getUnits().insertAfter(generatedUnits, arrayAssigment);
		} else if (errorLocalType instanceof PrimType
				|| JimpleUtils.equals(errorLocalType, Scene.v().getType("java.lang.String"))
				|| JimpleUtils.equals(errorLocalType, Scene.v().getType("java.math.BigInteger"))) {
			// TODO: not implemented yet
		} else {
			throw new RuntimeException();
		}
		
		patch = "content stored in file.";
		return body;
	}

	private String createFileWithContent(SootClass clazz, SootMethod method, Type valueType, Value... value) {
		String pathWithName = SourceLocator.v().getFileNameFor(clazz, Options.output_format_jimple);
		String path = pathWithName.substring(0,  pathWithName.lastIndexOf("\\")+1);
		String nameWithJimplePrefix = pathWithName.substring(pathWithName.lastIndexOf("\\") + 1);
		String nameWithoutPrefix = nameWithJimplePrefix.substring(0,  nameWithJimplePrefix.lastIndexOf("."));
		String name = nameWithoutPrefix.substring(0,  nameWithoutPrefix.lastIndexOf(".")+1)+method.getName()+"_"+ nameWithoutPrefix.substring(nameWithoutPrefix.lastIndexOf(".")+1, nameWithoutPrefix.length());

		String pathWithNameWithoutPrefix = path+"\\"+name;
		String pathWithNameWithTxtPrefix = path+"\\"+name+".txt";
		String nameWithTxtPrefix = name+".txt";
		String content = "";

		for (Value v : value) {
			if(JimpleUtils.equals(valueType, CharType.v())) {
				content += CharConstant.v(Integer.parseInt(v.toString()));
			} else {
				content = v + " ";
			}
		}
		try {
			Path filePath = Paths.get(pathWithNameWithTxtPrefix);
			if (!java.nio.file.Files.exists(filePath)) {
				File f = new File(filePath.toUri());
				f.createNewFile();
			} else {
				String uniqueName = Utils.findFileName(pathWithNameWithoutPrefix)+".txt";
				nameWithTxtPrefix = uniqueName.substring(uniqueName.lastIndexOf("\\") + 1);
				filePath = Paths.get(uniqueName);
				File f = new File(filePath.toUri());
				f.createNewFile();
			}

			java.nio.file.Files.write(filePath, content.getBytes());
		} catch (IOException e) {
			e.printStackTrace();
		}

		return nameWithTxtPrefix;
	}

	private Map<Local, List<Unit>> createReadContentFromFile(SootClass clazz, Local arrayLocal, String fileName) {
		Map<Local, List<Unit>> generatedUnits = Maps.newLinkedHashMap();
		List<Unit> generatedUnitList = Lists.newArrayList();

		Type localType = arrayLocal.getType();
		if (localType instanceof ArrayType) {
			localType = ((ArrayType) localType).baseType;
		}

		RefType objectRefType = RefType.v("java.lang.Object");
		SootClass objectClass = objectRefType.getSootClass();
		
		RefType clazzRefType = RefType.v("java.lang.Class");
		SootClass clazzClass = clazzRefType.getSootClass();

		RefType urlRefType = RefType.v("java.net.URL");
		SootClass urlClass = urlRefType.getSootClass();

		RefType fileReaderRefType = RefType.v("java.io.FileReader");
		SootClass fileReaderClass = fileReaderRefType.getSootClass();

		RefType buffReaderRefType = RefType.v("java.io.BufferedReader");
		SootClass buffReaderClass = buffReaderRefType.getSootClass();
		
		Local clazzLocal = localGenerator.generateFreshLocal(clazzRefType);
		SootMethod getClass = objectClass.getMethod("java.lang.Class getClass()");
		Unit getClassCall = callGenerator.generateCallUnits(JimpleUtils.getLocalByName(body, "this"), clazzLocal, getClass).values().iterator().next().get(0);

		/*- varReplacer0 = "Key.txt"; */
		Local filePathLocal = localGenerator.generateFreshLocal(Scene.v().getType("java.lang.String"), "varReplacer");
		StringConstant filePathValue = StringConstant.v(fileName);
		Unit filePathAssignment = assignGenerator.generateAssignStmt(filePathLocal, filePathValue);

		/*- $stack10 = virtualinvoke $stack9.<java.lang.Class: java.net.URL getResource(java.lang.String)>(varReplacer0); */
		Local urlLocal = localGenerator.generateFreshLocal(urlRefType, "url");
		SootMethod getResourceMethod = clazzClass.getMethod("java.net.URL getResource(java.lang.String)");
		Unit getResourceCall = callGenerator.generateCallUnits(clazzLocal, urlLocal, getResourceMethod, filePathLocal).values().iterator().next().get(0);

		/*- $stack11 = virtualinvoke $stack10.<java.net.URL: java.lang.String getPath()>(); */
		Local stringPathLocal = localGenerator.generateFreshLocal(Scene.v().getType("java.lang.String"), "path");
		SootMethod getPathMethod = urlClass.getMethod("java.lang.String getPath()");
		Unit getPathCall = callGenerator.generateCallUnits(urlLocal, stringPathLocal, getPathMethod).values().iterator().next().get(0);

		/*- specialinvoke $stack15.<java.io.FileReader: void <init>(java.lang.String)>(path); */
		SootMethod fileReaderInit = fileReaderClass.getMethod("void <init>(java.lang.String)");
		Local fileReaderLocal = localGenerator.generateFreshLocal(fileReaderRefType, "fileReader");
		Map<Local, List<Unit>> fileReaderInitCall = callGenerator.generateCallUnits(fileReaderLocal, fileReaderInit,
				stringPathLocal);

		/*- specialinvoke $stack14.<java.io.BufferedReader: void <init>(java.io.Reader)>($stack15); */
		SootMethod buffReaderInit = buffReaderClass.getMethod("void <init>(java.io.Reader)");
		Local buffReaderLocal = localGenerator.generateFreshLocal(buffReaderRefType, "buffedReader");
		Map<Local, List<Unit>> buffReaderCall = callGenerator.generateCallUnits(buffReaderLocal, buffReaderInit,
				fileReaderLocal);

		List<Unit> loop = Lists.newArrayList();
		
		if(JimpleUtils.equals(localType, CharType.v())){
			loop = readChar(buffReaderLocal, arrayLocal);
		} else {
			loop = readOther(buffReaderLocal, arrayLocal);
		}
		
		generatedUnitList.add(getClassCall);
		generatedUnitList.add(filePathAssignment);
		generatedUnitList.add(getResourceCall);
		generatedUnitList.add(getPathCall);
		generatedUnitList.addAll(fileReaderInitCall.values().iterator().next());
		generatedUnitList.addAll(buffReaderCall.values().iterator().next());
		generatedUnitList.addAll(loop);
		generatedUnits.put(arrayLocal, generatedUnitList);
		
		return generatedUnits;
	}

	private List<Unit> readChar(Local buffReaderLocal, Local arrayLocal) {
		List<Unit> generatedUnitList = Lists.newArrayList();

		RefType buffReaderRefType = RefType.v("java.io.BufferedReader");
		SootClass buffReaderClass = buffReaderRefType.getSootClass();
		SootMethod buffReaderRead = buffReaderClass.getMethod("int read()");
		SootMethod buffReaderClose = buffReaderClass.getMethod("void close()");

		// theCharNum = virtualinvoke reader.<java.io.BufferedReader: int read()>();
		Local charNumLocal = localGenerator.generateFreshLocal(IntType.v(), "charNum");
		Unit readCallOut = callGenerator.generateCallUnits(buffReaderLocal, charNumLocal, buffReaderRead).values()
				.iterator().next().get(0);

        // i = 0;
		Local indexLocal = localGenerator.generateFreshLocal(IntType.v(), "index");
		Unit indexAssignment = assignGenerator.generateAssignStmt(indexLocal, IntConstant.v(0));

		// virtualinvoke reader.<java.io.BufferedReader: void close()>();
		Unit closeCall = callGenerator.generateCallUnits(buffReaderLocal, null, buffReaderClose).values().iterator().next().get(0);

		// if charNumLocal == -1 goto closeCall;
		EqExpr clause = Jimple.v().newEqExpr(charNumLocal, IntConstant.v(-1));
		Unit ifStmt = Jimple.v().newIfStmt(clause, closeCall);		
		
		// charLocal = (char) charNumLocal;
		Local charLocal = localGenerator.generateFreshLocal(CharType.v(), "charVar");
		Value castExpr = Jimple.v().newCastExpr(charNumLocal, CharType.v());
		Unit castAssign = assignGenerator.generateAssignStmt(charLocal, castExpr);

		// content[$stack32] = $stack33;
		ArrayRef leftSide = Jimple.v().newArrayRef(arrayLocal, indexLocal);
		Unit arrayIndexAssignment = assignGenerator.generateAssignStmt(leftSide, charLocal);

		// charNumLocal = virtualinvoke reader.<java.io.BufferedReader: int read()>();
		Unit readCallIn = callGenerator.generateCallUnits(buffReaderLocal, charNumLocal, buffReaderRead).values().iterator().next().get(0);

		// index = index + 1;
		AddExpr addIndex = Jimple.v().newAddExpr(indexLocal, IntConstant.v(1));
		Unit incIndexAssigment = assignGenerator.generateAssignStmt(indexLocal, addIndex);

		// goto label1;
		GotoStmt gotoStmt = Jimple.v().newGotoStmt(ifStmt);

		generatedUnitList.add(readCallOut);
		generatedUnitList.add(indexAssignment);
		generatedUnitList.add(ifStmt);
		generatedUnitList.add(castAssign);
		generatedUnitList.add(arrayIndexAssignment);
		generatedUnitList.add(readCallIn);
		generatedUnitList.add(incIndexAssigment);
		generatedUnitList.add(gotoStmt);
		generatedUnitList.add(closeCall);
		return generatedUnitList;
	}

	private List<Unit> readOther(Local buffReaderLocal, Local arrayLocal) {
		List<Unit> generatedUnitList = Lists.newArrayList();

		RefType stringRefType = RefType.v("java.lang.String");
		SootClass stringClass = stringRefType.getSootClass();
		SootMethod split = stringClass.getMethod("java.lang.String[] split(java.lang.String)");

		RefType typeRefType = RefType.v("java.lang.Integer");
		SootClass typeClass = typeRefType.getSootClass();
		SootMethod parse = typeClass.getMethod("int parseInt(java.lang.String)");

		RefType buffReaderRefType = RefType.v("java.io.BufferedReader");
		SootClass buffReaderClass = buffReaderRefType.getSootClass();
		SootMethod buffReaderReadLine = buffReaderClass.getMethod("java.lang.String readLine()");
		SootMethod buffReaderClose = buffReaderClass.getMethod("void close()");

		// virtualinvoke reader.<java.io.BufferedReader: void close()>();
		Unit closeCall = callGenerator.generateCallUnits(buffReaderLocal, null, buffReaderClose).values().iterator()
				.next().get(0);

		// output = virtualinvoke bfReader.<java.io.BufferedReader: java.lang.String
		// readLine()>();
		Local outputLocal = localGenerator.generateFreshLocal(arrayLocal.getType(), "output"); // TODO: need check
		Unit readLineCall = callGenerator.generateCallUnits(buffReaderLocal, outputLocal, buffReaderReadLine).values()
				.iterator().next().get(0);

		// varReplacer3 = " ";
		Local spaceLocal = localGenerator.generateFreshLocal(stringRefType, "varReplacer");
		Unit spaceAssigment = assignGenerator.generateAssignStmt(spaceLocal, StringConstant.v(" "));

		// split = virtualinvoke output.<java.lang.String: java.lang.String[]
		// split(java.lang.String)>(varReplacer3);
		Local splitLocal = localGenerator.genereateFreshArrayLocal(stringRefType, "split", 1);
		Unit splitCall = callGenerator.generateCallUnits(outputLocal, splitLocal, split, spaceLocal).values().iterator()
				.next().get(0);

		// $stack23 = lengthof split;
		Local lengthLoal = localGenerator.generateFreshLocal(IntType.v(), "splitLength");
		LengthExpr lengthExpr = Jimple.v().newLengthExpr(splitLocal);
		Unit lengthAssigment = assignGenerator.generateAssignStmt(lengthLoal, lengthExpr);

		// i = 0;
		Local indexLocal = localGenerator.generateFreshLocal(IntType.v(), "i");
		Unit indexAssignment = assignGenerator.generateAssignStmt(indexLocal, IntConstant.v(0));

		// if i >= $stack24 goto label4;
		GeExpr clause = Jimple.v().newGeExpr(indexLocal, lengthLoal);
		Unit ifStmt = Jimple.v().newIfStmt(clause, closeCall);

		// $stack28 = split[i];
		Local stringLocal = localGenerator.generateFreshLocal(stringRefType);
		ArrayRef rightSide = Jimple.v().newArrayRef(splitLocal, indexLocal);
		Unit stringAssignment = assignGenerator.generateAssignStmt(stringLocal, rightSide);

		// $stack29 = staticinvoke <java.lang.Integer: int
		// parseInt(java.lang.String)>($stack28);
		Local parseResultLocal = localGenerator.generateFreshLocal(IntType.v());
		Unit parseCall = callGenerator.generateCallUnits(parseResultLocal, null, parse, stringLocal).values().iterator().next().get(0);

		// intContent[i] = $stack29;
		ArrayRef leftSide = Jimple.v().newArrayRef(arrayLocal, indexLocal);
		Unit arrayIndexAssignment = assignGenerator.generateAssignStmt(leftSide, parseResultLocal);

		// i = i + 1;
		AddExpr addIndex = Jimple.v().newAddExpr(indexLocal, IntConstant.v(1));
		Unit incIndexAssigment = assignGenerator.generateAssignStmt(indexLocal, addIndex);

		// goto label1;
		GotoStmt gotoStmt = Jimple.v().newGotoStmt(ifStmt);

		generatedUnitList.add(readLineCall);
		generatedUnitList.add(spaceAssigment);
		generatedUnitList.add(splitCall);
		generatedUnitList.add(lengthAssigment);
		generatedUnitList.add(indexAssignment);
		generatedUnitList.add(ifStmt);
		generatedUnitList.add(stringAssignment);
		generatedUnitList.add(parseCall);
		generatedUnitList.add(arrayIndexAssignment);
		generatedUnitList.add(incIndexAssigment);
		generatedUnitList.add(gotoStmt);
		generatedUnitList.add(closeCall);

		return generatedUnitList;
	}

	private Entry<List<Unit>, List<Value>> extraxtHardCodedArrayValue(ExtractedValue ev, Unit errorUnit)
			throws NotExpectedUnitException {
		List<Value> valueList = Lists.newArrayList();
		List<Unit> unitList = Lists.newArrayList();
		Entry<List<Unit>, List<Value>> retEntry = null;

		Unit evUnit = ev.stmt().getUnit().get();
		if (evUnit instanceof AssignStmt) {
			AssignStmt evAssignStmt = (AssignStmt) evUnit;
			if (evAssignStmt.getRightOp() instanceof NewArrayExpr) {
				unitList.add(evAssignStmt);

				Iterator<Unit> it = body.getUnits().iterator(evAssignStmt, errorUnit);
				while (it.hasNext()) {
					Unit next = it.next();
					if (next instanceof AssignStmt) {
						AssignStmt nextAssign = (AssignStmt) next;
						if (nextAssign.getLeftOp() instanceof ArrayRef) {
							ArrayRef nextAssignArrayRef = (ArrayRef) nextAssign.getLeftOp();
							if (nextAssignArrayRef.getBase() == evAssignStmt.getLeftOp()) {
								if (nextAssign.getRightOp() instanceof Constant) {
									unitList.add(nextAssign);
									valueList.add(nextAssign.getRightOp());
								}
							}

						}
					}
				}

				retEntry = new SimpleEntry<List<Unit>, List<Value>>(unitList, valueList);
			} else {
				throw new NotExpectedUnitException("ExtractedValue AssignStmt doesn't contain a NewArrayExpr");
			}
		} else {
			throw new NotExpectedUnitException("ExtractedValue Unit doesn't contain an AssignStmt");
		}
		return retEntry;
	}

	private Entry<Unit, Value> extactHardCodedValue(ExtractedValue ev, Unit errorUnit) throws NotExpectedUnitException {
		Unit evUnit = ev.stmt().getUnit().get();
		Value value = null;
		Entry<Unit, Value> retEntry = null;

		if (evUnit instanceof AssignStmt) {
			AssignStmt evAssignStmt = (AssignStmt) evUnit;
			if (evAssignStmt.getRightOp() instanceof Constant) {
				value = evAssignStmt.getRightOp();
				retEntry = new SimpleEntry<Unit, Value>(evAssignStmt, value);
			} else {
				throw new NotExpectedUnitException("ExtractedValue AssignStmt doesn't get a Constant assigned");
			}
		} else {
			throw new NotExpectedUnitException("ExtractedValue Unit doesn't contain an AssignStmt");
		}

		if (retEntry != null && retEntry.getKey() != null && retEntry.getValue() != null) {
			return retEntry;
		} else {
			throw new NotExpectedUnitException("Value couldn't extracted!");
		}
	}

	@Override
	public String toPatchString() {
		StringBuilder builder = new StringBuilder();
		builder.append("\n__________[HardCodedPatch]__________\n");
		builder.append("Class: \t\t" + error.getErrorLocation().getMethod().getDeclaringClass().toString() + "\n");
		builder.append("Method: \t" + error.getErrorLocation().getMethod().getSignature() + "\n");
		builder.append("Error: \t\t" + error.getClass().getSimpleName() + "\n");
		builder.append("CrySLRule: \t" + entity.getRule().getClassName() + "\n");
		builder.append("Message: \t" + error.toErrorMarkerString() + "\n");
		builder.append("Patch:\t\t"+patch+"\n");
		builder.append("________________________________________\n");
		return builder.toString();
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("HardCodedPatch [error=");
		builder.append(error);
		builder.append(", entity=");
		builder.append(entity.getRule().getClassName());
		builder.append("]");
		return builder.toString();
	}
}
