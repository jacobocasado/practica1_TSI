package src_Casado_deGracia_Jacobo;

import ontology.Types;
import tools.Vector2d;
import core.game.StateObservation;

import javax.swing.plaf.nimbus.State;
import java.util.Vector;


enum orientation{ARRIBA, ABAJO, IZQUIERDA, DERECHA};

public class nodoConCoste implements Comparable {

    // El nodo contiene la accion ultima que ha desembocado a llegar a ese nodo.
    Types.ACTIONS accion;
    // Vamos a ir almacenando los nodos padre porque no voy a usar una estructura de arbol, sino de nodos enlazados por punteros.
    nodoConCoste nodoPadre;
    // Es primordial tener la posicion y orientacion del jugador.
    Vector2d posJugador;
    orientation orientacionJugador;
    // Debemos de almacenar, para cada nodo, un vector
    Vector<nodoConCoste> nodosHijos = new Vector<nodoConCoste>();
    // Como A* es f = g + h
    float g;
    float h;
    // Necesitamos también almacenar la posición del siguiente destino al que queramos llegar.
    static Vector2d posSiguienteDestino;
    // Lo necesitamos para saber, más tarde, si el nodo es generable.
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
            recalcularOrientacion(accion, nodoPadre.orientacionJugador);
            this.g = nodoPadre.g + 2;
        }
        else
            this.g = nodoPadre.g + 1;

        this.nodoPadre = nodoPadre;
        this.accion = accion;
        recalcularPosicion(nodoPadre.posJugador, accion);
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
    private void recalcularOrientacion(Types.ACTIONS accion, orientation orientacionPadre){

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

        this.orientacionJugador = orientacionNueva;
    }
    // Método que, dada la posición del jugador y una acción a realizar, DEVUELVE la posición del jugador TRAS realizar la acción.
    private void recalcularPosicion(Vector2d posJugador, Types.ACTIONS accion){

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

        this.posJugador = nuevaPosicion;
    }
    // Función que nos devuelve true si el jugador puede moverse tras realizar una accion o por lo contrario hay un muro delante.
    private boolean esPosibleMoverme(Types.ACTIONS accion, StateObservation estadoActual){

        // TODO que además de comprobar si hay un muro, compruebe si hay un enemigo.
        recalcularEstado(estadoActual);
        boolean esPosibleMoverse = false;

        switch(accion){
            case ACTION_UP:
                if (posJugador.y - 1 >= 0) {
                    esPosibleMoverse = true;
                    break;
                }
            case ACTION_DOWN:
                if (posJugador.y + 1 <= estado.getObservationGrid()[0].length-1) {
                    esPosibleMoverse = true;
                    break;
                }
            case ACTION_LEFT:
                if (posJugador.x - 1 >= 0) {
                    esPosibleMoverse = true;
                    break;
                }
            case ACTION_RIGHT:
                if (posJugador.x + 1 <= estado.getObservationGrid().length - 1) {
                    esPosibleMoverse = true;
                    break;
                }
        }

        return esPosibleMoverse;

    }
    // Método que devuelve la distancia Manhattan desde la posición del jugador hasta el objetivo que se haya marcado como meta.
    private float distanciaManhattanDesde(Vector2d posJugador){
        return (float) (Math.abs(posJugador.x - posSiguienteDestino.x) + Math.abs(posJugador.y-posSiguienteDestino.y));
    }
    // Función que actualiza el estado del juego. Se llama cuando hay que comprobar si, por ejemplo, hay enemigos.
    // Se actualiza el estado antes de hacer la comprobación.
    private void recalcularEstado(StateObservation estadoNuevo){
        estado = estadoNuevo;
    }
    // Método que reasigna a un nodo su nodo padre; esto ocurre cuando llegamos a un nodo por otro camino más óptimo.
    // Esto es recursivo y va a llamar al método para cada uno de sus hijos.
    private void setPadreNuevo(nodoConCoste nodoPadre){

        this.nodoPadre = nodoPadre;
        if (nodoPadre.accion != accion){
            recalcularOrientacion(accion, nodoPadre.orientacionJugador);
            this.g = nodoPadre.g + 2;
        }
        else
            this.g = nodoPadre.g + 1;

        // Hago lo mismo con cada uno de los hijos.
        for (int i = 0; i < nodosHijos.size(); ++i){
            nodosHijos.get(i).setPadreNuevo(this);
        }
    }

    // Método necesario para poder comparar nodos.
    // Es posible gracias a que hemos podido
    @Override
    // TODO hacer el compareTo
    public int compareTo(Object o) {
        return 0;
    }

    void generarHijos(){
        // Miramos si es posible generar el hijo de cada tipo.
        // En caso de que sea posible, lo generamos.
        // Hijo ARRIBA (UP)
        if (esPosibleMoverme(Types.ACTIONS.ACTION_UP, estado))
            nodosHijos.add(new nodoConCoste(this, Types.ACTIONS.ACTION_UP));
        // Hijo ABAJO (DOWN)
        if (esPosibleMoverme(Types.ACTIONS.ACTION_DOWN, estado))
            nodosHijos.add(new nodoConCoste(this, Types.ACTIONS.ACTION_DOWN));
        // Hijo IZQUIERDA (LEFT)
        if (esPosibleMoverme(Types.ACTIONS.ACTION_LEFT, estado))
            nodosHijos.add(new nodoConCoste(this, Types.ACTIONS.ACTION_LEFT));
        // Hijo DERECHA (RIGHT)
        if (esPosibleMoverme(Types.ACTIONS.ACTION_RIGHT, estado))
            nodosHijos.add(new nodoConCoste(this, Types.ACTIONS.ACTION_RIGHT));
    }
}




