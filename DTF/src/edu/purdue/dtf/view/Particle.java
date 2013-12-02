package edu.purdue.dtf.view;

import android.util.FloatMath;

import com.threed.jpct.Matrix;
import com.threed.jpct.Object3D;
import com.threed.jpct.SimpleVector;

public class Particle extends Object3D {

	private static final long serialVersionUID = 7457027385898056020L;

	private static final float PI = (float) Math.PI;
	private SimpleVector vel = new SimpleVector();
	private long time = 0;
	private long maxTime = 1000;
	private static final SimpleVector GRAV = new SimpleVector(0, -0.0003, 0);
	private Matrix rotationM = new Matrix();
	private float dRotation = PI / 180.0f * 5.0f;

	Particle(Object3D plane) {
		super(plane);
		setMesh(plane.getMesh());
		setBillboarding(Object3D.BILLBOARDING_ENABLED);
		setVisibility(Object3D.OBJ_VISIBLE);
		setCulling(Object3D.CULLING_DISABLED);
		setTransparency(0);
		//setAdditionalColor(RGBColor.WHITE);
		setLighting(Object3D.LIGHTING_NO_LIGHTS);
		enableLazyTransformations();
		reset();
		build();
	}

	void setVelocity(SimpleVector vel) {
		this.vel.set(vel);
	}

	void reset() {
		time = System.currentTimeMillis();
		getTranslationMatrix().setIdentity();
		rotationM.setIdentity();
	}

	void update(int ticks) {
		if (getVisibility()) {
			for (int i = 0; i < ticks; i++) {
				vel.add(GRAV);
				translate(vel);
				rotationM.rotateZ(dRotation);
				setRotationMatrix(rotationM);
			}
			if (System.currentTimeMillis() - time > maxTime) {
				reset();
				setVisibility(Object3D.OBJ_INVISIBLE);
			}
		}
	}

}
