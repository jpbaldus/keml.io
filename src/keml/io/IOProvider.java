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
		
		String folder = "../../graphs/";
		String file = "objective3-2-2v4.graphml";
		String file2 = "objective3-1-1.graphml";
		
		String graphmlPath = folder+file2;
		String kemlPath = FilenameUtils.removeExtension(graphmlPath) + ".keml";

		Conversation conv = new GraphML2KEML().readFromPath(graphmlPath);
		new KemlFileHandler().saveKeml(conv, kemlPath);	
	}

}
