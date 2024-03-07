/**
 * 
 */
package keml.io;

import java.io.IOException;

import keml.Conversation;
import keml.io.graphml.GraphML2KEML;

/**
 * 
 */
public class IOProvider {
	

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws Exception {

		String currentPath = new java.io.File(".").getCanonicalPath();
		System.out.println("Current dir:" + currentPath);
		GraphML2KEML graphml2keml = new GraphML2KEML();
		Conversation conv = graphml2keml.readFromPath("../../graphs/objective3-2-2v4.graphml");
		new KemlFileHandler().saveKeml(conv, "../../graphs/objective3-2-2v4.keml");	
	}

}
