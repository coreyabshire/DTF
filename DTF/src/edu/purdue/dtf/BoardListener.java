package edu.purdue.dtf;

/**
 * BoardListener is implemented on objects that need to do something
 * whenever something important happens in the core game logic, such
 * as a piece being moved.
 */
public interface BoardListener {

	/**
	 * Called whenever a legal move occurs on the game board. 
	 * @param a The from position of the move.
	 * @param b The to position of the move.
	 */
	public void onPieceMoved(Position a, Position b);
	
}
