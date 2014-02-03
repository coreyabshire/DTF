package edu.purdue.dtf.game;

public final class Torch extends Piece {

	private boolean lit;
	
	public Torch(String token) {
		super(token);
		lit = true;
	}

	public boolean isLit() {
		return lit;
	}

	public void setLit(boolean lit) {
		this.lit = lit;
	}
	
	@Override
	public boolean canTake(Piece that) {
		return isLit() 
			&& (!(that instanceof Boulder || that instanceof Obelisk))
			&& !that.isShielded();
	}

}
