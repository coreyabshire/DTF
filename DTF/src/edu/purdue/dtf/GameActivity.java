package edu.purdue.dtf;

import java.io.IOException;

import android.app.Activity;
import android.content.res.Resources.NotFoundException;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

/**
 * GameActivity is responsible for the basic Android wrapper of the 
 * game. It owns the main game view and board, and sets everything up
 * between the two. It is also responsible for the top level Android
 * process lifecycle events.
 */
public final class GameActivity extends Activity {

	// Tag used for logging from this class.
	private static final String TAG = "GameActivity";
	
	// Responsible for game rendering and input.
	private GameView view;
	
	// Responsible for game rules and control.
	private Board board;

	// Size of the game board in squares.
	private static final int boardWidth = 11;
	private static final int boardHeight = 9;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG, "onCreate");
		
		// Make the game full screen, with no system UI features visible.
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);

		// Initialize the main game board.
		// TODO make this part of a new game (i.e. play button)
		board = new Board(boardWidth, boardHeight);
		try {
			board.setFromStream(getResources().openRawResource(R.raw.board));
		} catch (NotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		// Initialize the main game view.
		view = new GameView(this);
		view.setBoard(board);
		setContentView(view);
		Log.d(TAG, "Activity created successfully.");
	}

	@Override
	protected void onPause() {
		super.onPause();
		Log.d(TAG, "onPause");
		// pause the rendering thread and deallocate any
		// resources not needed while not active
		view.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
		// resume the rendering thread and reinitialize any
		// resources that were released during pause
		Log.d(TAG, "onResume");
		view.onResume();
	}

	@Override
	protected void onStop() {
		super.onStop();
		Log.d(TAG, "onStop");
	}

}
