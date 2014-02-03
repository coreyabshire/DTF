package edu.purdue.dtf.game;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import android.util.Log;

/**
 * Board provides the basic game grid and is responsible for all the core game
 * logic. It receives input from either human or AI based agents, and then
 * notifies all views and other listeners with something happens for which they
 * may need to update. At any given time, the board represents the current state
 * of the game.
 */
public final class Board {
	
	// Tag used for logging from this class.
	private final static String TAG = "BOARD";

	// Each player gets so many moves (actions) per turn.
	private static final int MOVES_PER_TURN = 3;

	private static final int SHIELD_COUNTER_START = 4;
	
	private static final int STUN_COUNTER_START = 4;
	
	// Keeps track of whose turn it is in this state. Cycles between G and R.
	private String whoseTurn;

	// Tracks how many moves the current player has left.
	private int movesRemaining;

	// The size of the board grid in terms of number of squares.
	private int width, height;

	// Tracks whom to notify of board events like a move occurred or next turn.
	private List<BoardListener> listeners;
	
	// Stores all the per square board state.
	private Piece grid[][] = null;

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
		this.grid = new Piece[width][height];
		this.whoseTurn = "G";
		this.movesRemaining = Board.MOVES_PER_TURN;
	}

	/**
	 * Removes all the pieces from the board leaving it empty.
	 */
	public void clearBoard() {
		for (int y = 0; y < height; ++y) {
			for (int x = 0; x < width; ++x) {
				grid[x][y] = null;
			}
		}
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
		clearBoard();
		BufferedReader r = new BufferedReader(new InputStreamReader(in));
		for (int y = 0; y < height; ++y) {
			String[] tokens = r.readLine().split(" ");
			for (int x = 0; x < width; ++x) {
				if (!"*".equals(tokens[x].substring(0, 1))) {
					Piece piece = Piece.valueOf(tokens[x]);
					grid[x][y] = piece;
					Log.d(TAG,
							String.format("added %s at (%d,%d)", piece, x, y));
				}
			}
		}
		this.whoseTurn = "G";
		this.movesRemaining = Board.MOVES_PER_TURN;
	}

	/**
	 * Returns the piece at position p.
	 * 
	 * @param p
	 *            The position of the square at which the piece is sought.
	 * @return The character code of the piece at this position.
	 */
	public Piece getPiece(Position p) {
		return grid[p.x][p.y];
	}

	/**
	 * Returns whether the game is over or not.
	 * 
	 * @return True if the game over condition is satisfied on the game board,
	 *         false otherwise.
	 */
	public boolean isGameOver() {
		Log.d(TAG, "Winner: " + getWinner());
		return getWinner() != null;
	}

	/**
	 * Return the player that won the game. Only applies when the game is
	 * actually over.
	 * 
	 * @return "G" or "R" depending on whether Gold or Red wins. Returns null in
	 *         the case where the game is not over, and TIE in case there's a
	 *         tie.
	 */
	public String getWinner() {
		// count up all the flags that are still alive for each player
		int g = 0, r = 0; // active flag counts
		for (int y = 0; y < height; ++y) {
			for (int x = 0; x < width; ++x) {
				Piece p = grid[x][y];
				if (p != null) {
					if (p instanceof Flag) {
						if (p.getHitPoints() > 0) {
							if ("G".equals(p.getBelongsTo()))
								++g;
							else
								++r;
						}
					}
				}
			}
		}
		// determine whether or not there is a winner based on those counts
		if (g == 0 && r != 0)
			return "R";
		else if (g != 0 && r == 0)
			return "G";
		else if (g == 0 && r == 0)
			return "TIE"; // not sure if this is even possible
		else
			return null;
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
	public boolean isOnBoard(Position p) {
		return p.x >= 0 && p.x < width && p.y >= 0 && p.y < height;
	}

	/**
	 * Switches to the other players turn.
	 */
	private void nextPlayer() {
		whoseTurn = whoseTurn.equals("G") ? "R" : "G";
		movesRemaining = Board.MOVES_PER_TURN;
		
		for (int y = 0; y < height; ++y) {
			for (int x = 0; x < width; ++x) {
				Piece p = grid[x][y];
				if (p != null) {
					p.moves = 0;
					p.fired = false;
					if (p.stunned) {
						--p.stunCounter;
						if (p.stunCounter <= 0) {
							p.stunned = false;
							for (BoardListener listener : listeners)
								listener.onPieceUnstunned(new Position(x, y));
						}
					}
					if (p.shielded) {
						--p.shieldCounter;
						if (p.shieldCounter <= 0) {
							p.shielded = false;
							for (BoardListener listener : listeners)
								listener.onPieceUnshielded(new Position(x, y));
						}
					}
				}
			}
		}

	}

	/**
	 * Move the piece at position a to position b.
	 * 
	 * @param a
	 *            The position to move from.
	 * @param b
	 *            The position to move to.
	 */
	public void movePiece(Position a, Position b) {
		grid[b.x][b.y] = grid[a.x][a.y];
		grid[a.x][a.y] = null;
		grid[b.x][b.y].moves++; 
		for (BoardListener listener : listeners)
			listener.onPieceMoved(a, b);
		--movesRemaining;
		if (movesRemaining == 0)
			nextPlayer();
	}

	public boolean isRotatable(Position a) {
		return hasPiece(a) && getPiece(a).isRotatable() && getPiece(a).hasMovesRemaining();
	}
	
	public boolean canBeFired(Position a, Projectile p) {
		return hasPiece(a) 
			&& ((getPiece(a) instanceof Slingshot && p == Projectile.ROCK) || 
				(getPiece(a) instanceof Obelisk   && p != Projectile.ROCK))
			&& getPiece(a).hasMovesRemaining()
			&& !getPiece(a).hasFired();
	}

	public void rotatePiece(Position a, Rotation d) {
		RotatablePiece p = (RotatablePiece) getPiece(a);
		p.rotate(d);
		for (BoardListener listener : listeners)
			listener.onPieceRotated(a, d);
		--movesRemaining;
		if (movesRemaining == 0)
			nextPlayer();
	}

	/**
	 * Determine if the position contains a piece that is hittable.
	 * 
	 * A piece is hittable if it is not rubble (hit points > 0);
	 * 
	 * @param p
	 *            The position to check.
	 * @return True if a hittable piece is there, false otherwise.
	 */
	public boolean hasHittablePiece(Position p) {
		return hasPiece(p);
	}

	/**
	 * Fires projectile p from the piece at position a.
	 * 
	 * Checks along the projectile path for any piece that is struck and applies
	 * the hit if any is found. If the projectile exits the board it has no
	 * effect.
	 * 
	 * @param a
	 *            The position of the piece firing the projectile.
	 * @param p
	 *            The type of projectile fired.
	 */
	public void firePiece(Position a, Projectile p) {
		Piece piece = getPiece(a);
		Direction d = piece.getDirection();
		Position pos = new Position(a);
		List<Position> path = new ArrayList<Position>();
		List<Direction> dirs = new ArrayList<Direction>();
		path.add(new Position(pos));
		while (d != null) {
			do
				pos = pos.add(d.getOffset());
			while (isOnBoard(pos) && !hasHittablePiece(pos));
			path.add(new Position(pos));
			dirs.add(d);
			if (isOnBoard(pos) && getPiece(pos) instanceof Reflector)
				d = ((Reflector) getPiece(pos)).getReflection(d);
			else 
				d = null;
		}
		
		// onProjectileFired needs to fire before any spell effect listeners
		// so the projectileAnim can activate and the subsequent events appear
		// after the animation completes
		for (BoardListener listener : listeners)
			listener.onProjectileFired(path, dirs, p);

		if (isOnBoard(pos) && hasHittablePiece(pos)) {
			Piece target = getPiece(pos);
			switch (p) {
			case FIRE:
				if (target instanceof Torch) {
					Torch torch = (Torch) target;
					if (!torch.isLit() && !torch.shielded && torch.hitPoints > 0) {
						torch.setLit(true);
						for (BoardListener listener : listeners)
							listener.onFireLit(pos);
					}
				} else if (!(target instanceof Boulder || target instanceof Obelisk)) {
					target.setBurned(true);
					target.hitPoints = 0;
				}
				break;
			case WATER:
				if (target instanceof Torch) {
					Torch torch = (Torch) target;
					if (torch.isLit() && !target.shielded) {
						torch.setLit(false);
						for (BoardListener listener : listeners)
							listener.onFireUnlit(pos);
					}
				}
				break;
			case ROOT:
				if (!(target instanceof Boulder || target instanceof Obelisk)) {
					if (!target.hasRoots() && !target.shielded) {
						target.setRoots(true);
						for (BoardListener listener : listeners)
							listener.onRooted(pos);
					}
				}
				break;
			case SHIELD:
				if (!target.shielded) { 
					if (target instanceof Torch) {
						Torch torch = (Torch) target;
						if (torch.isLit() && !target.shielded) {
							torch.setLit(false);
							for (BoardListener listener : listeners)
								listener.onFireUnlit(pos);
						}
					}
					target.shielded = true;
					target.shieldCounter = SHIELD_COUNTER_START;
					for (BoardListener listener : listeners)
						listener.onPieceShielded(pos);
				}
				break;
			case STUN:
				if (target instanceof Torch) {
					Torch torch = (Torch) target;
					if (torch.isLit() && !target.shielded) {
						torch.setLit(false);
						for (BoardListener listener : listeners)
							listener.onFireUnlit(pos);
					}
				}
				target.stunned = true;
				target.stunCounter = STUN_COUNTER_START;
				for (BoardListener listener : listeners)
					listener.onPieceStunned(pos);
				break;
			case HEAL:
				if (!target.shielded) {
					if (target.getHitPoints() < target.getMaxHitPoints()) {
						++target.hitPoints;
					}
					if (target.isStunned()) {
						target.stunned = false;
						for (BoardListener listener : listeners)
							listener.onPieceUnstunned(pos);
					}
					if (target.hasRoots()) {
						target.setRoots(false);
						for (BoardListener listener : listeners)
							listener.onUnrooted(pos);
					}
				}
				break;
			case ROCK:
				if (target instanceof Torch) {
					Torch torch = (Torch) target;
					if (torch.isLit() && !target.shielded) {
						torch.setLit(false);
						for (BoardListener listener : listeners)
							listener.onFireUnlit(pos);
					}
				}
				if (target.hitPoints > 0)
					--target.hitPoints;
				break;
			default:
				Log.e(TAG, "unhandled projectile: " + p.toString());
				break;
			}
		}
		--movesRemaining;
		piece.moves++;
		piece.fired = true;
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
		return isOnBoard(a) && hasPiece(a)
				&& getPiece(a).getBelongsTo().equals(getWhoseTurn());
	}

	/**
	 * Calculates the distance between two positions in terms of the number of
	 * moves it would take to get there.
	 * 
	 * @param a
	 *            The first position.
	 * @param b
	 *            The second position.
	 * @return The distance between the two positions.
	 */
	public int getDistance(Position a, Position b) {
		return Math.abs(a.x - b.x) + Math.abs(a.y - b.y);
	}

	/**
	 * Checks whether a given move is valid, from point a to point b.
	 * 
	 * @param a
	 *            The from position.
	 * @param b
	 *            The to position.
	 * @return
	 */
	public boolean isValidMove(Position a, Position b) {
		if (isOnBoard(a) && isOnBoard(b) && hasPiece(a)) {
			Piece piece = getPiece(a);
			return (!hasPiece(b) || piece.canTake(getPiece(b)))
				&& piece.getBelongsTo().equals(getWhoseTurn())
				&& getDistance(a, b) == 1
				&& piece.getMoves() < piece.getMovesPerTurn()
				&& !piece.hasRoots() 
				&& !piece.isStunned() 
				&& !piece.isBurned() 
				&& !(piece.getHitPoints() == 0);
		} else {
			return false;
		}
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

	/**
	 * Tells whose turn it is to move.
	 * 
	 * @return G if its the gold player's turn, R if it's red's.
	 */
	public String getWhoseTurn() {
		return whoseTurn;
	}
}
