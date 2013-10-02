package edu.purdue.dtf;

public class Obelisk extends Piece {

	public Obelisk(String token) {
		super(token);
	}

	@Override
	public int getMaxHitPoints() {
		return 2;
	}
	
}
