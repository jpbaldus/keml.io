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
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import keml.Author;
import keml.Conversation;
import keml.ConversationPartner;
import keml.KemlFactory;
import keml.KemlPackage;
import keml.NewInformation;
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
		
		//determine execution specs via positions:
		Float authorXL = 0F;
		Float authorXR = 0F;
		
		HashMap<String, Pair<Float, Float>> conversationPartnerXs = new HashMap<String, Pair<Float, Float>>(); // helper map for all conversation partners
		
		HashMap<String, Pair<Float, Float>> messageExecutionXs = new HashMap<String, Pair<Float, Float>>(); // helper map for all possible message executions

		
		
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
							case "y:SVGNode": {
								System.out.println("Found SVG");
								// these nodes represent the conversation partners and the instruction icons on information
								// each one with a label forms a new conversation partner (except for Author)
								String label = readLabel(childNode);
								if (!label.equals("")) {
									// this is a life line, determine xleft and xright
									Pair<Float, Float> x = readXPositions(childNode);
									if (label.equals("Author")) {
										authorXL = x.getLeft();
										authorXR = x.getRight();
									} else {
										ConversationPartner p = factory.createConversationPartner(); //v4: browser n83, LLM n79
										p.setName(label);
										kemlNodes.put(id,  p);
										conversationPartnerXs.put(id, x);
									}
								} else { //just icons on information (isInstruction = true) and the extra computer on the LLM
									
									
								}								
								break;
							}
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
							case "y:ShapeNode": {
								System.out.println("Found Shape");
								// TODO switch by color
								String color = readColor(childNode);
								switch (color) {
									case "#FFFF99": { //light-yellow, used on information by Webbrowser
										
									}
									case "#CCFFFF": { // light-blue, used on information by LLM
										
									}
									case "#99CC00": { //green, used on facts (!)
										
									}
									case "#FFCC00": { // yellow, behind human icon (isInstruction = true)
										
									}
									case "#C0C0C0": { //grey, used for message executions
										//we need this to complete the edges, we will just model the messageSpecs on author explicitly but first put all into the messageExecutionXs
										Pair<Float, Float> xPositions = readXPositions(childNode);
										messageExecutionXs.put(id, xPositions);
									}
									default: {
										
									}
								}
								
								String label = readLabel(childNode);
								switch (label) {
									case null: break;
									case (""): break;
									case ("!"): {
										// this is an addition to an information, meaning it is a fact (isInstruction = false)
										// TODO link to a new information via position
										break;
									}
									default: {
										NewInformation info = factory.createNewInformation();
										info.setMessage(label);
										kemlNodes.put(id, info);
										// todo maybe first collect corresponding ! or icon?
									}
								}	
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
		System.out.println(kemlNodes.size());
		
		println(authorXL.toString());
		println(authorXR.toString());
		
		println(conversationPartnerXs.toString());
		println(messageExecutionXs.toString());
		
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
	
	private String readColor(Node node) {
		NodeList children = node.getChildNodes();
		String color = "";
		
		for (int i=0; i<children.getLength(); i++) {
			Node childNode = children.item(i);
			if (childNode.getNodeName().equals("y:Fill")) {
				color = childNode.getAttributes().getNamedItem("color").getNodeValue();
				color = cleanLabel(color);
				println(color);
			}			
		}
		return color;
	}
	
	private Pair<Float, Float> readXPositions(Node node) {
		NodeList children = node.getChildNodes();
		
		for (int i=0; i<children.getLength(); i++) {
			Node childNode = children.item(i);
			if (childNode.getNodeName().equals("y:Geometry")) {
				String x = childNode.getAttributes().getNamedItem("x").getNodeValue();
				Float xl = Float.parseFloat(x);
				String width = childNode.getAttributes().getNamedItem("width").getNodeValue();
				Float xr = xl + Float.parseFloat(width);
				println(xl.toString());
				println(xr.toString());
				return new ImmutablePair<>(xl, xr);
			}			
		}
		throw new IllegalArgumentException();
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
