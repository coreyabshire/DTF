package edu.purdue.dtf;

import java.util.List;

/**
 * BoardListener is implemented on objects that need to do something whenever
 * something important happens in the core game logic, such as a piece being
 * moved.
 */
public interface BoardListener {

	/**
	 * Called whenever a legal move occurs on the game board.
	 * 
	 * @param a
	 *            The from position of the move.
	 * @param b
	 *            The to position of the move.
	 */
	public void onPieceMoved(Position a, Position b);

	/**
	 * Called whenever a legal piece rotation occurs on the game board.
	 * 
	 * @param a
	 *            The position of the rotated piece.
	 * @param d
	 *            The direction of rotation.
	 */
	public void onPieceRotated(Position a, Rotation d);

	/**
	 * Called whenever a projectile is fired on the game board.
	 * 
	 * @param path
	 *            The path of the projectile fired, starting with the position
	 *            of the piece that fired the projectile.
	 * @param projectile
	 *            The kind of projectile fired.
	 */
	public void onProjectileFired(List<Position> path, List<Direction> dirs,
			Projectile projectile);

}
