package edu.purdue.dtf.view;

import java.util.List;

import com.threed.jpct.Object3D;
import com.threed.jpct.SimpleVector;

import edu.purdue.dtf.game.Direction;
import edu.purdue.dtf.game.Position;
import edu.purdue.dtf.game.Projectile;

public class ProjectileAnim {

	private Object3D projectile = null;

	private SimpleVector projectileVelocity = new SimpleVector(0.0f, 0.1f, 0.0f);
	private List<Position> projectilePath = null;
	private List<Direction> projectileDirs = null;
	private int projectileMilestone = 0;
	private boolean projectileActive = false;
	private final float projectileSpeed = 0.2f;
	
	public void fire(Object3D projectile, SimpleVector startAt, final List<Position> path,
			final List<Direction> dirs, final Projectile proj) {
		this.projectile = projectile;
		this.projectile.clearTranslation();
		projectilePath = path;
		projectileDirs = dirs;
		this.projectile.translate(startAt);
		projectileVelocity = new SimpleVector(dirs.get(0).getVector());
		projectileVelocity.scalarMul(projectileSpeed);
		projectileMilestone = 0;
		projectileActive = true;
		this.projectile.setVisibility(true);
	}
	
	public void update() {
		final float closeEnough = 0.2f;
		SimpleVector pos = projectile.getTranslation();
		pos.z = 0.0f;
		SimpleVector next = projectilePath.get(projectileMilestone + 1)
				.toVector();
		next.z = 0.0f;
		if (pos.distance(next) <= closeEnough) {
			if (projectileMilestone + 1 == projectilePath.size() - 1) {
				projectile.setVisibility(false);
				projectileActive = false;
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
	
	public boolean isActive() {
		return projectileActive;
	}
	
	public Position getPosition() {
		return Position.valueOf(projectile.getTranslation());
	}
}
