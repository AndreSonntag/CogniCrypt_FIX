package de.upb.cognicryptfix.crysl.fsm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import crypto.rules.CrySLMethod;
import crypto.rules.CrySLRule;
import crypto.rules.StateMachineGraph;
import crypto.rules.StateNode;
import crypto.rules.TransitionEdge;
import crypto.typestate.CrySLMethodToSootMethod;
import de.upb.cognicryptfix.crysl.CrySLEntity;
import de.upb.cognicryptfix.crysl.CrySLMethodCall;
import de.upb.cognicryptfix.crysl.CrySLMethodCallCriteria;
import de.upb.cognicryptfix.crysl.CrySLVariable;
import de.upb.cognicryptfix.crysl.pool.CrySLVariablePool;
import de.upb.cognicryptfix.exception.path.NoCallFoundException;
import de.upb.cognicryptfix.exception.path.PathException;
import de.upb.cognicryptfix.utils.Pair;
import soot.SootMethod;

/**
 * @author Andre Sonntag
 * @date 10.03.2020
 */
public class CrySLCallFSM {

	private static final Logger LOGGER = LogManager.getLogger(CrySLCallFSM.class);
	private CrySLEntity entity;
	private CrySLRule rule;
	private CrySLVariablePool varPool;
	private StateMachineGraph smg;
	private Set<CrySLMethodCallState> startStates;
	private Set<CrySLMethodCallState> finalStates;
	private List<CrySLMethodCallTransition> transitions;
	private List<CrySLMethodCallState> states;
	private Map<StateNode, CrySLMethodCallState> wrapperStateMap;
	private Map<String, Pair<CrySLMethodCallState, CrySLMethodCallState>> methodStateMap;
	private Map<String, CrySLMethodCall> sootCrySLCallMap;

	
	/**
	 * Creates a new {@link CrySLCallFSMTest} object that represents
	 * the FSM for the {@link CrySLEntity} argument. 
	 * 
	 * @param entity the entity
	 * @param pool the variable pool
	 */
	public CrySLCallFSM(CrySLEntity entity, CrySLVariablePool pool) {
		this.entity = entity;
		this.rule = entity.getRule();
		this.varPool = pool;
		this.smg = rule.getUsagePattern();
		this.wrapperStateMap = Maps.newLinkedHashMap();
		this.methodStateMap = Maps.newHashMap();
		this.sootCrySLCallMap = Maps.newHashMap();
		this.transitions = Lists.newArrayList();
		this.states = Lists.newArrayList();
		this.startStates = Sets.newHashSet();
		this.finalStates = Sets.newHashSet();
		createFSM();
	}

	private void createFSM() {
		List<TransitionEdge> edges_ = smg.getEdges();
		List<StateNode> states_ = new ArrayList<>(smg.getNodes());
		
		for (StateNode state : states_) {
			CrySLMethodCallState callState = null;
			if(state == smg.getInitialTransition().getLeft()) {
				callState = new CrySLMethodCallState(Integer.parseInt(state.getName()), true, false);
				startStates.add(callState);
			} else if (smg.getAcceptingStates().contains(state)) {
				callState = new CrySLMethodCallState(Integer.parseInt(state.getName()), false, true);
				finalStates.add(callState);
			} else {
				callState = new CrySLMethodCallState(Integer.parseInt(state.getName()), false, false);
			}
			wrapperStateMap.put(state, callState);
			states.add(callState);
		}

		for (TransitionEdge edge : edges_) {
			List<CrySLMethodCall> calls = transformTransitionEdgeToCrySLMethodCalls(edge);
			for (CrySLMethodCall call : calls) {

				CrySLMethodCallState from = wrapperStateMap.get(edge.getLeft());
				CrySLMethodCallState to = wrapperStateMap.get(edge.getRight());

				if (from.getState() != to.getState()) {
					CrySLMethodCallTransition transition = new CrySLMethodCallTransition(call, from, to);
					transitions.add(transition);
					from.addOutTransition(transition);
					to.addInTransition(transition);
					methodStateMap.put(call.getSootMethod().getSignature(), new Pair<CrySLMethodCallState, CrySLMethodCallState>(from, to));
					sootCrySLCallMap.put(call.getSootMethod().getSignature(), call);
				}
			}
		}
	}

	public List<LinkedList<CrySLMethodCall>> calcBestPathsForPredicateGenerationFromState(StateNode state, List<CrySLVariable> predicatePramameters) throws PathException {
		List<LinkedList<CrySLMethodCall>> calcBackwardPaths = Lists.newArrayList();
		List<LinkedList<CrySLMethodCall>> calcForwardPaths = Lists.newArrayList();
		List<LinkedList<CrySLMethodCall>> paths = Lists.newArrayList();

		CrySLMethodCallState currentState = wrapperStateMap.get(state);
		calcBackwardPaths = calcPathsBackward(currentState, Lists.newArrayList());
		
		for (LinkedList<CrySLMethodCall> p : calcBackwardPaths) {
			Collections.reverse(p);
		}

		if (!currentState.isFinalState()) {
			calcForwardPaths = calcPathsForward(currentState);
			for (LinkedList<CrySLMethodCall> bPath : calcBackwardPaths) {
				for (LinkedList<CrySLMethodCall> fPath : calcForwardPaths) {
					LinkedList<CrySLMethodCall> connectedPath = Lists.newLinkedList(bPath);
					connectedPath.addAll(fPath);
					paths.add(new LinkedList<CrySLMethodCall>(connectedPath));
				}
			}
		} else {
			paths = calcBackwardPaths;
		}

		paths = CrySLPathFilter.filterCallPathsByUsedPredicateParameters(paths, predicatePramameters);
		paths = CrySLPathFilter.applyPathCriteriaFilters(paths);
		return paths;
	}

	public List<LinkedList<CrySLMethodCall>> calcBestPathsForPredicateGenerationFromFinalStates(List<CrySLVariable> predicatePramameters) throws  PathException {
		List<LinkedList<CrySLMethodCall>> calcBackwardPaths = Lists.newArrayList();

		for (CrySLMethodCallState finalState : finalStates) {
			calcBackwardPaths.addAll(calcPathsBackward(finalState, Lists.newArrayList()));
		}
		
		for (LinkedList<CrySLMethodCall> p : calcBackwardPaths) {
			Collections.reverse(p);
		}
		
		if(calcBackwardPaths.isEmpty()) {
			System.out.println();
		}
		
		calcBackwardPaths = CrySLPathFilter.filterCallPathsByUsedPredicateParameters(calcBackwardPaths, predicatePramameters);
		
		if(calcBackwardPaths == null) {
			System.out.println();
		}
		
		calcBackwardPaths = CrySLPathFilter.applyPathCriteriaFilters(calcBackwardPaths);	
		return calcBackwardPaths;
	}
	
	public List<LinkedList<CrySLMethodCall>> calcBestPathsToFinalStatesIncludeMultipleMethod(List<SootMethod> methods) throws PathException {
		List<LinkedList<CrySLMethodCall>> calcPaths = Lists.newArrayList();
		Map<CrySLMethodCallState, List<SootMethod>> stateSootMethodMap = Maps.newHashMap();
		
		
		for(SootMethod method : methods) {
			CrySLMethodCallState targetState = getTargetStateFromMethod(method);
			
			if(targetState == null || targetState.isFinalState()) {
				LinkedList<CrySLMethodCall> tempPath = Lists.newLinkedList();
				tempPath.add(getCrySLMethodCallBySootMethod(method));
				calcPaths.add(tempPath);
			} else {
				if(stateSootMethodMap.containsKey(targetState)) {
					stateSootMethodMap.get(targetState).add(method);
				} else {
					stateSootMethodMap.put(targetState, Lists.newArrayList(method));
				}
			}			
		}
		
		for(CrySLMethodCallState state : stateSootMethodMap.keySet()) {
			List<SootMethod> stateMethods = stateSootMethodMap.get(state);
			List<LinkedList<CrySLMethodCall>> tempPaths = calcBestPathsToFinalStateIncludeMethod(stateMethods.get(0));
			
			if(tempPaths.size() > 0) {
				if(stateMethods.size() > 1) {
					for(SootMethod method : stateMethods) {
						CrySLMethodCall call = getCrySLMethodCallBySootMethod(method); 
						for(LinkedList<CrySLMethodCall> path : tempPaths) {
							LinkedList<CrySLMethodCall> tempPath = Lists.newLinkedList(path);
							tempPath.set(0, call);
							calcPaths.add(tempPath);
						}
					}
				} else {
					calcPaths.addAll(tempPaths);
				}
			} else {
				if(stateMethods.size() > 1) {
					for(SootMethod method : stateMethods) {
						CrySLMethodCall call = getCrySLMethodCallBySootMethod(method); 
						LinkedList<CrySLMethodCall> tempPath = Lists.newLinkedList();
						tempPath.add(call);
						calcPaths.add(tempPath);
					}
				} else {
					calcPaths.addAll(tempPaths);
				}
			}	
		}
		
		calcPaths = CrySLPathFilter.applyPathCriteriaFilters(calcPaths);		
		return calcPaths;
	}

	/**
	 * Calculates all paths to the final state beginning with the method argument
	 * @param method start method 
	 * @return all possible paths to the final state
	 * @throws PathException if no path can be calculated
	 */
	public List<LinkedList<CrySLMethodCall>> calcBestPathsToFinalStateIncludeMethod(SootMethod method) throws PathException {
		List<LinkedList<CrySLMethodCall>> calcForwardPaths = Lists.newArrayList();
		CrySLMethodCallState succState = getStartStateFromMethod(method);
		
		//TODO: combine calcBestPathsToFinalStatesStartedByMultipleMethod
		
		calcForwardPaths = calcPathsForward(succState);
		calcForwardPaths = CrySLPathFilter.applyPathCriteriaFilters(calcForwardPaths);		
		CrySLMethodCall call = getCrySLMethodCallBySootMethod(method); 
	
		if(calcForwardPaths.size() > 0) {
			for(LinkedList<CrySLMethodCall> path : calcForwardPaths) {
				path.add(0, call);
			}
		} else {
			LinkedList<CrySLMethodCall> path = Lists.newLinkedList();
			path.add(call);
			calcForwardPaths.add(path);
		}
		calcForwardPaths = CrySLPathFilter.applyPathCriteriaFilters(calcForwardPaths);		
		
		return calcForwardPaths;
	}

	/**
	 * Calculates all paths backward to the start state starting from the {@link CrySLMethodCallState} argument. 
	 * Since the FSM can contain loops it is necessary to track all visited states by the second argument.
	 * @param state start state
	 * @param visitedStates the visited states
	 * @return all possible paths to the start state
	 */
	private ArrayList<LinkedList<CrySLMethodCall>> calcPathsBackward(CrySLMethodCallState state, List<CrySLMethodCallState> visitedStates) {
		visitedStates.add(state);
		
		ArrayList<LinkedList<CrySLMethodCall>> calcPaths = Lists.newArrayList();
		Map<CrySLMethodCallCriteria, List<CrySLMethodCallTransition>> transitionMap = Maps.newHashMap(state.getInTransitions());
		

		for (List<CrySLMethodCallTransition> transitionList : transitionMap.values()) {
			LinkedList<CrySLMethodCall> path;

			for (CrySLMethodCallTransition t : transitionList) {

				if (t.from().isInitState()) {
					path = Lists.newLinkedList();
					path.add(t.getCall());
					calcPaths.add(path);
				} else {

					List<CrySLMethodCallState> tempVisitedStates = Lists.newArrayList(visitedStates);
					if (visitedStates.contains(t.from())) {
						break;
					} else {
						tempVisitedStates.addAll(visitedStates);
					}

					ArrayList<LinkedList<CrySLMethodCall>> subPath = calcPathsBackward(t.from(), tempVisitedStates);
					for (LinkedList<CrySLMethodCall> t1 : subPath) {
						path = Lists.newLinkedList();
						path.add(t.getCall());
						path.addAll(t1);
						calcPaths.add(path);
					}
				}
			}

		}
		return calcPaths;
	}

	/**
	 * Calculates all paths forward to the next final state starting from the {@link CrySLMethodCallState} argument.
 	 * @param state start state
	 * @return all possible paths to the next final state
	 */
	private ArrayList<LinkedList<CrySLMethodCall>> calcPathsForward(CrySLMethodCallState state) {

		ArrayList<LinkedList<CrySLMethodCall>> calcPaths = Lists.newArrayList();
		Map<CrySLMethodCallCriteria, List<CrySLMethodCallTransition>> transitionMap = Maps.newHashMap(state.getOutTransitions());

		for (List<CrySLMethodCallTransition> transitionList : transitionMap.values()) {
			LinkedList<CrySLMethodCall> path;

			for (CrySLMethodCallTransition t : transitionList) {
				if (t.to().isFinalState()) {
					path = Lists.newLinkedList();
					path.add(t.getCall());
					calcPaths.add(path);

				} else {
					ArrayList<LinkedList<CrySLMethodCall>> subPath = calcPathsForward(t.to());
					for (LinkedList<CrySLMethodCall> t1 : subPath) {
						path = Lists.newLinkedList();
						path.add(t.getCall());
						path.addAll(t1);
						calcPaths.add(path);
					}
				}
			}
		}		
		return calcPaths;
	}

	/**
	 * Returns the corresponding {@link CrySLMethodCall} of the {@link SootMethod} argument
	 * @param method the method
	 * @return the corresponding {@link CrySLMethodCall} object
	 * @throws NoCallFoundException If the FSM does not know the method 
	 */
	public CrySLMethodCall getCrySLMethodCallBySootMethod(SootMethod method) throws NoCallFoundException {
		if(sootCrySLCallMap.get(method.getSignature()) != null) {
			return sootCrySLCallMap.get(method.getSignature());
		} else {
			throw new NoCallFoundException("No CrySLMethodCall for "+method.getSignature()+" avaliable");
		}	
	}
	
	/**
	 * Returns the start {@link CrySLMethodCallState} of the {@link SootMethod} argument.
	 * @param method the method
	 * @return the start state
	 * @throws NoCallFoundException If the FSM does not know the method 
	 */
	private CrySLMethodCallState getStartStateFromMethod(SootMethod method) throws NoCallFoundException {
		if(methodStateMap.get(method.getSignature()) != null) {
			return methodStateMap.get(method.getSignature()).getLeft();
		} else {
			throw new NoCallFoundException("Method "+method.getSignature()+" not avaliable in Method State Mapping");
		}
	}
	
	/**
	 * Returns the target {@link CrySLMethodCallState} of the {@link SootMethod} argument.
	 * @param method the method
	 * @return the target state
	 * @throws NoCallFoundException If the FSM does not know the method 
	 */
	private CrySLMethodCallState getTargetStateFromMethod(SootMethod method) throws NoCallFoundException {	
		if(methodStateMap.get(method.getSignature()) != null) {
			return methodStateMap.get(method.getSignature()).getRight();
		} else {
			throw new NoCallFoundException("Method "+method.getSignature()+" not avaliable in Method State Mapping");
		}		
	}

	/**
	 * Transforms the {@link CrySLMethod} objects contains in the {@link TransitionEdge} argument in a  {@link List} of {@link CrySLMethodCall} objects.  
	 * @param edge the edge with the corresponding {@link CrySLMethod} objects 
	 * @return the transformed {@link CrySLMethodCalls} as {@link List}
	 */
	private List<CrySLMethodCall> transformTransitionEdgeToCrySLMethodCalls(TransitionEdge edge) {
		List<CrySLMethodCall> calls = Lists.newArrayList();
		for (CrySLMethod crySLMethod : edge.getLabel()) {
			List<SootMethod> sootMethods = Lists.newArrayList(CrySLMethodToSootMethod.v().convert(crySLMethod));
			if (!sootMethods.isEmpty()) {
				for(SootMethod method : sootMethods) {
					calls.add(new CrySLMethodCall(rule, method, crySLMethod, varPool));
				}		
			} else {
				LOGGER.error("Rule: "+rule.getClassName()+" - No SootMethod found for CrySLMethod: "+crySLMethod);
			}
		}
		return calls;
	}
}
