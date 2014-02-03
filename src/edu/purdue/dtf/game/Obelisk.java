package edu.purdue.dtf.game;

public final class Obelisk extends RotatablePiece {

	public Obelisk(String token) {
		super(token);
	}

	@Override
	public int getMaxHitPoints() {
		return 2;
	}
	
	@Override
	public int getMovesPerTurn() {
		return 2;
	}
	
}
