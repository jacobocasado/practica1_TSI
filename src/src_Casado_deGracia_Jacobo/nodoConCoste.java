package src_Casado_deGracia_Jacobo;

import ontology.Types;
import tools.Vector2d;
import core.game.StateObservation;

import javax.swing.plaf.nimbus.State;
import java.util.Vector;


enum orientation{ARRIBA, ABAJO, IZQUIERDA, DERECHA};

public class nodoConCoste {

    // El nodo contiene la accion ultima que ha desembocado a llegar a ese nodo.
    Types.ACTIONS accion;
    // Vamos a ir almacenando los nodos padre porque no voy a usar una estructura de arbol, sino de nodos enlazados por punteros.
    nodoConCoste nodoPadre;
    // Es primordial tener la posicion y orientacion del jugador.
    Vector2d posJugador;
    orientation orientacionJugador;
    // Como A* es f = g + h
    float g;
    float h;
    // Necesitamos también almacenar la posición del siguiente destino al que queramos llegar.
    static Vector2d posSiguienteDestino;
    static StateObservation estado;

    public nodoConCoste(Vector2d posJugador, Vector2d orientacion, Vector2d posicionSiguienteDestino, StateObservation stateObs) {
        posSiguienteDestino = posicionSiguienteDestino;
        estado = stateObs;

        this.nodoPadre = null;
        this.posJugador = posJugador;
        this.orientacionJugador = getOrientation(orientacion);
        this.accion = null;
        this.g = 0;
        this.h = distanciaManhattanDesde(this.posJugador);

    }

    public nodoConCoste(nodoConCoste nodoPadre, Types.ACTIONS accion) {

        if (nodoPadre.accion != accion){
            this.orientacionJugador = recalcularOrientacion(accion, nodoPadre.orientacionJugador);
            this.g = nodoPadre.g + 2;
        }
        else
            this.g = nodoPadre.g + 1;

        this.nodoPadre = nodoPadre;
        this.accion = accion;
        this.posJugador = recalcularPosicion(nodoPadre.posJugador, accion);
        this.h = distanciaManhattanDesde(this.posJugador);
    }

    // Metodo que devuelve un enumerado llamado orientacion a partir de un vector2d de la orientacion del juego.
    private orientation getOrientation (Vector2d vectorOrientacion){

        orientation orientacionJugador = null;

        if (vectorOrientacion == new Vector2d(1,0))
            orientacionJugador = orientation.DERECHA;
        if (vectorOrientacion == new Vector2d(-1,0))
            orientacionJugador = orientation.IZQUIERDA;
        if (vectorOrientacion == new Vector2d(0,-1))
            orientacionJugador = orientation.ARRIBA;
        if (vectorOrientacion == new Vector2d(0,1))
            orientacionJugador = orientation.ABAJO;

        return orientacionJugador;
    }
    // Metodo que tras ejecutar una acción devuelva la nueva orientación correspondiente.
    private orientation recalcularOrientacion(Types.ACTIONS accion, orientation orientacionPadre){

        orientation orientacionNueva = orientacionPadre;

        switch(accion){
            case ACTION_UP:
                orientacionNueva = orientation.ARRIBA;
                break;
            case ACTION_DOWN:
                orientacionNueva = orientation.ABAJO;
                break;
            case ACTION_LEFT:
                orientacionNueva = orientation.IZQUIERDA;
                break;
            case ACTION_RIGHT:
                orientacionNueva = orientation.DERECHA;
                break;
        }

        return orientacionNueva;
    }
    // Método que, dada la posición del jugador y una acción a realizar, DEVUELVE la posición del jugador TRAS realizar la acción.
    private Vector2d recalcularPosicion(Vector2d posJugador, Types.ACTIONS accion){

        Vector2d nuevaPosicion = new Vector2d(posJugador);

        switch(accion){
            case ACTION_UP:
                if (posJugador.y - 1 >= 0) {
                    nuevaPosicion = new Vector2d(posJugador.x, nuevaPosicion.y-1);
                    break;
                }
            case ACTION_DOWN:
                if (posJugador.y + 1 <= estado.getObservationGrid()[0].length-1) {
                    nuevaPosicion = new Vector2d(posJugador.x, posJugador.y+1);
                    break;
                }
            case ACTION_LEFT:
                if (posJugador.x - 1 >= 0) {
                    nuevaPosicion = new Vector2d(posJugador.x - 1, posJugador.y);
                    break;
                }
            case ACTION_RIGHT:
                if (posJugador.x + 1 <= estado.getObservationGrid().length - 1) {
                    nuevaPosicion = new Vector2d(posJugador.x + 1, posJugador.y);
                    break;
                }
        }

        return nuevaPosicion;
    }
    // Método que devuelve la distancia Manhattan desde la posición del jugador hasta el objetivo que se haya marcado como meta.
    private float distanciaManhattanDesde(Vector2d posJugador){
        return (float) (Math.abs(posJugador.x - posSiguienteDestino.x) + Math.abs(posJugador.y-posSiguienteDestino.y));
    }

    private void recalcularEstado(StateObservation estadoNuevo){
        estado = estadoNuevo;
    }


}




