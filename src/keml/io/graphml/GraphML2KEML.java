package keml.io.graphml;

import java.io.IOException;
import java.util.Iterator;

import org.apache.commons.io.FilenameUtils;

import keml.Conversation;
import keml.KemlFactory;
import keml.KemlPackage;
import keml.impl.ConversationImpl;
import keml.util.KemlAdapterFactory;

import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.VertexProperty;
import org.apache.tinkerpop.gremlin.structure.io.IoCore;
import org.apache.tinkerpop.gremlin.structure.io.graphml.GraphMLReader;
import org.apache.tinkerpop.gremlin.util.Gremlin;


public class GraphML2KEML {
	
	static KemlFactory factory = KemlFactory.eINSTANCE;

	
	public Conversation readFromPath (String path) {
		
		
		Graph graph = TinkerGraph.open();
		graph.traversal().io(path).read().iterate();
		System.out.println(graph.toString());
				
		
		
		
		
		return graph2keml(graph, FilenameUtils.getBaseName(path));
		
	}
	
	private Conversation graph2keml(Graph graph, String title) {
		
		Conversation res = factory.createConversation();
		res.setTitle(title);
		
		for (Iterator<Vertex> iter = graph.vertices(); iter.hasNext(); ) {
		    Vertex vertex = iter.next();
		    System.out.println(vertex.toString());
		    
			for (Iterator<VertexProperty<Object>> it = vertex.properties(); it.hasNext(); ) {

				System.out.println(it.next().toString());
			}
		
		}
		
		return res;
	}
	

}
