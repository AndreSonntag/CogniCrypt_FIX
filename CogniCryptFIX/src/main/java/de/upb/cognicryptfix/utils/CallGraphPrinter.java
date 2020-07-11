package de.upb.cognicryptfix.utils;

import java.io.File;

import soot.MethodOrMethodContext;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.util.dot.DotGraph;
import soot.util.queue.QueueReader;

public class CallGraphPrinter {
	
	public static File serializeCallGraph(CallGraph graph, String fileName) {
	    if (fileName == null) {
	        fileName = soot.SourceLocator.v().getOutputDir();
	        if (fileName.length() > 0) {
	            fileName = fileName + java.io.File.separator;
	        }
	        fileName = fileName + "call-graph" + DotGraph.DOT_EXTENSION;
	    }
	    DotGraph canvas = new DotGraph("call-graph");
	    QueueReader<Edge> listener = graph.listener();
	    while (listener.hasNext()) {
	    	
	        Edge next = listener.next();
	        
	        if(next.getSrc().method().getSignature().contains("de.upb")) {
	            MethodOrMethodContext src = next.getSrc();
		        MethodOrMethodContext tgt = next.getTgt();
		        canvas.drawNode(src.toString());
		        canvas.drawNode(tgt.toString());
		        canvas.drawEdge(src.toString(), tgt.toString());
	        }
	    }
	    canvas.plot(fileName);
	    return new File(fileName);
	}
}
