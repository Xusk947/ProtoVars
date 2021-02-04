package ProtoVars;

import arc.math.Mathf;
import mindustry.Vars;
import mindustry.gen.Unit;

public class ProtoAI {

    Actions action = Actions.WAIT;
    Room room;
    public int waitTimer = 0;
    public int minGroupSize = 3, maxGroupSize = 2, groupSize = 1;

    public ProtoAI(Room room) {
        this.room = room;
        minGroupSize = minGroupSize + Mathf.random(4);
        maxGroupSize = minGroupSize + Mathf.random(2, 5);
    }

    public void update() {
        if (waitTimer <= 0) {
            switch (action) {
                case WAIT:
                    groupSize = Mathf.random(minGroupSize, maxGroupSize);
                    waitTimer =  groupSize * room.level;
                    action = Actions.ATTACK;
                    break;
                case GETTING:
                    waitTimer =  Mathf.random(5, 12);
                    action = Actions.WAIT;
                    break;
                case ATTACK:
                    for (int i = 0; i < groupSize; i++) {
                        room.spawn();
                        room.side++;
                    }
                    action = Mathf.random(100) > 95 ? Actions.WAIT : Actions.GETTING;
                    break;
                default:
                    action = Actions.WAIT;
                    break;
            }
        } else {
            waitTimer--;
        }
    }

    public enum Actions {
        WAIT, GETTING, ATTACK;
    }
}
