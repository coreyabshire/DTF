package edu.purdue.dtf;

public final class Obelisk extends RotatablePiece {

	public Obelisk(String token) {
		super(token);
	}

	@Override
	public int getMaxHitPoints() {
		return 2;
	}
	
}
