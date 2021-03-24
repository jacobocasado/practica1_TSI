package src_Casado_deGracia_Jacobo;

import ontology.Types;
import tools.Vector2d;
import core.game.StateObservation;

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
    float f;
    float g;
    float h;


    public nodoConCoste(Vector2d posJugador, Vector2d orientacion) {
        this.nodoPadre = null;
        this.g = 0;
        this.posJugador = posJugador;
        this.orientacionJugador = getOrientation(orientacion);
        this.accion = null;
    }

    public nodoConCoste(nodoConCoste nodoPadre, Types.ACTIONS accion) {

        if (nodoPadre.accion != accion)
            this.orientacionJugador = recalcularOrientacion(accion);

        this.nodoPadre = nodoPadre;
        this.posJugador = calcularNuevaPosicion();
        this.accion = accion;
        this.g = nodoPadre.g+1;

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

    private orientation recalcularOrientacion(Types.ACTIONS accion){

        orientation orientacionNueva;

        switch(accion){
            case (Types.ACTIONS.ACTION_UP):
                orientacionNueva = orientation.ARRIBA;



        }

        return orientacionNueva;
    }
}




