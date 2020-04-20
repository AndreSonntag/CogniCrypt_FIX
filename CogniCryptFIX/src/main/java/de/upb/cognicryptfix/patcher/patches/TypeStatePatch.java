package de.upb.cognicryptfix.patcher.patches;

import java.util.AbstractMap.SimpleEntry;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import boomerang.jimple.Statement;
import crypto.analysis.AnalysisSeedWithSpecification;
import crypto.analysis.errors.TypestateError;
import de.upb.cognicryptfix.crysl.CrySLEntity;
import de.upb.cognicryptfix.crysl.CrySLMethodCall;
import de.upb.cognicryptfix.crysl.pool.CrySLEntityPool;
import de.upb.cognicryptfix.exception.patch.RepairException;
import de.upb.cognicryptfix.generator.JimpleCodeGeneratorByRule;
import de.upb.cognicryptfix.generator.jimple.JimpleUtils;
import de.upb.cognicryptfix.utils.Utils;
import soot.Body;
import soot.Local;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.InvokeExpr;

public class TypeStatePatch extends AbstractPatch {

	private static final Logger LOGGER = LogManager.getLogger(TypeStatePatch.class);
	
	private TypestateError error;

	private CrySLEntity entity;
	private Body body;
	private JimpleCodeGeneratorByRule generator;
	private List<CrySLMethodCall> patch;

	/**
	 * Creates a new {@link TypeStatePatch} object that represents the patch for the
	 * {@link TypestateError} argument.
	 * 
	 * @param error the error
	 */
	public TypeStatePatch(TypestateError error) {
		this.error = error;
		this.entity = CrySLEntityPool.getInstance().getEntityByClassName(error.getRule().getClassName());
		this.body = error.getErrorLocation().getMethod().getActiveBody();
		this.generator = new JimpleCodeGeneratorByRule(body);
		this.patch = null;
	}

	@Override
	public Body applyPatch() throws RepairException{		
		Unit errorUnit = error.getErrorLocation().getUnit().get();
		Local errorLocal = null;
		if (JimpleUtils.containsInvokeExpr(errorUnit)) {
			errorLocal = JimpleUtils.getInvokeLocal(errorUnit);
		}

		List<SootMethod> expectedSootMethods = Lists.newArrayList(error.getExpectedMethodCalls());
		AnalysisSeedWithSpecification seed = (AnalysisSeedWithSpecification) error.getObjectLocation();
		Map<Statement, SootMethod> allCallsOnObject = seed.getAllCallsOnObject();
		boolean hasExpectedCalls = expectedSootMethods.isEmpty() ? false : true;
		Map<Entry<Statement, SootMethod>, Integer> orderedCallsOnObject = orderCallsOnObject(allCallsOnObject);

		Entry<Statement, SootMethod> errorUnitEntry = getEntryByUnit(errorUnit, orderedCallsOnObject);

		if (hasExpectedCalls) {
			List<Entry<Statement, SootMethod>> expectedMethodCallsEntrys = getEntrysBySootMethods(expectedSootMethods, orderedCallsOnObject);
			Entry<Statement, SootMethod> expectedMethodCallEntry = getClosestEntryForward(errorUnitEntry, expectedMethodCallsEntrys, orderedCallsOnObject);

			if (expectedMethodCallEntry != null) {
				Unit expectedMethodCallUnit = expectedMethodCallEntry.getKey().getUnit().get();
				if (expectedMethodCallEntry.getKey().getMethod().getSignature() == error.getErrorLocation().getMethod().getSignature()) {
					List<Unit> unitsToMove = getUnitsBetweenPoints(errorUnit, expectedMethodCallUnit);
					unitsToMove.add(expectedMethodCallUnit);
					body.getUnits().removeAll(unitsToMove);
					body.getUnits().insertBefore(unitsToMove, errorUnit);
					for (Unit movedUnit : unitsToMove) {
						generator.generateTryCatchBlock(movedUnit);
					}
				} else {
					CrySLMethodCall expectedCall = entity.getFSM().getCrySLMethodCallBySootMethod(expectedMethodCallEntry.getKey().getMethod());
					Map<Local, List<Unit>> generatedCallUnits = generator.generateCallWithParameter(errorLocal,expectedCall, true);
					List<Unit> units = Utils.summarizeUnitLists(generatedCallUnits.values());
					body.getUnits().insertBefore(units, errorUnit);
					patch = Lists.newArrayList(expectedCall);
				}
			} else {
				List<LinkedList<CrySLMethodCall>> pathsToFinalState = entity.getFSM().calcBestPathsToFinalStatesIncludeMultipleMethod(expectedSootMethods);
				Map<Local, List<Unit>> generatedCallUnits = generator.generateCallWithParameter(errorLocal, pathsToFinalState.get(0).get(0), true);
				List<Unit> units = Utils.summarizeUnitLists(generatedCallUnits.values());
				body.getUnits().insertBefore(units, errorUnit);
				patch = Lists.newArrayList(pathsToFinalState.get(0).get(0));

			}
		} else {
			body.getUnits().remove(errorUnit);
		}
		generator.removeUnnecessaryTryCatchBlock();	
		return body;
	}

	private List<Unit> getUnitsBetweenPoints(Unit p1, Unit p2) {
		List<Unit> units = Lists.newArrayList();
		Iterator<Unit> it = body.getUnits().iterator(body.getUnits().getSuccOf(p1), body.getUnits().getPredOf(p2));
		while (it.hasNext()) {
			units.add(it.next());
		}
		return units;
	}

	private Entry<Statement, SootMethod> getEntryByUnit(Unit u,
			Map<Entry<Statement, SootMethod>, Integer> orderedCallsOnObject) {

		for (Entry<Statement, SootMethod> entry : orderedCallsOnObject.keySet()) {
			Unit entryUnit = entry.getKey().getUnit().get();
			if (entryUnit == u) {
				return entry;
			}
		}
		return null;
	}

	private Entry<Statement, SootMethod> getClosestEntryForward(Entry<Statement, SootMethod> point,
			List<Entry<Statement, SootMethod>> entrys,
			Map<Entry<Statement, SootMethod>, Integer> orderedCallsOnObject) {

		int pointPosition = orderedCallsOnObject.get(point);
		Entry<Statement, SootMethod> candidate = null;
		int candidatePosition = -1;

		for (Entry<Statement, SootMethod> entry : entrys) {
			int entryPosition = orderedCallsOnObject.get(entry);

			if (entryPosition > pointPosition && candidate == null) {
				candidate = entry;
				candidatePosition = entryPosition;
			} else {
				if (entryPosition > pointPosition && entryPosition < candidatePosition) {
					candidate = entry;
					candidatePosition = entryPosition;
				}
			}
		}
		return candidate;
	}

	private List<Entry<Statement, SootMethod>> getEntrysBySootMethods(List<SootMethod> methodList,
			Map<Entry<Statement, SootMethod>, Integer> orderedCallsOnObject) {
		List<Entry<Statement, SootMethod>> entryList = Lists.newArrayList();

		for (SootMethod method : methodList) {
			Entry<Statement, SootMethod> me = getEntryBySootMethod(method, orderedCallsOnObject);
			if (me != null) {
				entryList.add(me);
			}
		}
		return entryList;
	}

	private Entry<Statement, SootMethod> getEntryBySootMethod(SootMethod method,
			Map<Entry<Statement, SootMethod>, Integer> orderedCallsOnObject) {
		for (Entry<Statement, SootMethod> entry : orderedCallsOnObject.keySet()) {
			SootMethod entryMethod = entry.getValue();
			if (entryMethod.getSignature() == method.getSignature()) {
				return entry;
			}
		}
		return null;
	}

	private Map<Entry<Statement, SootMethod>, Integer> orderCallsOnObject(Map<Statement, SootMethod> allCallsOnObject) {
		Map<Entry<Statement, SootMethod>, Integer> orderedMap = Maps.newLinkedHashMap();

		if (allCallsOnObject.isEmpty()) {
			return orderedMap;
		}

		Map<Unit, Statement> unitStatementMap = Maps.newHashMap();

		for (Statement stmt : allCallsOnObject.keySet()) {
			Unit stmtUnit = stmt.getUnit().get();
			unitStatementMap.put(stmtUnit, stmt);
		}

		int i = 0;
		for (Unit u : body.getUnits()) {
			if (unitStatementMap.containsKey(u)) {
				orderedMap.put(new SimpleEntry<Statement, SootMethod>(unitStatementMap.get(u),
						allCallsOnObject.get(unitStatementMap.get(u))), i++);
			}
		}
		return orderedMap;
	}

	@Override
	public String toPatchString() {
		StringBuilder builder = new StringBuilder();
		builder.append("\n__________[TypeStatePatch]__________\n");
		builder.append("Class: \t\t" + error.getErrorLocation().getMethod().getDeclaringClass().toString() + "\n");
		builder.append("Method: \t" + error.getErrorLocation().getMethod().getSignature() + "\n");
		builder.append("Error: \t\t" + error.getClass().getSimpleName() + "\n");
		builder.append("CrySLRule: \t" + entity.getRule().getClassName() + "\n");
		builder.append("Message: \t" + error.toErrorMarkerString() + "\n");
		builder.append("Patch:\t\t");
		if (patch == null) {
			if(error.getExpectedMethodCalls().isEmpty()) {
			builder.append("Unexpected call removed");
			} else {				
			builder.append("Expected call found in code and moved to the right location");
			}
		} else {
			builder.append("\n");
			for (CrySLMethodCall call : patch) {
				builder.append("\t" + call.getSootMethod() + "\n");
			}
		}
		builder.append("\n");
		builder.append("________________________________________\n");
		return builder.toString();
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("TypeStatePatch [error=");
		builder.append(error.toErrorMarkerString());
		builder.append(", entity=");
		builder.append(entity.getRule().getClassName());
		builder.append("]");
		return builder.toString();
	}
	
	

}
