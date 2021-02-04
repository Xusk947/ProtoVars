package ProtoVars;

import arc.graphics.Color;
import arc.math.Mathf;
import arc.math.geom.Point2;
import arc.util.Log;
import arc.util.Nullable;
import java.util.HashMap;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.content.Bullets;
import mindustry.content.Fx;
import mindustry.content.UnitTypes;
import mindustry.game.Team;
import mindustry.gen.Call;
import mindustry.gen.Player;
import mindustry.gen.Unit;
import mindustry.type.UnitType;
import mindustry.world.Block;
import mindustry.world.Tiles;
import mindustry.world.blocks.storage.CoreBlock;

public class Room {

    public static int SIZE = 25;
    public static int TEAM = 10;
    public static int SPACING = 5;
    public static int MAX_LEVEL = 3;
    public static HashMap<UnitType, Integer> getDamage;

    public static void init() {
        getDamage = new HashMap<>();
        getDamage.put(UnitTypes.dagger, 1);
        getDamage.put(UnitTypes.mace, 3);
        getDamage.put(UnitTypes.fortress, 5);
    }

    public int level = 1, count;

    public Point2 pos;
    public int id, x, y, centreX, centreY, endX, endY;
    public int side = 0;
    public float drawx, drawy, drawCentreX, drawCentreY;
    public boolean dead = false;
    public @Nullable
    Player controller;
    public ProtoAI ai;

    public Room(int id, int x, int y) {
        this.id = id;
        this.pos = new Point2(x, y);
        this.x = x * SIZE + x * SPACING;
        this.y = y * SIZE + y * SPACING;
        this.endX = (this.x + SIZE);
        this.endY = (this.y + SIZE);
        this.centreX = this.x + SIZE / 2;
        this.centreY = this.y + SIZE / 2;

        this.drawx = x * SIZE * Vars.tilesize;
        this.drawy = y * SIZE * Vars.tilesize;
        this.drawCentreX = centreX * Vars.tilesize;
        this.drawCentreY = centreY * Vars.tilesize;

        this.ai = new ProtoAI(this);
    }

    public void init(Tiles tiles) {
        //generate sand square
        for (int xx = 0; xx <= SIZE; xx++) {
            for (int yy = 0; yy <= SIZE; yy++) {
                if (x + xx < tiles.width && y + yy < tiles.height) {
                    tiles.getn(x + xx, y + yy).removeNet();
                    if ((xx == 0 || yy == 0) || (xx == SIZE || yy == SIZE)) {
                        tiles.getn(x + xx, y + yy).setNet(Blocks.sandWall);
                    }
                }
            }
        }
        // horizontal way to other cores
        for (int xx = 0; xx < SIZE + SPACING; xx++) {
            for (int yy = 0; yy <= 5; yy++) {
                if (x + xx < tiles.width && y + yy < tiles.height) {
                    if (x + xx > endX) {
                        tiles.getn(x + xx, centreY - 2 + yy).setFloorNet(Blocks.sand);
                    }
                    if ((yy == 0 || yy == 5) && x + xx > endX) {
                        tiles.getn(x + xx, centreY - 2 + yy).setNet(Blocks.sandWall);
                    } else {
                        tiles.getn(x + xx, centreY - 2 + yy).removeNet();
                    }
                }
            }
        }
        // vertical way
        for (int xx = 0; xx <= 5; xx++) {
            for (int yy = 0; yy < SIZE + SPACING; yy++) {
                if (x + xx < tiles.width && y + yy < tiles.height) {
                    if (y + yy > endY) {
                        tiles.getn(centreX - 2 + xx, y + yy).setFloorNet(Blocks.sand);
                    }
                    if ((xx == 0 || xx == 5) && y + yy > endY) {
                        tiles.getn(centreX - 2 + xx, y + yy).setNet(Blocks.sandWall);
                    } else {
                        tiles.getn(centreX - 2 + xx, y + yy).removeNet();
                    }
                }
            }
        }

        //spawn core
        if (centreX < tiles.width && centreY < tiles.height) {
            tiles.getn(centreX, centreY).setNet(Room.getBlock(level), Team.get(TEAM), 0);
        }

        TEAM++;
    }

    public void spawn() {
        Unit u = Room.getUnit(level).create(Vars.world.tile(centreX, centreY).team());
        int size = (Vars.world.tile(centreX, centreY).build.block.size);
        switch(side) {
            case 0: // up
                u.set(drawCentreX, drawCentreY + Vars.tilesize * size + 2);
                break;
            case 1: // right
                u.set(drawCentreX + Vars.tilesize * size + 2, drawCentreY);
                break;
            case 2: // bottom
                u.set(drawCentreX, drawCentreY - Vars.tilesize * size + 2);
                break;
            case 3: // left
                u.set(drawCentreX - Vars.tilesize * size + 2, drawCentreY);
                break;
            default: // up
                side = 1;
                u.set(drawCentreX, drawCentreY + Vars.tilesize * size + 2);
        }
        
        u.add();
        if (u.isValid()) {
            count = count - level;
        }
    }

    public static int level(int lvl) {
        switch (lvl) {
            case 1:
                return 40;
            case 2:
                return 80;
            case 3:
                return 160;
            default:
                return 0;
        }
    }

    public static UnitType getUnit(int level) {
        switch (level) {
            case 1:
                return UnitTypes.dagger;
            case 2:
                return UnitTypes.mace;
            case 3:
                return UnitTypes.fortress;
            default:
                return UnitTypes.dagger;
        }
    }
    
    public static Block getBlock(int level) {
        switch (level) {
            case 1:
                return Blocks.coreShard;
            case 2:
                return Blocks.coreFoundation;
            case 3:
                return Blocks.coreNucleus;
            default:
                return Blocks.coreShard;
        }
    }
    
    public void draw() {
        if (Vars.world.tile(centreX, centreY) != null) {
            Call.effect(Bullets.missileExplosive.despawnEffect, this.drawCentreX, this.drawCentreY, 0, Vars.world.tile(centreX, centreY).team().color);
        }
    }

    public boolean hasCore() {
        return Vars.world.tile(centreX, centreY) != null && Vars.world.tile(centreX, centreY).build instanceof CoreBlock.CoreBuild;
    }
}
