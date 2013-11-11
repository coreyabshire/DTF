package edu.purdue.dtf.game;

public class Piece {

	protected String belongsTo;
	protected String pieceType;
	protected int hitPoints;
	protected Direction directionFacing;
	protected boolean roots;
	
	public Piece(String token) {
		belongsTo = token.substring(0, 1);
		pieceType = token.substring(1, 2);
		hitPoints = getMaxHitPoints() - Integer.parseInt(token.substring(2, 3));
		directionFacing = Direction.values()[Integer.parseInt(token.substring(3,4))];
		roots = false;
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

	public int getHitPoints() {
		return hitPoints;
	}

	public Direction getDirection() {
		return directionFacing;
	}
	
	public boolean canTake(Piece that) {
		return getMaxHitPoints() > that.getMaxHitPoints();
	}

	public boolean hasRoots() {
		return roots;
	}
	
	public void setRoots(boolean roots) {
		this.roots = roots;
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
		--hitPoints;
	}
}
