package ProtoVars;

import static ProtoVars.Room.TEAM;
import arc.Events;
import arc.math.geom.Geometry;
import arc.struct.Seq;
import arc.util.CommandHandler;
import arc.util.Interval;
import arc.util.Log;
import arc.util.Timer;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.content.Fx;
import mindustry.content.UnitTypes;
import mindustry.entities.Damage;
import mindustry.entities.Units;
import mindustry.game.EventType;
import mindustry.gen.Call;
import mindustry.gen.Groups;
import mindustry.gen.Player;
import mindustry.mod.Plugin;

import mindustry.game.Rules;
import mindustry.game.Team;

public class Main extends Plugin {

    public static boolean DEBUG = false;
    Interval interval = new Interval(4);

    int coreUpdate = 0, coreUpdateTime = 60 * 2;
    int leaderBoardUpdate = 1, leaderBoardUpdateTime = 60 * 4;

    public static Rules rules = new Rules();

    @Override
    public void init() {
        initRules();
        Room.init();

        Log.info("| ProtoVars  Loaded |");

        Events.run(EventType.Trigger.update, () -> {
            if (interval.get(coreUpdate, coreUpdateTime)) {
                ProtoData.update();
            }

            if (interval.get(leaderBoardUpdate, leaderBoardUpdateTime)) {
                ProtoData.updateLeader();
            }

            // kill units with no core
            Groups.unit.each(u -> {
                if (u.core() == null) {
                    u.kill();
                }
            });
            
            // Like touch pad just for cores oh no 
            Groups.player.forEach(player -> {
                player.team().cores().forEach(core -> {
                    if (player.unit().aimX > core.x - core.block.size * Vars.tilesize && player.unit().aimX < core.x + core.block.size * Vars.tilesize
                            && player.unit().aimY > core.y - core.block.size * Vars.tilesize && player.unit().aimY < core.y + core.block.size * Vars.tilesize) {
                        Call.effect(Fx.ballfire, player.unit().aimX, player.unit().aimY, 0, player.team().color);
                        Room room = ProtoData.getRoom(core.pos());
                        if (room != null) {
                            if (room.count - room.level > 0) {
                                room.spawn();
                                room.side++;
                            }
                        }
                    }
                });
            });
        });

        Events.on(EventType.ServerLoadEvent.class, (event) -> {
            startGame();
            Vars.netServer.openServer();
        });
        // Find Room
        Events.on(EventType.PlayerJoin.class, event -> {
            Seq<Room> rooms = ProtoData.rooms.copy();
            rooms.shuffle();
            Room room = rooms.find(r -> r.controller == null && r.dead == false);
            if (room != null) {
                // set controller to this player
                room.controller = event.player;
                // set team for player
                event.player.team(Vars.world.tile(room.centreX, room.centreY).team());
                // add room for hexdata.controlled
                ProtoData.controlled.put(event.player.team().id, new Seq<>(new Room[]{room}));
            }
            String text = "\n[accent]Target[white]: Capture 70% of the map";
            text += "\n[accent]Summon: [white]Click on the core to summon units";
            text += "\n[accent]Upgrade: [white]Wait when score reach max value";
            text += "\n[accent]Capture: [white]Send units to other cores";
            Call.sendMessage(event.player.con, text, "[yellow]Proto", event.player);
        });

        Events.on(EventType.PlayerLeave.class, event -> {
            Seq<Room> rooms = ProtoData.getControlledRooms(event.player);
            if (rooms == null) {
                return;
            }
            for (Room room : rooms) {
                Room.TEAM++;
                if (room.hasCore()) {
                    Vars.world.tile(room.centreX, room.centreY).build.team(Team.get(Room.TEAM));
                }
            }
        });

        Events.on(EventType.BlockDestroyEvent.class, event -> {
            Room room = ProtoData.getRoom(event.tile.pos());

            if (room != null) {
                room.dead = true;
                Units.nearbyEnemies(event.tile.team(), event.tile.drawx() - Vars.tilesize * (Room.SIZE / 2), event.tile.drawy() - Vars.tilesize * (Room.SIZE / 2), Vars.tilesize * Room.SIZE, Vars.tilesize * Room.SIZE, u -> {
                    if (!u.spawnedByCore) {
                        room.controller = ProtoData.teamMap.get(u.team.id);
                        if (room.dead) {
                            room.dead = false;
                            ProtoData.controlled.get(u.team.id).add(room);
                        }
                        u.kill();
                    }
                });
            }
        });
    }

    @Override
    public void registerClientCommands(CommandHandler handler) {
    }

    @Override
    public void registerServerCommands(CommandHandler handler) {
    }

    public static void startGame() {
        ProtoData.controlled.clear();
        ProtoData.roomPos.clear();
        ProtoData.teamMap.clear();
        ProtoData.rooms.clear();
        Seq<Player> players = new Seq<>();

        for (Player p : Groups.player) {
            players.add(p);
        }

        // Logic Reset Start
        Vars.logic.reset();

        // World Load Start
        Call.worldDataBegin();

        Vars.state.rules = rules.copy();

        ProtoData.generate();
        // Logic Reset End
        Vars.logic.play();

        // Send World Data To All Players
        for (Player p : players) {
            Vars.netServer.sendWorldData(p);
        }
    }

    public void initRules() {
        rules.canGameOver = false;
        Vars.content.units().each(unit -> {
            unit.weapons.clear();
            unit.range = 3f;
        });
    }
}
