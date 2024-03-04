package keml.io.graphml;

public class PositionalInformation {
	
	//public String id = null;
	
	private Float xLeft = null;
	private Float xRight = null;
	private Float yLow = null;
	private Float yHigh = null;
	
	
	
	public PositionalInformation(Float xLeft, Float xRight, Float yLow, Float yHigh) {
		this.xLeft = xLeft;
		this.xRight = xRight;
		this.yLow = yLow;
		this.yHigh = yHigh;
	}
	
	@Override
	public String toString() {
		return "PositionalInformation [xLeft=" + xLeft + ", xRight=" + xRight + ", yLow=" + yLow + ", yHigh=" + yHigh
				+ "]";
	}

	public Float getxLeft() {
		return xLeft;
	}
	public void setxLeft(Float xLeft) {
		this.xLeft = xLeft;
	}
	public Float getxRight() {
		return xRight;
	}
	public void setxRight(Float xRight) {
		this.xRight = xRight;
	}
	public Float getyLow() {
		return yLow;
	}
	public void setyLow(Float yLow) {
		this.yLow = yLow;
	}
	public Float getyHigh() {
		return yHigh;
	}
	public void setyHigh(Float yHigh) {
		this.yHigh = yHigh;
	}

	
}
