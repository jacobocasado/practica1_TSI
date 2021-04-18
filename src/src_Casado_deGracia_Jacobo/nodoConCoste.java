package src_Casado_deGracia_Jacobo;

import core.game.Observation;
import ontology.Types;
import tools.Vector2d;
import core.game.StateObservation;

import java.util.ArrayList;
import java.util.Stack;
import java.util.Vector;


/**
*   Creamos un enumerado de la orientacion para trabajar mucho mejor con este parametro, en vez de usar un Vector2d todo el rato.
*   Las referencias a partir de ahora para la orientacion del jugador seran las que aparecen en el enumerado.*
* */
enum orientation{ARRIBA, ABAJO, IZQUIERDA, DERECHA}

/**Clase nodoConCoste. Esta clase, es la que va a generar el arbol del A Star, simplemente al obtener el nodo objetivo (solucion),
al ser una estructura con nodos con punteros al padre, cuando encontremos un nodo solucion, iremos apuntando al padre sucesivamente hasta llegar al nodo inicial.*/

public class nodoConCoste implements Comparable<nodoConCoste> {

    // El nodo contiene la accion ultima que ha desembocado a llegar a ese nodo.
    Types.ACTIONS accion;
    // Vamos a ir almacenando los nodos padre porque no voy a usar una estructura de arbol, sino de nodos enlazados por punteros.
    nodoConCoste nodoPadre;
    // Es primordial tener la posicion y orientacion del jugador.
    Vector2d posJugador;
    orientation orientacionJugador;
    // Debemos de almacenar, para cada nodo, un vector de sus nodos hijos (esto nos vale por si tenemos que recalcular en algun momento la ruta dentro del arbol).
    Vector<nodoConCoste> nodosHijos = new Vector<>();
    // Como A* es f = g + h, no necesitamos almacenar f, sino que es un valor calculable a partir de los dos parametros anteriores.
    float g;
    float h;
    // Necesitamos también almacenar la posición del siguiente destino al que queramos llegar.
    static Vector2d posSiguienteDestino;
    // Lo necesitamos para saber, más tarde, si el nodo es generable.
    static StateObservation estado;
    // ACTUALIZACION: Se le ha incluido a cada nodo, la profundidad de expansion; util para el A Star con profundidad
    // que se usara para el nivel 5.
    int profundidad;


    /** CONSTRUCTOR DEL PRIMER NODO (OJO, SOLO EL PRIMERO, YA QUE EL SEGUNDO RECIBE COMO ARGUMENTO SU NODO PADRE).
        @param la posicion del jugador en ese momento, la orientacion y la posicion del siguiente destino como Vector2d, y el estado del juego.
        El constructor inicializa la variable de clase de estado y la de posicionSiguienteDestino, y calcula la orientacion a enumerado
        usando el metodo getorientation y recibiendo de parametro el Vector2d anteriormente descrito.
        Su funcion heuristica es la distanciaManhattan desde la posicion del jugador hasta el destino.
        La g es 0 ya que es el primer nodo a generar y el camino ya recorrido es 0.
    *
    * */
    public nodoConCoste(Vector2d posJugador, Vector2d orientacion, Vector2d posicionSiguienteDestino, StateObservation stateObs) {
        posSiguienteDestino = posicionSiguienteDestino;
        estado = stateObs;

        this.nodoPadre = null;
        this.posJugador = posJugador;
        this.orientacionJugador = getOrientation(orientacion);
        this.accion = null;
        this.g = 0;
        this.h = distanciaManhattanDesde();
        this.profundidad = 0;

    }

    /**  CONSTRUCTOR DE nodoConCoste para los hijos
        @param recibe el nodoPadre, del cual va a heredar ciertas cosas, y una accion la cual va a realizar.
        Por tanto, cuando un padre quiera generar los 4 hijos, simplemente habra que llamar al constructor
        pasandole este nodo padre y los 4 tipos diferentes de acciones.
        El nodo automaticamente calcula la funcion heuristica viendo si la accion es la misma que su padre
        (ya que si es distinta, se suman 2 tics al juego) y si es la misma solo se suma 1.
        El agente modifica su posicion dada la posicion del estado anterior y la accion que ha realizado,
        obviamente comprobando si se puede mover (pero eso se vera en la declaracion del metodo modificarPosicion)
        y vuelve a recalcular la funcion heuristica, siempre siendo la distancia Manhattan desde la posicion del nodo hasta
        el objetivol
    *
    *
    * */

    public nodoConCoste(nodoConCoste nodoPadre, Types.ACTIONS accion) {

        // Aumento en 1 la profundidad.
        this.profundidad = nodoPadre.profundidad + 1;

        // Recalculo la orientacion.
        recalcularOrientacion(accion, nodoPadre.orientacionJugador);
        // Sumo 2 o 1 a mi funcion heuristica dependiendo de la orientacion mia y la del nodo que me genero.
        if (nodoPadre.orientacionJugador != orientacionJugador){
            this.g = nodoPadre.g + 2;
        }
        else{
            this.g = nodoPadre.g + 1;
        }

        // El puntero a mi nodo padre hago que apunte a el y la accion que le he pasado por parametro la guardo como mi accion.
        this.nodoPadre = nodoPadre;
        this.accion = accion;
        // Modifico la posicion si puedo y se me permite y posteriormente calculo la distancia Manhattan de la posicion actualizada.
        modificarPosicion(nodoPadre.posJugador, accion);
        this.h = distanciaManhattanDesde();

    }
    /** Metodo que devuelve un enumerado llamado orientacion a partir de un vector2d de la orientacion del juego.
    *  @param vectorOrientacion vector de orientacion 2d
    *  @return El enumerado de orientacion correspondiente al vector, teniendo en cuenta de que las casillas van aumentando en unidad conforme vayas
    *          avanzando hacia abajo y hacia la derecha (ya que el 0,0 esta en la esquina superior izquierda).
    * */

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

    public void setDestino(Vector2d nuevoDestino){
        posSiguienteDestino = nuevoDestino;
    }

    /** Metodo que tras ejecutar una acción devuelva la nueva orientación correspondiente.
        @param la accion a realizar, y la orientacion del nodo padre.
        @return la orientacion nueva tras realizar esa accion.
        Este metodo es importante, sobre todo a la hora de calcular la (g) en los nodos.
    */

    private void recalcularOrientacion(Types.ACTIONS accion, orientation orientacionPadre){

        this.orientacionJugador = switch (accion) {
            case ACTION_UP -> orientation.ARRIBA;
            case ACTION_DOWN -> orientation.ABAJO;
            case ACTION_LEFT -> orientation.IZQUIERDA;
            case ACTION_RIGHT -> orientation.DERECHA;
            default -> orientacionPadre;
        };
    }

    /** Método que, dada la posición del jugador y una acción a realizar, ASIGNA A LA posición del jugador TRAS realizar la acción.
        @param La posicion del jugador en Vector2d y la accion a realizar por el jugador.
        @return NO DEVUELVE NADA SINO QUE ASIGNA LA POSICION AL NODO.
        OJO: EN EL METODO INFERIOR, calcularPosicion, se hace lo mismo pero no se asigna a la variable de estancia posJugador sino que se devuelve.
    */
    private void modificarPosicion(Vector2d posJugador, Types.ACTIONS accion){

        Vector2d nuevaPosicion = new Vector2d(posJugador);

        /*switch(accion){
            case ACTION_UP:
                    nuevaPosicion = new Vector2d(posJugador.x, posJugador.y-1);
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
        }*/

        this.posJugador = calcularPosicion(posJugador, accion);
    }

    /** Metodo que recalcula la posicion del jugador tras una accion y la DEVUELVE.
        @param La posicion del jugador en Vector2d y la accion a realizar por el jugador.
        @return La nueva posicion tras realiar esa accion, en un Vector2d.
        OJO: El metodo hace el mismo calculo que la funcion superior pero en la otra funcion la asignamos a la variable
        de estancia y en este caso la calculamos y la devolvemos en el return.
    */
    private Vector2d calcularPosicion(Vector2d posJugador, Types.ACTIONS accion){

        Vector2d nuevaPosicion = new Vector2d(posJugador);

        switch(accion){
            case ACTION_UP:
                nuevaPosicion = new Vector2d(posJugador.x, posJugador.y-1);
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

    /** Función que nos devuelve true si el jugador puede moverse tras realizar una accion o por lo contrario hay un muro delante.
        UPDATE: El agente se quedaba bloqueado si no era posible moverse hacia una casilla donde hubiera un enemigo ya que,
        si hay un UNICO camino y tiene que pasar por el enemigo, el programa se queda bloqueado ya que no hay un camino hacia la solucion
        (lo tiene en cuenta como si fuera un muro).
        Por tanto, es posible moverse hacia todas las casillas EXCEPTO hacia aquellas que sean codificadas como muro.
        @param accion accion del jugador
        @param estadoActual estado del juego en ese momento.
        @return Si es posible moverse tras realizar esa accion o por el contrario hay un muro delante y la accion no sirve para nada.

        OJO: Este metodo es muy importante para evitarnos bucles, ya que cortamos el nodo en cuanto detectemos que no nos podemos mover hacia ese sitio.
    */
    private boolean esPosibleMovermeHasta(Types.ACTIONS accion, StateObservation estadoActual){

        new Vector2d();
        Vector2d posicionNueva = switch (accion) {
            case ACTION_UP -> calcularPosicion(this.posJugador, Types.ACTIONS.ACTION_UP);
            case ACTION_DOWN -> calcularPosicion(this.posJugador, Types.ACTIONS.ACTION_DOWN);
            case ACTION_LEFT -> calcularPosicion(this.posJugador, Types.ACTIONS.ACTION_LEFT);
            case ACTION_RIGHT -> calcularPosicion(this.posJugador, Types.ACTIONS.ACTION_RIGHT);
            default -> new Vector2d();
        };

        // Esto lo usaremos mucho mas a menudo, usando la funcion obtenerTipo para obtener el tipo de casilla y posteriormente
        // hacer calculos basado en ese tipo.
        int tipoDeCasilla;
        tipoDeCasilla = obtenerTipo(posicionNueva);
        boolean esPosibleMoverse = false;
        if  (tipoDeCasilla != 0){
            esPosibleMoverse = true;
        }

        return esPosibleMoverse;

    }


    /** Funcion que dada la posicion de una casilla, calcule el tipo de esta, devolviendolo en un numero.
            @param posicion posicion de la casilla la cual queremos saber el tipo.
            @return El tipo de la casilla de forma numerica.
            OJO: El tipo de la casilla es aquel que el ESTADO DEL JUEGO LE ASIGNE NUMERICAMENTE,
                ES DECIR, NO ES ALGO QUE NOSOTROS PODAMOS CONTROLAR.
                POR EJEMPLO, EL MURO ES TIPO 0.
                Eso viene en la propia codificacion del juego.
        */
    private int obtenerTipo (Vector2d posicion){

        if(((posicion.x < estado.getObservationGrid().length) && (posicion.x >= 0)) &&
                ((posicion.y < estado.getObservationGrid()[0].length && (posicion.y >= 0)))){
            // Guardamos la posicion en un vector de Observation y, si es suelo, devolvemos -1, y si no es suelo, devuelve el tipo, que es otro entero.
            ArrayList<Observation> aux = estado.getObservationGrid()[(int)posicion.x][(int)posicion.y];
            // Si es vacio, es suelo, por lo tanto, podemos andar sin problemas, y devolvemos -1.
            if(!aux.isEmpty()){
                return aux.get(0).itype;
            }
            return -1;
        }
        else return 0;
    }

    // Método que devuelve la distancia Manhattan desde la posición del jugador hasta el objetivo que se haya marcado como meta.
    // Es una simple resta en coordenadas x en valor absoluto sumado a una resta en coordenadas y en valor absoluto.
    private int distanciaManhattanDesde(){
        return (int) (Math.abs(posJugador.x - posSiguienteDestino.x) + Math.abs(posJugador.y-posSiguienteDestino.y));
    }

    /** Funcion que actualiza el nodo PADRE de un nodo.
       Esto lo necesito para que, en caso de que el algoritmo tome otro camino para ir a la solucion porque la heuristica le haga recalcular
       el camino, simplemente lo actualizo actualizando el nodo padre al que apuntan estos nodos.
       @param nodoPadre nodo padre nuevo el cual quiero asignarle al nodo actual.

     */
    public void setPadreNuevo(nodoConCoste nodoPadre){

        // Le asigno el nuevo nodo padre.
        this.nodoPadre = nodoPadre;
        // Recalculo la profundidad.
        this.profundidad = nodoPadre.profundidad + 1;
        // Debo de comprobar que la orientacion sea igual a la del nodo actual ya que si es diferente, hay que recalcular g.
        recalcularOrientacion(accion, nodoPadre.orientacionJugador);
        if (nodoPadre.orientacionJugador != orientacionJugador){
            this.g = nodoPadre.g + 2;
        }
        else{
            this.g = nodoPadre.g + 1;
        }

        // Hago lo mismo con cada uno de los hijos DEL NODO ACTUAL, PARA RECALCULAR G!!!!!!
        for (nodoConCoste nodosHijo : nodosHijos) {
            nodosHijo.setPadreNuevo(this);
        }
    }

    // Método necesario para poder comparar nodos.
    // Fue necesario ya que un nodo con coste no tiene un comparador explicito.
    // Por tanto, para poder compararlos, hemos de ver si la posicion es igual.
    // Si son distintos en posicion, comparamos las g.
    // MUY NECESARIO PARA QUE EL SET DE NODOS ABIERTOS Y CERRADOS LOS ALMACENE EN FUNCION DE MENOR G, Y POR TANTO,
    // EL SIGUIENTE NODO QUE COJA DE LA LISTA DE ABIERTOS SEA EL QUE MENOR G TENGA!!!!!!
    @Override
    public int compareTo(nodoConCoste nodo) {

        if (this.posJugador.x == nodo.posJugador.x && this.posJugador.y == nodo.posJugador.y)
            return 0; // Significa que son iguales.
        else if ((this.g + this.h) > (nodo.g + nodo.h))
            return 1;
        else
            return -1;

    }

    /** Funcion que genera todos los hijos de un nodo al hacer nodo.generarHijos()
        Lo que ocurre es lo siguiente:
            Miramos si es posible generar el hijo de cada tipo (FORWARD, LEFT, RIGHT, DOWN)
            viendo si a la casilla a la que se moveria el hijo es posible moverse.
            En caso de que sea posible, lo generamos.
     */

    void generarHijos(){

        // Este if es en caso de que el nodo padre sea el primero, ya que en el resto de nodos, comprobamos
        // si la accion es la contraria a la que queremos generar.

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

        // Esta condicion se cumple para todos los nodos excepto el inicial.
        // Generamos el nodo si es posible moverse hasta alli y si la accion anterior realizada no es la contraria a la que queremos crear.
        // ES INUTIL QUE UN NODO QUE TENGA DE ACCION IZQUIERDA GENERE UNA ACCION A LA DERECHA, YA QUE SIMPLEMENTE HARIA
        // QUE EL VALOR HEURISTICO AUMENTASE PERO SIN REALIZAR NINGUN TIPO DE ACCION. INCLUSO QUEDARSE QUIETO ES MUCHO MAS RENTABLE.

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


    /** Funcion que devuelve el camino hasta el nodo inicial y lo guarda en un stack de ACCIONES (pasado como parametro).
        @param camino El Stack (pila) de acciones donde se van a guardar estas, inicialmente vacio, pero puede no estarlo.
        @return el propio Stack.
     */
    public Stack<Types.ACTIONS> getCamino(Stack<Types.ACTIONS> camino) {
        // Estoy en el nodo padre (el unico con accion nula)
        if (this.accion != null){
            camino.push(accion);
            // Antes de meter la accion en la pila, compruebo si la orientacion es la misma que la del padre.
            // Esto lo hago porque si la orientacion es distinta, tengo que meter la accion dos veces en la pila para que el agente la realice correctamente, ya que
            // si la orientacion es distinta, y quiero hacer un movimiento en otra orientacion, debo de girar y despues realizar el movimiento.
            if (orientacionJugador != nodoPadre.orientacionJugador){
                camino.push(accion);
            }
            nodoPadre.getCamino(camino);
        }

        return camino;
    }
}




