/**
 * 
 */
package keml.io;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FilenameUtils;

import keml.Conversation;
import keml.io.graphml.GraphML2KEML;
import keml.io.llm.ChatGPTReader;

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
		
		String conversations = folder + "conversations.json";
		new ChatGPTReader().split(conversations, folder+"conv/");
		
		File[] files = new File(folder).listFiles((dir, name) -> name.toLowerCase().endsWith(".graphml"));
		
		KemlFileHandler fileHandler = new KemlFileHandler();
		
		for (File file: files) {
			try {
				//transformFile(file, fileHandler);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}

	private static void transformFile(File graphmlPath, KemlFileHandler fileHandler) throws Exception {
		Conversation conv = new GraphML2KEML().readFromPath(graphmlPath.getAbsolutePath());
		String kemlPath = FilenameUtils.removeExtension(graphmlPath.getAbsolutePath()) + ".keml";
		fileHandler.saveKeml(conv, kemlPath);
	}

}
