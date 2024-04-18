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
import keml.io.llm.ConversationAdder;

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
		String conversationFolder = folder+"conv/";
		new ChatGPTReader().split(conversations, conversationFolder);
		
		File[] files = new File(folder).listFiles((dir, name) -> name.toLowerCase().endsWith(".graphml"));
		
		KemlFileHandler fileHandler = new KemlFileHandler();
		
		for (File file: files) {
			try {	
				transformFile(file, fileHandler, getConvFileFromFile(file, conversationFolder));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}

	private static void transformFile(File graphmlPath, KemlFileHandler fileHandler, File originalConv) throws Exception {
		Conversation conv = new GraphML2KEML().readFromPath(graphmlPath.getAbsolutePath());
		ConversationAdder.addOriginalConv(conv, originalConv);
		String kemlPath = FilenameUtils.removeExtension(graphmlPath.getAbsolutePath()) + ".keml";
		fileHandler.saveKeml(conv, kemlPath);
	}
	
	private static File getConvFileFromFile(File file, String conversationFolder) {
		return new File(conversationFolder + FilenameUtils.getBaseName(file.getPath())+".json");
	}

}
