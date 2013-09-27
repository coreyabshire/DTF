package edu.purdue.dtf;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;

import android.util.Log;

/**
 * Board provides the basic game grid and is responsible for all the core game
 * logic. It receives input from either human or AI based agents, and then
 * notifies all views and other listeners with something happens for which they
 * may need to update. At any given time, the board represents the current 
 * state of the game.
 */
public class Board {

	// Tag used for logging from this class.
	private final static String TAG = "BOARD";

	// Each player gets so many moves (actions) per turn.
	private static final int MOVES_PER_TURN = 3;

	// Keeps track of whose turn it is in this state. Cycles between 0 and 1.
	private int whoseTurn;
	
	// Tracks how many moves the current player has left.
	private int movesRemaining;
	
	// The size of the board grid in terms of number of squares.
	private int width, height;
	
	// Tracks whom to notify of board events like a move occured or next turn.
	private List<BoardListener> listeners;
	
	// Stores all the per square board state.
	private String grid[][] = null;

	/**
	 * Constructs a board with the specified width and height, in terms or the
	 * number of squares the board is composed of. The standard game board for
	 * Destroy the Flags is 11 x 9.
	 * 
	 * @param width
	 *            The width of the game board in squares.
	 * @param height
	 *            The height of the game board in squares.
	 */
	public Board(int width, int height) {
		this.width = width;
		this.height = height;
		this.grid = new String[width][height];
		this.whoseTurn = 0;
		this.movesRemaining = Board.MOVES_PER_TURN;
	}
	
	/**
	 * Initializes the board from a text file input stream.
	 * 
	 * @param in
	 *            The input stream with the file in the standard format.
	 * @throws IOException
	 *             For any errors during reading the stream.
	 */
	public void setFromStream(InputStream in) throws IOException {
		BufferedReader r = new BufferedReader(new InputStreamReader(in));
		for (int y = 0; y < height; ++y) {
			String[] tokens = r.readLine().split(" ");
			for (int x = 0; x < width; ++x) {
				String piece = tokens[x].substring(0, 2);
				if (!"**".equals(piece)) {
					grid[x][y] = piece;
					Log.d(TAG, String.format("added %s at (%d,%d)", piece, x, y));
				}
			}
		}
	}

	/**
	 * Returns the piece at position p.
	 * 
	 * @param p
	 *            The position of the square at which the piece is sought.
	 * @return The character code of the piece at this position.
	 */
	public String getPiece(Position p) {
		return grid[p.x][p.y];
	}
	
	/**
	 * Returns whether the game is over or not.
	 * 
	 * @return True if the game over condition is satisfied on the game board,
	 *         false otherwise.
	 */
	public boolean isGameOver() {
		return false;
	}
	
	/**
	 * Return the player that won the game. Only applies when the game is
	 * actually over.
	 * 
	 * @return 0 if nobody has won yet, 1 if player 1 (the gold player) won, and
	 *         2 if player 2 (the red player) won.
	 */
	public int getWinner() {
		return 0;
	}

	/**
	 * Gets the width of the game board in terms of grid squares.
	 * 
	 * @return The width of the grid.
	 */
	public int getWidth() {
		return width;
	}

	/**
	 * Gets the height of the game board in terms of grid squares.
	 * 
	 * @return The height of the grid.
	 */
	public int getHeight() {
		return height;
	}
	
	/**
	 * Check if the given board position is actually on the board.
	 * 
	 * @param p
	 *            The board position to check.
	 * @return
	 */
	boolean isOnBoard(Position p) {
		return p.x >= 0 && p.x < width && p.y >= 0 && p.y < height; 
	}
	
	/**
	 * Switches to the other players turn.
	 */
	private void nextPlayer() {
		whoseTurn = whoseTurn == 1 ? 0 : 1;
		movesRemaining = Board.MOVES_PER_TURN;
	}
	
	/**
	 * Move the piece at position a to position b.
	 * 
	 * @param a
	 *            The position to move from.
	 * @param b
	 *            The position to move to.
	 */
	public void moveUnit(Position a, Position b) {
		grid[b.x][b.y] = grid[a.x][a.y];
		grid[a.x][a.y] = null;
		for (BoardListener listener : listeners)
			listener.onPieceMoved(a, b);
		--movesRemaining;
		if (movesRemaining == 0)
			nextPlayer();
	}
	
	/**
	 * Check if board position p currently has a piece.
	 * 
	 * @param p
	 *            The board position to check.
	 * @return True if there is the position is occupied, false otherwise.
	 */
	public boolean hasPiece(Position p) {
		return grid[p.x][p.y] != null;
	}

	/**
	 * Determines whether the given position is a valid square that the player
	 * could potentially move.
	 * 
	 * @param a
	 *            The position to check.
	 * @return True if this piece would be OK to move, false otherwise.
	 */
	public boolean isValidMoveStart(Position a) {
		return isOnBoard(a) && hasPiece(a);
	}

	/**
	 * Checks whether a given move is valid, from point a to point b.
	 * @param a
	 * @param b
	 * @return
	 */
	public boolean isValidMove(Position a, Position b) {
		return isOnBoard(a) && isOnBoard(b) && hasPiece(a) && !hasPiece(b);
	}
	
	/**
	 * The given listener will be called for important game updates like when a
	 * piece is moved on the board.
	 * 
	 * @param listener
	 *            The listener instance to be called.
	 */
	public void addBoardListener(BoardListener listener) {
		if (listeners == null) {
			listeners = new LinkedList<BoardListener>();
		}
		listeners.add(listener);
	}

	
}
