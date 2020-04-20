package de.upb.cognicryptfix.patcher.patches;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.Lists;

import crypto.analysis.errors.IncompleteOperationError;
import de.upb.cognicryptfix.crysl.CrySLEntity;
import de.upb.cognicryptfix.crysl.CrySLMethodCall;
import de.upb.cognicryptfix.crysl.pool.CrySLEntityPool;
import de.upb.cognicryptfix.exception.patch.RepairException;
import de.upb.cognicryptfix.exception.path.PathException;
import de.upb.cognicryptfix.generator.JimpleCodeGeneratorByRule;
import de.upb.cognicryptfix.generator.jimple.JimpleUtils;
import de.upb.cognicryptfix.utils.Utils;
import soot.Body;
import soot.Local;
import soot.SootMethod;
import soot.Unit;

public class IncompleteOperationPatch extends AbstractPatch {

	private static final Logger LOGGER = LogManager.getLogger(IncompleteOperationPatch.class);

	private IncompleteOperationError error;

	private CrySLEntity entity;
	private Body body;
	private JimpleCodeGeneratorByRule generator;
	private List<CrySLMethodCall> patch;

	/**
	 * Creates a new {@link IncompleteOperationPatch} object that represents the
	 * patch for the {@link IncompleteOperationError} argument.
	 * 
	 * @param error the error
	 */
	public IncompleteOperationPatch(IncompleteOperationError error) {
		this.error = error;
		this.entity = CrySLEntityPool.getInstance().getEntityByClassName(error.getRule().getClassName());
		this.body = error.getErrorLocation().getMethod().getActiveBody();
		this.generator = new JimpleCodeGeneratorByRule(body);
		this.patch = Lists.newArrayList();
	}

	@Override
	public Body applyPatch() throws RepairException{		
		List<SootMethod> expectedCalls = Lists.newArrayList(error.getExpectedMethodCalls());
		Unit unit = error.getErrorLocation().getUnit().get();
		generateCalls(unit, Lists.newArrayList(expectedCalls));
		return body;
	}

	public void generateCalls(Unit indexUnit, List<SootMethod> expectedCalls) throws PathException {
		List<Unit> generatedUnits = Lists.newArrayList();

		Local invokeVariable = JimpleUtils.getInvokeLocal(indexUnit);
		List<LinkedList<CrySLMethodCall>> pathsToNextFinalState = entity.getFSM().calcBestPathsToFinalStatesIncludeMultipleMethod(expectedCalls);
		List<CrySLMethodCall> path = pathsToNextFinalState.get(0);

		LOGGER.debug("Path to generate: "+path.toString());
		for (CrySLMethodCall call : path) {
			Map<Local, List<Unit>> generatedCallUnits = generator.generateCallWithParameter(invokeVariable, call, true);
			generatedUnits.addAll(Utils.summarizeUnitLists(generatedCallUnits.values()));
		}

		body.getUnits().insertAfter(generatedUnits, indexUnit);
		patch.addAll(path);
	}

	@Override
	public String toPatchString() {
		StringBuilder builder = new StringBuilder();
		builder.append("\n__________[IncompleteOperationPatch]__________\n");
		builder.append("Class: \t\t" + error.getErrorLocation().getMethod().getDeclaringClass().toString() + "\n");
		builder.append("Method: \t" + error.getErrorLocation().getMethod().getSignature() + "\n");
		builder.append("Error: \t\t" + error.getClass().getSimpleName() + "\n");
		builder.append("CrySLRule: \t" + entity.getRule().getClassName() + "\n");
		builder.append("Message: \t" + error.toErrorMarkerString() + "\n");
		builder.append("Patch:\n");
		for (CrySLMethodCall call : patch) {
			builder.append("\t" + call.getSootMethod() + "\n");
		}
		builder.append("\n");
		builder.append("________________________________________\n");
		return builder.toString();
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("IncompleteOperationPatch [error=");
		builder.append(error);
		builder.append(", entity=");
		builder.append(entity.getRule().getClassName());
		builder.append("]");
		return builder.toString();
	}
}
