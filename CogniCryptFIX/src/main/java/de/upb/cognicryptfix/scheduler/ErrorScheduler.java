package de.upb.cognicryptfix.scheduler;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.AbstractMap.SimpleEntry;
import java.util.HashSet;
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

import crypto.analysis.errors.AbstractError;
import crypto.analysis.errors.ConstraintError;
import crypto.analysis.errors.ForbiddenMethodError;
import crypto.analysis.errors.HardCodedError;
import crypto.analysis.errors.IncompleteOperationError;
import crypto.analysis.errors.NeverTypeOfError;
import crypto.analysis.errors.RequiredPredicateError;
import crypto.analysis.errors.TypestateError;
import crypto.rules.CrySLPredicate;
import de.upb.cognicryptfix.exception.patch.RepairException;
import de.upb.cognicryptfix.patcher.IPatcher;
import de.upb.cognicryptfix.patcher.JimplePatcher;

public class ErrorScheduler {
	
	private static final Logger logger = LogManager.getLogger(ErrorScheduler.class.getSimpleName());
	private static ErrorScheduler instance;
	private IPatcher patcher;
	private Map<Integer, Map<Class, Set<Entry<String, AbstractError>>>> roundHistoryMap;
	private Map<Class, Set<Entry<String, AbstractError>>> actualRound;
	private Map<Class, Set<Entry<String, AbstractError>>> previousRound;
	private HashSet<Entry<String, AbstractError>> unsolvableErros;
	public static int round = 1;
	private boolean reqPredicateRound;

	private ErrorScheduler() {
		this.patcher = JimplePatcher.getInstance();		
		this.roundHistoryMap = Maps.newHashMap();
		initActualRound();
		unsolvableErros = Sets.newHashSet();
		reqPredicateRound = false;
	}
	
	public static ErrorScheduler getInstance() {
		if (ErrorScheduler.instance == null) {
			ErrorScheduler.instance = new ErrorScheduler();
		}
		return ErrorScheduler.instance;
	}
	
	public boolean run(){	
		roundHistoryMap.put(round, actualRound);
		LinkedHashSet<Entry<String, AbstractError>> orderedErrors = orderErrors();
		if(!orderedErrors.isEmpty()) {			
			logger.info("Run CogniCrypt_Fix Repair Round: "+round++);
			Instant start = Instant.now();
			
			for(Entry<String, AbstractError> error : orderedErrors) {
				run(error.getValue());
			}
			previousRound = Maps.newHashMap(actualRound);
			initActualRound();
			Instant finish = Instant.now();
			long timeElapsed = Duration.between(start, finish).toMillis();
			logger.info("Repair phase took "+timeElapsed+" milliseconds!");
			return true;
		} else {
			logger.info("Repair phase done!");			
			if(isEmpty(roundHistoryMap.get(round))) {
				logger.info("All detected errors were repaired!");
			} else {
				logger.info("Not all detected errors could be repaired: "+ actualRound.toString());
			}
			return false;
		}
	}
		
	private void run(AbstractError error) {
		try {
			patcher.getPatchedClass(error);
		} catch (RepairException e) {
			logger.error("Error: "+ error.toString()+ "couldn't repaired!", e);
		}
	}	
	
	private LinkedHashSet<Entry<String, AbstractError>> getFirstRunErrorOrder(){
		LinkedHashSet<Entry<String, AbstractError>> order = Sets.newLinkedHashSet();
		Set<Entry<String, AbstractError>> compValueError = actualRound.get(ConstraintError.class);
		Set<Entry<String, AbstractError>> nTypeOfError = actualRound.get(NeverTypeOfError.class);
		Set<Entry<String, AbstractError>> hardCodedError = actualRound.get(HardCodedError.class);
		Set<Entry<String, AbstractError>> fMethodError = actualRound.get(ForbiddenMethodError.class);
		Set<Entry<String, AbstractError>> incompleteError = actualRound.get(IncompleteOperationError.class);
		Set<Entry<String, AbstractError>> typeStateError = actualRound.get(TypestateError.class);

		if(!compValueError.isEmpty() || !fMethodError.isEmpty() || !nTypeOfError.isEmpty() || !hardCodedError.isEmpty() || !incompleteError.isEmpty() || !typeStateError.isEmpty()) {
			order.addAll(compValueError);
			order.addAll(nTypeOfError);
			order.addAll(hardCodedError);
			order.addAll(fMethodError);
			order.addAll(incompleteError);
			order.addAll(typeStateError);
			actualRound.put(RequiredPredicateError.class, Sets.newHashSet());
		} else {
			Set<Entry<String, AbstractError>> reqPredicateError = actualRound.get(RequiredPredicateError.class);
			order.addAll(reqPredicateError);
		}
		return order;
	}
	
	private LinkedHashSet<Entry<String, AbstractError>> getRunErrorOrder(){
		LinkedHashSet<Entry<String, AbstractError>> order = Sets.newLinkedHashSet();

		Set<Entry<String, AbstractError>> compValueError = actualRound.get(ConstraintError.class);
		Set<Entry<String, AbstractError>> nTypeOfError = actualRound.get(NeverTypeOfError.class);
		Set<Entry<String, AbstractError>> hardCodedError = actualRound.get(HardCodedError.class);
		Set<Entry<String, AbstractError>> fMethodError = actualRound.get(ForbiddenMethodError.class);
		Set<Entry<String, AbstractError>> incompleteError = actualRound.get(IncompleteOperationError.class);
		Set<Entry<String, AbstractError>> typeStateError = actualRound.get(TypestateError.class);
		Set<Entry<String, AbstractError>> reqPredicateError = actualRound.get(RequiredPredicateError.class);

		if(reqPredicateRound) {
			if(!reqPredicateError.isEmpty()) {
				//moves recurring error to unsolvable errors 
				reqPredicateError = removeAlreadyDetectedErrors(previousRound.get(RequiredPredicateError.class), reqPredicateError);
			}
		}
		
		compValueError = removeAlreadyDetectedErrors(previousRound.get(ConstraintError.class), compValueError);
		fMethodError = removeAlreadyDetectedErrors(previousRound.get(ForbiddenMethodError.class), fMethodError);
		nTypeOfError = removeAlreadyDetectedErrors(previousRound.get(NeverTypeOfError.class), nTypeOfError);
		hardCodedError = removeAlreadyDetectedErrors(previousRound.get(HardCodedError.class), hardCodedError);
		typeStateError = removeAlreadyDetectedErrors(previousRound.get(TypestateError.class), typeStateError);
		incompleteError = removeAlreadyDetectedErrors(previousRound.get(IncompleteOperationError.class), incompleteError);
		
		if(!compValueError.isEmpty() || !nTypeOfError.isEmpty() || !hardCodedError.isEmpty() || !fMethodError.isEmpty() || !typeStateError.isEmpty() || !incompleteError.isEmpty()) {
			order.addAll(compValueError);
			order.addAll(nTypeOfError);
			order.addAll(hardCodedError);
			order.addAll(fMethodError);
			order.addAll(incompleteError);
			order.addAll(typeStateError);
			actualRound.put(RequiredPredicateError.class, Sets.newHashSet());
			reqPredicateRound = false;
		} else {				
			order.addAll(reqPredicateError);
			reqPredicateRound = true;
		}
		return order;
	}
 	
	private LinkedHashSet<Entry<String, AbstractError>> orderErrors(){
		LinkedHashSet<Entry<String, AbstractError>> order = Sets.newLinkedHashSet();

		if(previousRound == null) {
			order = getFirstRunErrorOrder();
		} else {
			order = getRunErrorOrder();
		}	
		return order;
	}

	private Set<Entry<String, AbstractError>> removeAlreadyDetectedErrors (Set<Entry<String, AbstractError>> previousRound, Set<Entry<String, AbstractError>> actualRound){
		
		if(containsAlreadyDetectedErrors(previousRound, actualRound)) {
			Set<Entry<String, AbstractError>> intersections = getAlreadyDetectedErrors(previousRound, actualRound);
			unsolvableErros.addAll(intersections);
			actualRound.removeAll(intersections);
		}
		
		return actualRound;
	}
	
	private boolean containsAlreadyDetectedErrors(Set<Entry<String, AbstractError>> previousRound, Set<Entry<String, AbstractError>> actualRound){
		Set<String> a = Sets.newHashSet();
		Set<String> b = Sets.newHashSet();
		
		for(Entry<String, AbstractError> entry : actualRound) {
			a.add(entry.getKey());
		}
		for(Entry<String, AbstractError> entry : previousRound) {
			b.add(entry.getKey());
		}
		return !a.retainAll(b);
	}
	
	private Set<Entry<String, AbstractError>> getAlreadyDetectedErrors(Set<Entry<String, AbstractError>> previousRound, Set<Entry<String, AbstractError>> actualRound){
		
		Map<String, Entry<String, AbstractError>> hashEntryMap = Maps.newHashMap();
		Set<Entry<String, AbstractError>> intersection = Sets.newHashSet();
		Set<String> a = Sets.newHashSet();
		Set<String> b = Sets.newHashSet();

		for(Entry<String, AbstractError> entry : actualRound) {
			a.add(entry.getKey());
			hashEntryMap.put(entry.getKey(), entry);
		}
		for(Entry<String, AbstractError> entry : previousRound) {
			b.add(entry.getKey());
			hashEntryMap.put(entry.getKey(), entry);
		}
		
		a.retainAll(b);
		for(String hash : a) {
			intersection.add(hashEntryMap.get(hash));
		}
		return intersection;
	}
	
	public void add(AbstractError error) {	
		String hash = createErrorHash(error);
		
		if (error instanceof ConstraintError && !(((ConstraintError) error).getBrokenConstraint() instanceof CrySLPredicate)) {
			actualRound.get(ConstraintError.class).add(new SimpleEntry<String, AbstractError>(hash, error));
		} else if(error instanceof NeverTypeOfError) {
			actualRound.get(NeverTypeOfError.class).add(new SimpleEntry<String, AbstractError>(hash, error));
		} else if(error instanceof HardCodedError) {
			actualRound.get(HardCodedError.class).add(new SimpleEntry<String, AbstractError>(hash, error));
		}else if (error instanceof ForbiddenMethodError) {
			actualRound.get(ForbiddenMethodError.class).add(new SimpleEntry<String, AbstractError>(hash, error));
		}else if (error instanceof ConstraintError && !(((ConstraintError) error).getBrokenConstraint() instanceof CrySLPredicate)) {
			actualRound.get(ConstraintError.class).add(new SimpleEntry<String, AbstractError>(hash, error));
		} else if (error instanceof IncompleteOperationError) {
			actualRound.get(IncompleteOperationError.class).add(new SimpleEntry<String, AbstractError>(hash, error));
		} else if (error instanceof TypestateError) {
			actualRound.get(TypestateError.class).add(new SimpleEntry<String, AbstractError>(hash, error));
		} else if (error instanceof RequiredPredicateError) {
			actualRound.get(RequiredPredicateError.class).add(new SimpleEntry<String, AbstractError>(hash, error));	
		} else {
			unsolvableErros.add(new SimpleEntry<String, AbstractError>(hash, error));
		}
	}
	
	public void addAll(List<AbstractError> errors) {
		for(AbstractError error : errors) {
			add(error);
		}
	}
	
	private String createErrorHash(AbstractError error) {
		String decClass = error.getErrorLocation().getMethod().getDeclaringClass().toString();
		String outerMethod = error.getErrorLocation().getMethod().getSignature();
		String clazz = error.getClass().getSimpleName();
		String rule = error.getRule().getClassName();
		String message = error.toErrorMarkerString();
		String unit = error.getErrorLocation().getUnit().get().containsInvokeExpr()+"";
		
		StringBuilder builder = new StringBuilder();
		builder.append(decClass+",");
		builder.append(outerMethod+",");
		builder.append(clazz+",");
		builder.append(rule+",");
		builder.append(message+",");
		builder.append(unit+",");
		
		MessageDigest messageDigest = null;
		String hashString = "";
		try {
			messageDigest = MessageDigest.getInstance("SHA-256");
			messageDigest.update(builder.toString().getBytes());
			hashString = bytesToHex(messageDigest.digest());
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		
		if(hashString.length() > 0) {
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

	private boolean isUnsolvableError(AbstractError error) {
		String hash = createErrorHash(error);
		for(Entry<String, AbstractError> unsolveableError : unsolvableErros) {
			if(hash.equals(unsolveableError.getKey())) {
				return true;
			}
		}
		return false;
	}
	
	private Entry<String, AbstractError> getUnsolvableError(AbstractError error) {
		String hash = createErrorHash(error);

		for(Entry<String, AbstractError> unsolveableError : unsolvableErros) {
			if(hash.equals(unsolveableError.getKey())) {
				return unsolveableError;
			}
		}
		return null;
	}
	
	private boolean isEmpty(Map<Class, Set<Entry<String, AbstractError>>> errorMap) {
		for(Set<Entry<String, AbstractError>> set : errorMap.values()) {
			if(!set.isEmpty()) {
				return false;
			}
		}
		return true;
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

}


