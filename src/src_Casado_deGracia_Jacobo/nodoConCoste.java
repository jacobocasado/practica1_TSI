package src_Casado_deGracia_Jacobo;

import core.game.Observation;
import ontology.Types;
import tools.Vector2d;
import core.game.StateObservation;

import java.util.ArrayList;
import java.util.Stack;
import java.util.Vector;


enum orientation{ARRIBA, ABAJO, IZQUIERDA, DERECHA};

public class nodoConCoste implements Comparable<nodoConCoste> {

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
        this.h = distanciaManhattanDesde();

    }


    public nodoConCoste(nodoConCoste nodoPadre, Types.ACTIONS accion) {

        recalcularOrientacion(accion, nodoPadre.orientacionJugador);
        if (nodoPadre.orientacionJugador != orientacionJugador){
            this.g = nodoPadre.g + 2;
        }
        else{
            this.g = nodoPadre.g + 1;
        }


        this.nodoPadre = nodoPadre;
        this.accion = accion;
        modificarPosicion(nodoPadre.posJugador, accion);
        // todo perfeccionar. He hecho el metodo de detectar si hay enemigo. Pero con zona de calor renta mas.
        // todo si pasamos al lado de un muro que la funcion heuristica sea un poco mas cara. Asi el agente no le renta ir pegado al muro tampoco para que no le encierren y menos en las esquinas.
        this.h = distanciaManhattanDesde();
        this.h += Agent.nivelDePeligro(posJugador, estado);

    }

    // Metodo que devuelve un enumerado llamado orientacion a partir de un vector2d de la orientacion del juego.
    private orientation getOrientation (Vector2d vectorOrientacion){

        orientation orientacionJugador = null;

        if (vectorOrientacion.x == 1 && vectorOrientacion.y == 0)
            orientacionJugador = orientation.DERECHA;
        if (vectorOrientacion.x == -1 && vectorOrientacion.y == 0)
            orientacionJugador = orientation.IZQUIERDA;
        if (vectorOrientacion.x == 0 && vectorOrientacion.y == -1)
            orientacionJugador = orientation.ARRIBA;
        if (vectorOrientacion.x == 0 && vectorOrientacion.y == 1)
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
    private void modificarPosicion(Vector2d posJugador, Types.ACTIONS accion){

        Vector2d nuevaPosicion = new Vector2d(posJugador);

        switch(accion){
            case ACTION_UP:
                    nuevaPosicion = new Vector2d(posJugador.x, nuevaPosicion.y-1);
                    break;
            case ACTION_DOWN:
                    nuevaPosicion = new Vector2d(posJugador.x, posJugador.y+1);
                    break;
            case ACTION_LEFT:
                    nuevaPosicion = new Vector2d(posJugador.x - 1, posJugador.y);
                    break;
            case ACTION_RIGHT:
                    nuevaPosicion = new Vector2d(posJugador.x + 1, posJugador.y);
                    break;
        }

        this.posJugador = nuevaPosicion;
    }

    // Metodo que recalcula la posicion del jugador tras una accion y la DEVUELVE.
    private Vector2d calcularPosicion(Vector2d posJugador, Types.ACTIONS accion){

        Vector2d nuevaPosicion = new Vector2d(posJugador);

        switch(accion){
            case ACTION_UP:
                nuevaPosicion = new Vector2d(posJugador.x, nuevaPosicion.y-1);
                break;
            case ACTION_DOWN:
                nuevaPosicion = new Vector2d(posJugador.x, posJugador.y+1);
                break;
            case ACTION_LEFT:
                nuevaPosicion = new Vector2d(posJugador.x - 1, posJugador.y);
                break;
            case ACTION_RIGHT:
                nuevaPosicion = new Vector2d(posJugador.x + 1, posJugador.y);
                break;
        }

        return nuevaPosicion;
    }

    // Función que nos devuelve true si el jugador puede moverse tras realizar una accion o por lo contrario hay un muro delante.
    private boolean esPosibleMovermeHasta(Types.ACTIONS accion, StateObservation estadoActual){

        Vector2d posicionNueva = new Vector2d();

        switch(accion){
            case ACTION_UP:
                posicionNueva = calcularPosicion(this.posJugador, Types.ACTIONS.ACTION_UP);
                break;
            case ACTION_DOWN:
                posicionNueva = calcularPosicion(this.posJugador, Types.ACTIONS.ACTION_DOWN);
                break;
            case ACTION_LEFT:
                posicionNueva = calcularPosicion(this.posJugador, Types.ACTIONS.ACTION_LEFT);
                break;
            case ACTION_RIGHT:
                posicionNueva = calcularPosicion(this.posJugador, Types.ACTIONS.ACTION_RIGHT);
                break;
        }

        int tipoDeCasilla;
        tipoDeCasilla = obtenerTipo(posicionNueva);
        boolean esPosibleMoverse = false;
        if (tipoDeCasilla != 10 && tipoDeCasilla != 11 && tipoDeCasilla != 0){
            esPosibleMoverse = true;
        }


        return esPosibleMoverse;

    }

    boolean hayEnemigoEn(Vector2d posicion){
        return (obtenerTipo(posicion) == 10 || obtenerTipo(posicion) == 11);
    }

    private int obtenerTipo (Vector2d posicion){
            // Guardamos la posicion en un vector de Observation y, si es suelo, devolvemos -1, y si no es suelo, devuelve el tipo, que es otro entero.
            ArrayList<Observation> aux = estado.getObservationGrid()[(int)posicion.x][(int)posicion.y];

            // Si es vacio, es suelo, por lo tanto, podemos andar sin problemas, y devolvemos -1.
            if(!aux.isEmpty()){
                return aux.get(0).itype;
            }
        return -1;
    }

    // Método que devuelve la distancia Manhattan desde la posición del jugador hasta el objetivo que se haya marcado como meta.
    private int distanciaManhattanDesde(){
        return (int) (Math.abs(posJugador.x - posSiguienteDestino.x) + Math.abs(posJugador.y-posSiguienteDestino.y));
    }

    // Función que actualiza el estado del juego. Se llama cuando hay que comprobar si, por ejemplo, hay enemigos.
    // Se actualiza el estado antes de hacer la comprobación.
    // Método que reasigna a un nodo su nodo padre; esto ocurre cuando llegamos a un nodo por otro camino más óptimo.
    // Esto es recursivo y va a llamar al método para cada uno de sus hijos.

    public void setPadreNuevo(nodoConCoste nodoPadre){

        this.nodoPadre = nodoPadre;
        recalcularOrientacion(accion, nodoPadre.orientacionJugador);
        if (nodoPadre.orientacionJugador != orientacionJugador){
            this.g = nodoPadre.g + 2;
        }
        else{
            this.orientacionJugador = nodoPadre.orientacionJugador;
            this.g = nodoPadre.g + 1;
        }

        // Hago lo mismo con cada uno de los hijos.
        for (int i = 0; i < nodosHijos.size(); ++i){
            nodosHijos.get(i).setPadreNuevo(this);
        }
    }

    // Método necesario para poder comparar nodos.
    // Es posible gracias a que hemos podido
    @Override
    public int compareTo(nodoConCoste nodo) {

        if (this.posJugador.x == nodo.posJugador.x && this.posJugador.y == nodo.posJugador.y)
            return 0; // Significa que son iguales.

        else if ((this.g + this.h) > (nodo.g + nodo.h))
            return 1;
        else
            return -1;

    }

    void generarHijos(){
        // Miramos si es posible generar el hijo de cada tipo.
        // En caso de que sea posible, lo generamos.
        // Hijo ARRIBA (UP)

        if (this.nodoPadre == null){
            if (esPosibleMovermeHasta(Types.ACTIONS.ACTION_UP, estado))
                nodosHijos.add(new nodoConCoste(this, Types.ACTIONS.ACTION_UP));
            // Hijo ABAJO (DOWN)
            if (esPosibleMovermeHasta(Types.ACTIONS.ACTION_DOWN, estado))
                nodosHijos.add(new nodoConCoste(this, Types.ACTIONS.ACTION_DOWN));
            // Hijo IZQUIERDA (LEFT)
            if (esPosibleMovermeHasta(Types.ACTIONS.ACTION_LEFT, estado))
                nodosHijos.add(new nodoConCoste(this, Types.ACTIONS.ACTION_LEFT));
            // Hijo DERECHA (RIGHT)
            if (esPosibleMovermeHasta(Types.ACTIONS.ACTION_RIGHT, estado))
                nodosHijos.add(new nodoConCoste(this, Types.ACTIONS.ACTION_RIGHT));
        }
        else{

            if (esPosibleMovermeHasta(Types.ACTIONS.ACTION_UP, estado) && (nodoPadre.accion != Types.ACTIONS.ACTION_DOWN))
                nodosHijos.add(new nodoConCoste(this, Types.ACTIONS.ACTION_UP));
            // Hijo ABAJO (DOWN)
            if (esPosibleMovermeHasta(Types.ACTIONS.ACTION_DOWN, estado) && (nodoPadre.accion != Types.ACTIONS.ACTION_UP))
                nodosHijos.add(new nodoConCoste(this, Types.ACTIONS.ACTION_DOWN));
            // Hijo IZQUIERDA (LEFT)
            if (esPosibleMovermeHasta(Types.ACTIONS.ACTION_LEFT, estado) && (nodoPadre.accion != Types.ACTIONS.ACTION_RIGHT))
                nodosHijos.add(new nodoConCoste(this, Types.ACTIONS.ACTION_LEFT));
            // Hijo DERECHA (RIGHT)
            if (esPosibleMovermeHasta(Types.ACTIONS.ACTION_RIGHT, estado) && (nodoPadre.accion != Types.ACTIONS.ACTION_LEFT))
                nodosHijos.add(new nodoConCoste(this, Types.ACTIONS.ACTION_RIGHT));
        }

    }

    public Stack<Types.ACTIONS> getCamino(Stack<Types.ACTIONS> camino) {

        // Antes de meter la accion en la pila, compruebo si la orientacion es la misma que la del padre.
        // Esto lo hago porque si la orientacion es distinta, tengo que meter la accion dos veces en la pila para que el agente la realice correctamente.
        // Estoy en el nodo padre (el unico con accion nula)
        if (this.accion != null){
            camino.push(accion);
            if (orientacionJugador != nodoPadre.orientacionJugador){
                camino.push(accion);
            }
            nodoPadre.getCamino(camino);
        }

        return camino;
    }
}




