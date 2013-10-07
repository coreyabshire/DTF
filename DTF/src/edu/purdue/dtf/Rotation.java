package edu.purdue.dtf;

public enum Rotation {

	CLOCKWISE         ( 45.0),
	COUNTER_CLOCKWISE (-45.0);
	
	private final float angle;
	
	private static float toRadians(double degrees) {
		return (float) (Math.PI / 180.0 * degrees);
	}
	
	Rotation(double degrees) {
		angle = toRadians(degrees);
	}
	
	public float getAngle() {
		return angle;
	}

}
