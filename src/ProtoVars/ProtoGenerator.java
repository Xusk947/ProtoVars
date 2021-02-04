/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ProtoVars;

import static ProtoVars.Room.SIZE;
import static ProtoVars.Room.SPACING;
import arc.func.Cons;
import arc.math.geom.Point2;
import arc.struct.IntSeq;
import arc.struct.Seq;
import arc.struct.StringMap;
import arc.util.Log;
import arc.util.Nullable;
import mindustry.Vars;
import mindustry.world.Tiles;

import mindustry.content.Blocks;
import mindustry.game.Team;
import mindustry.maps.Map;
import mindustry.world.Tile;

public class ProtoGenerator implements Cons<Tiles> {

    public static int ID = 0;

    int size = 250;
    int roomSize = 25;
    int mapWidth, mapHeight;
    Seq<Room> rooms = new Seq<>();
    
    @Override
    public void get(Tiles tiles) {
        for (int x = 0; x < tiles.width; x++) {
            for (int y = 0; y < tiles.height; y++) {
                tiles.set(x, y, new Tile(x, y, Blocks.stone, Blocks.air, Blocks.stoneWall));
            }
        }
        // Generate Square Map
        for (int xx = 0; xx < tiles.width / Room.SIZE; xx++) {
            for (int yy = 0; yy < tiles.height / Room.SIZE; yy++) {
                if (xx * Room.SIZE + (Room.SPACING * yy) >= tiles.width || yy * roomSize + (Room.SPACING * yy) >= tiles.height) {
                    continue;
                }
                if (xx * SIZE + xx * SPACING > tiles.width || yy * SIZE + yy * SPACING > tiles.height) {
                    continue;
                }
                mapWidth = Math.max(xx, mapWidth);
                mapHeight = Math.max(yy, mapHeight);
            }
        }

        tiles.getn(5, 5).setNet(Blocks.coreShard, Team.sharded, 0);

        Vars.state.map = new Map(StringMap.of("name", "Proto : " + ID));
    }
    
    public IntSeq getRooms() {
        IntSeq ar = new IntSeq();
        for (int x = 0; x < mapWidth; x++) {
            for (int y = 0; y < mapHeight; y++) {
                ar.add(Point2.pack(x, y));
            }
        }
        return ar;
    } 
}
