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
		String[] files = new String[4];
		files[0] = "objective3-1-1.graphml";
		files[1] = "objective3-1-2.graphml";
		files[2] = "objective3-2-1v3.graphml";
		files[3] = "objective3-2-2v5.graphml";
		
		String graphmlPath = folder+files[3];
		String kemlPath = FilenameUtils.removeExtension(graphmlPath) + ".keml";

		Conversation conv = new GraphML2KEML().readFromPath(graphmlPath);
		new KemlFileHandler().saveKeml(conv, kemlPath);	
	}

}
