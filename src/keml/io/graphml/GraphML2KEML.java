package keml.io.graphml;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.FilenameUtils;

import keml.Conversation;
import keml.KemlFactory;
import keml.KemlPackage;
import keml.impl.ConversationImpl;
import keml.util.KemlAdapterFactory;


import org.w3c.dom.*;
import org.xml.sax.SAXException;


public class GraphML2KEML {
	
	static KemlFactory factory = KemlFactory.eINSTANCE;

	
	public Conversation readFromPath (String path) throws ParserConfigurationException, FileNotFoundException, IOException, SAXException {
		
		DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		Document doc;
		try (FileInputStream is = new FileInputStream(path)) {
			doc = builder.parse(is);			
		}
		
		doc.getDocumentElement().normalize();
			
		return graph2keml(doc, FilenameUtils.getBaseName(path));
		
	}
	
	private Conversation graph2keml(Document doc, String title) {
		
		Conversation res = factory.createConversation();
		res.setTitle(title);
		
		NodeList nodeList = doc.getElementsByTagName("node");

		System.out.println(nodeList.getLength());
		for (int i = 0; i< nodeList.getLength(); i++) {
			Node node = nodeList.item(i);
			String id = node.getAttributes().item(0).getNodeValue();
			
			NodeList data = node.getChildNodes();
			System.out.println(id +": "+data.getLength());

			for (int j = 0; j < data.getLength(); j++) {
				System.out.println(data.item(j).getNodeName());
			}
								
		}

		
		
		
		return res;
	}
	

}
