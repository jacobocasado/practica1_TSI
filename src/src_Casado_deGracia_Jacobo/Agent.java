package src_Casado_deGracia_Jacobo;

import core.game.Observation;
import core.game.StateObservation;
import core.player.AbstractPlayer;
import ontology.Types;
import tools.ElapsedCpuTimer;
import tools.Vector2d;
import java.util.ArrayList;
import java.util.Stack;
import java.util.TreeSet;
import java.util.Vector;


/** Clase Agente que implementa la coleccion de nodosConCoste creadas en el otro paquete.
   El agente es capaz de almacenar informacion de la partida util para el desarrollo de sus decisiones y los
   metodos que se han implementado son metodos que usan una coleccion de nodos.
 **/

public class Agent extends AbstractPlayer {
    // Almacenar la posicion del avatar. En cada tic del juego, si es necesario, se actualiza.
    static Vector2d avatar;
    // Almacenar el factor de escala. Util para escalar las coordenadas del Grid.
    static Vector2d fescala;
    // Almacenar la posicion del portal. Se utiliza en todos los niveles que sea necesario volver cuando se cumpla la condicion.
    static Vector2d pos_Portal;
    // Necesitamos guardar el nivel en el que estamos para, dependiendo de eso, actuar de una manera u otra.
    // Es necesario ya que implementamos el comportamiento Reactivo, Deliberativo y Mixto en un solo agente,
    // por tanto, dependiendo del nivel, este cambiara.
    static int nivel;
    // Variable que, quizas se pueda sustituir, pero debido a comodidad almaceno las gemas que el jugador va obteniendo.
    // Si llegamos a la posicion de una gema, aumenta este contador.
    static int gemasObtenidas = 0;
    // Es la pieza fundamental del agente.
    // Guardamos las acciones en una pila, de manera que, cuando calculemos el camino o incluso recalculemos,
    // borramos la pila, volvemos a meter el camino en el Stack y la siguiente accion a ejecutar por el agente
    // es un simple pop.
    static public Stack<Types.ACTIONS> caminoRecorrido = new Stack<>();

    /** Metodo que se invoca cuando el nivel es el 1.
     * Simplemente, al no tener que recalcular el camino ya que el entorno no es cambiante
     * (no hay enemigos ni nada que cambie en cada act) se le asigna a la variable del caminoRecorrido
     * el metodo de aplicar el A* desde el nodo inicial hasta el portal.
     * El camino que devuelve es el optimo.
     **/
    public void deliberativoSimple(nodoConCoste nodoInicial){
        caminoRecorrido = calculaCaminoOptimo(nodoInicial);
    }

    /** Método que devuelve la distancia Manhattan desde la posición del jugador hasta el objetivo que se haya marcado como meta.
     Es una simple resta en coordenadas x en valor absoluto sumado a una resta en coordenadas y en valor absoluto.
     @param pos1 la posicion en Vector2d del objeto1.
     @param pos2 la posicion en Vector2d del objeto2.
     @return la distancia manhattan entre ambos.
     */
    private static int distanciaManhattan(Vector2d pos1, Vector2d pos2){
        return (int) (Math.abs(pos1.x - pos2.x) + Math.abs(pos1.y-pos2.y));
    }

    /** Funcion que dada la posicion de una casilla, calcule el tipo de esta, devolviendolo en un numero.
     @param posicion posicion de la casilla la cual queremos saber el tipo.
     @param estado el estado el juego en ese momento, en variable de StateObservation.
     @return El tipo de la casilla de forma numerica.
     OJO: El tipo de la casilla es aquel que el ESTADO DEL JUEGO LE ASIGNE NUMERICAMENTE,
     ES DECIR, NO ES ALGO QUE NOSOTROS PODAMOS CONTROLAR.
     POR EJEMPLO, EL MURO ES TIPO 0.
     Eso viene en la propia codificacion del juego.
     */
    private static int obtenerTipo(Vector2d posicion, StateObservation estado){

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

    /**
     * Metodo que busca la gema mas cercana al jugador.
     * Se ha implementado de manera que el criterio para elegir la gema mas cercana es con distancia Manhattan,
     * no con distancia A Star.
     * De todas las gemas que tiene por coger (almacenadas en la variable estado) toma la que mas cerca esta en Distancia Manhattan.
     * // Todo buscar la gema mas cercana, no en distancia Manhattan, sino en A Star.
     * // Todo no solo buscar una, sino buscar dos y la que menos tenga de peligro es a la que vas.
     *
     * @param estado estado del juego. Util para ver que gemas quedan por coger.
     * @return La posicion en Vector2d de la gema mas cercana calculada usando el criterio dicho anteriormente.
     */
    public Vector2d buscarGemaMasCercana(StateObservation estado){

        ArrayList<Observation> gema = new ArrayList<Observation>(estado.getResourcesPositions()[0]);
        // Necesito escalar las gemas ya que estan sacadas de StateObs.
        ArrayList<Vector2d> gemasEscaladas = new ArrayList<>();
        // Las escalo.
        for (Observation d: gema){
            Vector2d posGema = new Vector2d();
            posGema = d.position;
            posGema.x = Math.floor(posGema.x / fescala.x);
            posGema.y = Math.floor(posGema.y / fescala.y);
            gemasEscaladas.add(posGema);
        }
        // Aplico Greedy para ver cual es la gema mas cercana. Cuando acabe de iterar, la devuelvo.
        int gemaMasCercana = 0;
        int menorDistancia = distanciaManhattan(avatar, gemasEscaladas.get(0));

        for (int i = 0; i < gemasEscaladas.size(); ++i){
            if (distanciaManhattan(avatar, gemasEscaladas.get(i)) < menorDistancia){
                menorDistancia = distanciaManhattan(avatar, gemasEscaladas.get(i));
                gemaMasCercana = i;
            }
        }
        // Devuelvo la posicion de la gema mas cercana (escalada) para aplicar un Pathing directamente.
        return gemasEscaladas.get(gemaMasCercana);

    }

    /**
     * Metodo que busca la gema mas cercana, en vez de a un jugador, a una posicion dada.
     * Ademas, la gema que encuentra la quita de la lista de gemas recibida como parametro.
     * Es totalmente igual que el metodo de arriba pero en vez de evaluar la gema mas cercana con el jugador,
     * la evalua con la posicion que recibe de parametro.
     * Se ha implementado de manera que el criterio para elegir la gema mas cercana es con distancia Manhattan,
     * no con distancia A Star.
     * De todas las gemas que tiene por coger (almacenadas en la variable estado) toma la que mas cerca esta en Distancia Manhattan.
     *
     * @param gemasEscaladas Vector que contiene las gemas que quedan por coger.
     * @return La posicion en Vector2d de la gema mas cercana calculada usando el criterio dicho anteriormente.
     * La gema se elimin del vector de gemasEscaladas
     */
    public Vector2d buscarGemaMasCercanaAPosicion(ArrayList<Vector2d> gemasEscaladas, Vector2d posicion){

        // Aplico Greedy para ver cual es la gema mas cercana. Cuando acabe de iterar, la devuelvo.
        int gemaMasCercana = 0;
        int menorDistancia = distanciaManhattan(posicion, gemasEscaladas.get(0));

        for (int i = 0; i < gemasEscaladas.size(); ++i){
            if (distanciaManhattan(posicion, gemasEscaladas.get(i)) < menorDistancia){
                menorDistancia = distanciaManhattan(posicion, gemasEscaladas.get(i));
                gemaMasCercana = i;
            }
        }
        // Devuelvo la posicion de la gema mas cercana (escalada) para aplicar un Pathing directamente.
        Vector2d gema = new Vector2d(gemasEscaladas.get(gemaMasCercana));
        gemasEscaladas.remove(gemaMasCercana);
        return gema;
    }

    /**
     * Metodo que, dada una posicion del juego y un estado, te calcula el nivel de peligro que hay en esa posicion.
     * Llamamos peligro a la distancia, bien sea Manhattan, o A Star, a los enemigos presentes en el mapa.
     * El metodo hace varias cosas:
     *  1) Escala los enemigos.
     *  2) Por cada enemigo:
     *      2.1) Calculo la distancia que hay de la posicion al enemigo.
     *      2.2) Dependiendo de la distancia, le asigno un valor de peligro que se corresponde con el valor heuristico de esa casilla.,
     *      2.3) Aclaracion: El valor de peligro se ha ido modificando para obtener un comportamiento agresivo pero tambien seguro del agente.
     * @param posicion La posicion donde queremos medir el nivel de peligro.
     * @param estado El estado el juego en ese momento, util para saber en que casillas estan los enemigos y poder calcular la distancia Manhattan o A Star.
     *     OJO: A dia de hoy, el nivel de peligro esta calculado por la distancia Manhattan.
     *          Es posible que haya actualizaciones y que se calcule de otra manera.
     * @return El nivel de peligro en la casilla dada por parametro.
     */

    static double nivelDePeligro(Vector2d posicion, StateObservation estado){
        // Si no hay enemigos, el nivel de peligro es 0 y terminamos.
        // Si no esta esta linea, en los niveles sin enemigos el programa crashea.
        if (estado.getNPCPositions() == null)
            return 0;

        // Como ya hemos visto en otros metodos, es necesario escalar los enemigos para poder
        // hallar la distancia Manhattan o A Star de manera correcta.
        double peligro = 0;
        ArrayList<Observation> enemigos = new ArrayList<Observation>(estado.getNPCPositions()[0]);
        ArrayList<Vector2d> enemigosEscalados = new ArrayList<>();


        // Escalo los enemigos.
        for (Observation d: enemigos) {
            Vector2d posEnemigo = new Vector2d();
            posEnemigo = d.position;
            posEnemigo.x = Math.floor(posEnemigo.x / fescala.x);
            posEnemigo.y = Math.floor(posEnemigo.y / fescala.y);

            // Mido la distancia a la que cada enemigo esta de mi posicion, y...
            int distancia = distanciaManhattan(posicion, posEnemigo);

            if (distancia < 6){
                nodoConCoste nodo = new nodoConCoste(avatar, estado.getAvatarOrientation(), posEnemigo, estado);
                distancia = calculaCaminoOptimoConProfundidad(nodo, 10).size();

                // Dependiendo de esa distancia, a cada casilla le asigno un nivel de peligro.
                // Las casillas mas peligrosas, obviamente, seran aquellas mas cercanas al enemigo.
                switch (distancia) {
                    case (6):
                        peligro += 1;
                        break;
                    case (5):
                        peligro += 1;
                        break;
                    case (4):
                        peligro += 2;
                        break;
                    case (3):
                        peligro += 3;
                        break;
                    case (2):
                        peligro += 4;
                        break;
                    case (1):
                        peligro += 6;
                        break;
                    case (0):
                        peligro += 9;
                        break;
                    default:
                        peligro += 0;
                }
            }

            /*ArrayList<Vector2d> casillasCercanas = posicionesADistanciaDe(2, posicion);
            for (Vector2d casilla: casillasCercanas){
                if (obtenerTipo(casilla, estado) == 0)
                    peligro =+ 0.5;
            }*/

            // peligro = Math.pow(peligro, (1.3));
        }

        return peligro;
    }

    /**
     * Metodo que calcula las posiciones, como Vectores2d, a una distancia numerica EN CASILLAS (1 casilla, 2 casillas...) de una posicion dada.
     * Es util para casi todo ya que, por ejemplo, cuando queremos ponernos a salvo, buscamos una posicion a distancia 1 que este segura.
     * Si no hay posiciones a distancia 1, buscamos posiciones de distancia 2...
     * @param distancia La distancia de busqueda a la cual queremos buscar posiciones.
     * @param posicion La posicion actual desde donde queremos buscar.
     * @return Un ArrayList de Vector2d de todas las posiciones a distancia X de la casilla DADA.
     * Posteriormente, de ese Array, se tomara, por ejemplo, la casilla a la que menos cueste ir, para ir a la casilla segura de MENOR COSTE.
     */
    public static ArrayList<Vector2d> posicionesADistanciaDe(int distancia, Vector2d posicion){

        ArrayList<Vector2d> posiciones = new ArrayList<>();

        // Unicamente lo que hago en el bucle es aniadir las casillas correspondientes a esa distancia al vector.
        // OJO, tambien aniado las casillas a distancia -1, distancia -2.... Es decir, desde 0 hasta la distancia dada.
        for(int i = 0; i <= distancia; ++i){
            posiciones.add(new Vector2d(posicion.x + i, posicion.y + distancia-i));
            posiciones.add(new Vector2d(posicion.x + (-1)*i, posicion.y + distancia-i));
            posiciones.add(new Vector2d(posicion.x + i, posicion.y + (-1)*(distancia-i)));
            posiciones.add(new Vector2d(posicion.x + (-1)*i, posicion.y + (-1)*(distancia-i)));
        }

        return posiciones;
    }

    /**
     * Metodo que, basicamente usa el metodo anterior (de calcular las posiciones a distancia x de una casilla)
     * pero que, ademas de eso, empieza buscando a partir de las posiciones de distancia 2 de tal manera que:
     *      1) Empiezo buscando en posiciones en distancia x.
     *      2) Busco las posiciones en distancia x que no haya peligro (no haya enemigos) y ademas que pueda moverme hacia ellas.
     *      3) Si he encontrado alguna, dejo de iterar sin aumentar el rango de busqueda (por ejemplo, pasar de distancia 2 a 3).
     *      4) Mientras que siga habiendo peligro en posiciones de distancia X, aumento el rango de busqueda a X + 1.
     *
     *      Por tanto, solo parara cuando haya encontrado una posicion a salvo del enemigo en el rango de busqueda minimo,
     *      y ademas de eso, selecciona LA CASILLA A LA QUE MENOS LE CUESTA IR, DE MANERA QUE SI TIENE QUE ELEGIR ENTRE VARIAS
     *      CASILLAS DE RANGO 2 POR EJEMPLO, SELECCIONA A LA QUE MENOS COSTE TIENE IR.
     *      De esa manera, no solo priorizo que el camino sea mas corto, sino que me evito que yendo al camino genere otras situaciones que quizas sean mas peligrosas.
     *
     * @param estado Estado del juego
     * @param timer El timer necesario para, en caso de que la busqueda sea demasiado larga, el algoritmo pare.
     * @return
     */
    Vector2d buscarPosicionASalvoDeEnemigo(StateObservation estado, ElapsedCpuTimer timer) {

        boolean hayPeligro = true;
        int indice;
        int distanciaBusqueda = 2;
        ArrayList<Vector2d> cercanas = new ArrayList<>();
        Vector2d posicionASalvo = new Vector2d();

        int menorDistancia = 200;

        // Mientras que hay peligro, busco las casillas cercanas a esa posicion en la distancia de busqueda pasada por parametro.
        while (hayPeligro) {
            // Guardo las posiciones en un vector.
            cercanas = posicionesADistanciaDe(distanciaBusqueda, avatar);
            for (Vector2d posicion : cercanas) {
                // Voy recorriendome estas posiciones de casillas a distancia X,
                // si la casilla es valida:
                if (((posicion.x < estado.getObservationGrid().length) && (posicion.x >= 0)) &&
                        ((posicion.y < estado.getObservationGrid()[0].length && (posicion.y >= 0)))) {
                    // Consigo el tipo de casilla al que me moveria
                    int elemento = obtenerTipo(posicion, estado);
                    // Si esta casilla no es un enemigo ni un muro y encima estoy a salvo, PERFECTO.
                    if (nivelDePeligro(posicion, estado) == 0 && elemento != 0 &&
                                                                 elemento != 10 && elemento != 11) {
                        // Dejo de iterar en la siguiente iteracion porque ya NO HAY PELIGRO.
                        // He encontrado una casilla a salvo a distancia minima.
                        hayPeligro = false;
                        // De entre todas, me quedo con la que menor distancia A Star tiene.
                        // Por tanto, calculo el camino desde mi avatar hasta la casilla segura y mido el COSTE.
                        nodoConCoste nodo = new nodoConCoste(avatar, estado.getAvatarOrientation(), posicion, estado);
                        int distancia = calculaCaminoOptimoDinamico(nodo, timer).size();
                        // Me quedo con la de menor coste.
                        if (distancia <= menorDistancia) {
                            posicionASalvo = posicion;
                        }
                    }
                }
            }
            // Si ha acabado el bucle y sigue habiendo peligro, aumento la distancia de busqueda.
            distanciaBusqueda++;
        }
        // Si ya he encontrado la casilla porque !hay peligro, entonces devuelvo la posicion de menor coste la cual me he asegurado de que estoy a salvo.
        return posicionASalvo;
    }

    /**
     * Algoritmo A Star para encontrar el camino optimo a partir de un nodo inicial.
     * Este algoritmo implementado tomando las diapositivas y material de teoria, encuentra el camino optimo generando nodos hijos a partir
     * de uno inicial y haciendo un recorrido recursivo por los nodos aplicando las funciones heuristicas correspondientes,
     * y por tanto, devolviendo en el Stack la sucesion de nodos, desde el nodo INICIAl hasta el nodo SOLUCION con un camino.
     * En el nivel 1 y 2, al no haber enemigos y ser un entorno determinista (ya que este no cambia) y conociendo el coste de hacer cada accion en su totalidad, es decir
     * teniendo una HEURISTICA ADMISIBLE, es capaz de calcular el camino mas optimo ya que el algoritmo A Star, cuando H es admisible, calcula la solucion mas optima en caso de que haya solucion.
     * ya que A Star es admisible bajo una heuristica admisible.
     *
     * En el codigo de la funcion calculaCaminoOptimo se comentara el proceso de desarrollo del algoritmo A Star.
     * @param nodoInicial El nodo inicial a partir del cual queremos generar el camino.
     * @return Un Stack de ACTIONS el cual me dara el proceso de acciones que debe de seguir el personaje para llegar al camino de manera optima.
     *
     * Si hay enemigos en el camino, este metodo se llamara en cuanto nuestro agente se vea en peligro para recalcular el camino hacia una zona segura,
     * mientras que, si estoy a salvo de los enemigos o directamente no hay enemigos (en el caso del modelo deliberativo) generara el camino hacia una gema de manera optima.
     * */
    public static Stack<Types.ACTIONS> calculaCaminoOptimo(nodoConCoste nodoInicial){

        // Creamos el Stack de acciones que vamos a devolver.
        Stack<Types.ACTIONS> caminoRecorrido = new Stack<>();

        // Genero los contenedores, abiertos y cerrados para los nodos.
        /*
            Almacenamos los nodos en un set ya que, en el paquete del nodoConCoste hemos sobrecargado el operador de comparacion (metodo compareTo)
            de esta manera, cuando insertemos los nodos en el Set, este los va ordenar de manera que el primer nodo al que se accede en el set sea el de menor g.
            Por tanto, iremos sacando nodos y moviendolos de un Set a otro, y, ademas de ello, el acceso a estos es rapido, ya que abiertos.get(0) nos coge el nodo de menor g
            para asegurarnos de que vamos expandiendo por el camino de menor coste.
            */
        TreeSet<nodoConCoste> abiertos = new TreeSet<nodoConCoste>();
        TreeSet<nodoConCoste> cerrados = new TreeSet<nodoConCoste>();

        // Aniadimos el nodo inicial a abiertos.
        abiertos.add(nodoInicial);

        // Mientras que no se haya alcanzado la posicion destino,
        // Sacamos el primer nodo.
        nodoConCoste nodoActual = abiertos.pollFirst();
        while (nodoActual.posJugador.x != nodoActual.posSiguienteDestino.x || nodoActual.posJugador.y != nodoActual.posSiguienteDestino.y){
            // Generamos los hijos del nodo el cual estamos evaluando.
            nodoActual.generarHijos();
            for (nodoConCoste nodoI: nodoActual.nodosHijos){
                // Por cada uno de los nodos hijos del nodo que estamos expandiendo, vemos si esta o no en abiertos.
                // Si esta en abiertos, y ese nodo
                if (abiertos.contains(nodoI)){
                    nodoConCoste aux = abiertos.ceiling(nodoI);
                    if ((aux.g + aux.h) > (nodoI.g + nodoI.h))
                        aux.setPadreNuevo(nodoI.nodoPadre);
                }
                // Si por el contrario el nodo a evaluar esta en cerrados, hacemos exactamente lo mismo que el anterior.
                // Separamos ambos en un IF y en un ELSE IF ya que el ceiling del nodo estara o bien en abiertos, o bien en cerrados.
                // Como el algoritmo no sabe donde esta el nodo exactamente, lo separamos en una comprobacion u otra.
                else if (cerrados.contains(nodoI)){
                    nodoConCoste aux = cerrados.ceiling(nodoI);
                    if ((aux.g + aux.h) > (nodoI.g + nodoI.h))
                        aux.setPadreNuevo(nodoI.nodoPadre);
                }
                // Si por el contrario el nodo no esta ni en abiertos ni en cerrados, lo insertamos en la lista de abiertos,
                // para posteriormente actualizarlo y evaluarlo.
                else
                    abiertos.add(nodoI);
            }
            // Cuando hayamos terminado de generar ese nodo y sus hijos y evaluarlos, insertamos ese nodo en cerrados.
            cerrados.add(nodoActual);
            // Seleccionamos el nodo primero de abiertos, por la logica que he dicho anteriormente de que el primer nodo de abiertos sera el mejor a expandir.
            nodoActual = abiertos.pollFirst();
        }
        // Cuando salgamos de este bucle, hemos obtenido un nodo solucion ya que la posicion de ese nodo es la posicion a buscar.
        // Por tanto, llamamos a getCamino que hace que, a partir de un nodo solucion, devuelva el camino total hasta el inicio.
        // Esto lo conseguimos ya que la estructura que hemos creado es a partir de nodos que apuntan a sus nodos padres, por tanto,
        // consiguiendo el nodo solucion y haciendo un recorrido recursivo hacia atras, encontramos el camino.

        // Al insertarlo en un Stack y de final a principio, el primer nodo que se quedara en el tope de la pila sera el nodo INICIAL,
        // proseguido del nodo segundo, luego del tercero...
        // por tanto, es una buena estructura a usar para sacar los nodos ya que haciendo camino.pop() vamos obteniendo la secuencia de acciones.
        nodoActual.getCamino(caminoRecorrido);

        // Devolvemos el camino en un Stack de ACTIONS.
        return caminoRecorrido;
    }

    /**
     *
     * ** MODIFICACION DEL ALGORITMO calculaCaminoOptimo (encima de este metodo) que recibe un timer y, en caso de que haya poco tiempo restante, deja de explorar el arbol.
     *     Util para el nivel 5.
     * */

    public static Stack<Types.ACTIONS> calculaCaminoOptimoDinamico(nodoConCoste nodoInicial, ElapsedCpuTimer timer){

        // Creamos el Stack de acciones que vamos a devolver.
        Stack<Types.ACTIONS> caminoRecorrido = new Stack<>();

        // Genero los contenedores, abiertos y cerrados para los nodos.
        /*
            Almacenamos los nodos en un set ya que, en el paquete del nodoConCoste hemos sobrecargado el operador de comparacion (metodo compareTo)
            de esta manera, cuando insertemos los nodos en el Set, este los va ordenar de manera que el primer nodo al que se accede en el set sea el de menor g.
            Por tanto, iremos sacando nodos y moviendolos de un Set a otro, y, ademas de ello, el acceso a estos es rapido, ya que abiertos.get(0) nos coge el nodo de menor g
            para asegurarnos de que vamos expandiendo por el camino de menor coste.
            */
        TreeSet<nodoConCoste> abiertos = new TreeSet<nodoConCoste>();
        TreeSet<nodoConCoste> cerrados = new TreeSet<nodoConCoste>();

        // Aniadimos el nodo inicial a abiertos.
        abiertos.add(nodoInicial);

        // Mientras que no se haya alcanzado la posicion destino,
        // Sacamos el primer nodo.
        nodoConCoste nodoActual = abiertos.pollFirst();
        while ((nodoActual.posJugador.x != nodoActual.posSiguienteDestino.x || nodoActual.posJugador.y != nodoActual.posSiguienteDestino.y) &&
        timer.remainingTimeMillis() > 5){
            // Generamos los hijos del nodo el cual estamos evaluando.
            nodoActual.generarHijos();
            for (nodoConCoste nodoI: nodoActual.nodosHijos){
                // Por cada uno de los nodos hijos del nodo que estamos expandiendo, vemos si esta o no en abiertos.
                // Si esta en abiertos, y ese nodo
                if (abiertos.contains(nodoI)){
                    nodoConCoste aux = abiertos.ceiling(nodoI);
                    if ((aux.g + aux.h) > (nodoI.g + nodoI.h))
                        aux.setPadreNuevo(nodoI.nodoPadre);
                }
                // Si por el contrario el nodo a evaluar esta en cerrados, hacemos exactamente lo mismo que el anterior.
                // Separamos ambos en un IF y en un ELSE IF ya que el ceiling del nodo estara o bien en abiertos, o bien en cerrados.
                // Como el algoritmo no sabe donde esta el nodo exactamente, lo separamos en una comprobacion u otra.
                else if (cerrados.contains(nodoI)){
                    nodoConCoste aux = cerrados.ceiling(nodoI);
                    if ((aux.g + aux.h) > (nodoI.g + nodoI.h))
                        aux.setPadreNuevo(nodoI.nodoPadre);
                }
                // Si por el contrario el nodo no esta ni en abiertos ni en cerrados, lo insertamos en la lista de abiertos,
                // para posteriormente actualizarlo y evaluarlo.
                else
                    abiertos.add(nodoI);
            }
            // Cuando hayamos terminado de generar ese nodo y sus hijos y evaluarlos, insertamos ese nodo en cerrados.
            cerrados.add(nodoActual);
            // Seleccionamos el nodo primero de abiertos, por la logica que he dicho anteriormente de que el primer nodo de abiertos sera el mejor a expandir.
            nodoActual = abiertos.pollFirst();
        }
        // Cuando salgamos de este bucle, hemos obtenido un nodo solucion ya que la posicion de ese nodo es la posicion a buscar.
        // Por tanto, llamamos a getCamino que hace que, a partir de un nodo solucion, devuelva el camino total hasta el inicio.
        // Esto lo conseguimos ya que la estructura que hemos creado es a partir de nodos que apuntan a sus nodos padres, por tanto,
        // consiguiendo el nodo solucion y haciendo un recorrido recursivo hacia atras, encontramos el camino.

        // Al insertarlo en un Stack y de final a principio, el primer nodo que se quedara en el tope de la pila sera el nodo INICIAL,
        // proseguido del nodo segundo, luego del tercero...
        // por tanto, es una buena estructura a usar para sacar los nodos ya que haciendo camino.pop() vamos obteniendo la secuencia de acciones.
        nodoActual.getCamino(caminoRecorrido);

        // Devolvemos el camino en un Stack de ACTIONS.
        return caminoRecorrido;
    }

    /**
     * MODIFICACION DEL ALGORITMO ANTERIOR A STAR, EN ESTE CASO, ES EXACTAMENTE LO MISMO PERO TIENE EN CUENTA LA PROFUNDIDAD DE BUSQUEDA.
     * ESTA COTA LA USAREMOS PARA QUE NUESTRO AGENTE BUSQUE EL CAMINO OPTIMO DADA UNA PROFUNDIDAD, LO CUAL NOS VIENE BIEN EN TERMINOS
     * DE TIEMPO Y DE EFICIENCIA PARA EL ULTIMO NIVEL.
     * EN EL NIVEL 1 Y 2 USAREMOS EL OTRO A STAR YA QUE SI QUEREMOS EL CAMINO OPTIMO.
     * EN ESTE, NOS DEVOLVERA EL CAMINO OPTIMO DE LA PROFUNDIDAD QUE LE INDIQUEMOS.
     * POR TANTO, SE USARA EN EL NIVEL 4.
     * Algoritmo A Star con profundidad, para encontrar el camino optimo a partir de un nodo inicial, dada una profundidad.
     * Este algoritmo implementado tomando las diapositivas y material de teoria, encuentra el camino optimo generando nodos hijos a partir
     * de uno inicial y haciendo un recorrido recursivo por los nodos aplicando las funciones heuristicas correspondientes,
     * y por tanto, devolviendo en el Stack la sucesion de nodos, desde el nodo INICIAl hasta el nodo SOLUCION con un camino.
     * En el nivel 1 y 2, al no haber enemigos y ser un entorno determinista (ya que este no cambia) y conociendo el coste de hacer cada accion en su totalidad, es decir
     * teniendo una HEURISTICA ADMISIBLE, es capaz de calcular el camino mas optimo ya que el algoritmo A Star, cuando H es admisible, calcula la solucion mas optima en caso de que haya solucion.
     * ya que A Star es admisible bajo una heuristica admisible.
     *
     * En el codigo de la funcion calculaCaminoOptimo se comentara el proceso de desarrollo del algoritmo A Star.
     * @param nodoInicial El nodo inicial a partir del cual queremos generar el camino.
     * @return Un Stack de ACTIONS el cual me dara el proceso de acciones que debe de seguir el personaje para llegar al camino de manera optima.
     *
     * Si hay enemigos en el camino, este metodo se llamara en cuanto nuestro agente se vea en peligro para recalcular el camino hacia una zona segura,
     * mientras que, si estoy a salvo de los enemigos o directamente no hay enemigos (en el caso del modelo deliberativo) generara el camino hacia una gema de manera optima.
     * */

    public static Stack<Types.ACTIONS> calculaCaminoOptimoConProfundidad(nodoConCoste nodoInicial, int profundidadMaxima){

        // Creamos el Stack de acciones que vamos a devolver.
        Stack<Types.ACTIONS> caminoRecorrido = new Stack<>();

        // Genero los contenedores, abiertos y cerrados para los nodos.
        /*
            Almacenamos los nodos en un set ya que, en el paquete del nodoConCoste hemos sobrecargado el operador de comparacion (metodo compareTo)
            de esta manera, cuando insertemos los nodos en el Set, este los va ordenar de manera que el primer nodo al que se accede en el set sea el de menor g.
            Por tanto, iremos sacando nodos y moviendolos de un Set a otro, y, ademas de ello, el acceso a estos es rapido, ya que abiertos.get(0) nos coge el nodo de menor g
            para asegurarnos de que vamos expandiendo por el camino de menor coste.
            */
        TreeSet<nodoConCoste> abiertos = new TreeSet<nodoConCoste>();
        TreeSet<nodoConCoste> cerrados = new TreeSet<nodoConCoste>();

        // Aniadimos el nodo inicial a abiertos.
        abiertos.add(nodoInicial);

        // Mientras que no se haya alcanzado la posicion destino,
        // Sacamos el primer nodo.
        nodoConCoste nodoActual = abiertos.pollFirst();
        while (nodoActual.posJugador.x != nodoActual.posSiguienteDestino.x || nodoActual.posJugador.y != nodoActual.posSiguienteDestino.y && nodoActual.profundidad <= profundidadMaxima){
            // Generamos los hijos del nodo el cual estamos evaluando.
            nodoActual.generarHijos();
            for (nodoConCoste nodoI: nodoActual.nodosHijos){
                // Por cada uno de los nodos hijos del nodo que estamos expandiendo, vemos si esta o no en abiertos.
                // Si esta en abiertos, y ese nodo
                if (abiertos.contains(nodoI)){
                    nodoConCoste aux = abiertos.ceiling(nodoI);
                    if ((nodoI.g + nodoI.h) < (aux.g + aux.h))
                        aux.setPadreNuevo(nodoI.nodoPadre);
                }
                // Si por el contrario el nodo a evaluar esta en cerrados, hacemos exactamente lo mismo que el anterior.
                // Separamos ambos en un IF y en un ELSE IF ya que el ceiling del nodo estara o bien en abiertos, o bien en cerrados.
                // Como el algoritmo no sabe donde esta el nodo exactamente, lo separamos en una comprobacion u otra.
                else if (cerrados.contains(nodoI)){
                    nodoConCoste aux = cerrados.ceiling(nodoI);
                    if ((nodoI.g + nodoI.h) < (aux.g + aux.h))
                        aux.setPadreNuevo(nodoI.nodoPadre);
                }
                // Si por el contrario el nodo no esta ni en abiertos ni en cerrados, lo insertamos en la lista de abiertos,
                // para posteriormente actualizarlo y evaluarlo.
                else
                    abiertos.add(nodoI);
            }
            // Cuando hayamos terminado de generar ese nodo y sus hijos y evaluarlos, insertamos ese nodo en cerrados.
            cerrados.add(nodoActual);
            // Seleccionamos el nodo primero de abiertos, por la logica que he dicho anteriormente de que el primer nodo de abiertos sera el mejor a expandir.
            nodoActual = abiertos.pollFirst();
        }
        // Cuando salgamos de este bucle, hemos obtenido un nodo solucion ya que la posicion de ese nodo es la posicion a buscar.
        // Por tanto, llamamos a getCamino que hace que, a partir de un nodo solucion, devuelva el camino total hasta el inicio.
        // Esto lo conseguimos ya que la estructura que hemos creado es a partir de nodos que apuntan a sus nodos padres, por tanto,
        // consiguiendo el nodo solucion y haciendo un recorrido recursivo hacia atras, encontramos el camino.

        // Al insertarlo en un Stack y de final a principio, el primer nodo que se quedara en el tope de la pila sera el nodo INICIAL,
        // proseguido del nodo segundo, luego del tercero...
        // por tanto, es una buena estructura a usar para sacar los nodos ya que haciendo camino.pop() vamos obteniendo la secuencia de acciones.
        nodoActual.getCamino(caminoRecorrido);

        // Devolvemos el camino en un Stack de ACTIONS.
        return caminoRecorrido;
    }

    /**
     * Constructor de nuestro AGENTE.
     * Al ser uno solo, este tiene que detectar, usando la variable stateObs, si hay o no enemigos, y si hay o no objetos que coger.
     * De esa manera, dependiendo de su entorno, define su comportamiento: Deliberativo (Nivel 1 y 2), Reactivo (Nivel 3 y 4) o Reactivo/Deliberativo (Nivel 4).
     * Lo unico que ocurre en este constructor es que recalcula algunas variables utiles como la posicion del portal, que no varia en todo el juego, el factor de escala y
     * la posicion del avatar en el momento de inicio del juego (util porque el agente deliberativo puro no tiene que volver a recalcularla y solo traza el camino).
     * @param stateObs El estado del juego en el momento inicial de este.
     * @param elapsedTimer Un temporizador.
     */

    public Agent (StateObservation stateObs, ElapsedCpuTimer elapsedTimer){

        // 1. Calculamos el factor de escala entre mundos (pixeles -> grid)
        fescala = new Vector2d(stateObs.getWorldDimension().width / stateObs.getObservationGrid().length ,
                stateObs.getWorldDimension().height / stateObs.getObservationGrid()[0].length);

        // 2. Calculamos la posicion del portal.
        pos_Portal = stateObs.getPortalsPositions()[0].get(0).position;
        pos_Portal.x = Math.floor(pos_Portal.x / fescala.x);
        pos_Portal.y = Math.floor(pos_Portal.y / fescala.y);

        // Calculamos la posición del agente al principio del problema.
        // La volveremos a actualizar en cada iteracion si el problema es reactivo.
        avatar =  new Vector2d(stateObs.getAvatarPosition().x / fescala.x,
                stateObs.getAvatarPosition().y / fescala.y);

        /**
         * Dependiendo de si hay o no recursos y de si hay o no enemigos, almaceno en la variable de clase 'nivel'
         * el nivel en el que estoy; de esa manera, en el ACT, puedo hacer una cosa u otra ya que mi comportamiento
         * sera diferente dependiendo del nivel.
         */

        // AGENTE NIVEL 1. NO HAY GEMAS NI ENEMIGOS. ADEMÁS DE ESO, COMO EL ENTORNO NO VARÍA, PODEMOS LANZARLO DESDE AQUI.
        if (stateObs.getResourcesPositions() == null && stateObs.getNPCPositions() == null){
            nivel = 1; // Nivel 1.
            // Puedo lanzar el pathing desde aqui ya que va a ser fijo.
            nodoConCoste nodoPadre = new nodoConCoste(avatar, stateObs.getAvatarOrientation(), pos_Portal, stateObs);
            deliberativoSimple(nodoPadre);
        }

        // AGENTE NIVEL 2. NO HAY ENEMIGOS, PERO HAY UNA LISTA DE GEMAS POR COGER ANTES DE LLEGAR AL PORTAL.
        else if (stateObs.getResourcesPositions() != null && stateObs.getNPCPositions() == null) {
            nivel = 2; // Nivel 2.
            /*Vector2d posicionActual =  new Vector2d(stateObs.getAvatarPosition().x / fescala.x,
                    stateObs.getAvatarPosition().y / fescala.y);
            ArrayList<Observation> gema = new ArrayList<Observation>(stateObs.getResourcesPositions()[0]);
            // Necesito escalar las gemas ya que estan sacadas de StateObs.
            ArrayList<Vector2d> gemasEscaladas = new ArrayList<>();
            // Las escalo.
            for (Observation d: gema){
                Vector2d posGema = new Vector2d();
                posGema = d.position;
                posGema.x = Math.floor(posGema.x / fescala.x);
                posGema.y = Math.floor(posGema.y / fescala.y);
                gemasEscaladas.add(posGema);
            }

            Vector2d posicion_diamante;
            nodoConCoste buscaDiamante = new nodoConCoste(posicionActual, stateObs.getAvatarOrientation(), posicionActual, stateObs);

            while (gemasObtenidas < 9){
                posicion_diamante = buscarGemaMasCercanaAPosicion(gemasEscaladas, posicionActual);
                buscaDiamante.setDestino(posicion_diamante);
                buscaDiamante = calculaCaminoOptimoNodo(buscaDiamante);
                posicionActual = buscaDiamante.posJugador;
                gemasObtenidas++;
            }

            buscaDiamante.setDestino(pos_Portal);
            buscaDiamante = calculaCaminoOptimoNodo(buscaDiamante);
            buscaDiamante.getCamino(caminoRecorrido);*/
        }

        // AGENTE NIVEL 3 Y 4. HAY ENEMIGOS PERO NO HAY GEMAS. REACTIVO.
        else if (stateObs.getResourcesPositions() == null && stateObs.getNPCPositions() != null){
            nivel = 4; // Nivel 3 y 4.
        }

        // AGENTE NIVEL 5. REACTIVO DELIBERATIVO. HAY GEMAS Y ENEMIGOS.
        else
            nivel = 5;
    }


    /**
     * Metodo ACT que se ejecuta en cada tic del juego.
     * Como en el constructor del agente hemos definido que la variable nivel tome un valor dependiendo de en que nivel estemos,
     * en este metodo se comprueba el valor de esta variable y dependiendo de eso el agente realizara acciones diferentes.
     *
     * Dentro de cada case del propio switch se procedera a comentar lo que el agente realiza y por que lo hace.
     *
     */

    @Override
    public Types.ACTIONS act(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {

        switch(nivel){
            case 1:
                /**
                 * Como en el nivel 1 (agente deliberativo) el camino se puede calcular en el propio constructor, aqui
                 * se realizan pops de la pila hasta que el agente llegue al portal.
                 */
                return caminoRecorrido.pop();

            case 2:
                /*
                    En este caso, el agente tambien es deliberativo pero he optado por realizar una ruta por cada vez que
                    lleguemos a una gema nueva.
                    Cada vez que el camino se vacie (es decir, en el primer act o cuando hayamos llegado a una gema),
                    el agente recalcula su posicion y, si ha cogido las 9 gemas, se vuelve al portal;
                    en otro caso, busca el siguiente diamante y realiza un Pathing hasta ese camino.

                    Siempre hace un return del tope de la pila, ya que o bien va hacia una gema, o bien va hacia la salida.
                 */
                if (caminoRecorrido.isEmpty()){

                    avatar =  new Vector2d(stateObs.getAvatarPosition().x / fescala.x,
                            stateObs.getAvatarPosition().y / fescala.y);

                    if (gemasObtenidas == 9){
                        nodoConCoste caminoHastaSalida = new nodoConCoste(avatar, stateObs.getAvatarOrientation(), pos_Portal, stateObs);
                        caminoRecorrido = calculaCaminoOptimo(caminoHastaSalida);
                    }

                    else{
                        Vector2d posicion_diamante = buscarGemaMasCercana(stateObs);
                        nodoConCoste buscaDiamante = new nodoConCoste(avatar, stateObs.getAvatarOrientation(), posicion_diamante, stateObs);
                        caminoRecorrido = calculaCaminoOptimo(buscaDiamante);
                        gemasObtenidas ++;
                    }

                }
                return caminoRecorrido.pop();

                /*
                    En este case se aplica el comportamiento necesario para  los niveles reactivos, tanto el de un
                    solo enemigo como el de dos enemigos.
                    El personaje, en cada act, recalcula su posicion y evalua su nivel de peligro.
                    Si se siente en peligro, busca una posicion a salvo y hace un Pathing hacia esa posicion.
                    En otro caso, si no se siente en peligro, simplemente devuelve un ACTION_NIL (se queda en el sitio).
                 */
            case 4:
                avatar =  new Vector2d(stateObs.getAvatarPosition().x / fescala.x,
                        stateObs.getAvatarPosition().y / fescala.y);

               if (nivelDePeligro(avatar, stateObs) > 0){
                    Vector2d posicionASalvo = buscarPosicionASalvoDeEnemigo(stateObs, elapsedTimer);
                    nodoConCoste irASalvo = new nodoConCoste(avatar, stateObs.getAvatarOrientation(), posicionASalvo, stateObs);
                    caminoRecorrido = calculaCaminoOptimo(irASalvo);
                    return caminoRecorrido.pop();
                }

                else if (nivelDePeligro(avatar, stateObs) == 0)
                    return Types.ACTIONS.ACTION_NIL;

                break;

            case 5:
                /*
                    Este es el comportamiento necesario para implementar el agente reactivo/deliberativo.
                    El agente comienza, en cada iteracion del act, calculando su posicion.
                    Si quedan gemas por coger, su objetivo es la gema mas cercana (dist. Manhattan).
                    En otro caso, su objetivo es el portal.

                    A continuacion, si el camino recorrido esta vacio o bien esta en peligro,
                    busca una posicion a salvo del enemigo y realiza un pathing hacia ella hasta que no se encuentre en peligro,
                    momento en el que ira a por la siguiente gema.

                    Por tanto, alternamos entre momentos en los que nos interesa esquivar al enemigo y otros en los que vamos directos
                    a por la gema, dependiendo del momento y el estado del juego.

                 */

                avatar =  new Vector2d(stateObs.getAvatarPosition().x / fescala.x,
                        stateObs.getAvatarPosition().y / fescala.y);

                Vector2d objetivo;
                if(stateObs.getAvatarResources().isEmpty() || stateObs.getAvatarResources().get(6) < 9){
                    // Calculamos la posición de la gema más cercana al avatar
                    objetivo = buscarGemaMasCercana(stateObs);
                }
                else
                    objetivo = pos_Portal;

                if(caminoRecorrido.isEmpty() || nivelDePeligro(avatar, stateObs) > 0){
                    if (nivelDePeligro(avatar, stateObs) > 0){
                        objetivo = buscarPosicionASalvoDeEnemigo(stateObs, elapsedTimer);
                    }

                    nodoConCoste nodoInicial = new nodoConCoste(avatar, stateObs.getAvatarOrientation(), objetivo, stateObs);
                    caminoRecorrido = calculaCaminoOptimoDinamico(nodoInicial, elapsedTimer);
                }

                if (!caminoRecorrido.isEmpty())
                    return caminoRecorrido.pop();
                else
                    return Types.ACTIONS.ACTION_NIL;

        }
        return null;

    }

}
