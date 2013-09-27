package edu.purdue.dtf;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
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
public class GameView extends GLSurfaceView implements OnTouchListener,
		GLSurfaceView.Renderer, BoardListener {

	// Tag used for logging from this class.
	private static final String TAG = "GameView";

	// Float copy of PI constant for piece rotation and so forth.
	private static float PI = (float) Math.PI;
	
	
	private static GameView master = null;

	// The World object is the main object in jPCT that coordinates all the
	// geometry, transformations, and rendering.
	private World world;

	// The framebuffer represents the rendered pixels that the world is drawn
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

	// The moveStart position is used to capture the first part of a users
	// move. When the user first touches the screen it indicates the piece
	// they want to try to move, and when they click again, hopefully on a
	// valid target for whichever piece they've chosen, it either moves there
	// or executes the ability they chose.
	private Position moveStart;
	
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
	 * @param board The board to visualize.
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
		v.scalarMul(13.0f);
		v.add(new SimpleVector(5.0f, 4.0f, 0.0f));
		int boardX = (int) Math.round(v.x);
		int boardY = (int) Math.round(v.y);
		return new Position(boardX, boardY);
	}

	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
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
				Log.e(TAG, "initialization error", e);
			}
		}
	}

	/**
	 * Updates the frames per second counter and writes it to the log.
	 */
	private void updateFPS() {
		if (System.currentTimeMillis() - time >= 1000) {
			// Log.v("FPS", fps + "fps");
			fps = 0;
			time = System.currentTimeMillis();
		}
		fps++;
	}

	/**
	 * Draws the frame to the screen.
	 */
	@Override
	public void onDrawFrame(GL10 gl) {
		fb.clear(backgroundColor);
		world.renderScene(fb);
		world.draw(fb);
		fb.display();
		updateFPS();
	}

	/**
	 * Move the piece at position a to position b.
	 * 
	 * @param a
	 *            The position to move from.
	 * @param b
	 *            The position to move to.
	 */
	public void onPieceMoved(Position a, Position b) {
		Object3D piece = pieces[a.x][a.y];
		pieces[a.x][a.y] = null;
		pieces[b.x][b.y] = piece;
		Object3D template = pieceTemplates.get(board.getPiece(b));
		piece.clearTranslation();
		piece.translate(template.getTranslation());
		piece.translate(b.x, b.y, 0.0f);
	}

	/**
	 * Called when the user touches the view to interact with the game.
	 */
	public boolean onTouch(View view, MotionEvent e) {
		switch (e.getAction()) {
		case MotionEvent.ACTION_DOWN:
			Position move = screenToBoard((int) e.getX(), (int) e.getY());
			if (moveStart == null) {
				if (board.isValidMoveStart(move)) {
					moveStart = move;
					selector.clearTranslation();
					selector.translate((float) move.x, (float) move.y, 0.0f);
					selector.setVisibility(true);
				}
			} else {
				if (board.isValidMove(moveStart, move)) {
					board.moveUnit(moveStart, move);
					moveStart = null;
					selector.setVisibility(false);
				}
			}
			return true;
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
	private Object3D loadObject3D(int id) throws IOException {
		InputStream in = getResources().openRawResource(id);
		Object3D objects[] = Loader.load3DS(in, 1.0f);
		in.close();
		return objects[0];
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
		float boundingBox[] = template.getMesh().getBoundingBox();
		float offset = boundingBox[5]; // listed as minZ, but really minY?
		template.translate(0.0f, 0.0f, -offset * scaleFactor);
		pieceTemplates.put(key, template);
		return template;
	}

	/**
	 * Load each of the piece templates.
	 */
	private void initPieceTemplates() throws IOException {
		pieceTemplates = new HashMap<String, Object3D>();
		loadPieceTemplate(R.raw.flag_gold, "GF");
		loadPieceTemplate(R.raw.flag_red, "RF");
		loadPieceTemplate(R.raw.boulder_gold, "GB");
		loadPieceTemplate(R.raw.boulder_red, "RB");
		loadPieceTemplate(R.raw.obelisk_gold, "GX");
		loadPieceTemplate(R.raw.obelisk_red, "RX");
		loadPieceTemplate(R.raw.reflector_gold, "GR");
		loadPieceTemplate(R.raw.reflector_red, "RR");
		loadPieceTemplate(R.raw.torch_gold, "GT");
		loadPieceTemplate(R.raw.torch_red, "RT");
		loadPieceTemplate(R.raw.slingshot_gold, "GV");
		loadPieceTemplate(R.raw.slingshot_red, "RV");
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
		world.setAmbientLight(20, 20, 20);

		sun = new Light(world);
		sun.setIntensity(255, 255, 255);
		sun.setPosition(new SimpleVector(0.0f, 0.0f, -10.0f));
		sun.setAttenuation(-1); // disable attenuation

		loadTextures();
		initSquares();
		initPieceTemplates();
		initSelector();
		initPieces();

		// temp code to check if texture names are coming in from object loader
		for (String s : TextureManager.getInstance().getNames()) {
			Log.d(TAG, String.format("Texture: %s", s));
		}

		Camera cam = world.getCamera();
		cam.setPosition(new SimpleVector(5.0f, 4.0f, -13.0f));
		cam.setOrientation(new SimpleVector(0.0f, 0.0f, 1.0f),
				new SimpleVector(0.0f, -1.0f, 0.0f));

		// temp code to flip board on its side to check piece height and board
		// distance
		// cam.setPosition(new SimpleVector(4.0f, 15.0f, -1.0f));
		// cam.setOrientation(new SimpleVector(0.0f, -1.0f, 0.0f), new
		// SimpleVector(0.0f, 0.0f, -1.0f));
		// cam.lookAt(new SimpleVector(0.0f, 0.0f, 0.0f));

		MemoryHelper.compact();

		if (master == null) {
			// Logger.log("Saving master Activity!");
			master = this;
		}

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

	private void initPieces() throws IOException {
		pieces = new Object3D[board.getWidth()][board.getHeight()];
		for (int y = 0; y < board.getHeight(); ++y) {
			for (int x = 0; x < board.getWidth(); ++x) {
				Position p = new Position(x, y);
				String piece = board.getPiece(p);
				if (piece != null) {
					Object3D template = pieceTemplates.get(piece);
					pieces[x][y] = template.cloneObject();
					pieces[x][y].translate((float) x, (float) y, 0.0f);
					world.addObject(pieces[x][y]);
				}
			}
		}
	}

	private void loadTexture(String name, int id, int width, int height) {
		Drawable drawable = getResources().getDrawable(id);
		Bitmap bitmap = BitmapHelper.convert(drawable);
		bitmap = BitmapHelper.rescale(bitmap, width, height);
		Texture texture = new Texture(bitmap);
		TextureManager.getInstance().addTexture(name, texture);
	}

	private void loadTextures() {
		loadTexture("squareLight", R.drawable.square_light, 64, 64);
		loadTexture("squareDark", R.drawable.square_dark, 64, 64);
		loadTexture("rock.bmp", R.drawable.rock, 64, 64);
		loadTexture("gold.png", R.drawable.gold, 256, 256);
		loadTexture("red.png", R.drawable.red, 256, 256);
		loadTexture("selection", R.drawable.selection, 64, 64);
		loadTexture("torch_re.bmp", R.drawable.torch_red, 64, 64);
		loadTexture("grass_li.bmp", R.drawable.grass_light, 256, 256);
		loadTexture("grass_da.bmp", R.drawable.grass_dark, 256, 256);
	}

}
