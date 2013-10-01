package edu.purdue.dtf;

import java.util.Locale;

/**
 * Position is a set of grid coordinates for a particular board on the square.
 */
public class Position {

	// Stores the x, y board coordinates the position represents.
	public int x, y;

	/**
	 * Constructs a new position given the x, y coordinates of the square.
	 * @param x The zero based column in the grid the position represents.
	 * @param y The zero based row in the grid the position represents.
	 */
	Position(int x, int y) {
		this.x = x;
		this.y = y;
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
	
}
