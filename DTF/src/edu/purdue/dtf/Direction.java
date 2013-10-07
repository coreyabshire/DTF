package edu.purdue.dtf;

import com.threed.jpct.SimpleVector;
import static java.lang.Math.*;

public enum Direction {

	NORTH     (  0.0, new Position( 0, -1)),
	NORTHEAST ( 45.0, new Position( 1, -1)),
	EAST      ( 90.0, new Position( 1,  0)),
	SOUTHEAST (135.0, new Position( 1,  1)),
	SOUTH     (180.0, new Position( 0,  1)),
	SOUTHWEST (225.0, new Position(-1,  1)),
	WEST      (270.0, new Position(-1,  0)),
	NORTHWEST (315.0, new Position(-1, -1));
	
	private final float angle;
	private final SimpleVector vector;
	private final Position offset;
	
	private static float toRadians(double degrees) {
		return (float) (PI / 180.0 * degrees);
	}
	
	Direction(double degrees, Position offset) {
		this.angle = toRadians(degrees);
		this.vector = new SimpleVector(cos(angle - (Math.PI / 2.0)), sin(angle - (PI / 2.0)), 0.0f);
		this.offset = offset;
	}
	
	public float getAngle() {
		return angle;
	}
	
	public SimpleVector getVector() {
		return vector;
	}
	
	public Position getOffset() {
		return offset;
	}

}
