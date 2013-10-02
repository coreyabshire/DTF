package edu.purdue.dtf;

public class Piece {

	private String belongsTo;
	private String pieceType;
	private int hitPoints;
	private int direction;

	public Piece(String token) {
		belongsTo = token.substring(0, 1);
		pieceType = token.substring(1, 2);
		hitPoints = getMaxHitPoints() - Integer.parseInt(token.substring(2, 3));
		direction = Integer.parseInt(token.substring(3, 4));
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

	public int getDirection() {
		return direction;
	}

	public String getTemplateCode() {
		return belongsTo + pieceType;
	}

	public boolean canTake(Piece that) {
		return getMaxHitPoints() > that.getMaxHitPoints();
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

}
