package de.upb.cognicryptfix.patcher.patches;

import java.util.AbstractMap.SimpleEntry;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.MapUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import boomerang.jimple.Statement;
import boomerang.jimple.Val;
import crypto.analysis.AnalysisSeedWithSpecification;
import crypto.analysis.errors.TypestateError;
import de.upb.cognicryptfix.Constants;
import de.upb.cognicryptfix.crysl.CrySLEntity;
import de.upb.cognicryptfix.crysl.CrySLMethodCall;
import de.upb.cognicryptfix.crysl.pool.CrySLEntityPool;
import de.upb.cognicryptfix.exception.generation.GenerationException;
import de.upb.cognicryptfix.exception.patch.RepairException;
import de.upb.cognicryptfix.generator.JimpleCodeGeneratorByRule;
import de.upb.cognicryptfix.generator.jimple.JimpleUtils;
import de.upb.cognicryptfix.scheduler.ErrorScheduler;
import de.upb.cognicryptfix.utils.Utils;
import soot.Body;
import soot.Local;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.InvokeExpr;
import sync.pds.solver.nodes.Node;

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
	public Body applyPatch() throws RepairException {
		Unit errorUnit = error.getErrorLocation().getUnit().get();
		SootMethod outerMethod = error.getErrorLocation().getMethod();
		Local errorLocal = null;
		if (JimpleUtils.containsInvokeExpr(errorUnit)) {
			errorLocal = JimpleUtils.getInvokeLocal(errorUnit);
		}

		List<SootMethod> expectedSootMethods = Lists.newArrayList(error.getExpectedMethodCalls());
		if (expectedSootMethods.isEmpty()) {
			body.getUnits().remove(errorUnit);
			return body;
		} else {
			Set<Node<Statement, Val>> dataFlowPathErrorVariable = error.getDataFlowPath();
			for (Node<Statement, Val> node : dataFlowPathErrorVariable) {
				if (JimpleUtils.containsInvokeExpr(node.stmt().getUnit().get())) {
					SootMethod invokedMethod = node.stmt().getUnit().get().getInvokeExpr().getMethod();
					if (expectedSootMethods.contains(invokedMethod)
							&& node.fact().m().getSignature().equals(outerMethod.getSignature())) {
						if (isCalledAfter(errorUnit, node.stmt().getUnit().get())) {
							List<Unit> unitsToMove = getUnitsBetweenPoints(errorUnit, node.stmt().getUnit().get());
							unitsToMove.add(node.stmt().getUnit().get());
							body.getUnits().removeAll(unitsToMove);
							body.getUnits().insertBefore(unitsToMove, errorUnit);
							return body;
						}
					}
				}
			}

			List<LinkedList<CrySLMethodCall>> pathsToFinalState = entity.getFSM()
					.calcPathsToFinalStateStartingByMethods(expectedSootMethods);
			Map<Local, List<Unit>> generatedCallUnits = generator.generateCallWithParameter(errorLocal,
					pathsToFinalState.get(0).get(0), true);
			List<Unit> units = Utils.summarizeUnitLists(generatedCallUnits.values());

			if (!units.isEmpty()) {
				List<Unit> afterPredicateUnits = units.stream()
						.filter(unit -> unit.getTag(Constants.AFTER_PREDICATE_TAG) != null)
						.collect(Collectors.toList());

				if (!afterPredicateUnits.isEmpty()) {
					units.removeAll(afterPredicateUnits);
					body.getUnits().insertAfter(afterPredicateUnits, errorUnit);
				}
				body.getUnits().insertBefore(units, errorUnit);
				patch = Lists.newArrayList(pathsToFinalState.get(0).get(0));
				return body;
			} 
		}
		return body;
	}

	private List<Unit> getUnitsBetweenPoints(Unit p1, Unit p2) {
		List<Unit> units = Lists.newArrayList();
		Iterator<Unit> it = body.getUnits().iterator(body.getUnits().getSuccOf(p1), body.getUnits().getPredOf(p2));
		while (it.hasNext()) {
			try {
				units.add(it.next());
			} catch(java.util.NoSuchElementException nse) {
				break;
			}
		}
		return units;
	}

	private boolean isCalledAfter(Unit p1, Unit p2) throws RepairException {
		
		if(p1.equals(p2)) {
			return false;
		}
		
		Iterator<Unit> it = body.getUnits().iterator(p1, body.getUnits().getLast());
		while (it.hasNext()) {
			if(it.next().equals(p2)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public String toPatchString() {
		StringBuilder builder = new StringBuilder();
		builder.append("\n__________[TypeStatePatch]__________\n");
		builder.append("Repaired in Round: \t" + ErrorScheduler.round+ "\n");
		builder.append("Class: \t" + error.getErrorLocation().getMethod().getDeclaringClass().toString() + "\n");
		builder.append("Method: \t" + error.getErrorLocation().getMethod().getSignature() + "\n");
		builder.append("Error: \t" + error.getClass().getSimpleName() + "\n");
		builder.append("CrySLRule: \t" + entity.getRule().getClassName() + "\n");
		builder.append("Message: \t" + error.toErrorMarkerString() + "\n");
		builder.append("Patch:\t");
		if (patch == null) {
			if (error.getExpectedMethodCalls().isEmpty()) {
				builder.append("Unexpected call removed");
			} else {
				builder.append("Expected call found in code and moved to the right location");
			}
		} else {
			for (CrySLMethodCall call : patch) {
				builder.append("\t" + call.getSootMethod() + "\n");
			}
		}
		builder.append("\n");
		builder.append("__________________________________________");
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
