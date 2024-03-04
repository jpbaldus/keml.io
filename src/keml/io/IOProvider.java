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
	public static void main(String[] args) throws IOException {

		String currentPath = new java.io.File(".").getCanonicalPath();
		System.out.println("Current dir:" + currentPath);
		GraphML2KEML myReader = new GraphML2KEML();
		Conversation conv = myReader.readFromPath("../../graphs/objective3-2-2v2.graphml");
		System.out.println(conv.getTitle());
	

	}

}
