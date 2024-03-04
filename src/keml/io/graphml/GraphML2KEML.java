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
		
		HashMap<String, Object> kemlNodes = new HashMap<String, Object>();
		
		//determine execution specs via positions:
		PositionalInformation authorPosition = null;	
		HashMap<String, PositionalInformation> conversationPartnerXs = new HashMap<String, PositionalInformation>(); // helper map for all conversation partners' positions	
		HashMap<String, PositionalInformation> potentialMessageExecutionXs = new HashMap<String, PositionalInformation>(); // helper map for all possible message executions

		HashMap<String, PositionalInformation> informationPositions = new HashMap<String, PositionalInformation>();
		HashMap<String, PositionalInformation> informationIsInstructionPositions = new HashMap<String, PositionalInformation>();
		HashMap<String, PositionalInformation> informationIsNoInstructionPositions = new HashMap<String, PositionalInformation>();
		
		HashMap<String, String> ignoreNodes = new HashMap<String, String>();
		
		NodeList nodeList = doc.getElementsByTagName("node");

		System.out.println(nodeList.getLength());
		for (int i = 0; i< nodeList.getLength(); i++) {
			Node node = nodeList.item(i);
			String id = node.getAttributes().item(0).getNodeValue();
			
			NodeList data = node.getChildNodes();

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
									// this is a life line, determine Position
									PositionalInformation x = readPositions(childNode);
									if (label.equals("Author")) {
										authorPosition = x;
									} else {
										ConversationPartner p = factory.createConversationPartner(); //v4: browser n83, LLM n79
										p.setName(label);
										kemlNodes.put(id,  p);
										conversationPartnerXs.put(id, x);
									}
								} else { //just icons on information (isInstruction = true) and the extra computer on the LLM
									// TODO maybe need because of edges pointing here rather than on the main part? Currently skip (ignoreNodes) 	
									ignoreNodes.put(id, id);
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
								PositionalInformation pos = readPositions(childNode);

								switch (color) {
									case "#FFFF99":  //light-yellow, used on information by WebBrowser
									case "#CCFFFF": { // light-blue, used on information by LLM
										String label = readLabel(childNode);
										NewInformation info = factory.createNewInformation();
										info.setMessage(label);
										kemlNodes.put(id, info);
										// also store positions to find corresponding ! or person
										informationPositions.put(id, pos);								
										break;
									}
									case "#99CC00": { //green, used on facts (!)
										informationIsNoInstructionPositions.put(id, pos);								
										break;
									}
									case "#FFCC00": { // yellow, behind human icon (isInstruction = true)
										informationIsInstructionPositions.put(id, pos);								
										break;
										
									}
									case "#C0C0C0": { //grey, used for message executions
										//we need this to complete the edges, we will just model the messageSpecs on author explicitly but first put all into the messageExecutionXs
										// also need y position to order them on the author
										potentialMessageExecutionXs.put(id, pos);
										break;
									}
									default: {
										throw new IllegalArgumentException("Unrecognized color: "+color);
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
				}
			}								
		}
		
		System.out.println(kemlNodes.toString());
		System.out.println(kemlNodes.size());
		
		if (authorPosition != null)
			println(authorPosition.toString());
		
		println(conversationPartnerXs.toString());
		println(potentialMessageExecutionXs.toString());
		
		println(ignoreNodes.toString());
		println("Infos:");
		println(informationPositions.toString());
		println("IsInstruction:");
		println(informationIsInstructionPositions.toString());
		println("NoInstruction:");
		println(informationIsNoInstructionPositions.toString());
		
		// TODO1: nodeForwardList: HashMap<String, String> lookup for which real node is used (e.g. when 2 nodes form message + icon)
		// TODO2: sequence diagram: get Nodes in order
		
	
		// TODO 3: edges
		
		return conversation;
	}
	
	private String readLabel(Node node) {
		String label = "";
		Element e = (Element) node;
		NodeList nodeLabels = e.getElementsByTagName("y:NodeLabel");
		for (int i=0; i<nodeLabels.getLength(); i++) {
			Node childNode = nodeLabels.item(i);
			if (childNode.getAttributes().getNamedItem("hasText") == null) {//a text exists
				label = childNode.getChildNodes().item(0).getNodeValue(); //TODO is item 0 guaranteed?
				label = cleanLabel(label);
				println(label);
			}
		}
		return label;
	}
	
	private String readColor(Node node) {
		Element e = (Element) node;
		NamedNodeMap fill = e.getElementsByTagName("y:Fill").item(0).getAttributes();
		String color = fill.getNamedItem("color").getNodeValue();
		println(color);
		return color;
	}
	
	private PositionalInformation readPositions(Node node) {
		Element e = (Element) node;
		NamedNodeMap geo = e.getElementsByTagName("y:Geometry").item(0).getAttributes();
		String x = geo.getNamedItem("x").getNodeValue();
		Float xl = Float.parseFloat(x);
		String width = geo.getNamedItem("width").getNodeValue();
		Float xr = xl + Float.parseFloat(width);
		String y = geo.getNamedItem("y").getNodeValue();
		Float yl = Float.parseFloat(y);
		String height = geo.getNamedItem("height").getNodeValue();
		Float yh = yl + Float.parseFloat(height);
		return new PositionalInformation(xl, xr, yl, yh);
		
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
