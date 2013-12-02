package edu.purdue.dtf.view;

import com.threed.jpct.Matrix;
import com.threed.jpct.Object3D;
import com.threed.jpct.RGBColor;
import com.threed.jpct.SimpleVector;
import com.threed.jpct.World;

public class ParticleManager {
	
	private Particle[] parts = null;
	private int count = 0;
	private String ct = "particles";
	private int pos = 0;
	private String[] ts = { "particles", "particles" };
	private SimpleVector origin;
	private Object3D plane;
	public long lastUpdate = System.currentTimeMillis();

	ParticleManager(SimpleVector origin) {
		parts = new Particle[100];
		plane = new Object3D(2);
		this.origin = origin;
		plane.addTriangle(
				new SimpleVector(-0.5,  0.5, 0), 0.498046875f, 0.5f,
				new SimpleVector( 0.5,  0.5, 0), 0.998046875f, 0.5f, 
				new SimpleVector( 0.5, -0.5, 0), 0.998046875f, 0.0f);
		plane.addTriangle(
				new SimpleVector(-0.5,  0.5, 0), 0.498046875f, 0.5f,
				new SimpleVector( 0.5, -0.5, 0), 0.998046875f, 0.0f,
				new SimpleVector(-0.5, -0.5, 0), 0.498046875f, 0.0f);
		plane.setTransparency(100);
		plane.setTransparencyMode(Object3D.TRANSPARENCY_MODE_ADD);
		plane.setTexture("particles");
		plane.build();
	}

	void cycleTexture() {
		pos++;
		pos %= 2;
		ct = ts[pos];
	}

	void update(int ticks) {
		for (int i = 0; i < count; i++) {
			Particle pp = parts[i];
			if (pp.getVisibility()) {
				pp.update(ticks);
			}
		}
	}
	
	void move(SimpleVector pos) {
		this.origin = pos;
		for (int i = 0; i < count; i++) {
			Particle p = parts[i];
			p.setOrigin(origin);
		}
	}

	void addParticle(World w, SimpleVector vel) {
		Particle p = getParticle(w);
		if (p != null) {
			p.setTranslationMatrix(new Matrix());
			p.setOrigin(origin);
			p.setVelocity(vel);
			p.reset();
			p.setTransparency(0);
			p.setAdditionalColor(240, 120, 5);
			p.setTransparencyMode(Object3D.TRANSPARENCY_MODE_ADD);
			p.setTexture("particles");
			p.build();
		}
	}

	private Particle getParticle(World w) {
		for (int i = 0; i < count; i++) {
			Particle pp = parts[i];
			if (!pp.getVisibility()) {
				pp.setVisibility(Object3D.OBJ_VISIBLE);
				return pp;
			}
		}
		if (count < parts.length - 1) {
			Particle p = new Particle(plane);
			p.scale(0.4f);
			w.addObject(p);
			parts[count] = p;
			count++;
			return p;
		}
		else {
			return null;
		}
	}

	public void remove(World w) {
		for (int i = 0; i < count; i++) {
			Particle pp = parts[i];
			w.removeObject(pp);
		}
	}
	
}
