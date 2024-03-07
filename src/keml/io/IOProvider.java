/**
 * 
 */
package keml.io;

import java.io.IOException;

import org.apache.commons.io.FilenameUtils;

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
		
		String graphmlPath = "../../graphs/objective3-2-2v4.graphml";
		String kemlPath = FilenameUtils.removeExtension(graphmlPath) + ".keml";

		Conversation conv = new GraphML2KEML().readFromPath(graphmlPath);
		new KemlFileHandler().saveKeml(conv, kemlPath);	
	}

}
