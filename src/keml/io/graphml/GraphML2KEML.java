package keml.io.graphml;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.FilenameUtils;

import keml.Author;
import keml.Conversation;
import keml.KemlFactory;
import keml.KemlPackage;
import keml.PreKnowledge;
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
		
		Conversation conversation = factory.createConversation();
		conversation.setTitle(title);
		Author author = factory.createAuthor();
		conversation.setAuthor(author);
		
		HashMap<String, Object> kemlNodes = new HashMap();
		
		
		NodeList nodeList = doc.getElementsByTagName("node");

		System.out.println(nodeList.getLength());
		for (int i = 0; i< nodeList.getLength(); i++) {
			Node node = nodeList.item(i);
			String id = node.getAttributes().item(0).getNodeValue();
			
			NodeList data = node.getChildNodes();
			System.out.println(id +": "+data.getLength());

			for (int j = 0; j < data.getLength(); j++) {
				Node cur = data.item(j);
				if (cur.getNodeName().equals("data") && cur.getAttributes().item(0).getNodeValue().equals("d6")) { 
					//only these data nodes hold the relevant properties
					// we can now distinguish by shape:
					NodeList children = cur.getChildNodes();
					for (int k = 0; k < children.getLength(); k++) {
						Node childNode = children.item(k);
						String nodeName = childNode.getNodeName();

						switch (nodeName) {
							case "#text": break;
							case "y:GenericNode": {
								System.out.println("Found Generic");
								// we just need the pre-knowledge from it, that has <y:GenericNode configuration="com.yworks.bpmn.Artifact.withShadow">
								if (childNode.getAttributes().item(0).getNodeValue().equals("com.yworks.bpmn.Artifact.withShadow")) {
									System.out.println("Found preknowledge");
									String label = readLabel(childNode);
									PreKnowledge pre = 	factory.createPreKnowledge();
									pre.setMessage(label);
									kemlNodes.put(id, pre);
								}
								break;
							}
							case "y:SVGNode": {
								System.out.println("Found SVG");
								break;
							}
							case "y:ShapeNode": {
								System.out.println("Found Shape");
								break;
							}
							default: {
								throw new IllegalArgumentException("Unrecognized shape: "+nodeName);
							}
						
						}

					}
				
					
					cur.getChildNodes().getLength();
					
					//System.out.println(cur.getNodeName());
					//System.out.println(cur.getAttributes().item(0).getNodeValue());
				}
			}
								
		}
		
		System.out.println(kemlNodes.toString());
		
		return conversation;
	}
	
	private String readLabel(Node node) {
		NodeList children = node.getChildNodes();
		String label = "";
		
		for (int i=0; i<children.getLength(); i++) {
			Node childNode = children.item(i);
			if (childNode.getNodeName().equals("y:NodeLabel") && childNode.getAttributes().getNamedItem("hasText") == null) { //a text exists
				label = childNode.getChildNodes().item(0).getNodeValue(); //TODO is item 0 guaranteed?
				label = cleanLabel(label);
				println(label);
			}			
		}
		return label;
	}
	
	
	private String cleanLabel(String s) {
		return s.trim().replace('\n', ' ');
	}
	
	private void println(String s) {
		System.out.println(s);
	}
	
	private void printAttributeNames (Node node) {
		NamedNodeMap map = node.getAttributes();
		for (int j =0; j< map.getLength(); j++)
		{
			println(map.item(j).getNodeName());
		}
	}

}
