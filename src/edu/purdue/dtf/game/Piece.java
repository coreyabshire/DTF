package edu.purdue.dtf.game;

import android.util.Log;

public class Piece {

	private static String TAG = "Piece";
	
	protected static int DEFAULT_MOVES_PER_TURN = 3; 

	protected String belongsTo;
	protected String pieceType;
	protected int hitPoints;
	protected Direction directionFacing;
	protected boolean roots;
	protected boolean fired;
	protected int moves;
	protected boolean stunned;
	protected int stunCounter;
	protected boolean burned;
	protected boolean shielded;
	protected int shieldCounter;
	
	public Piece(String token) {
		belongsTo = token.substring(0, 1);
		pieceType = token.substring(1, 2);
		hitPoints = getMaxHitPoints() - Integer.parseInt(token.substring(2, 3));
		directionFacing = Direction.values()[Integer.parseInt(token.substring(3,4))];
		roots = false;
		fired = false;
		moves = 0;
		stunned = false;
		stunCounter = 0;
		burned = false;
		shielded = false;
		shieldCounter = 0;
	}

	public String getBelongsTo() {
		return belongsTo;
	}

	public String getPieceType() {
		return pieceType;
	}

	public int getMaxHitPoints() {
		return 1;
	}
	
	public int getMovesPerTurn() {
		return DEFAULT_MOVES_PER_TURN;
	}

	public int getMoves() {
		return moves;
	}
	
	public int getHitPoints() {
		return hitPoints;
	}
	
	public boolean hasMovesRemaining() {
		return getMoves() < getMovesPerTurn();
	}

	public Direction getDirection() {
		return directionFacing;
	}
	
	public boolean canTake(Piece that) {
		return getMaxHitPoints() > that.getMaxHitPoints() && !that.isShielded();
	}

	public boolean hasRoots() {
		return roots;
	}
	
	public void setRoots(boolean roots) {
		this.roots = roots;
	}
	
	public boolean isStunned() {
		return stunned;
	}
	
	public boolean hasFired() {
		return fired;
	}
	
	public void setFired(boolean fired) {
		this.fired = fired;
	}
	
	public boolean isBurned() {
		return burned;
	}
	
	public void setBurned(boolean burned) {
		this.burned = burned;
	}
	
	public boolean isShielded() {
		return shielded;
	}
	
	public void setShielded(boolean shielded) {
		this.shielded = shielded;
	}
	
	public static Piece valueOf(String token) {
		switch (token.charAt(1)) {
		case 'B':
			return new Boulder(token);
		case 'F':
			return new Flag(token);
		case 'X':
			return new Obelisk(token);
		case 'V':
			return new Slingshot(token);
		case 'T':
			return new Torch(token);
		case 'R':
			return new Reflector(token);
		default:
			throw new RuntimeException("invalid piece type: "
					+ token.substring(1, 2));
		}
	}

	public boolean isRotatable() {
		return false;
	}
	
	public void hit(Projectile p) {

	}
}
