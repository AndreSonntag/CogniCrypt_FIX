package de.upb.cognicryptfix.scheduler;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Collator;
import java.time.Duration;
import java.time.Instant;
import java.util.AbstractMap.SimpleEntry;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import boomerang.callgraph.ObservableICFG;
import boomerang.jimple.Statement;
import crypto.analysis.errors.AbstractError;
import crypto.analysis.errors.ConstraintError;
import crypto.analysis.errors.ForbiddenMethodError;
import crypto.analysis.errors.HardCodedError;
import crypto.analysis.errors.IncompleteOperationError;
import crypto.analysis.errors.NeverTypeOfError;
import crypto.analysis.errors.RequiredPredicateError;
import crypto.analysis.errors.TypestateError;
import crypto.extractparameter.ExtractedValue;
import crypto.interfaces.ISLConstraint;
import crypto.rules.CrySLComparisonConstraint;
import crypto.rules.CrySLPredicate;
import crypto.rules.CrySLValueConstraint;
import de.upb.cognicryptfix.analysis.CryptoAnalysis;
import de.upb.cognicryptfix.exception.patch.RepairException;
import de.upb.cognicryptfix.generator.jimple.JimpleUtils;
import de.upb.cognicryptfix.patcher.IPatcher;
import de.upb.cognicryptfix.patcher.JimplePatcher;
import de.upb.cognicryptfix.utils.BoomerangUtils;
import de.upb.cognicryptfix.utils.Utils;
import soot.Local;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.AssignStmt;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;

public class ErrorScheduler {

	private static final Logger LOGGER = LogManager.getLogger(ErrorScheduler.class);
	private static ErrorScheduler instance;
	private IPatcher patcher;
	private List<Entry<Integer, Long>> timePerRound;
	public static int round = 1;

	private Map<Integer, Map<Class, Set<Entry<String, AbstractError>>>> roundHistoryMap;
	private Map<Class, Set<Entry<String, AbstractError>>> initialErrors;
	private Map<Class, Set<Entry<String, AbstractError>>> actualRound;
	private Map<Class, Set<Entry<String, AbstractError>>> previousRound;
	private List<Set<Entry<String, AbstractError>>> reqPredicateRounds;

	private Set<Entry<String, AbstractError>> notRepairableErrors;
	private Map<String, String> notrepairableErrorReasons;

	private ErrorScheduler() {
		this.patcher = JimplePatcher.getInstance();
		this.initialErrors = Maps.newHashMap();
		this.roundHistoryMap = Maps.newHashMap();
		this.notRepairableErrors = Sets.newHashSet();
		this.notrepairableErrorReasons = Maps.newHashMap();
		this.timePerRound = Lists.newArrayList();
		this.reqPredicateRounds = Lists.newArrayList();
		this.previousRound = Maps.newHashMap();
		initActualRound();
	}

	public static ErrorScheduler getInstance() {
		if (ErrorScheduler.instance == null) {
			ErrorScheduler.instance = new ErrorScheduler();
		}
		return ErrorScheduler.instance;
	}

	public boolean run() {

		if (round == 1) {
			LOGGER.info("All errors of the FIRST round:\n" + printRound(actualRound, false));
		}
		List<Entry<String, AbstractError>> removedReqPredErros = removeConnectedRequiredPredicateErrors();
		
		if (round == 1) {
			LOGGER.info("Removed ReqPredErrors:\n" + printErrors(removedReqPredErros, true));
		}
		
		roundHistoryMap.put(round, actualRound);
		LinkedHashSet<Entry<String, AbstractError>> orderedErrors = orderErrors();
		
		if (!orderedErrors.isEmpty()) {
			LOGGER.info("Run CogniCrypt_Fix Repair Round: " + round);
			Instant start = Instant.now();

			for (Entry<String, AbstractError> error : orderedErrors) {
				try {
					patcher.getPatchedClass(error.getValue());
				} catch (RepairException e) {
					LOGGER.error("Round: " + round + " Error: " + error.toString() + " couldn't repaired! - " + e.getMessage());
					notRepairableErrors.add(error);
					notrepairableErrorReasons.put(error.getKey(), e.getMessage());
				}
			}
			
			previousRound = Maps.newHashMap(actualRound);
			initActualRound();
			Instant finish = Instant.now();
			long timeElapsed = Duration.between(start, finish).toMillis();
			LOGGER.info("Repair phase took " + timeElapsed + " milliseconds!");
			timePerRound.add(new SimpleEntry(round, timeElapsed));
			round++;
			return true;
		} else {
			LOGGER.info("Repair phase done!");
			if (isEmpty(roundHistoryMap.get(round)) && notRepairableErrors.isEmpty()) {
				LOGGER.info("All detected errors were repaired!");
			} else {
				LOGGER.info("Not all detected errors could be repaired:\n" + getNotRepariableErrorString());
			}

			LOGGER.info(getRequiredTimeString());
			return false;
		}
	}


	private LinkedHashSet<Entry<String, AbstractError>> orderErrors() {
		LinkedHashSet<Entry<String, AbstractError>> order = Sets.newLinkedHashSet();
		Set<Entry<String, AbstractError>> compValueError = actualRound.get(ConstraintError.class);
		Set<Entry<String, AbstractError>> nTypeOfError = actualRound.get(NeverTypeOfError.class);
		Set<Entry<String, AbstractError>> hardCodedError = actualRound.get(HardCodedError.class);
		Set<Entry<String, AbstractError>> fMethodError = actualRound.get(ForbiddenMethodError.class);
		Set<Entry<String, AbstractError>> incompleteError = actualRound.get(IncompleteOperationError.class);
		Set<Entry<String, AbstractError>> typeStateError = actualRound.get(TypestateError.class);
		Set<Entry<String, AbstractError>> reqPredicateError = actualRound.get(RequiredPredicateError.class);

		if(!previousRound.isEmpty()) {	//check for first round
			compValueError = removeAlreadyDetectedErrors(previousRound.get(ConstraintError.class), compValueError);
			fMethodError = removeAlreadyDetectedErrors(previousRound.get(ForbiddenMethodError.class), fMethodError);
			nTypeOfError = removeAlreadyDetectedErrors(previousRound.get(NeverTypeOfError.class), nTypeOfError);
			hardCodedError = removeAlreadyDetectedErrors(previousRound.get(HardCodedError.class), hardCodedError);
			typeStateError = removeAlreadyDetectedErrors(previousRound.get(TypestateError.class), typeStateError);
			incompleteError = removeAlreadyDetectedErrors(previousRound.get(IncompleteOperationError.class),incompleteError);
		}
		
		if (!compValueError.isEmpty() || !fMethodError.isEmpty() || !nTypeOfError.isEmpty() || !hardCodedError.isEmpty() || !incompleteError.isEmpty() || !typeStateError.isEmpty()) {
			order.addAll(compValueError);
			order.addAll(nTypeOfError);
			order.addAll(hardCodedError);
			order.addAll(fMethodError);
			order.addAll(incompleteError);
			order.addAll(typeStateError);	
			
		} else if(!reqPredicateError.isEmpty()) {
			if(reqPredicateRounds.isEmpty()) {	//First RequiredPredicateErrorRound
				reqPredicateRounds.add(reqPredicateError);
				order.addAll(reqPredicateError);
			} else {
				reqPredicateError = removeAlreadyDetectedErrors(reqPredicateRounds.get(reqPredicateRounds.size()-1),reqPredicateError);
				reqPredicateRounds.add(reqPredicateError);
				order.addAll(reqPredicateError);
			}
		}
		return order;
	}

	private Set<Entry<String, AbstractError>> removeAlreadyDetectedErrors(
			Set<Entry<String, AbstractError>> previousRound, Set<Entry<String, AbstractError>> actualRound) {

		if(previousRound.isEmpty() || actualRound.isEmpty()) {
			return actualRound;
		}
		
		List<Entry<String, AbstractError>> previousRoundList = Lists.newArrayList(previousRound);
		List<Entry<String, AbstractError>> currentRoundList = Lists.newArrayList(actualRound);

		
		if (containsAlreadyDetectedErrors(previousRound, actualRound)) {
			Set<Entry<String, AbstractError>> intersections = getAlreadyDetectedErrors(previousRound, actualRound);			
			actualRound.removeAll(intersections);
			
			for(Entry<String, AbstractError> duplicate : intersections) {
				notRepairableErrors.add(duplicate);	
				notrepairableErrorReasons.put(duplicate.getKey(), "already detected!");
			}
		}		
	return actualRound;

	}

	private boolean containsAlreadyDetectedErrors(Set<Entry<String, AbstractError>> previousRound,
			Set<Entry<String, AbstractError>> actualRound) {
		Set<String> actualRoundSet = Sets.newHashSet();
		Set<String> prevRoundSet = Sets.newHashSet();

		for (Entry<String, AbstractError> entry : actualRound) {
			actualRoundSet.add(entry.getKey());
		}
		for (Entry<String, AbstractError> entry : previousRound) {
			prevRoundSet.add(entry.getKey());
		}
		actualRoundSet.retainAll(prevRoundSet);	
		return actualRoundSet.size() > 0;
	}

	
	private Set<Entry<String, AbstractError>> getAlreadyDetectedErrors(Set<Entry<String, AbstractError>> previousRound,
			Set<Entry<String, AbstractError>> actualRound) {

		Map<String, Entry<String, AbstractError>> hashEntryMap = Maps.newHashMap();
		Set<Entry<String, AbstractError>> intersection = Sets.newHashSet();
		Set<String> a = Sets.newHashSet();
		Set<String> b = Sets.newHashSet();

		for (Entry<String, AbstractError> entry : actualRound) {
			a.add(entry.getKey());
			hashEntryMap.put(entry.getKey(), entry);
		}
		for (Entry<String, AbstractError> entry : previousRound) {
			b.add(entry.getKey());
			hashEntryMap.put(entry.getKey(), entry);
		}

		a.retainAll(b);
		for (String hash : a) {
			intersection.add(hashEntryMap.get(hash));
		}
		return intersection;
	}

	public void add(AbstractError error) {
		String hash = createErrorHash(error);

		if (isNotRepariableError(hash)) {
			return;
		}

		if (error instanceof ConstraintError
				&& !(((ConstraintError) error).getBrokenConstraint() instanceof CrySLPredicate)) {
			actualRound.get(ConstraintError.class).add(new SimpleEntry<String, AbstractError>(hash, error));
		} else if (error instanceof NeverTypeOfError) {
			actualRound.get(NeverTypeOfError.class).add(new SimpleEntry<String, AbstractError>(hash, error));
		} else if (error instanceof HardCodedError) {
			actualRound.get(HardCodedError.class).add(new SimpleEntry<String, AbstractError>(hash, error));
		} else if (error instanceof ForbiddenMethodError) {
			actualRound.get(ForbiddenMethodError.class).add(new SimpleEntry<String, AbstractError>(hash, error));
		} else if (error instanceof ConstraintError
				&& !(((ConstraintError) error).getBrokenConstraint() instanceof CrySLPredicate)) {
			actualRound.get(ConstraintError.class).add(new SimpleEntry<String, AbstractError>(hash, error));
		} else if (error instanceof IncompleteOperationError) {
			actualRound.get(IncompleteOperationError.class).add(new SimpleEntry<String, AbstractError>(hash, error));
		} else if (error instanceof TypestateError) {
			actualRound.get(TypestateError.class).add(new SimpleEntry<String, AbstractError>(hash, error));
		} else if (error instanceof RequiredPredicateError) {
			actualRound.get(RequiredPredicateError.class).add(new SimpleEntry<String, AbstractError>(hash, error));
		} else {

		}
	}

	public void addAll(List<AbstractError> errors) {
		for (AbstractError error : errors) {
			add(error);
		}
	}

	private String createErrorHash(AbstractError error) {
		
		String decClass = error.getErrorLocation().getMethod().getDeclaringClass().toString();
		String outerMethod = error.getErrorLocation().getMethod().getSignature();
		String clazz = error.getClass().getSimpleName();
		String rule = error.getRule().getClassName();
		
		StringBuilder builder = new StringBuilder();
		builder.append(decClass + ",");
		builder.append(outerMethod + ",");
		builder.append(clazz + ",");
		builder.append(rule + ",");
		
		
		Unit u = error.getErrorLocation().getUnit().get();
		String expr = "";

		if (u instanceof InvokeStmt || u instanceof AssignStmt) {
			if (JimpleUtils.containsInvokeExpr(u)) {
				InvokeExpr invoke = JimpleUtils.getInvokeExpr(u);			
				expr = error.getErrorLocation().getUnit().get().getInvokeExpr().getMethod().getSignature();
				
				String var = JimpleUtils.getInvokeLocal(u).getName();
				if(var.contains("$stack") || var.contains("varReplacer") || var.startsWith("$")) {
				} else {
					expr += var;
				}
				builder.append(expr + ",");
			}
		} 

		String errorType = "";
		if (error instanceof NeverTypeOfError) {
			errorType = "NeverTypeOfError";
		} else if (error instanceof HardCodedError) {
			errorType = "HardCodedError";
		} else if (error instanceof ForbiddenMethodError) {
			errorType = "ForbiddenMethodError";
		} else if (error instanceof ConstraintError) {
			ConstraintError conError = (ConstraintError) error;
			ISLConstraint constraint = conError.getBrokenConstraint();
			
			if(constraint instanceof CrySLValueConstraint) {
				List<String> values = ((CrySLValueConstraint) constraint).getValueRange();
				String concatenate = "";
				Collections.sort(values, Collator.getInstance());
				for (String method : values) {
					concatenate += method;
				}
				builder.append(concatenate + ",");
			}
			
			
			errorType = "ConstraintError";
		} else if (error instanceof TypestateError) {
			errorType = "TypestateError";
			TypestateError tsError = (TypestateError) error;
			List<String> expectedMethods = Lists.newArrayList();
			for (SootMethod method : tsError.getExpectedMethodCalls()) {
				expectedMethods.add(method.getSignature());
			}

			Collections.sort(expectedMethods, Collator.getInstance());
			String concatenate = "";
			for (String method : expectedMethods) {
				concatenate += method;
			}
			builder.append(concatenate + ",");

		} else if (error instanceof IncompleteOperationError) {
			errorType = "IncompleteOperationError";
			IncompleteOperationError ioError = (IncompleteOperationError) error;
			List<String> expectedMethods = Lists.newArrayList();

			for (SootMethod method : ioError.getExpectedMethodCalls()) {
				expectedMethods.add(method.getSignature());
			}

			String concatenate = "";
			Collections.sort(expectedMethods, Collator.getInstance());
			for (String method : expectedMethods) {
				concatenate += method;
			}
			
			builder.append(concatenate + ",");
		} else if (error instanceof RequiredPredicateError) {
			errorType = "RequiredPredicateError";
			RequiredPredicateError reError = (RequiredPredicateError) error;

			String predicate = reError.getContradictedPredicate().getPredName();
			String index = reError.getExtractedValues().getCallSite().getIndex()+"";
			builder.append(predicate + ",");
			builder.append(index + ",");
		}
		builder.append(errorType);

		MessageDigest messageDigest = null;
		String builderString = builder.toString();
		String hashString = "";
		try {
			messageDigest = MessageDigest.getInstance("SHA-256");
			messageDigest.update(builder.toString().getBytes());
			hashString = bytesToHex(messageDigest.digest());
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}

		if (hashString.length() > 0) {
			return hashString;
		} else {
			throw new RuntimeException("Hash for AbstractError is empty!");
		}
	}

	private String bytesToHex(byte[] bytes) {
		StringBuilder sb = new StringBuilder();
		for (byte b : bytes)
			sb.append(String.format("%02x", b));
		return sb.toString();
	}

	private boolean isEmpty(Map<Class, Set<Entry<String, AbstractError>>> errorMap) {
		for (Set<Entry<String, AbstractError>> set : errorMap.values()) {
			if (!set.isEmpty()) {
				return false;
			}
		}
		return true;
	}

	private String printRound(Map<Class, Set<Entry<String, AbstractError>>> round, boolean header) {
		StringBuilder builder = new StringBuilder();
		for (Set<Entry<String, AbstractError>> set : round.values()) {
			if (!set.isEmpty()) {
				for (Entry<String, AbstractError> entry : set) {
					String errorString = Utils.printErrorInformation(entry.getValue(),header);
					builder.append(errorString);
				}

			}
		}
		return builder.toString();
	}
	
	private String printErrors(List<Entry<String, AbstractError>> errors, boolean header) {
		StringBuilder builder = new StringBuilder();
			for (Entry<String, AbstractError> entry : errors) {
				String errorString = Utils.printErrorInformation(entry.getValue(),header);
				builder.append(errorString);
			}
		return builder.toString();
	}

	private String getNotRepariableErrorString() {
		StringBuilder builder = new StringBuilder();
		for (Set<Entry<String, AbstractError>> set : roundHistoryMap.get(round).values()) {
			if (!set.isEmpty()) {
				for (Entry<String, AbstractError> entry : set) {
					String outerMethod = entry.getValue().getErrorLocation().getMethod().getSignature();
					String clazz = entry.getValue().getClass().getSimpleName();

					builder.append("Hash: " + entry.getKey() + "\n");
					builder.append(clazz + ":\n");
					builder.append(outerMethod + "\n");
					builder.append(entry.getValue().getErrorLocation().getUnit().get() + "\n");
					builder.append(entry.getValue() + "\n\n");
				}

			}
		}
		for (Entry<String, AbstractError> entry : notRepairableErrors) {
			String outerMethod = entry.getValue().getErrorLocation().getMethod().getSignature();
			String clazz = entry.getValue().getClass().getSimpleName();
			builder.append("Hash: " + entry.getKey() + "\n");
			builder.append(clazz + ":\n");
			builder.append(outerMethod + "\n");
			builder.append(entry.getValue() + "\n");
			builder.append(notrepairableErrorReasons.get(entry.getKey()) + "\n\n");
		}
		return builder.toString();
	}

	private String getRequiredTimeString() {
		StringBuilder builder = new StringBuilder();
		for (Entry<Integer, Long> round : timePerRound) {
			builder.append("\nRound " + round.getKey() + " required " + round.getValue() + " milliseconds!");
		}
		return builder.toString();
	}

	private boolean isNotRepariableError(String hash) {
		for (Entry<String, AbstractError> notRepairedError : notRepairableErrors) {
			if (hash.equals(notRepairedError.getKey())) {
				return true;
			}
		}
		return false;
	}

	private void initActualRound() {
		this.actualRound = Maps.newHashMap();
		this.actualRound.put(ConstraintError.class, Sets.newHashSet());
		this.actualRound.put(ForbiddenMethodError.class, Sets.newHashSet());
		this.actualRound.put(NeverTypeOfError.class, Sets.newHashSet());
		this.actualRound.put(IncompleteOperationError.class, Sets.newHashSet());
		this.actualRound.put(TypestateError.class, Sets.newHashSet());
		this.actualRound.put(RequiredPredicateError.class, Sets.newHashSet());
		this.actualRound.put(HardCodedError.class, Sets.newHashSet());
	}

	public Map<Integer, Map<Class, Set<Entry<String, AbstractError>>>> getRoundHistoryMap() {
		return roundHistoryMap;
	}

	public Map<Class, Set<Entry<String, AbstractError>>> getInitiallyErrors() {
		return initialErrors;
	}

	private List<Entry<String, AbstractError>> removeConnectedRequiredPredicateErrors() {
		List<Entry<String, AbstractError>> removedErrors = Lists.newArrayList();

		List<Entry<String, AbstractError>> requiredPredicateErrorList = Lists
				.newArrayList(actualRound.get(RequiredPredicateError.class));
		if (requiredPredicateErrorList.size() > 1) {

			Map<String, List<RequiredPredicateError>> methodErrorMap = Maps.newHashMap();
			for (Entry<String, AbstractError> entry : requiredPredicateErrorList) {
				RequiredPredicateError reqPredError = (RequiredPredicateError) entry.getValue();
				String errorLocationMethod = reqPredError.getErrorLocation().getMethod().getSignature();
				if (methodErrorMap.containsKey(errorLocationMethod)) {
					methodErrorMap.get(errorLocationMethod).add(reqPredError);
				} else {
					methodErrorMap.put(errorLocationMethod, Lists.newArrayList(reqPredError));
				}
			}

			for (String errorLocation : methodErrorMap.keySet()) {
				
				List<RequiredPredicateError> reqPredErrorList = methodErrorMap.get(errorLocation);
				if (reqPredErrorList.size() > 1) {
					for (RequiredPredicateError reqPredError : reqPredErrorList) {
						ObservableICFG<Unit, SootMethod> icfg = CryptoAnalysis.getCryptoScanner().icfg();
						Statement errorStmt = reqPredError.getExtractedValues().getCallSite().stmt();
						Unit errorUnit = reqPredError.getErrorLocation().getUnit().get();

						if (!(reqPredError.getExtractedValues().getCallSite().fact().value() instanceof Local)) {
							continue;
						}
						Local l = (Local) reqPredError.getExtractedValues().getCallSite().fact().value();
						List<ExtractedValue> evList = BoomerangUtils.runBommerang(icfg, l, errorStmt, errorUnit);
						for (RequiredPredicateError otherReqPredError : reqPredErrorList) {
							Unit otherErrorUnit = otherReqPredError.getErrorLocation().getUnit().get();
							for (ExtractedValue ev : evList) {
								if (otherErrorUnit == ev.stmt().getUnit().get()) {
									for (Entry<String, AbstractError> originalEntry : requiredPredicateErrorList) {
										if (originalEntry.getKey().equals(createErrorHash(reqPredError))) {
											actualRound.get(RequiredPredicateError.class).remove(originalEntry);
											notRepairableErrors.add(originalEntry);
											removedErrors.add(originalEntry);
											notrepairableErrorReasons.put(originalEntry.getKey(), "sequenceError");
										}
									}
								}
							}
						}
					}
				}
			}

		}
		return removedErrors;
	}
}
