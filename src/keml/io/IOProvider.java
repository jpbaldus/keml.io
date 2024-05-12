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
		
		String folder = "../keml.sample/case-study-log4j/2024-03-30-paperversion";//args[0];
		System.out.println(folder);
				
		String conversations = folder + "/../conversations.json";
		String conversationFolder = folder+"/../conv/";
		new ChatGPTReader().split(conversations, conversationFolder);
		
		File resultsFolder = new File(folder + "/keml/");
		
		File[] files = new File(folder+"/graphml/").listFiles((dir, name) -> name.toLowerCase().endsWith(".graphml"));
		
		KemlFileHandler fileHandler = new KemlFileHandler();
		
		for (File file: files) {
			try {	
				transformFile(file, fileHandler, resultsFolder, getConvFileFromFile(file, conversationFolder));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}

	private static void transformFile(File graphmlPath, KemlFileHandler fileHandler, File targetFolder, File originalConv) throws Exception {
		Conversation conv = new GraphML2KEML().readFromPath(graphmlPath.getAbsolutePath());
		ConversationAdder.addOriginalConv(conv, originalConv);
		String kemlPath = targetFolder +"/" + FilenameUtils.removeExtension(graphmlPath.getName()) + ".keml";
		fileHandler.saveKeml(conv, kemlPath);
	}
	
	private static File getConvFileFromFile(File file, String conversationFolder) {
		return new File(conversationFolder + FilenameUtils.getBaseName(file.getPath())+".json");
	}

}
