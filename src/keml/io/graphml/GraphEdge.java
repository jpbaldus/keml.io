package keml.io.graphml;

public class GraphEdge {
	
	private String id;
	private String source;
	private String target;
	private String label;
	
	
	public GraphEdge(String id, String sourceNodeId, String targetNodeId, String label) {
		super();
		this.id = id;
		this.source = sourceNodeId;
		this.target = targetNodeId;
		this.label = label;
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

}
