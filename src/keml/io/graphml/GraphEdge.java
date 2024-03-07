package keml.io.graphml;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import keml.InformationLinkType;

public class GraphEdge {
	
	private String id;
	private String source;
	private String target;
	private String label;
	private InformationLinkType type; //only used on InformationLink edges
	
	public GraphEdge(Node node) {
		
		Element e = (Element) node;
		this.id = e.getAttributes().getNamedItem("id").getNodeValue();
		this.source = e.getAttributes().getNamedItem("source").getNodeValue();
		this.target = e.getAttributes().getNamedItem("target").getNodeValue();
		this.label = GraphMLUtils.readLabel(e, "y:EdgeLabel");
		this.type = determineType(e);
		
	}

	public GraphEdge(String id, String sourceNodeId, String targetNodeId, String label, InformationLinkType type) {
		super();
		this.id = id;
		this.source = sourceNodeId;
		this.target = targetNodeId;
		this.label = label;
		this.type = type;
	}
	
	private InformationLinkType determineType(Element e) {
		InformationLinkType type = null;
		
		
		return type;
	}
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getSource() {
		return source;
	}
	public void setSource(String source) {
		this.source = source;
	}
	public String getTarget() {
		return target;
	}
	public void setTarget(String target) {
		this.target = target;
	}
	public String getLabel() {
		return label;
	}
	public void setLabel(String label) {
		this.label = label;
	}
	@Override
	public String toString() {
		return "GraphEdge [id=" + id + ", sourceNodeId=" + source + ", targetNodeId=" + target + ", label="
				+ label + "]";
	}
	public InformationLinkType getType() {
		return type;
	}
	public void setType(InformationLinkType type) {
		this.type = type;
	}

}
