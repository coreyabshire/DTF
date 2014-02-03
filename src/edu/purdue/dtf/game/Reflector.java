package edu.purdue.dtf.game;

public final class Reflector extends RotatablePiece {

	private final static int X = -1;
	
	private int[][] reflections = 
			{{4,3,X,1,0,7,X,5},
			 {6,5,4,X,2,1,0,X},
			 {X,7,6,5,X,3,2,1},
			 {2,X,0,7,6,X,4,3},
			 {4,3,X,1,0,7,X,5},
			 {6,5,4,X,2,1,0,X},
			 {X,7,6,5,X,3,2,1},
			 {2,X,0,7,6,X,4,3}};
	
	public Reflector(String token) {
		super(token);
	}

	public Direction getReflection(Direction pd) {
		int i = reflections[getDirection().ordinal()][pd.ordinal()];
		return i == X ? null : Direction.values()[i];
	}
	
}
