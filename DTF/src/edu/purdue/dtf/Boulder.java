package edu.purdue.dtf;

public final class Boulder extends Piece {

	public Boulder(String token) {
		super(token);
	}

	@Override
	public int getMaxHitPoints() {
		return 4;
	}
	
}
