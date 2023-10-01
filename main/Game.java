package main;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import javax.imageio.ImageIO;

/*
 * All the things I could theoretically do in the future if I feel like returning back to this:
 * - clear documentation and better notes for the future
 * - bug fixes and balance changes (so, so, so, so many)
 * - waves of increasing difficulty, logical progression
 * - improve overall code robustness & cleanliness
 * - improve polish (e.g. game freezes on death, etc.)
 * - improve code so that it's really easy to add new features/build on to the game
 * - health regeneration mechanics
 * - obstacles, line-of-sight
 * - improved handling of stacked status effects
 * - menu with start, options, etc.
 * - enemies that apply negative status effects
 * - enemies with more complex behavior/AI (e.g. necromancers, move + shoot, etc.)
 * - friendly turrets
 * - pet (needs pathfinding AI, bullet avoidance AI, etc.)
 * - scrolling background for larger levels
 * - proper lighting and particle effects
 * - more weapons (lasers, ricochet, homing, splash, portal gun, etc.)
 * - unlockable doors and keys, different rooms, etc.
 * - many more kinds of enemies and status effects (the possibilities are endless)
 * - proper items, currencies, quests
 * - roguelike generation, progress retention, actual progression
 * 
 */

/* 
 * graphics credit to some random resized google images as well as:
 * - https://secrethideout.itch.io/rogue-dungeon-tileset-16x16
 * - https://luizmelo.itch.io/evil-wizard
 * - https://opengameart.org/content/rotating-orbs
 * 
 */

enum BaseItemType {
	
	NONE, WEAPON, POTION
	
}

enum ItemType {
	
	NONE("", BaseItemType.NONE, HW03.iconSprites[2][0]),
	HEALTH_POTION("Health Potion", BaseItemType.POTION, HW03.iconSprites[2][0]),
	SPEED_POTION("Speed Potion", BaseItemType.POTION, HW03.iconSprites[6][0]),
	ICE_POTION("Ice Walker Potion", BaseItemType.POTION, HW03.iconSprites[7][0]),
	DAMAGE_POTION("Damage Potion", BaseItemType.POTION, HW03.iconSprites[6][1]),
	SHIELD_POTION("Shield Potion", BaseItemType.POTION, HW03.iconSprites[4][0]),
	HEART_POTION("Extra Health", BaseItemType.POTION, HW03.iconSprites[5][0]),
	
	FIRE_RATE_POTION("Fire Rate Potion", BaseItemType.POTION, HW03.iconSprites[3][0]),
	RAILGUN_PICKUP("Orb of the Marksman", BaseItemType.WEAPON, HW03.iconSprites[1][3]),
	GATLING_PICKUP("Orb of the Flood", BaseItemType.WEAPON, HW03.iconSprites[2][3]),
	BFG_PICKUP("B.F.O.", BaseItemType.WEAPON, HW03.iconSprites[4][3]),
	SHOTGUN_PICKUP("Orb of the Swarm", BaseItemType.WEAPON, HW03.iconSprites[3][3]);
	
	static int DEFAULT_POTION_LENGTH = 300;
	static double SPEED_POTION_SPEED = 8.0;
	static double SPEED_POTION_MAX_SPEED = 12.0;
	static double DAMAGE_POTION_MULTIPLIER = 2.0;
	static double FIRE_RATE_POTION_MULTIPLIER = 0.5;
	static double HEALTH_POTION_HEALTH = 10.0;
	static double ICE_POTION_SPEED = 4.0;
	static double ICE_POTION_MAX_SPEED = 8.0;
	static double ICE_POTION_MOTION_DECAY = 0.95;
	static double SHIELD_POTION_WEAKNESS = 0.0;
	static double HEART_POTION_HEALTH = 2.0;
	static double HEART_POTION_MAX_HEALTH = 2.0;
	static double DEFAULT_SCORE_INCREASE = 5.0;
	
	String id;
	BaseItemType baseItemType;
	BufferedImage iconImage;
	ItemType(String id, BaseItemType type, BufferedImage iconImage) {
		
		this.id = id;
		this.baseItemType = type;
		this.iconImage = iconImage;
		
	}
	
}

enum BulletType {
	
	NONE, PLAYER, TURRET, BFG
	
}

enum WeaponType {
	
	NONE(BulletType.NONE, HW03.bulletSprites[0], 0, 0, 0, 0, 0, 0),
	DEFAULT(BulletType.PLAYER, HW03.bulletSprites[3], 12, 5, 1, 16, 1, 0.04),
	RAILGUN(BulletType.PLAYER, HW03.bulletSprites[0], 12, 25, 10, 32, 10, 0),
	GATLING(BulletType.PLAYER, HW03.bulletSprites[1], 12, 6, 1, 4, 50, 0.1),
	BFG(BulletType.BFG, HW03.bulletSprites[6], 12, 2, 20, 32, 1, 0),
	SHOTGUN(BulletType.PLAYER, HW03.bulletSprites[2], 12, 5, 2.5, 24, 10, 0.175),
	TURRET_DEFAULT(BulletType.TURRET, HW03.bulletSprites[7], 12, 3, 1, 16, -1, 0.05),
	TURRET_BIG(BulletType.TURRET, HW03.bulletSprites[4], 12, 10, 4, 96, -1, 0.01),
	TURRET_RAPID(BulletType.TURRET, HW03.bulletSprites[5], 12, 3, 0.5, 6, -1, 0.02),
	CHASER(BulletType.TURRET, HW03.bulletSprites[0], 0, 2, 5, 0, -1, 0);
	
	BulletType bulletType;
	BufferedImage[] imageCycle;
	double radius;
	double speed;
	double damage;
	double fireRate;
	int ammo;
	double bloom;
	
	WeaponType(BulletType bulletType, BufferedImage[] imageCycle, double radius, double speed, double damage, double fireRate, int ammo, double bloom) {
		
		this.bulletType = bulletType;
		this.imageCycle = imageCycle;
		this.radius = radius;
		this.speed = speed;
		this.damage = damage;
		this.fireRate = fireRate;
		this.ammo = ammo;
		this.bloom = bloom;
		
	}
	
}

enum EnemyType {
	
	NONE(0, 0, 0, 0, 0, HW03.turretSprites, WeaponType.NONE),
	TURRET(300, 32, 2, 12, 25, HW03.turretSprites, WeaponType.TURRET_DEFAULT),
	TURRET_BIG(400, 32, 2, 15, 40, HW03.turretBigSprites, WeaponType.TURRET_BIG),
	TURRET_RAPID(200, 32, 2, 10, 30, HW03.turretRapidSprites, WeaponType.TURRET_RAPID),
	CHASER(0, 32, 2, 10, 15, HW03.chaserSprites, WeaponType.CHASER);
	
	double range;
	double drawOffset;
	double radius;
	double health;
	double killReward;
	BufferedImage[][] storedImages;
	WeaponType weaponType;
	EnemyType(double range, double radius, double drawOffset, double health, double killReward, BufferedImage[][] storedImages, WeaponType weaponType) {
		
		this.range = range;
		this.radius = radius;
		this.drawOffset = drawOffset;
		this.health = health;
		this.killReward = killReward;
		this.storedImages = storedImages;
		this.weaponType = weaponType;
		
	}
	
}

class HW03 extends App {
	
	private static final long serialVersionUID = 1L;
	static class Drawable {
		
		Vector2 position;
		double radius;
		BufferedImage defaultImage;
		
		BufferedImage[][] storedImages;
		BufferedImage[] currentCycle;
		int frame;
		int frameIndex;
		int frameLength;
		int cycleIndex;
		String cycleStatus;
		boolean alive;
		boolean specialAnimation;
		
		static final int DEFAULT_FRAME_LENGTH = 4;
		
		public Drawable(Vector2 position, double radius, BufferedImage defaultImage) {
			
			this.position = position;
			this.radius = radius;
			this.defaultImage = defaultImage;
			this.frame = 0;
			this.frameIndex = 0;
			this.frameLength = DEFAULT_FRAME_LENGTH;
			this.cycleIndex = -1;
			this.cycleStatus = "DEFAULT";
			this.alive = false;
			this.specialAnimation = false;
			
		}
		
		public boolean collidesWith(Drawable d) {
			
			return collidesWith(d.position, d.radius);
			
		}
		
		public boolean collidesWith(Vector2 otherPos, double otherRadius) {
			
			return this.position.minus(otherPos).length() < (this.radius + otherRadius);
						
		}
		
		public boolean simpleOutsideBounds(Vector2 topLeft, Vector2 bottomRight) {
			
			boolean[] bounds = outsideBounds(topLeft, bottomRight);
			return bounds[0] || bounds[1] || bounds[2] || bounds[3];
			
		}
		
		public boolean[] outsideBounds(Vector2 topLeft, Vector2 bottomRight) {
			
			// NOTE: radius must be multiplied by 2 for bottom right for this to work
			boolean[] bounds = {this.position.x < topLeft.x, this.position.x + this.radius*2 > bottomRight.x, this.position.y < topLeft.y, this.position.y + this.radius*2 > bottomRight.y};
			return bounds;
			
		}
		
		public String getDirection(Vector2 v) {
			
			if (Math.abs(v.x) >= Math.abs(v.y)) {
				if (v.x >= 0) {
					return "RIGHT";
				} else {
					return "LEFT";
				}
			} else {
				if (v.y >= 0) {
					return "DOWN";
				} else {
					return "UP";
				}
			}
			
		}
		
		public void cycleFrame() {
			
			if (this.frame++ > this.frameLength) {
				
				this.frame = 0;
				this.frameIndex = ++this.frameIndex % (this.currentCycle.length);
				
			}
			
			if (this.specialAnimation && this.frameIndex == this.currentCycle.length-1) {
				
				this.specialAnimation = false;
				
			}
			
			
		}
		
		public void setCurrentCycle(String s) { }
		
		public void setCurrentCycle(int cycleIndex) {

			if (cycleIndex != this.cycleIndex) {
			
				this.frameIndex = 0;
				this.frame = 0;
				
			}
			
			this.cycleIndex = cycleIndex;	
			this.currentCycle = this.storedImages[cycleIndex];
			
		}
		
	}
	
	static class Player extends Drawable {
		
		Vector2 velocity;
		
		static double DEFAULT_MAX_HEALTH = 10.0;
		static final double DEFAULT_SPEED = 5.0;
		static final double DEFAULT_MAX_SPEED = 5.0;
		static final double DEFAULT_MOTION_DECAY = 0.1;
		static final double DEFAULT_WEAKNESS = 1.0;
		static final double DEFAULT_DAMAGE_MULTIPLIER = 1.0;
		static final double DEFAULT_ROLL_STRENGTH = 5.0;
		static final int DEFAULT_IFRAME_LENGTH = 32;
		static final int DEFAULT_IFRAME_RATE = 128;
		static final int DEFAULT_REGEN_RATE = 128;
		
		WeaponType weaponType;
		
		int framesSinceFired;
		int framesSinceRolled;
		int iFrame;
		int potionFrame;
		int ammo;
		double health;
		double weakness;
		double speed;
		double maxSpeed;
		double motionDecay;
		double fireRate;
		double damageMultiplier;
		
		public Player(Vector2 position, double radius, BufferedImage[][] storedImages) {
			
			super(position, radius, storedImages[0][0]);
			this.storedImages = storedImages;
			this.framesSinceFired = 0;
			this.framesSinceRolled = 0;
			this.velocity = new Vector2();
			this.health = DEFAULT_MAX_HEALTH;
			this.weaponType = WeaponType.DEFAULT;
			this.iFrame = 0;
			this.alive = true;
			resetPotion();
			resetWeapon();
			
		}
		
		public void resetPotion() {
			
			this.weakness = DEFAULT_WEAKNESS;
			this.motionDecay = DEFAULT_MOTION_DECAY;
			this.maxSpeed = DEFAULT_MAX_SPEED;
			this.speed = DEFAULT_SPEED;
			this.damageMultiplier = DEFAULT_DAMAGE_MULTIPLIER;
			if (this.health > DEFAULT_MAX_HEALTH) {
				
				this.health = DEFAULT_MAX_HEALTH;
				
			}
			this.fireRate = this.weaponType.fireRate;
			
		}
		
		public void resetWeapon() {
			
			this.weaponType = WeaponType.DEFAULT;
			this.fireRate = WeaponType.DEFAULT.fireRate;
			
		}
		
		public void setCurrentCycle(String cycleID) {
			
			if (this.specialAnimation) { return; }
			
			int id = 0;
			switch (cycleID.toUpperCase()) {
				
				case "IDLE FRONT": id = 0; this.frameLength = DEFAULT_FRAME_LENGTH; break;
				case "IDLE BACK": id = 1; this.frameLength = DEFAULT_FRAME_LENGTH; break;
				case "RUN FRONT": id = 2; break;
				case "RUN BACK": id = 3; break;
				case "ATTACK RIGHT": id = 4; this.specialAnimation = true; break;
				case "ATTACK DOWN": id = 5; this.specialAnimation = true; break;
				case "ATTACK UP": id = 6; this.specialAnimation = true; break;
				case "ATTACK LEFT": id = 7; this.specialAnimation = true; break;
				case "TAKE DAMAGE": id = 8; this.specialAnimation = true; break;
				case "DEATH": id = 9; this.specialAnimation = true; this.frameLength = DEFAULT_FRAME_LENGTH*4; this.alive = false; break;
				case "SPAWN": id = 10; this.specialAnimation = true; this.frameLength = DEFAULT_FRAME_LENGTH*2; break;
				default: return;
			
			}
			
			this.cycleStatus = cycleID;
			setCurrentCycle(id);
			
		}
		
	}
		
	static class Enemy extends Drawable {

		double drawOffset;
		double health;
		double range;
		double killReward;
		EnemyType type;
		
		public Enemy(EnemyType type, Vector2 position, double radius, double drawOffset, BufferedImage[][] storedImages, double health, double killReward) {
			
			super(position, radius, storedImages[0][0]);
			this.drawOffset = drawOffset;
			this.type = type;
			this.health = health;
			this.storedImages = storedImages;
			this.currentCycle = storedImages[0];
			this.range = type.range;
			this.killReward = killReward;
			this.alive = true;
			
		}
		
		public boolean isTurret() {
			
			return this.type == EnemyType.TURRET || this.type == EnemyType.TURRET_BIG || this.type == EnemyType.TURRET_RAPID;
			
		}
		
	}
	
	static class Turret extends Enemy {
		
		int framesSinceFired;
		WeaponType weaponType;
				
		public Turret(EnemyType type, Vector2 position) {
			
			super(type, position, type.radius, type.drawOffset, type.storedImages, type.health, type.killReward);
			this.framesSinceFired = 0;
			this.weaponType = type.weaponType;
			
		}
		
		public void setCurrentCycle(String cycleID) {
			
			if (this.specialAnimation) { return; }

			int id = 0;
			switch (cycleID.toUpperCase()) {
				
				case "IDLE": id = 0; break;
				case "TAKE DAMAGE": id = 1; this.specialAnimation = true; break;
				case "MOVE": id = 2; this.specialAnimation = true; break;
				case "FIRE": id = 3; this.specialAnimation = true; break;
				case "DEATH": id = 4; this.specialAnimation = true; break;
				default: assert false; return;
			
			}
			
			this.cycleStatus = cycleID;
			setCurrentCycle(id);
			
		}
		
	}
	
	static class Chaser extends Enemy {
		
		Vector2 velocity;
		double framesSinceCollided;
		
		static final double DEFAULT_HEALTH = 10.0;
		static final double DEFAULT_DRAW_OFFSET = 2.0;
		static final double DEFAULT_RADIUS = 16;
		static final double DEFAULT_COLLISION_LIMIT = 30;
		static double DEFAULT_CHASER_SPEED = 1.0;
		static double CHASER_KILL_REWARD = 10.0;
		
		public Chaser(Vector2 position) {
			
			super(EnemyType.CHASER, position, DEFAULT_RADIUS, DEFAULT_DRAW_OFFSET, EnemyType.CHASER.storedImages, DEFAULT_HEALTH, CHASER_KILL_REWARD);
			this.framesSinceCollided = 0;
			
		}
		
		public void setCurrentCycle(String cycleID) {
			
			if (this.specialAnimation) { return; }

			int id = 0;
			switch (cycleID.toUpperCase()) {
				
				case "IDLE": id = 0; break;
				case "TAKE DAMAGE": id = 1; this.specialAnimation = true; break;
				case "DEATH": id = 2; this.specialAnimation = true; break;
				default: assert false; return;
			
			}
			
			this.cycleStatus = cycleID;
			setCurrentCycle(id);
			
		}

		
	}

	static class Bullet extends Drawable {
		
		Vector2 velocity;
		boolean alive;
		BulletType type;
		WeaponType weaponType;

		public Bullet(Vector2 position, WeaponType type) {
			
			super(position, type.radius, type.imageCycle[0]);
			this.alive = true;
			this.weaponType = type;
			this.type = type.bulletType;
			this.currentCycle = type.imageCycle;
			BufferedImage[][] storedImages = {type.imageCycle};
			this.storedImages = storedImages;
			this.radius = type.radius;
			
			
		}

		static Vector2 generateBloom(Vector2 v, double bloom) {
			
			double angle = randomBounds(-bloom, bloom);
			return new Vector2(Math.cos(angle) * v.x - Math.sin(angle) * v.y, Math.sin(angle) * v.x + Math.cos(angle) * v.y);
			
		}
		
	}
	
	static class Item extends Drawable {
		
		int age;
		ItemType type;
		
		public Item(Vector2 position, ItemType type) {
			
			super(position, type.iconImage.getWidth()/2, type.iconImage);
			this.alive = true;
			this.age = 0;
			this.type = type;
			
		}
		
	}
	
	static class DrawableText {
		
		String content;
		Vector2 position;
		Vector3 color;
		int fontSize;
		boolean center;
		int age;
		int maxAge;
		
		static final int DEFAULT_DAMAGE_AGE = 16;
		static final int DEFAULT_COLLECTED_AGE = 32;
		static final int DEFAULT_FONT_SIZE = 20;
		
		public DrawableText(String content, Vector2 position, Vector3 color, int fontSize, boolean center) {
			
			this.content = content;
			this.position = position;
			this.color = color;
			this.fontSize = fontSize;
			this.center = center;
			this.age = 0;
			
		}
		
	}
	
	double clamp(double x, double min, double max) {
		
		return Math.min(Math.max(x, min), max);
		
	}
	
	
	Vector2 clampBounds(Drawable d, Vector2 topLeft, Vector2 bottomRight) {
		
		boolean[] bounds = d.outsideBounds(topLeft, bottomRight);
		Vector2 returnVector = new Vector2(d.position);
		if (bounds[0]) { returnVector.x = topLeft.x; }
		if (bounds[1]) { returnVector.x = bottomRight.x - d.radius*2; }
		if (bounds[2]) { returnVector.y = topLeft.y; }
		if (bounds[3]) { returnVector.y = bottomRight.y - d.radius*2; }
		
		return returnVector;
		
	}
	
	static boolean withinRange(double r, Vector2 position1, Vector2 position2) {
		
		return Vector2.distanceBetween(position1, position2) <= r;
		
	}
	
	static double randomBounds(double a, double b) {
		
		return a + Math.random() * (b - (-a));
		
	}
	
	void drawString(DrawableText dt, int randRange) {
					
        _graphics.setFont(new Font(Font.MONOSPACED, Font.BOLD, dt.fontSize)); 
		drawString(dt.content, new Vector2(dt.position.x + randomBounds(-randRange, randRange), -dt.position.y), dt.color, dt.fontSize, dt.center);
		
	}
	
	void drawString(String string, Vector2 _position, Vector3 color, int fontSize, boolean center) {
		
		 // Suppress Mac warnings about missing Times and Lucida.
        PrintStream systemDotErr = System.err;
        System.setErr(new PrintStream(new NullOutputStream())); {
            _graphics.setFont(new Font(Font.DIALOG, Font.BOLD, fontSize)); 
            Vector2 position = _windowPixelFromWorld(_position);
            if (center) {
                FontMetrics fontMetrics = _graphics.getFontMetrics(); 
                position.x -= 0.5 * fontMetrics.stringWidth(string);
                position.y += 0.25 * fontMetrics.getHeight();
            }
            _graphicsSetColor(color);
            _graphics.drawString(string, (int) position.x, (int) position.y);
        } System.setErr(systemDotErr);
		
	}

	void drawAnimatedImage(Drawable d, double offset) {
		
		try {
			
			drawImage(d, d.currentCycle[d.frameIndex], offset);
			
		} catch (Exception e) {
			
			System.out.println("Animation crash");
			System.out.println(d.cycleStatus);
			
		}
				
	}
	
	void drawImage(Drawable d, double offset) {
		
		drawImage(d.defaultImage, (int)(d.position.x-d.radius*offset), (int)(d.position.y-d.radius*offset));
		
	}
	
	void drawImage(Drawable d, BufferedImage img, double offset) {
		
		drawImage(img, (int)(d.position.x-d.radius*offset), (int)(d.position.y-d.radius*offset));
		
	}

	void drawImage(Image img, int x, int y) {

		_graphics.drawImage(img, x, y, this);

	}
	
	void spawnBullet(Vector2 position, Vector2 velocity, WeaponType type) {
		
		for (int i = 0; i < bullets.length; ++i) {
			
			if (!bullets[i].alive) {
				
				bullets[i] = new Bullet(position, type);
				bullets[i].velocity = velocity;
				bullets[i].alive = true;
				return;
				
			}
		
		}
		
	}
	
	void spawnString(String content, Vector2 position, Vector3 color, int fontSize, boolean centered, int age) {
		
		for (int i = 0; i < drawableTexts.length; i++) {
			
			if (drawableTexts[i].age <= 0) {
				
				drawableTexts[i] = new DrawableText(content, position, color, fontSize, centered);
				drawableTexts[i].age = age;
				return;
				
			}
			
		}
		
	}
	
	Vector2 getFreeSpace(Drawable[] blockers, double range, Vector2 topLeft, Vector2 bottomRight) {
		
		Vector2 position;
		boolean freeSpace = true;
		
		do {

			freeSpace = true;
			position = new Vector2(randomBounds(topLeft.x, bottomRight.x), randomBounds(topLeft.y, bottomRight.y));

			for (Drawable e : blockers) {

				if (!e.alive) { continue; }
				if (withinRange(range, position, e.position)) { freeSpace = false; }

			}


		} while (!freeSpace);
		
		return position;
		
	}
	
	void spawnItem(int n) {

		for (int i = 0; i < n; i++) {

			spawnItem(ItemType.values()[(int)randomBounds(0, ItemType.values().length-1)+1]);

		}

	}
	
	void spawnItem(ItemType type) {
		
		int size = type.iconImage.getWidth();
		spawnItem(type, getFreeSpace(items, size, TOP_LEFT.plus(new Vector2(size, size)), BOTTOM_RIGHT.minus(new Vector2(size, size))));
		
	}
	
	
	void spawnItem(ItemType type, Vector2 position) {
		
		for (int i = 0; i < items.length; i++) {
			
			if (!items[i].alive) {
				
				items[i] = new Item(position, type);
				return;
				
			}
			
		}
		
	}
	
	void spawnEnemy(EnemyType type, int n) {
		
		for (int i = 0; i < n; i++) {
			
			spawnEnemy(type);
			
		}
		
	}
	
	void spawnEnemy(EnemyType type) {
		
		spawnEnemy(type, getFreeSpace(enemies, type.radius*2, TOP_LEFT.plus(new Vector2(type.radius*2, type.radius*2)), BOTTOM_RIGHT.minus(new Vector2(type.radius*2, type.radius*2))));
		
	}
	
	void spawnEnemy(EnemyType type, Vector2 position) {
				
		for (int i = 0; i < enemies.length; i++) {
			
			if (!enemies[i].alive) {
				
				if (type == EnemyType.CHASER) {
					
					enemies[i] = new Chaser(position);
					
				} else {
					
					enemies[i] = new Turret(type, position);
					
				}
				return;
				
			}
			
		}
		
	}
	
	BufferedImage[] slice(BufferedImage[][] arr, int row, int from, int to) {
		
		BufferedImage[] returnArr = new BufferedImage[to-from];
		for (int i = 0; i < to-from; i++) {
			
			returnArr[i] = arr[i+from][row];
			
		}
		
		return returnArr;
		
	}
	
	BufferedImage[][] loadImages(BufferedImage spriteSheet, int width) {
		
		BufferedImage[][] returnArr = new BufferedImage[spriteSheet.getWidth()/width][spriteSheet.getHeight()/width];
		for (int x = 0; x < spriteSheet.getWidth()/width; x++) {
			
			for (int y = 0; y < spriteSheet.getHeight()/width; y++) {
				
				returnArr[x][y] = spriteSheet.getSubimage(x*width, y*width, width, width);
				
			}
			
		}
		
		return returnArr;
		
	}
	
	static Vector2 TOP_LEFT = new Vector2(0, 0);
	static Vector2 BOTTOM_RIGHT = new Vector2(1376, 768);
	
	double waveTime;
	double waveLength;
	int itemDropTime;
	int itemDropMin;
	int itemDropMax;
	
	int gameDuration;
	double score;
	
	Player player;
	Enemy[] enemies;
	Bullet[] bullets;
	Item[] items;
	DrawableText[] drawableTexts;
	Drawable[] tiles;
	Drawable[] decor;
	Drawable[] walls;
	
	static BufferedImage heartIconImage;
	static BufferedImage ammoIconImage;
	static BufferedImage ammoInfiniteIconImage;
	
	static BufferedImage[][] tileSprites;
	static BufferedImage[][] iconSprites;
	static BufferedImage[][] turretSprites;
	static BufferedImage[][] turretBigSprites;
	static BufferedImage[][] turretRapidSprites;
	static BufferedImage[][] chaserSprites;
	static BufferedImage[][] playerSprites;
	static BufferedImage[][] bulletSprites;
	
	void setup() {
		
		Player.DEFAULT_MAX_HEALTH = 20.0;
		
		ammoIconImage = null;
		ammoInfiniteIconImage = null;

		BufferedImage tileSpriteSheet = null;
		BufferedImage iconSpriteSheet = null;
		BufferedImage playerSpriteSheet = null;
		BufferedImage turretSpriteSheet = null;
		BufferedImage turretBigSpriteSheet = null;
		BufferedImage turretRapidSpriteSheet = null;
		BufferedImage chaserSpriteSheet = null;
		BufferedImage bulletSpriteSheet = null;

		try {
			
			tileSpriteSheet = ImageIO.read(new File("src/assets/tiles.png"));
			iconSpriteSheet = ImageIO.read(new File("src/assets/items.png"));
			playerSpriteSheet = ImageIO.read(new File("src/assets/player.png"));
			turretSpriteSheet = ImageIO.read(new File("src/assets/turret.png")); 
			turretBigSpriteSheet = ImageIO.read(new File("src/assets/turretBig.png")); 
			turretRapidSpriteSheet = ImageIO.read(new File("src/assets/turretRapid.png")); 
			chaserSpriteSheet = ImageIO.read(new File("src/assets/chaser.png"));
			bulletSpriteSheet = ImageIO.read(new File("src/assets/bullets.png"));

		} catch (IOException e) { e.printStackTrace(); }
				
		// generate tile map
		int tileSpriteWidth = 16;
		tileSprites = loadImages(tileSpriteSheet, tileSpriteWidth);
		tiles = new Drawable[(int) (BOTTOM_RIGHT.x / tileSpriteWidth * BOTTOM_RIGHT.y / tileSpriteWidth)];
		walls = new Drawable[(int) ((BOTTOM_RIGHT.x / tileSpriteWidth) * 2 + (BOTTOM_RIGHT.y / tileSpriteWidth) * 2)];
		int c = 0;
		int d = 0;
		for (int x = 0; x < BOTTOM_RIGHT.x / tileSpriteWidth; x++) {
			
			for (int y = 0; y < BOTTOM_RIGHT.y / tileSpriteWidth; y++) {
				
				Vector2 selection = new Vector2(15, 0);
				double choice = Math.random();
				if (choice < 0.2) { selection = new Vector2(13, 0); }
				else if (choice < 0.3) { selection = new Vector2(14, 0); }
				else if (choice < 0.4) { selection = new Vector2(15, 0); }
				else if (choice < 0.6) { selection = new Vector2(13, 1); }
				else if (choice < 0.7) { selection = new Vector2(14, 1); }
				else if (choice < 0.8) { selection = new Vector2(15, 1); }

				BufferedImage img = tileSprites[(int) selection.x][(int) selection.y];
				tiles[c++] = new Drawable(new Vector2(x*tileSpriteWidth+tileSpriteWidth/2, y*tileSpriteWidth+tileSpriteWidth/2), tileSpriteWidth/2, img);
				tiles[c-1].alive = true;
				
				if (x == 0 || x == BOTTOM_RIGHT.x / tileSpriteWidth - 1 && y != 0 && y != BOTTOM_RIGHT.y / tileSpriteWidth - 1) {
					
					walls[d++] = new Drawable(new Vector2(x*tileSpriteWidth+tileSpriteWidth/2, y*tileSpriteWidth+tileSpriteWidth/2), tileSpriteWidth/2, tileSprites[8][2]);

				}
				
				if (y == 0 || y == BOTTOM_RIGHT.y / tileSpriteWidth - 1 && x != 0 && x != BOTTOM_RIGHT.x / tileSpriteWidth - 1) {
					
					walls[d++] = new Drawable(new Vector2(x*tileSpriteWidth+tileSpriteWidth/2,y*tileSpriteWidth+tileSpriteWidth/2), tileSpriteWidth/2, tileSprites[10][0]);
					
				}
				
			}
			
		}
		
		BufferedImage[] faucetImages = slice(tileSprites, 15, 6, 15);
		int numFaucets = (int) randomBounds(3, 6);
		int numRubble = (int) randomBounds(10, 30);
		decor = new Drawable[numFaucets+numRubble];
		for (int i = 0; i < decor.length; i++) {
			
			decor[i] = new Drawable(new Vector2(), 0, tileSprites[0][0]);
			
		}
		
		walls[d++] = new Drawable(TOP_LEFT.plus(new Vector2(tileSpriteWidth/2, tileSpriteWidth/2)), tileSpriteWidth/2, tileSprites[8][0]);			
		walls[d++] = new Drawable(new Vector2(TOP_LEFT.x+tileSpriteWidth/2, BOTTOM_RIGHT.y-tileSpriteWidth/2), tileSpriteWidth/2, tileSprites[8][2]);			
		walls[d++] = new Drawable(new Vector2(BOTTOM_RIGHT.x-tileSpriteWidth/2, TOP_LEFT.y+tileSpriteWidth/2), tileSpriteWidth/2, tileSprites[11][0]);			
		walls[d++] = new Drawable(BOTTOM_RIGHT.minus(new Vector2(tileSpriteWidth/2, tileSpriteWidth/2)), tileSpriteWidth/2, tileSprites[11][2]);			

		c = 0;
		for (int i = 0; i < walls.length; i++) {
			
			if (walls[i] == null) c++;
			
		}
		
		c = 0;
		for (int i = 0; i < numFaucets; i++) {
			
			decor[c++] = new Drawable(getFreeSpace(decor, tileSpriteWidth*2, TOP_LEFT.plus(new Vector2(tileSpriteWidth*1.5, tileSpriteWidth/2)), new Vector2(BOTTOM_RIGHT.x-tileSpriteWidth*1.5, TOP_LEFT.y+tileSpriteWidth/2)), tileSpriteWidth/2, faucetImages[0]);
			decor[c-1].alive = true;
			decor[c-1].currentCycle = faucetImages;
			decor[c-1].frameIndex += (int) randomBounds(0, 5);
			decor[c-1].frameLength = 8;
			
		}
		
		for (int i = 0; i < numRubble; i++) {
			
			BufferedImage icon = slice(tileSprites, 2, 14, 16)[1];
			double rand = Math.random();
			if (rand < 0.5) { icon = slice(tileSprites, 2, 14, 16)[0]; }
			decor[c++] = new Drawable(getFreeSpace(decor, tileSpriteWidth*2, TOP_LEFT.plus(new Vector2(tileSpriteWidth, tileSpriteWidth)), BOTTOM_RIGHT.minus(new Vector2(tileSpriteWidth, tileSpriteWidth))), tileSpriteWidth/2, icon);
			BufferedImage[] cycle = {icon};
			decor[c-1].currentCycle = cycle;
			
		}
		
		// generate icons
		iconSprites = loadImages(iconSpriteSheet, 32);
		heartIconImage = iconSprites[5][0];
		ammoIconImage = iconSprites[7][2];
		ammoInfiniteIconImage = iconSprites[6][2];
		
		// generate bullet animation cycles (transpose sprite sheet)
		BufferedImage[][] temp = loadImages(bulletSpriteSheet, 32);
		bulletSprites = new BufferedImage[temp[0].length][temp.length];
		for (int x = 0; x < temp.length; x++) {
			
			for (int y = 0; y < temp[0].length; y++) {
				
				bulletSprites[y][x] = temp[x][y];
				
			}
			
		}
		
		// generate player animation cycles
		playerSprites = loadImages(playerSpriteSheet, 96);
		BufferedImage[][] playerImages = new BufferedImage[11][];
		playerImages[0] = slice(playerSprites, 0, 0, 8); // idle front
		playerImages[1] = slice(playerSprites, 1, 0, 8); // idle back
		playerImages[2] = slice(playerSprites, 2, 0, 6); // run front
		playerImages[3] = slice(playerSprites, 3, 0, 6); // run back
		playerImages[4] = slice(playerSprites, 2, 6, 9); // attack right
		playerImages[5] = slice(playerSprites, 3, 6, 9); // attack down
		playerImages[6] = slice(playerSprites, 4, 6, 9); // attack up
		playerImages[7] = slice(playerSprites, 8, 5, 8); // attack left
		playerImages[8] = slice(playerSprites, 5, 6, 9); // take damage
		playerImages[9] = slice(playerSprites, 8, 0, 5); // death
		playerImages[10] = slice(playerSprites, 9, 0, 5); // spawn
		player = new Player(new Vector2(BOTTOM_RIGHT.x/2, BOTTOM_RIGHT.y/2), playerImages[0][0].getWidth()/3, playerImages);		
		
		// generate turret animation cycles
		BufferedImage[][] turretImages = loadImages(turretSpriteSheet, 150);
		BufferedImage[][] turretBigImages = loadImages(turretBigSpriteSheet, 150);
		BufferedImage[][] turredRapidImages = loadImages(turretRapidSpriteSheet, 150);
		turretSprites = new BufferedImage[5][];
		turretBigSprites = new BufferedImage[5][];
		turretRapidSprites = new BufferedImage[5][];
		int[][] turretSliceIndices = {{2, 0, 8}, {4, 0, 4}, {3, 0, 8}, {0, 0, 8}, {1, 0, 5}}; // idle, take damage, move, fire, death
		
		for(int i = 0; i < turretSliceIndices.length; i++) {
			
			turretSprites[i] = slice(turretImages, turretSliceIndices[i][0], turretSliceIndices[i][1], turretSliceIndices[i][2]);
			turretBigSprites[i] = slice(turretBigImages, turretSliceIndices[i][0], turretSliceIndices[i][1], turretSliceIndices[i][2]);
			turretRapidSprites[i] = slice(turredRapidImages, turretSliceIndices[i][0], turretSliceIndices[i][1], turretSliceIndices[i][2]);

		}
		
		BufferedImage[][] chaserImages = loadImages(chaserSpriteSheet, 64);
		chaserSprites = new BufferedImage[3][];
		chaserSprites[0] = slice(chaserImages, 0, 0, 6); // idle (always moving)
		chaserSprites[1] = slice(chaserImages, 3, 0, 4); // take damage
		chaserSprites[2] = slice(chaserImages, 4, 0, 6 ); // death
		
		// load drawing arrays
		enemies = new Enemy[32];
		for (int i = 0; i < enemies.length; i++ ) {
			
			enemies[i] = new Enemy(EnemyType.NONE, new Vector2(), 0, 0, turretSprites, 0, 0);
			enemies[i].alive = false;
			
		}
				
		items = new Item[32];
		for (int i = 0; i < items.length; i++) {
			
			items[i] = new Item(new Vector2(), ItemType.NONE);
			items[i].alive = false;
			
		}
		
		bullets = new Bullet[256];
		for (int i = 0; i < bullets.length; ++i) {
			
			bullets[i] = new Bullet(new Vector2(), WeaponType.NONE);
			bullets[i].alive = false;
			
		}

		drawableTexts = new DrawableText[256];
		for (int i = 0; i < drawableTexts.length; ++i) {
			
			drawableTexts[i] = new DrawableText("", new Vector2(), new Vector3(), -1, false);
			
		}
		
		gameDuration = -1;
		score = 0;
		
		player.setCurrentCycle("SPAWN");
		
		waveTime = 0;
		waveLength = 1200;
		itemDropTime = 0;
		itemDropMin = 150;
		itemDropMax = 350;
		
		player.iFrame = 128;
				
	}

	void loop() {
		
		++gameDuration;
		
		// generate waves of enemies
		if (gameDuration == waveTime) {
			
			int turretsToSpawn = (int) randomBounds(1, 4);
			int bigTurretsToSpawn = (int) randomBounds(0, 2);
			int rapidTurretsToSpawn = (int) randomBounds(0, 2);
			int chasersToSpawn = (int) (randomBounds(2, 5));
			spawnEnemy(EnemyType.TURRET, turretsToSpawn);
			spawnEnemy(EnemyType.TURRET_BIG, bigTurretsToSpawn);
			spawnEnemy(EnemyType.TURRET_RAPID, rapidTurretsToSpawn);
			spawnEnemy(EnemyType.CHASER, chasersToSpawn);
			waveLength -= 50;
			waveLength = clamp(waveLength, 400, 1200);
			waveTime += waveLength;
			
		}
		
		// generate items
		if (gameDuration == itemDropTime) {
			
			itemDropTime += (int) randomBounds(itemDropMin, itemDropMax);
			spawnItem((int)randomBounds(1, 4));
			
		}
		
		// convert between cartesian and graphical coordinates
		if (mousePosition.y < 0) { mousePosition.y = -mousePosition.y; }
		
		// draw tiles
		for (Drawable d : tiles) {
			
			drawImage(d, 1);
			
		}
		
		for (Drawable d : walls) {
			
			drawImage(d, 1);
			
		}
		
		for (Drawable d : decor) {
			
			drawAnimatedImage(d, 1);
			d.cycleFrame();
			
		}
		
		// points for living
		if (gameDuration % 50 == 0) {
			
			score ++;
			
		}
		
		// player physics and animation
		player.setCurrentCycle("IDLE FRONT");
		
		if (keyHeld('W')) { player.velocity.y -= player.speed; }
		if (keyHeld('A')) { player.velocity.x -= player.speed; }
		if (keyHeld('S')) { player.velocity.y += player.speed; }
		if (keyHeld('D')) { player.velocity.x += player.speed; }
		player.velocity.x = clamp(player.velocity.x, -player.maxSpeed, player.maxSpeed);
		player.velocity.y = clamp(player.velocity.y, -player.maxSpeed, player.maxSpeed);
		
		if (player.framesSinceRolled++ >= Player.DEFAULT_IFRAME_LENGTH && keyHeld(' ')) {
			
			player.velocity = player.velocity.times(Player.DEFAULT_ROLL_STRENGTH);
			player.framesSinceRolled = 0;
			player.iFrame = Player.DEFAULT_IFRAME_LENGTH;
			player.setCurrentCycle("ATTACK " + player.getDirection(player.velocity));
						
		}
		
		if (player.velocity.length() > 0.1) {
			
			if (player.velocity.y < -0.1) { player.setCurrentCycle("RUN BACK"); }
			else { player.setCurrentCycle("RUN FRONT"); }
				
		}
		
		player.iFrame--;
		if (!player.alive || player.cycleStatus == "SPAWN" || player.cycleStatus == "DEATH") { player.velocity = new Vector2(0, 0); }
		
 		player.position = player.position.plus(player.velocity);
		
		player.position = clampBounds(player, TOP_LEFT, BOTTOM_RIGHT);
		
		player.velocity = player.velocity.times(player.motionDecay);		

		if (player.framesSinceFired++ >= player.fireRate) {
						
			if (player.ammo <= 0) { player.weaponType = WeaponType.DEFAULT; }
			
			Vector2 v = null;
			if (keyHeld('I')) { v = new Vector2(0, -1); } 
			if (keyHeld('J')) { v = new Vector2(-1, 0); } 
			if (keyHeld('K')) { v = new Vector2(0, 1); } 
			if (keyHeld('L')) { v = new Vector2(1, 0); } 
			if (mouseHeld) { v = Vector2.directionVectorFrom(player.position, mousePosition); }
			
			if (keyHeld('I') || keyHeld('J') || keyHeld('K') || keyHeld('L') || mouseHeld) {
				
				player.setCurrentCycle("ATTACK " + player.getDirection(v));			
				player.framesSinceFired = 0;
				player.ammo--;
				
				v = Bullet.generateBloom(v, player.weaponType.bloom);
				spawnBullet(player.position, v.times(player.weaponType.speed), player.weaponType);	

				if (player.weaponType == WeaponType.SHOTGUN) {
					
					double shotgunAngle1 = WeaponType.SHOTGUN.bloom; double shotgunAngle2 = shotgunAngle1/2;
					spawnBullet(player.position, Bullet.generateBloom(v, shotgunAngle1).times(player.weaponType.speed), player.weaponType);	
					spawnBullet(player.position, Bullet.generateBloom(v, -shotgunAngle1).times(player.weaponType.speed), player.weaponType);	
					spawnBullet(player.position, Bullet.generateBloom(v, shotgunAngle2).times(player.weaponType.speed), player.weaponType);	
					spawnBullet(player.position, Bullet.generateBloom(v, -shotgunAngle2).times(player.weaponType.speed), player.weaponType);	

				}
				
			}
						
		}
				
		player.cycleFrame();
		drawAnimatedImage(player, 1.5);
				
		// enemy physics and animation
		for (int i = 0; i < enemies.length; i++) {
			
			Enemy enemy = enemies[i];
			
			if (!enemy.alive) { continue; }

			enemy.setCurrentCycle("IDLE");

			if (enemy.health <= 0.01) { enemy.health = 1; enemy.specialAnimation = false; enemy.setCurrentCycle("DEATH"); score += enemy.killReward; }
			
			if (enemy.cycleStatus == "DEATH") {
				
				if (enemy.frameIndex == enemy.currentCycle.length-2) { enemy.alive = false; }
				
			}
			
			if (enemy.isTurret()) {
			
				Turret turret = (Turret) enemy;
				
				if (turret.framesSinceFired++ >= turret.weaponType.fireRate) {
					
					if (withinRange(turret.range, turret.position, player.position)) {
						
						turret.framesSinceFired = 0;
						spawnBullet(turret.position, Bullet.generateBloom(Vector2.directionVectorFrom(turret.position, player.position), turret.weaponType.bloom).times(turret.weaponType.speed), turret.weaponType);
						turret.setCurrentCycle("FIRE");
						
					}
									
				}
				
			
			} else if (enemy.type == EnemyType.CHASER) {
				
				Chaser chaser = (Chaser) enemy;
				
				chaser.position = chaser.position.plus(Vector2.directionVectorFrom(chaser.position, player.position).times(chaser.type.weaponType.speed));
				
				if (chaser.collidesWith(player) && chaser.framesSinceCollided-- == 0) {
					
					if (player.iFrame >= 0) { continue; }
					
					chaser.framesSinceCollided = Chaser.DEFAULT_COLLISION_LIMIT;
					player.health -= chaser.type.weaponType.damage;
					chaser.setCurrentCycle("TAKE DAMAGE");
					player.setCurrentCycle("TAKE DAMAGE");
					player.health -= chaser.type.weaponType.damage * player.weakness;
					spawnString(""+chaser.type.weaponType.damage * player.weakness, player.position.plus(new Vector2(0, -(player.radius))), Vector3.red, DrawableText.DEFAULT_FONT_SIZE, true, DrawableText.DEFAULT_DAMAGE_AGE);

				}
				
			}
			
			enemy.cycleFrame();
			drawAnimatedImage(enemy, enemy.drawOffset);
			
		}
		
		// bullet physics and animation
		for (Bullet bullet : bullets) {
			
			if (bullet.alive) {
								
				if (bullet.type == BulletType.BFG) {
					
					for (Bullet b : bullets) {
						
						if (b.alive && b.type == BulletType.TURRET && b.collidesWith(bullet)) { b.alive = false; }
						
					}
					
				}
				
				for (Enemy e : enemies) {	
					
					if (e.alive && bullet.collidesWith(e) && (bullet.type == BulletType.PLAYER || bullet.type == BulletType.BFG)) {
						
						e.health -= bullet.weaponType.damage * player.damageMultiplier;
						e.setCurrentCycle("TAKE DAMAGE");
						if (bullet.type == BulletType.PLAYER) { bullet.alive = false; }
						spawnString(""+bullet.weaponType.damage * player.damageMultiplier, e.position.plus(new Vector2(0, -(e.radius*2))), Vector3.red, DrawableText.DEFAULT_FONT_SIZE, true, DrawableText.DEFAULT_DAMAGE_AGE);
						score += bullet.weaponType.damage * player.damageMultiplier;
						continue;
						
					}	
				
				}
								
				if (bullet.simpleOutsideBounds(TOP_LEFT, BOTTOM_RIGHT)) {
					
					bullet.alive = false;
					
				} else if (bullet.collidesWith(player) && bullet.type == BulletType.TURRET && player.weakness > 0) {
					
					if (player.iFrame >= 0) { continue; } 
						
					player.setCurrentCycle("TAKE DAMAGE");
					player.health -= bullet.weaponType.damage * player.weakness;
					bullet.alive = false;
					spawnString(""+bullet.weaponType.damage * player.weakness, player.position.plus(new Vector2(0, -(player.radius))), Vector3.red, DrawableText.DEFAULT_FONT_SIZE, true, DrawableText.DEFAULT_DAMAGE_AGE);
				
					
				} else {
					
					bullet.cycleFrame();
					bullet.position = bullet.position.plus(bullet.velocity);
					drawAnimatedImage(bullet, 1);	
					
				}

			}
			
		}
				
		// item pickups
		if(player.potionFrame-- <= 0) { player.resetPotion(); }
		for (Item item : items) {
			
			if (item.alive) {
				
				drawImage(item, 1);
			
				if (item.collidesWith(player)) {
					
					score += ItemType.DEFAULT_SCORE_INCREASE;
					
					spawnString("Collected " + item.type.id + "!", player.position.plus(new Vector2(0, -player.radius)), Vector3.green, DrawableText.DEFAULT_FONT_SIZE, true, DrawableText.DEFAULT_COLLECTED_AGE);
					
					if (item.type.baseItemType == BaseItemType.WEAPON) {
												
						switch(item.type) {
						
							case RAILGUN_PICKUP: player.weaponType = WeaponType.RAILGUN; break;
							case GATLING_PICKUP: player.weaponType = WeaponType.GATLING; break;
							case BFG_PICKUP: player.weaponType = WeaponType.BFG; break;
							case SHOTGUN_PICKUP: player.weaponType = WeaponType.SHOTGUN; break;
							default: break;
						
						}
						
						player.ammo = player.weaponType.ammo;
						
					} else if (item.type.baseItemType == BaseItemType.POTION) {
						
						player.potionFrame = ItemType.DEFAULT_POTION_LENGTH;
						
						switch (item.type) {
						
							case HEART_POTION: player.health += ItemType.HEART_POTION_HEALTH; Player.DEFAULT_MAX_HEALTH += ItemType.HEART_POTION_MAX_HEALTH; break;
							case HEALTH_POTION: player.health += ItemType.HEALTH_POTION_HEALTH; break;
							case DAMAGE_POTION: player.damageMultiplier = ItemType.DAMAGE_POTION_MULTIPLIER; break;
							case FIRE_RATE_POTION: player.fireRate *= ItemType.FIRE_RATE_POTION_MULTIPLIER; break;
							case ICE_POTION: player.speed = ItemType.ICE_POTION_SPEED; player.maxSpeed = ItemType.ICE_POTION_MAX_SPEED; player.motionDecay = ItemType.ICE_POTION_MOTION_DECAY; break;
							case SHIELD_POTION: player.weakness = ItemType.SHIELD_POTION_WEAKNESS; break;
							case SPEED_POTION: player.speed = ItemType.SPEED_POTION_SPEED;  player.maxSpeed = ItemType.SPEED_POTION_MAX_SPEED; break;
							default: break;
						
						}
						
					}
					
					item.alive = false;
					
				}
				
			}
			
		}
		
		// draw damage indicators, etc.
		for (DrawableText dt : drawableTexts) {
			
			if (dt.age-- > 0) {
				
				drawString(dt, 20);
				
			}
			
		}
		
		// draw icons
		if (player.weaponType == WeaponType.DEFAULT) { player.ammo = 1; }
		
		for (int i = 0; i < Math.ceil(player.health/2); i++) {

			drawImage(heartIconImage, (int)(16+i*heartIconImage.getWidth()), 10);
			
		}
		
		if (!(player.weaponType == WeaponType.DEFAULT)) {
			
			for (int i = 0; i < Math.ceil((double)player.ammo/5); i++) {
				
				drawImage(ammoIconImage, (int)(16+i*ammoIconImage.getWidth()*1.5), 50);
				
			}
			
		} else {
			
			drawImage(ammoInfiniteIconImage, 16, 50);
			
		}
		
		drawString("Score: " + score, new Vector2(16, -110), Vector3.white, 20, false);
				
		// check for player death
		if (player.health <= 0.01) { 
			
			player.setCurrentCycle("DEATH");
			if (player.frameIndex == player.currentCycle.length-1) {
				
				reset(); 
			
			}
			
		}
		
	}

	public static void main(String[] arguments) {
		
		App app = new HW03();
		app.setWindowBackgroundColor(0.0, 0.0, 0.0);
		app.setWindowCenterInWorldUnits(BOTTOM_RIGHT.x/2, -BOTTOM_RIGHT.y/2);
		app.setWindowSizeInWorldUnits(BOTTOM_RIGHT.x, BOTTOM_RIGHT.y);
		app.setWindowHeightInPixels((int)BOTTOM_RIGHT.y);
        app.setWindowTopLeftCornerInPixels(64, 64);
		app.run();
		
	}
}