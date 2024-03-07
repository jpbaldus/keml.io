package keml.io.graphml;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.FilenameUtils;

import keml.Author;
import keml.Conversation;
import keml.ConversationPartner;
import keml.Information;
import keml.InformationLinkType;
import keml.KemlFactory;
import keml.MessageExecution;
import keml.NewInformation;
import keml.PreKnowledge;
import keml.ReceiveMessage;
import keml.SendMessage;


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
		HashMap<String, NodeType> nodeTypes = new HashMap<String, NodeType>();
		
		//helper information: determine execution specs via positions, also group nodes and hence edges
		PositionalInformation authorPosition = null;	
		HashMap<String, PositionalInformation> conversationPartnerXs = new HashMap<String, PositionalInformation>(); // helper map for all conversation partners' positions	
		HashMap<String, PositionalInformation> potentialMessageExecutionXs = new HashMap<String, PositionalInformation>(); // helper map for all possible message executions

		HashMap<String, PositionalInformation> informationPositions = new HashMap<String, PositionalInformation>();
		HashMap<String, PositionalInformation> informationIsInstructionPositions = new HashMap<String, PositionalInformation>();
		HashMap<String, PositionalInformation> informationIsNoInstructionPositions = new HashMap<String, PositionalInformation>();
		
		HashMap<String, String> ignoreNodes = new HashMap<String, String>();
		
		
		// first work on nodes, fill the lists above:
		NodeList nodeList = doc.getElementsByTagName("node");

		for (int i = 0; i< nodeList.getLength(); i++) {
			Node node = nodeList.item(i);
			String id = node.getAttributes().getNamedItem("id").getNodeValue();

			
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
								// these nodes represent the conversation partners and the instruction icons on information
								// each one with a label forms a new conversation partner (except for Author)
								String label = GraphMLUtils.readLabel(childNode);
								if (!label.equals("")) {
									// this is a life line, determine Position
									PositionalInformation x = readPositions(childNode);
									if (label.equals("Author")) {
										nodeTypes.put(id, NodeType.AUTHOR);
										authorPosition = x;
									} else {
										nodeTypes.put(id, NodeType.CONVERSATION_PARTNER);										
										ConversationPartner p = factory.createConversationPartner();
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
								// we just need the pre-knowledge from it, that has <y:GenericNode configuration="com.yworks.bpmn.Artifact.withShadow">
								if (childNode.getAttributes().item(0).getNodeValue().equals("com.yworks.bpmn.Artifact.withShadow")) {
									nodeTypes.put(id, NodeType.PRE_KNOWLEDGE);
									String label = GraphMLUtils.readLabel(childNode);
									PreKnowledge pre = 	factory.createPreKnowledge();
									pre.setMessage(label);
									kemlNodes.put(id, pre);
									author.getPreknowledge().add(pre);
								}
								break;
							}
							case "y:ShapeNode": {
								String color = readColor(childNode);
								PositionalInformation pos = readPositions(childNode);

								switch (color) {
									case "#FFFF99":  //light-yellow, used on information by WebBrowser
									case "#CCFFFF": { // light-blue, used on information by LLM
										String label = GraphMLUtils.readLabel(childNode);
										nodeTypes.put(id, NodeType.NEW_INFORMATION);
										NewInformation info = factory.createNewInformation();
										info.setMessage(label);
										kemlNodes.put(id, info);
										// also store positions to find corresponding ! or person
										informationPositions.put(id, pos);								
										break;
									}
									case "#99CC00": { //green, used on facts (!)
										nodeTypes.put(id, NodeType.NEW_INFORMATION);
										informationIsNoInstructionPositions.put(id, pos);								
										break;
									}
									case "#FFCC00": { // yellow, behind human icon (isInstruction = true)
										nodeTypes.put(id, NodeType.NEW_INFORMATION);
										informationIsInstructionPositions.put(id, pos);								
										break;
										
									}
									case "#C0C0C0": { //grey, used for message executions
										//we need this to complete the edges, we will just model the messageSpecs on author explicitly but first put all into the messageExecutionXs
										// also need y position to order them on the author
										nodeTypes.put(id, NodeType.MESSAGE_SPEC);
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
				}
			}								
		}
		
		// nodeForwardList: HashMap<String, String> lookup for which real information node is used (we have 2 nodes form message + icon)
		Map<String, String> informationNodeForwardMap = createNodeForwardList(informationPositions, informationIsInstructionPositions, informationIsNoInstructionPositions, kemlNodes);
		
		
		
		// ************* edges ****************************
		NodeList edgeList = doc.getElementsByTagName("edge");
		List<GraphEdge> edges = IntStream.range(0, edgeList.getLength())
			    .mapToObj(edgeList::item)
			    .map(s -> new GraphEdge(s))
			    .collect(Collectors.toList());
		
		// now work on edges to separate them: we need those of the sequence diagram to arrange the messages and can already define all relations between information in a second method
		List<GraphEdge> sequenceDiagramEdges = new ArrayList<GraphEdge>();
		List<GraphEdge> informationConnection = new ArrayList<GraphEdge>();
		List<GraphEdge> usedBy = new ArrayList<GraphEdge>();
		List<GraphEdge> generates = new ArrayList<GraphEdge>();
		edges.forEach(e -> 
		{
			NodeType src = nodeTypes.get(e.getSource());
			switch (src) {
				case MESSAGE_SPEC: {
					switch(nodeTypes.get(e.getTarget())) {
						case MESSAGE_SPEC: {
							sequenceDiagramEdges.add(e);
							break;
						}
						case NEW_INFORMATION: {
							generates.add(e);
							break;
						}
						default: {
							throw new IllegalArgumentException("Node "+ e.getTarget() + " of type " + nodeTypes.get(e.getTarget()) + " not valid on edge from " +src);
						}
					}
					break;
				}
				case AUTHOR: case CONVERSATION_PARTNER: {
					if (nodeTypes.get(e.getTarget()) == NodeType.MESSAGE_SPEC) {
						sequenceDiagramEdges.add(e);
					} else {
						throw new IllegalArgumentException("Node "+ e.getTarget() + " of type " + nodeTypes.get(e.getTarget()) + " not valid on edge from "+src);
					}
					break;
				}
				case NEW_INFORMATION: {
					switch(nodeTypes.get(e.getTarget())) {
						case MESSAGE_SPEC: {
							usedBy.add(e);
							break;
						}
						case NEW_INFORMATION: {
							informationConnection.add(e);
							break;
						}
						default: {
							throw new IllegalArgumentException("Node "+ e.getTarget() + " of type " + nodeTypes.get(e.getTarget()) + " not valid on edge from " +src);
						}
					}
					break;
					
				}
				case PRE_KNOWLEDGE: {
					if (nodeTypes.get(e.getTarget()) == NodeType.MESSAGE_SPEC) {
						usedBy.add(e);
					} else {
						throw new IllegalArgumentException("Node "+ e.getTarget() + " of type " + nodeTypes.get(e.getTarget()) + " not valid on edge from "+src);
					}
					break;
				}
			}
		});
		
		// ***************** Sequence Diagram ********************************
		ArrayList<String> msgsInOrder = buildSequenceDiagram(conversation, authorPosition, conversationPartnerXs, kemlNodes, potentialMessageExecutionXs, sequenceDiagramEdges);
		
		// ***************** Connecting information and sequence diagram ********
		
		// TODO maybe more verbose read?
		// 			MessageExecution m = (MessageExecution) kemlNodes.get(e.getTarget()); to be able to print it on log msg
		generates.forEach(e -> {
			ReceiveMessage msg = (ReceiveMessage) kemlNodes.get(e.getSource());
			NewInformation info = (NewInformation) kemlNodes.get(informationNodeForwardMap.get(e.getTarget()));
			msg.getGenerates().add(info);
		});
		
		usedBy.forEach(e -> {
			Information info = (Information) kemlNodes.get(e.getSource());			
			if (info == null) { // info is neither preknowledge, nor the real node -> need to follow helper map (it does not contain pre-knowledge)
				info = (Information) kemlNodes.get(informationNodeForwardMap.get(e.getSource()));			
			}
			SendMessage msg = (SendMessage) kemlNodes.get(e.getTarget());
			info.getIsUsedOn().add(msg);
		});
		
		// ***************** Information Connections **********************
		// TODO needs to be re-worked on diagram
		
		
		
		System.out.println(sequenceDiagramEdges);
		System.out.println(usedBy);		
		System.out.println(generates);
		System.out.println(informationConnection);
		
		System.out.println(kemlNodes.toString());
		System.out.println(kemlNodes.size());	
	
		
		System.out.println("Read "+ nodeList.getLength() + " nodes and " + edgeList.getLength() + " edges.");

		
		return conversation;
	}
	
	private void addOrderedConversationPartners(Conversation conversation, HashMap<String, PositionalInformation> conversationPartnerXs,
			HashMap<String, Object> kemlNodes) {
		conversationPartnerXs.entrySet().stream()
		.sorted((s,t) -> Float.compare(s.getValue().getxLeft(),t.getValue().getxLeft()))
		.forEach(c -> {
			conversation.getConversationPartners().add((ConversationPartner) kemlNodes.get(c.getKey()));
		});
	}
	
	private ArrayList<String> buildSequenceDiagram(Conversation conversation, PositionalInformation authorPosition,
			HashMap<String, PositionalInformation> conversationPartnerXs,
			HashMap<String, Object> kemlNodes, HashMap<String, PositionalInformation> potentialMessageExecutionXs,
			List<GraphEdge> edges) {
		
		addOrderedConversationPartners(conversation, conversationPartnerXs, kemlNodes);
		
		Author author = conversation.getAuthor();
		// just work by position: all message Specs should be below their LifeLine in order
		ArrayList<String> authorMessagesInOrder = potentialMessageExecutionXs.entrySet().stream()
				.filter(s -> isOnLifeLine(s.getValue(), authorPosition))
				.sorted((s,t) -> Float.compare(s.getValue().getyHigh(),t.getValue().getyHigh()))
				.map(Map.Entry::getKey)
				.collect(Collectors 
                        .toCollection(ArrayList::new));
				
		// for all other lifelines create a map of node id to lifeline id 
		// TODO current code uses filter and is terribly inefficient if we ever have many conversation partners
		HashMap<String, String> lifeLineFinder = new HashMap<String, String>();
		conversationPartnerXs.entrySet().stream().forEach(partner -> {
			potentialMessageExecutionXs.entrySet().stream()
			.filter(s -> isOnLifeLine(s.getValue(), partner.getValue()))
			.forEach(v -> {
				lifeLineFinder.put(v.getKey(), partner.getKey());	
			});
		});
		
		//now find messages (edges) that connect the author with another lifeline
		// we can first eliminate all that have no label to improve efficiency
		edges.removeIf(s -> s.getLabel().isEmpty());
		
		MessageExecution[] msgs = new MessageExecution[authorMessagesInOrder.size()];

		edges.forEach(e -> {
			OptionalInt srcIndexOnAuth = IntStream.range(0, authorMessagesInOrder.size())
					   .filter(i -> authorMessagesInOrder.get(i).equals(e.getSource()))
					   .findFirst();
			if (srcIndexOnAuth.isPresent()) {
				int index = srcIndexOnAuth.getAsInt();
				String partnerId = lifeLineFinder.get(e.getTarget());
				if (partnerId != null) {
					//create a send message execution spec
					SendMessage msg = factory.createSendMessage();
					msg.setContent(e.getLabel());
					msg.setYPosition(index);
					msg.setCounterPart((ConversationPartner) kemlNodes.get(partnerId));
					kemlNodes.put(authorMessagesInOrder.get(index), msg);
					msgs[index] = msg;
				}
			} else {
				OptionalInt targetIndexOnAuth = IntStream.range(0, authorMessagesInOrder.size())
						   .filter(i -> authorMessagesInOrder.get(i).equals(e.getTarget()))
						   .findFirst();
				if (targetIndexOnAuth.isPresent()) {
					int index = targetIndexOnAuth.getAsInt();
					String partnerId = lifeLineFinder.get(e.getSource());
					if (partnerId != null) {
						//create a receive message execution spec
						ReceiveMessage msg = factory.createReceiveMessage();
						msg.setContent(e.getLabel());
						msg.setYPosition(index);
						msg.setCounterPart((ConversationPartner) kemlNodes.get(partnerId));
						kemlNodes.put(authorMessagesInOrder.get(index), msg);
						msgs[index] = msg;
					}
				}
				
			}
		});
		//now finally insert all messages in order
		author.getMessageExecutions().addAll(Arrays.asList(msgs));
		
		return authorMessagesInOrder;
	}
	
	private boolean isOnLifeLine(PositionalInformation pos, PositionalInformation lifeLinePos) {
		return ( lifeLinePos.getxLeft() - pos.getxLeft() <= 0 && lifeLinePos.getxRight() - pos.getxRight() >=0);
	}
		
	// works on the knowledge part of the graph to unite the information (with text) and its type (isInstruction)
	// also sets the isInstruction flag on the information
	private Map<String, String> createNodeForwardList(
			HashMap<String, PositionalInformation> informationPositions,
			HashMap<String, PositionalInformation> informationIsInstructionPositions,
			HashMap<String, PositionalInformation> informationIsNoInstructionPositions,
			HashMap<String, Object> kemlNodes) {
		
		if (informationPositions.size() != informationIsInstructionPositions.size() + informationIsNoInstructionPositions.size()) {
			throw new IllegalArgumentException("Sizes do not match!");
		}
		Map<String, String> forwardList = informationPositions.keySet().stream()
				.collect(Collectors.toMap(s -> s, s -> s));
			
		informationPositions.forEach((str, pos)-> {
			boolean matched = findMatchForMessage(str, pos, informationIsInstructionPositions, true, forwardList, kemlNodes);
			if (!matched) {
				boolean nowMatched = findMatchForMessage(str, pos, informationIsNoInstructionPositions, false, forwardList, kemlNodes);
				if (!nowMatched)
					throw new IllegalArgumentException("No match for node "+str);
			}	
		});
		return forwardList;
	}
	
	
	boolean findMatchForMessage(String str, PositionalInformation pos, HashMap<String, PositionalInformation>posToMatch, boolean isInstr, Map<String, String>forwardList, HashMap<String,Object>kemlNodes ) {
		for (Map.Entry<String, PositionalInformation> e: posToMatch.entrySet()) {
			//type is on the right, so use right side of information (xr) and left side of isInstruction (xl) to match, also both heights (y)
			if ( floatEquality( e.getValue().getxLeft(), pos.getxRight() ) 
					&& floatEquality (e.getValue().getyLow(), pos.getyLow() )
					&& floatEquality (e.getValue().getyHigh(), pos.getyHigh()) ) {
				forwardList.put(e.getKey(), str);
				NewInformation info = (NewInformation) kemlNodes.get(str);
				info.setIsInstruction(isInstr);
				return true;
			}
		}
		return false;		
	}
	
	
	private String readColor(Node node) {
		Element e = (Element) node;
		NamedNodeMap fill = e.getElementsByTagName("y:Fill").item(0).getAttributes();
		return fill.getNamedItem("color").getNodeValue();
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
	
	private boolean floatEquality(Float f1, Float f2) {
		return Math.abs(f1-f2) < 0.001F;
	}
	
	private void printAttributeNames (Node node) {
		NamedNodeMap map = node.getAttributes();
		for (int j =0; j< map.getLength(); j++)
		{
			System.out.println(map.item(j).getNodeName());
		}
	}

}
