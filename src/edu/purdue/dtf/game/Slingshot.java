package edu.purdue.dtf.game;

public final class Slingshot extends RotatablePiece {

	public Slingshot(String token) {
		super(token);
	}

	@Override
	public int getMovesPerTurn() {
		return 2;
	}
	
}
