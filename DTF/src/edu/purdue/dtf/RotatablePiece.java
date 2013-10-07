package edu.purdue.dtf;

import static edu.purdue.dtf.Rotation.*;

public class RotatablePiece extends Piece {

	public RotatablePiece(String token) {
		super(token);
	}
	
	public void rotate(Rotation rotationDirection) {
		Direction[] ds = Direction.values();
		int i = 0;
		while (ds[i] != directionFacing)
			++i;
		if (rotationDirection == CLOCKWISE)
			++i;
		else
			--i;
		if (i < 0)
			i = ds.length - 1;
		if (i >= ds.length)
			i = 0;
		directionFacing = ds[i];
	}

	@Override
	public boolean isRotatable() {
		return true;
	}
	
}
