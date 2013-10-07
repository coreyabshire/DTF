package edu.purdue.dtf;

import java.util.Locale;

import com.threed.jpct.SimpleVector;
import static java.lang.Math.*;

/**
 * Position is a set of grid coordinates for a particular board on the square.
 */
public final class Position {

	// Stores the x, y board coordinates the position represents.
	public final int x, y;

	/**
	 * Constructs a new position given the x, y coordinates of the square.
	 * @param x The zero based column in the grid the position represents.
	 * @param y The zero based row in the grid the position represents.
	 */
	Position(int x, int y) {
		this.x = x;
		this.y = y;
	}
	
	Position(Position p) {
		this.x = p.x;
		this.y = p.y;
	}

	/**
	 * Returns a string representation of the position.
	 */
	public String toString() {
		return String.format(Locale.ENGLISH, "(%d, %d)", x, y);
	}

	@Override
	public boolean equals(Object other) {
		Position that = (Position) other;
		return this.x == that.x && this.y == that.y;
	}
	
	public Position add(Position p) {
		return Position.valueOf(this.x + p.x, this.y + p.y);
	}
	
	public SimpleVector toVector() {
		return new SimpleVector((float) this.x, (float) this.y, 0.0f);
	}
	
	public static Position valueOf(SimpleVector v) {
		return new Position(round(v.x), round(v.y));
	}
	
	public static Position valueOf(int x, int y) {
		return new Position(x, y);
	}
}
