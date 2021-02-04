package ProtoVars;

import static ProtoVars.Main.DEBUG;
import arc.math.Mathf;
import arc.math.geom.Point2;
import arc.struct.IntMap;
import arc.struct.IntSeq;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Nullable;
import arc.util.Timer;
import mindustry.Vars;
import static mindustry.Vars.world;
import mindustry.content.Blocks;
import mindustry.entities.Units;
import mindustry.game.Team;
import mindustry.game.Teams.TeamData;
import mindustry.gen.Call;
import mindustry.gen.Groups;
import mindustry.gen.Player;
import mindustry.gen.Unit;
import mindustry.world.Tile;
import mindustry.world.blocks.storage.CoreBlock;

public class ProtoData {

    public static Seq<Room> rooms = new Seq<>();
    public static IntMap<Room> roomPos = new IntMap<>();
    public static IntMap<Player> teamMap = new IntMap<>();
    public static IntMap<Seq<Room>> controlled = new IntMap<>();

    public static void generate() {
        roomPos.clear();
        rooms.clear();

        ProtoGenerator gen = new ProtoGenerator();

        world.loadGenerator(gen.size, gen.size, gen);

        rooms = gen.rooms.copy();
        IntSeq tilepos = gen.getRooms();

        for (int i = 0; i < tilepos.size; i++) {
            int index = tilepos.get(i);
            Room room = new Room(i, Point2.x(index), Point2.y(index));
            room.init(Vars.world.tiles);
            rooms.add(room);
        }
        roomPos.clear();
        for (Room room : rooms) {
            roomPos.put(Point2.pack(room.centreX, room.centreY), room);
        }
    }

    public @Nullable
    static Room getRoom(int pos) {
        return roomPos.get(pos);
    }

    public static Seq<Room> getControlledRooms(Player player) {
        return controlled.get(player.team().id);
    }

    public static Seq<Room> getControllerRooms(Team team) {
        return controlled.get(team.id);
    }

    public static void update() {
        teamMap.clear();
        Groups.player.each(p -> {
            teamMap.put(p.team().id, p);
        });
        for (Room room : rooms) {
            if (!room.dead) {
                // Fx draw
                if (DEBUG) {
                    room.draw();
                }
                // give money to core and check 
                if (room.count <= Room.level(room.level)) {
                    room.count++;
                }
                // upgrade core when get max money count
                if (room.level < Room.MAX_LEVEL && room.count >= Room.level(room.level)) {
                    room.level++;
                    room.count = 0;
                }
                // Label some information about core
                if (Vars.world.tile(room.centreX, room.centreY) != null) {
                    StringBuilder text = new StringBuilder();
                    text.append("[#").append(Vars.world.tile(room.centreX, room.centreY).team().color.toString()).append("]").append(room.count).append(" / ").append(Room.level(room.level)).append(" | ").append(room.level);
                    
                    if (Main.DEBUG && room.controller == null) {
                        text.append("\n").append("[white]Action: ").append(room.ai.action);
                        text.append("\ntimer: ").append(room.ai.waitTimer);
                    }

                    Call.label(text.toString(), 2, room.drawCentreX, room.drawCentreY);
                }

                if (room.controller == null) {
                    room.ai.update();
                }

                if (!room.hasCore()) {
                    if (room.controller != null) {
                        Vars.world.tile(room.centreX, room.centreY).setNet(Room.getBlock(room.level), room.controller.team(), 0);
                    } else {
                        Vars.world.tile(room.centreX, room.centreY).setNet(Room.getBlock(room.level), Team.derelict, 0);
                    }
                } else if (room.hasCore()) {
                    Team team = Vars.world.tile(room.centreX, room.centreY).team();
                    Units.nearbyEnemies(team, room.drawCentreX - Vars.tilesize * (Room.SIZE / 2), room.drawCentreY - Vars.tilesize * (Room.SIZE / 2), Vars.tilesize * Room.SIZE, Vars.tilesize * Room.SIZE, u -> {
                        if (!u.spawnedByCore && room.count > 0) {
                            if (Room.getDamage.containsKey(u.type)) {
                                room.count = room.count - Room.getDamage.get(u.type);
                            } else {
                                room.count--;
                            }
                            u.kill();
                        } else if (!u.spawnedByCore) {
                            room.count = 0;
                            Unit unit = u.team().data().units.find(uu -> uu.isPlayer());
                            if (unit != null) {
                                room.controller = unit.getPlayer();
                            } else {
                                room.controller = null;
                            }

                            Tile tile = Vars.world.tile(room.centreX, room.centreY);
                            tile.setNet(tile.block(), u.team(), 0);
                            u.kill();
                        }
                    });
                }
            } else {
                rooms.remove(room);
            }
        }

        // game endder lol
        Seq<TeamData> winners = getLeaderboard();
        if (winners.size > 0 && rooms.size > 0) {
            for (TeamData winner : winners) {
                if (winner.cores.size <= 0) continue;
                if (winner.cores.size > Mathf.floor((float) rooms.size / 100 * 70)) {
                    String text = "[gold]Winner is: ";

                    Unit unit = winner.units.find(u -> u.isPlayer());
                    if (unit != null) { 
                        text += unit.getPlayer().name;
                    } else {
                        Team team = winner.team;
                        text += "[#" + team.color + "]" + team;
                    }

                    Call.infoMessage(text);
                    Main.startGame();
                }
            }
        }
    }

    public static void updateLeader() {
        Seq<TeamData> leaders = getLeaderboard();

        if (leaders.size > 0) {
            Player player = Groups.player.find(p -> p.team() == leaders.get(0).team);
            String text = "[gold]-- leader --[white]\n";
            if (player != null) {
                text += player.name;
            } else {
                text += "[#" + leaders.get(0).team.color.toString() + "]" + leaders.get(0).team;
            }
            for (Player player1 : Groups.player) {
                if (player1.team().cores().size > 0) {
                    CoreBlock.CoreBuild core = player1.team().cores().sort(c -> -Mathf.dst(c.x, c.y, player1.x, player1.y)).get(0);
                    if (core != null) {
                        text += "\n[accent]core at:[white] " + core.tileX() + " : " + core.tileY();
                    } else {
                        text += "\n[accent]you |[red] ded";
                    }
                }
                Call.setHudText(player1.con, text);
            }
        } else {
            Call.hideHudText();
        }
    }

    public static Seq<TeamData> getLeaderboard() {
        Seq<TeamData> data = new Seq<>();
        for (Room room : rooms) {
            if (room.hasCore()) { 
                TeamData dat = Vars.world.tile(room.centreX, room.centreY).build.team.data();
                if (!data.contains(dat)) {
                    data.add(dat);
                }
            }
        }
        return data.sort(team -> -team.cores.size);
    }
}
