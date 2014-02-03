package edu.purdue.dtf.game;

public final class Boulder extends Piece {

	public Boulder(String token) {
		super(token);
	}

	@Override
	public int getMaxHitPoints() {
		return 4;
	}
	
	@Override
	public int getMovesPerTurn() {
		return 1;
	}
	
}
