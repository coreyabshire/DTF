package edu.purdue.dtf;

import static edu.purdue.dtf.Rotation.CLOCKWISE;
import static edu.purdue.dtf.Rotation.COUNTER_CLOCKWISE;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

import com.threed.jpct.Camera;
import com.threed.jpct.FrameBuffer;
import com.threed.jpct.Interact2D;
import com.threed.jpct.Light;
import com.threed.jpct.Loader;
import com.threed.jpct.Object3D;
import com.threed.jpct.RGBColor;
import com.threed.jpct.SimpleVector;
import com.threed.jpct.Texture;
import com.threed.jpct.TextureManager;
import com.threed.jpct.World;
import com.threed.jpct.util.BitmapHelper;
import com.threed.jpct.util.MemoryHelper;

/**
 * GameView takes care of drawing and receiving user input. It is also
 * responsible for loading all the resources it needs to do so, such as the 3D
 * objects and textures.
 */
public final class GameView extends GLSurfaceView implements OnTouchListener,
		GLSurfaceView.Renderer, BoardListener {

	// Tag used for logging from this class.
	private static final String TAG = "GameView";

	// Float copy of PI constant for piece rotation and so forth.
	private static float PI = (float) Math.PI;

	// Represents a fully opaque transparency setting, which is useful for
	// just turning on blending mode without making the object transparent.
	private static final int FULLY_OPAQUE = 255;

	private static GameView master = null;

	// The World object is the main object in jPCT that coordinates all the
	// geometry, transformations, and rendering.
	private World world;

	// The frame buffer represents the rendered pixels that the world is drawn
	// onto and then displayed in the game view.
	private FrameBuffer fb;

	// The background color that shows through on the main game screen for any
	// part not obscured by some game object.
	private RGBColor backgroundColor = new RGBColor(0, 0, 0);

	// The sun just represents one master light source that we need in order to
	// be able to see any of our 3D objects when we draw our game world.
	private Light sun = null;

	// Piece templates are used to have a copy of each 3D object to clone to put
	// units onto the board. There are two for each piece type, one for the red
	// player for each type and one for the gold player for each type. The key
	// is based off of the codes in the board text file template, RF for red
	// flag for instance.
	private Map<String, Object3D> pieceTemplates;

	// Squares contains the clones of the light and dark squares that are used
	// to represent the game board in the world.
	private Object3D squares[][] = null;

	// Contains the actual clones of the piece templates for all the 3D
	// objects representing each players units on the board. This is a
	// convenient representation, because we can easily look up the piece based
	// on the board representation we get back after projecting the screen space
	// touch back into 3D, and from there back into board coordinates.
	private Object3D pieces[][] = null;

	// Selector is the 3D object that is used to highlight a square when a
	// player chooses the from part of one of their moves.
	private Object3D selector = null;

	// Turn indicator is a 3D object that is used to indicate whose turn it is.
	private Object3D turnIndicatorGold = null;
	private Object3D turnIndicatorRed = null;

	// Win overlays are displayed when somebody wins the game.
	private Map<String, Object3D> winOverlays;

	private Object3D projectile = null;

	private SimpleVector projectileVelocity = new SimpleVector(0.0f, 0.1f, 0.0f);
	private List<Position> projectilePath = null;
	private List<Direction> projectileDirs = null;
	private int projectileMilestone = 0;
	private boolean projectileActive = false;
	private final float projectileSpeed = 0.2f;

	// Stores the objects making up the side panels. One per player.
	private class ActionPanel {
		Object3D rotateCW = null;
		Object3D rotateCCW = null;
		Object3D shootRock = null;

		// other spells and capabilities

		void addToWorld(World world) {
			world.addObject(rotateCW);
			world.addObject(rotateCCW);
			world.addObject(shootRock);
		}
	}

	// The (G)old and (R)ed action panels.
	private Map<String, ActionPanel> actionPanels;

	private final SimpleVector cameraPosition = new SimpleVector(5.5f, 4.0f,
			-13.0f);

	// The moveStart position is used to capture the first part of a users
	// move. When the user first touches the screen it indicates the piece
	// they want to try to move, and when they click again, hopefully on a
	// valid target for whichever piece they've chosen, it either moves there
	// or executes the ability they chose.
	private Position selected;

	// Board is a reference to the main game board, which represents the model
	// that this view is representing visually for the user.
	private Board board;

	private Context context;

	// The frames per second that the view is able to render the game. This is
	// a standard measure of performance.
	private int fps = 0;

	// The prior time captured during the last update, to help with calculation
	// of the fps mentioned above.
	private long time = System.currentTimeMillis();

	// Temporary fix to ensure onDrawFrame doesn't generate null pointer
	// exceptions because it runs before all objects are loaded. Better way
	// would be to investigate and figure out a better way to load.
	private boolean fullyLoaded = false;

	/**
	 * Constructs a new GameView.
	 * 
	 * @param context
	 *            Typically this would be the activity to which you are adding
	 *            the view.
	 */
	public GameView(Context context) {
		super(context);
		this.context = context;
		setEGLConfigChooser(new GLSurfaceView.EGLConfigChooser() {
			public EGLConfig chooseConfig(EGL10 egl, EGLDisplay display) {
				// Ensure that we get a 16bit framebuffer. Otherwise, we'll fall
				// back to Pixelflinger on some device (read: Samsung I7500)
				int[] attributes = new int[] { EGL10.EGL_DEPTH_SIZE, 16,
						EGL10.EGL_NONE };
				EGLConfig[] configs = new EGLConfig[1];
				int[] result = new int[1];
				egl.eglChooseConfig(display, attributes, configs, 1, result);
				return configs[0];
			}
		});
		setOnTouchListener(this);
		setRenderer(this);
	}

	/**
	 * Sets the board that this view is supposed to be displaying.
	 * 
	 * @param board
	 *            The board to visualize.
	 */
	public void setBoard(Board board) {
		// moved out of constructor because it was complaining in a warning
		// about not using a standard method signature for the view.
		this.board = board;
		board.addBoardListener(this);
	}

	/**
	 * Convert from screen coordinates back to a usable board position.
	 * 
	 * @param x
	 *            The x screen coordinate.
	 * @param y
	 *            The y screen coordinate.
	 * @return The board position.
	 */
	private Position screenToBoard(int x, int y) {
		SimpleVector v = Interact2D
				.reproject2D3DWS(world.getCamera(), fb, x, y);
		v.scalarMul(-cameraPosition.z);
		v.add(new SimpleVector(cameraPosition.x, cameraPosition.y, 0.0f));
		int boardX = (int) Math.round(v.x);
		int boardY = (int) Math.round(v.y);
		return new Position(boardX, boardY);
	}

	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		// This method is not used at present. Based on an example from
		// JPCT, all the initialization occurs in onSurfaceChanged instead
		// after we know the width and height of the screen.
	}

	@Override
	public void onSurfaceChanged(GL10 gl, int width, int height) {
		if (fb != null) {
			fb.dispose();
		}
		fb = new FrameBuffer(gl, width, height);

		if (master == null) {
			try {
				initMaster();
			} catch (Exception e) {
				// TODO probably need user level notification as well here
				Log.e(TAG, "initialization error", e);
			}
		}
	}

	/**
	 * Updates the frames per second counter and writes it to the log.
	 */
	private void updateFPS() {
		if (System.currentTimeMillis() - time >= 1000) {
			Log.v("FPS", fps + "fps");
			fps = 0;
			time = System.currentTimeMillis();
		}
		fps++;
	}

	/**
	 * Handles animations, projectiles and so forth.
	 * 
	 * This handles both updates that need to happen per frame (and thus at a
	 * different frequency than the underlying model), or that simply need to
	 * happen on the render thread instead of the draw thread.
	 */
	private void updateDrawState() {
		if (projectileActive)
			updateProjectile();
	}

	/**
	 * Draws the frame to the screen.
	 */
	@Override
	public void onDrawFrame(GL10 gl) {
		// TODO remove this if possible - should not need to check fully loaded
		if (!fullyLoaded)
			return;
		updateDrawState();
		fb.clear(backgroundColor);
		world.renderScene(fb);
		world.draw(fb);
		fb.display();
		updateFPS();
	}

	/**
	 * Returns the 3D object at position a.
	 * 
	 * @param a
	 *            The position of the 3D object to find.
	 * @return The 3D object representing the piece at the position.
	 */
	private Object3D getPiece(Position a) {
		return pieces[a.x][a.y];
	}

	/**
	 * Returns the appropriate piece template for the given piece.
	 * 
	 * @param piece
	 *            The piece to find the template of.
	 * @return The pieces template 3D object.
	 */
	private Object3D getTemplate(Piece piece) {
		return pieceTemplates.get(piece.getPieceType());
	}

	/**
	 * Checks if the game is over and handles the case when it is.
	 */
	private void checkGameOver() {
		if (board.isGameOver())
			winOverlays.get(board.getWinner()).setVisibility(true);
	}

	/**
	 * Move the piece at position a to position b.
	 * 
	 * @param a
	 *            The position to move from.
	 * @param b
	 *            The position to move to.
	 */
	public void onPieceMoved(final Position a, final Position b) {
		queueEvent(new Runnable() {
			public void run() {
				final Object3D piece = pieces[a.x][a.y];

				if (getPiece(b) != null) {
					world.removeObject(getPiece(b));
					pieces[b.x][b.y] = null;
				}

				pieces[a.x][a.y] = null;
				pieces[b.x][b.y] = piece;
				final Object3D template = getTemplate(board.getPiece(b));
				piece.clearTranslation();
				piece.translate(template.getTranslation());
				piece.translate(b.x, b.y, 0.0f);

				selected = null;
				selector.setVisibility(false);

				checkGameOver();
				updateTurnIndicator();
			}
		});
	}

	public void onPieceRotated(final Position a, final Rotation d) {
		queueEvent(new Runnable() {
			public void run() {
				getPiece(a).rotateZ(-d.getAngle());
				selected = null;
				selector.setVisibility(false);
				updateTurnIndicator();
			}
		});

	}

	@Override
	public void onProjectileFired(final List<Position> path,
			final List<Direction> dirs, final Projectile proj) {
		queueEvent(new Runnable() {
			public void run() {
				projectile.clearTranslation();
				projectilePath = path;
				projectileDirs = dirs;
				projectile.translate(getPiece(selected).getTranslation());
				projectileVelocity = new SimpleVector(dirs.get(0).getVector());
				projectileVelocity.scalarMul(projectileSpeed);
				projectileMilestone = 0;
				projectileActive = true;
				projectile.setVisibility(true);
			}
		});
	}

	/**
	 * Determines the appropriate texture for the given piece.
	 * 
	 * Figures out which texture should be shown based on both who the piece
	 * belongs to and the level of damage it has sustined.
	 * 
	 * @param p
	 *            The piece whose texture should be determined.
	 * @return The name of the texture that it should have.
	 */
	private String getTexture(Piece p) {
		final float numTextures = 4.0f;
		final String prefix = p.getBelongsTo().equals("G") ? "gold" : "red";
		final float maxhp = (float) p.getMaxHitPoints();
		final float hp = (float) p.getHitPoints();
		final float damage = maxhp - hp;
		final int texNum = (int) ((damage / maxhp) * numTextures);
		return String.format("%s%d.png", prefix, texNum);
	}

	/**
	 * Moves the projectile one step along its path.
	 * 
	 * If it has reached its destination, handles appropriate actions.
	 */
	private void updateProjectile() {
		final float closeEnough = 0.2f;
		SimpleVector pos = projectile.getTranslation();
		SimpleVector next = projectilePath.get(projectileMilestone + 1)
				.toVector();
		if (pos.distance(next) <= closeEnough) {
			if (projectileMilestone + 1 == projectilePath.size() - 1) {
				projectile.setVisibility(false);
				projectileActive = false;
				selected = null;
				selector.setVisibility(false);
				Position position = Position.valueOf(pos);
				if (board.isOnBoard(position)) {
					Object3D piece = getPiece(position);
					if (piece != null) {
						piece.setTexture(getTexture(board.getPiece(position)));
						checkGameOver();
						updateTurnIndicator();
					}
				}
			} else {
				++projectileMilestone;
				projectileVelocity = new SimpleVector(projectileDirs.get(
						projectileMilestone).getVector());
				projectileVelocity.scalarMul(projectileSpeed);
				projectile.clearTranslation();
				projectile.translate(projectilePath.get(projectileMilestone)
						.toVector());
			}
		} else {
			projectile.translate(projectileVelocity);
		}

	}

	/**
	 * Updates the UI element that lets users know whose turn it is.
	 */
	private void updateTurnIndicator() {
		boolean goldsTurn = "G".equals(board.getWhoseTurn());
		turnIndicatorGold.setVisibility(goldsTurn);
		turnIndicatorRed.setVisibility(!goldsTurn);
	}

	/**
	 * Marks the given position as the selected piece of the current player.
	 * 
	 * @param p
	 *            The position to mark as selected.
	 */
	private void queueSelectPiece(final Position p) {
		queueEvent(new Runnable() {
			public void run() {
				selected = p;
				selector.clearTranslation();
				selector.translate(p.toVector());
				selector.setVisibility(true);
			}
		});
	}

	/**
	 * Disable whatever current selection may be there.
	 */
	private void queueDeselectPiece() {
		queueEvent(new Runnable() {
			public void run() {
				selected = null;
				selector.setVisibility(false);
			}
		});
	}

	/**
	 * Called onTouch in the normal game playing condition.
	 * 
	 * @param view
	 *            The view from onTouch.
	 * @param e
	 *            The event from onTouch.
	 * @return The return value for onTouch.
	 */
	public boolean onTouchMove(View view, MotionEvent e) {
		final Position move = screenToBoard((int) e.getX(), (int) e.getY());
		if (selected == null) {
			if (board.isValidMoveStart(move)) {
				queueSelectPiece(move);
			}
		} else {
			if (selected.equals(move)) {
				// choosing same square deselects the piece
				queueDeselectPiece();
			} else if (move.x == -2 && move.y == 4
					&& board.isRotatable(selected)) {
				board.rotatePiece(selected, COUNTER_CLOCKWISE);
			} else if (move.x == -1 && move.y == 4
					&& board.isRotatable(selected)) {
				board.rotatePiece(selected, CLOCKWISE);
			} else if (move.x == -2 && move.y == 3
					&& board.getPiece(selected) instanceof Slingshot) {
				// fire rock
				board.firePiece(selected, Projectile.ROCK);
			} else if (board.isValidMove(selected, move)) {
				board.movePiece(selected, move);
			}
		}
		return true;
	}

	private void queueBoardReset() {
		queueEvent(new Runnable() {
			public void run() {
				try {
					winOverlays.get(board.getWinner()).setVisibility(false);
					board.setFromStream(getResources().openRawResource(
							R.raw.board));
					initPieces();
					updateTurnIndicator();
				} catch (IOException e) {
					// TODO fix me (should not reload from stream)
				}
			}
		});
	}

	/**
	 * Called onTouch in the win condition.
	 * 
	 * @param view
	 *            The view from onTouch.
	 * @param e
	 *            The event from onTouch.
	 * @return The return value for onTouch.
	 */
	private boolean onTouchWin(View view, MotionEvent e) {
		queueBoardReset();
		return true;
	}

	/**
	 * Called when the user touches the view to interact with the game.
	 */
	public boolean onTouch(View view, MotionEvent e) {
		switch (e.getAction()) {
		case MotionEvent.ACTION_DOWN:
			if (!board.isGameOver()) {
				return onTouchMove(view, e);
			} else {
				return onTouchWin(view, e);
			}
		case MotionEvent.ACTION_UP:
			return true;
		case MotionEvent.ACTION_MOVE:
			return true;
		default:
			return super.onTouchEvent(e);
		}
	}

	/**
	 * Initialize the visual representation of the board, square by square.
	 * 
	 * @throws IOException
	 */
	private void initSquares() throws IOException {
		Object3D light = loadObject3D(R.raw.square_light);
		Object3D dark = loadObject3D(R.raw.square_dark);
		Log.d(TAG, light.toString());
		Log.d(TAG, dark.toString());
		light.strip();
		light.build();
		dark.strip();
		dark.build();
		squares = new Object3D[board.getWidth()][board.getHeight()];
		for (int y = 0; y < board.getHeight(); ++y) {
			for (int x = 0; x < board.getWidth(); ++x) {
				squares[x][y] = ((x % 2 != y % 2) ? light : dark).cloneObject();
				squares[x][y].translate((float) x, (float) y, 0.0f);
				world.addObject(squares[x][y]);
			}
		}
	}

	/**
	 * Convenience method for loading a 3D object using JPCT from a raw resource
	 * used for all objects in the game.
	 * 
	 * @param id
	 *            The raw resource id of the 3DS file.
	 * @return
	 * @throws IOException
	 */
	private Object3D loadObject3D(int id, float scale) throws IOException {
		InputStream in = getResources().openRawResource(id);
		Object3D objects[] = Loader.load3DS(in, scale);
		in.close();
		return objects[0];
	}

	private Object3D loadObject3D(int id) throws IOException {
		return loadObject3D(id, 1.0f);
	}

	/**
	 * Moves the object along the Z axis so that it appears on the ground.
	 * 
	 * @param obj
	 *            The object to translate.
	 */
	private void putOnGround(Object3D obj) {
		float boundingBox[] = obj.getMesh().getBoundingBox();
		float offset = boundingBox[5]; // listed as minZ, but really minY?
		obj.translate(0.0f, 0.0f, -offset * obj.getScale());
	}

	/**
	 * Load a single piece template.
	 */
	private Object3D loadPieceTemplate(int id, String key) throws IOException {
		Object3D template = loadObject3D(id);
		float scaleFactor = 0.6f;
		template.strip();
		template.build();
		template.scale(scaleFactor);
		putOnGround(template);
		pieceTemplates.put(key, template);
		return template;
	}

	/**
	 * Load each of the piece templates.
	 */
	private void initPieceTemplates() throws IOException {
		pieceTemplates = new HashMap<String, Object3D>();
		loadPieceTemplate(R.raw.flag, "F");
		loadPieceTemplate(R.raw.boulder, "B");
		loadPieceTemplate(R.raw.obelisk, "X");
		loadPieceTemplate(R.raw.reflector, "R");
		loadPieceTemplate(R.raw.torch, "T");
		loadPieceTemplate(R.raw.slingshot, "V");
	}

	private void initLights() {
		world.setAmbientLight(20, 20, 20);
		sun = new Light(world);
		sun.setIntensity(255, 255, 255);
		sun.setPosition(new SimpleVector(0.0f, 0.0f, -10.0f));
		sun.setAttenuation(-1); // disable attenuation
	}

	private void initCamera() {
		Camera cam = world.getCamera();
		cam.setPosition(cameraPosition);
		cam.setOrientation(new SimpleVector(0.0f, 0.0f, 1.0f),
				new SimpleVector(0.0f, -1.0f, 0.0f));

		// temp code to flip board on its side to check piece height and board
		// distance
		// cam.setPosition(new SimpleVector(1.0f, 15.0f, -1.0f));
		// cam.setOrientation(new SimpleVector(0.0f, -1.0f, 0.0f), new
		// SimpleVector(0.0f, 0.0f, -1.0f));
	}

	private void logAllTextureNames() {
		// temp code to check if texture names are coming in from object loader
		for (String s : TextureManager.getInstance().getNames()) {
			Log.d(TAG, String.format("Texture: %s", s));
		}

	}

	/**
	 * Main initialization method. Responsible for calling most the other
	 * initializations and some high level initialization on its own.
	 * 
	 * @throws IOException
	 *             If this or any of its callers has IO problems.
	 */
	private void initMaster() throws IOException {
		world = new World();
		initLights();
		loadTextures();
		initSquares();
		initPieceTemplates();
		initSelector();
		initTurnIndicator();
		initWinOverlays();
		initActionPanels();
		initPieces();
		updateTurnIndicator();
		logAllTextureNames();
		initCamera();

		MemoryHelper.compact();

		if (master == null) {
			// Logger.log("Saving master Activity!");
			master = this;
		}

		fullyLoaded = true;

	}

	private void initSelector() throws IOException {
		selector = loadObject3D(R.raw.selection);
		selector.strip();
		selector.build();
		selector.setTexture("selection");
		selector.calcTextureWrap();
		selector.scale(0.8f);
		selector.translate(0.0f, 0.0f, 0.0f);
		selector.setVisibility(false);
		world.addObject(selector);
	}

	private void initTurnIndicator() throws IOException {
		turnIndicatorGold = loadObject3D(R.raw.turn_indicator_gold);
		turnIndicatorGold.strip();
		turnIndicatorGold.build();
		turnIndicatorGold.translate(13.0f, 4.0f, 0.0f);
		world.addObject(turnIndicatorGold);
		turnIndicatorRed = loadObject3D(R.raw.turn_indicator_red);
		turnIndicatorRed.strip();
		turnIndicatorRed.build();
		turnIndicatorRed.translate(13.0f, 4.0f, 0.0f);
		world.addObject(turnIndicatorRed);
	}

	private void initActionPanels() throws IOException {
		actionPanels = new HashMap<String, ActionPanel>();
		ActionPanel g = new ActionPanel();
		ActionPanel r = new ActionPanel();
		actionPanels.put("G", g);
		actionPanels.put("R", r);
		g.rotateCCW = loadObject3D(R.raw.rotate);
		g.rotateCW = g.rotateCCW.cloneObject();
		g.rotateCCW.translate(new SimpleVector(-2.0f, 4.0f, 0.0f));
		g.rotateCW.translate(new SimpleVector(-1.0f, 4.0f, 0.0f));
		g.rotateCW.rotateY(PI);
		g.shootRock = loadObject3D(R.raw.rock);
		g.shootRock.translate(new SimpleVector(-2.0f, 3.0f, 0.0f));
		g.addToWorld(world);
		projectile = g.shootRock.cloneObject();
		projectile.scale(0.25f);
		world.addObject(projectile);
	}

	private void initWinOverlay(String key, int id) throws IOException {
		float scale = 9.0f;
		SimpleVector pos = new SimpleVector(cameraPosition);
		pos.z += 10.0f;
		Object3D overlay = loadObject3D(id, scale);
		overlay.translate(pos);
		overlay.setTransparency(FULLY_OPAQUE);
		overlay.setVisibility(false);
		winOverlays.put(key, overlay);
		world.addObject(overlay);
	}

	private void initWinOverlays() throws IOException {
		winOverlays = new HashMap<String, Object3D>();
		initWinOverlay("G", R.raw.win_gold);
		initWinOverlay("R", R.raw.win_red);
		initWinOverlay("TIE", R.raw.win_tie);
	}

	/**
	 * Removes all pieces from the world.
	 */
	private void clearPieces() {
		for (int y = 0; y < board.getHeight(); ++y) {
			for (int x = 0; x < board.getWidth(); ++x) {
				if (pieces[x][y] != null) {
					world.removeObject(pieces[x][y]);
					pieces[x][y] = null;
				}
			}
		}
	}

	/**
	 * Initializes the 3D objects representing the pieces on the board.
	 * 
	 * @throws IOException
	 */
	private void initPieces() throws IOException {
		if (pieces == null) {
			pieces = new Object3D[board.getWidth()][board.getHeight()];
		} else {
			clearPieces();
		}
		for (int y = 0; y < board.getHeight(); ++y) {
			for (int x = 0; x < board.getWidth(); ++x) {
				Position p = new Position(x, y);
				Piece piece = board.getPiece(p);
				if (piece != null) {
					Object3D template = getTemplate(piece);
					pieces[x][y] = template.cloneObject();
					pieces[x][y]
							.setTexture(piece.getBelongsTo().equals("G") ? "gold.png"
									: "red.png");
					pieces[x][y].build();
					pieces[x][y].setRotationPivot(SimpleVector.ORIGIN);
					pieces[x][y].rotateZ(-piece.getDirection().getAngle());
					pieces[x][y].translate((float) x, (float) y, 0.0f);
					world.addObject(pieces[x][y]);
				}
			}
		}
	}

	/**
	 * Loads a texture in from a drawable resource, resizing as specified.
	 * 
	 * After calling this, the texture manager should have a copy of the texture
	 * available for rendering any 3D objects that have this texture name
	 * associate with their material.
	 * 
	 * @param name
	 *            The name of the texture matching the 3D object texture.
	 * @param id
	 *            The id of the drawable to load.
	 * @param width
	 *            The width to rescale to.
	 * @param height
	 *            The height to rescale to.
	 */
	private void loadTexture(String name, int id, int width, int height) {
		Log.d(TAG, String.format("loading texture: %s", name));
		Drawable drawable = getResources().getDrawable(id);
		Bitmap bitmap = BitmapHelper.convert(drawable);
		bitmap = BitmapHelper.rescale(bitmap, width, height);
		Texture texture = new Texture(bitmap, true);
		TextureManager.getInstance().addTexture(name, texture);
		Log.d(TAG, String.format("done loading texture: %s", name));
	}

	private void loadTextures() {
		loadTexture("squareLight", R.drawable.square_light, 64, 64);
		loadTexture("squareDark", R.drawable.square_dark, 64, 64);
		loadTexture("rock.bmp", R.drawable.rock, 64, 64);
		loadTexture("gold.png", R.drawable.gold, 256, 256);
		loadTexture("gold0.png", R.drawable.gold0, 256, 256);
		loadTexture("gold1.png", R.drawable.gold1, 256, 256);
		loadTexture("gold2.png", R.drawable.gold2, 256, 256);
		loadTexture("gold3.png", R.drawable.gold3, 256, 256);
		loadTexture("gold4.png", R.drawable.gold4, 256, 256);
		loadTexture("red.png", R.drawable.red, 256, 256);
		loadTexture("red0.png", R.drawable.red0, 256, 256);
		loadTexture("red1.png", R.drawable.red1, 256, 256);
		loadTexture("red2.png", R.drawable.red2, 256, 256);
		loadTexture("red3.png", R.drawable.red3, 256, 256);
		loadTexture("red4.png", R.drawable.red4, 256, 256);
		loadTexture("selection", R.drawable.selection, 64, 64);
		loadTexture("torch_re.bmp", R.drawable.torch_red, 64, 64);
		loadTexture("grass_li.bmp", R.drawable.grass_light, 256, 256);
		loadTexture("grass_da.bmp", R.drawable.grass_dark, 256, 256);
		loadTexture("win_gold.png", R.drawable.win_gold, 512, 512);
		loadTexture("win_red.png", R.drawable.win_red, 512, 512);
		loadTexture("win_tie.png", R.drawable.win_tie, 512, 512);
	}

}
