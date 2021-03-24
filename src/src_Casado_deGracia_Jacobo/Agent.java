package src_Casado_deGracia_Jacobo;

import core.game.Observation;
import core.game.StateObservation;
import core.player.AbstractPlayer;
import ontology.Types;
import tools.ElapsedCpuTimer;
import tools.Vector2d;
import tools.pathfinder.AStar;

import java.util.ArrayList;

public class Agent extends AbstractPlayer {

    Vector2d fescala;
    Vector2d pos_Portal;
    int nivel;

    public Agent (StateObservation stateObs, ElapsedCpuTimer elapsedTimer){

        // 1. Calculamos el factor de escala entre mundos (pixeles -> grid)
        fescala = new Vector2d(stateObs.getWorldDimension().width / stateObs.getObservationGrid().length ,
                stateObs.getWorldDimension().height / stateObs.getObservationGrid()[0].length);

        // 2. Calculamos la posicion del portal.
        pos_Portal = stateObs.getPortalsPositions()[0].get(0).position;
        pos_Portal.x = Math.floor(pos_Portal.x / fescala.x);
        pos_Portal.y = Math.floor(pos_Portal.y / fescala.y);

        // 3. Comprobamos si no hay gemas ni enemigos, en ese caso, ejecutamos el agente.
        if (stateObs.getResourcesPositions() == null && stateObs.getNPCPositions() == null)
            System.out.println("Estoy en lvl 1.");

    }

    /**
     * return the best action to arrive faster to the closest portal
     * @param stateObs Observation of the current state.
     * @param elapsedTimer Timer when the action returned is due.
     * @return best	ACTION to arrive faster to the closest portal
     */

    @Override
    public Types.ACTIONS act(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {
        //Posicion del avatar
        Vector2d avatar = new Vector2d(stateObs.getAvatarPosition().x / fescala.x,
                stateObs.getAvatarPosition().y / fescala.y);

        //Probamos las cuatro acciones y calculamos la distancia del nuevo estado al portal.
        Vector2d newPos_up = avatar, newPos_down = avatar, newPos_left = avatar, newPos_right = avatar;
        if (avatar.y - 1 >= 0) {
            newPos_up = new Vector2d(avatar.x, avatar.y - 1);
        }
        if (avatar.y + 1 <= stateObs.getObservationGrid()[0].length - 1) {
            newPos_down = new Vector2d(avatar.x, avatar.y + 1);
        }
        if (avatar.x - 1 >= 0) {
            newPos_left = new Vector2d(avatar.x - 1, avatar.y);
        }
        if (avatar.x + 1 <= stateObs.getObservationGrid().length - 1) {
            newPos_right = new Vector2d(avatar.x + 1, avatar.y);
        }

        return Types.ACTIONS.ACTION_NIL;
    }

}
