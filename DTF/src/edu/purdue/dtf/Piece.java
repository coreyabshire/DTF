package edu.purdue.dtf;

public class Piece {

	private String belongsTo;
	private String pieceType;
	private int maxHitPoints;
	private int hitPoints;
	private int direction;

	public Piece(String token) {
		belongsTo = token.substring(0, 1);
		pieceType = token.substring(1, 2);
		hitPoints = Integer.parseInt(token.substring(2, 3));
		direction = Integer.parseInt(token.substring(3, 4));
	}
	
	public Piece(String belongsTo, String pieceType, int hitPoints,
			int maxHitPoints, int direction) {
		this.belongsTo = belongsTo;
		this.pieceType = pieceType;
		this.hitPoints = hitPoints;
		this.maxHitPoints = maxHitPoints;
		this.direction = direction;
	}

	public String getBelongsTo() {
		return belongsTo;
	}

	public String getPieceType() {
		return pieceType;
	}

	public int getMaxHitPoints() {
		return maxHitPoints;
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
	
}
