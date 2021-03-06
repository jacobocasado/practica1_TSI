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



public class Agent extends AbstractPlayer {
    static Vector2d avatar;
    static Vector2d fescala;
    static Vector2d pos_Portal;
    int nivel;
    int gemasObtenidas = 0;
    public Stack<Types.ACTIONS> caminoRecorrido =  new Stack<Types.ACTIONS>();

    public void deliberativoSimple(nodoConCoste nodoInicial){
        caminoRecorrido = calculaCaminoOptimo(nodoInicial);
    }

    private static int distanciaManhattan(Vector2d pos1, Vector2d pos2){
        return (int) (Math.abs(pos1.x - pos2.x) + Math.abs(pos1.y-pos2.y));
    }

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

    boolean hayMuro(Vector2d posicion, StateObservation estado){
        return (obtenerTipo(posicion, estado) == 0);
    }

    public Vector2d buscarGemaMasCercana(StateObservation estado){

        ArrayList<Observation> gema = new ArrayList<Observation>(estado.getResourcesPositions()[0]);
        ArrayList<Vector2d> gemasEscaladas = new ArrayList<>();

        for (Observation d: gema){
            Vector2d posGema = new Vector2d();
            posGema = d.position;
            posGema.x = Math.floor(posGema.x / fescala.x);
            posGema.y = Math.floor(posGema.y / fescala.y);
            gemasEscaladas.add(posGema);
        }

        int gemaMasCercana = 0;
        int menorDistancia = distanciaManhattan(avatar, gemasEscaladas.get(0));

        for (int i = 0; i < gemasEscaladas.size(); ++i){
            if (distanciaManhattan(avatar, gemasEscaladas.get(i)) < menorDistancia){
                menorDistancia = distanciaManhattan(avatar, gemasEscaladas.get(i));
                gemaMasCercana = i;
            }
        }

        return gemasEscaladas.get(gemaMasCercana);

    }

    static int nivelDePeligro(Vector2d posicion, StateObservation estado){
        if (estado.getNPCPositions() == null)
            return 0;

        int peligro = 0;
        ArrayList<Observation> enemigos = new ArrayList<Observation>(estado.getNPCPositions()[0]);
        ArrayList<Vector2d> enemigosEscalados = new ArrayList<>();

        for (Observation d: enemigos) {
            Vector2d posEnemigo = new Vector2d();
            posEnemigo = d.position;
            posEnemigo.x = Math.floor(posEnemigo.x / fescala.x);
            posEnemigo.y = Math.floor(posEnemigo.y / fescala.y);

            int distancia = distanciaManhattan(posicion, posEnemigo);

            switch (distancia) {
                case (5):
                    peligro += 4;
                    break;
                case (4):
                    peligro += 5;
                    break;
                case (3):
                    peligro += 6;
                    break;
                case (2):
                    peligro += 7;
                    break;
                case (1):
                    peligro += 8;
                    break;
                default:
                    peligro += 0;
            }

            ArrayList<Vector2d> casillasCercanas = posicionesADistanciaDe(1, posicion);
            for (Vector2d casilla: casillasCercanas){
                if (obtenerTipo(casilla, estado) == 0)
                    peligro++;
            }

        }

        return peligro;
    }


    public ArrayList<Vector2d> posicionesADistancia(int distancia){
        ArrayList<Vector2d> posiciones = new ArrayList<>();

        for(int i = 0; i <= distancia; ++i){
            posiciones.add(new Vector2d(avatar.x + i, avatar.y + distancia-i));
            posiciones.add(new Vector2d(avatar.x + (-1)*i, avatar.y + distancia-i));
            posiciones.add(new Vector2d(avatar.x + i, avatar.y + (-1)*(distancia-i)));
            posiciones.add(new Vector2d(avatar.x + (-1)*i, avatar.y + (-1)*(distancia-i)));
        }

        return posiciones;
    }

    public static ArrayList<Vector2d> posicionesADistanciaDe(int distancia, Vector2d posicion){
        ArrayList<Vector2d> posiciones = new ArrayList<>();

        for(int i = 0; i <= distancia; ++i){
            posiciones.add(new Vector2d(posicion.x + i, posicion.y + distancia-i));
            posiciones.add(new Vector2d(posicion.x + (-1)*i, posicion.y + distancia-i));
            posiciones.add(new Vector2d(posicion.x + i, posicion.y + (-1)*(distancia-i)));
            posiciones.add(new Vector2d(posicion.x + (-1)*i, posicion.y + (-1)*(distancia-i)));
        }

        return posiciones;
    }

    Vector2d buscarPosicionASalvoDeEnemigo(StateObservation estado){

        ArrayList<Observation> enemigos = new ArrayList<Observation>(estado.getNPCPositions()[0]);
        ArrayList<Vector2d> enemigosEscalados = new ArrayList<>();

        for (Observation d: enemigos){
            Vector2d posEnemigo = new Vector2d();
            posEnemigo = d.position;
            posEnemigo.x = Math.floor(posEnemigo.x / fescala.x);
            posEnemigo.y = Math.floor(posEnemigo.y / fescala.y);
            enemigosEscalados.add(posEnemigo);
        }


        boolean hayPeligro = true;
        int indice = 0;
        int distanciaBusqueda = 1;
        ArrayList<Vector2d> cercanas = new ArrayList<>();
        Vector2d posicionASalvo = new Vector2d();

        while (hayPeligro){
            cercanas = posicionesADistancia(distanciaBusqueda);
            indice = 0;
            while (hayPeligro && indice < cercanas.size()){
                posicionASalvo = (cercanas.get(indice));
                if(((posicionASalvo.x < estado.getObservationGrid().length) && (posicionASalvo.x >= 0)) &&
                        ((posicionASalvo.y < estado.getObservationGrid()[0].length && (posicionASalvo.y >= 0)))){
                    int elemento = obtenerTipo(posicionASalvo, estado);
                    if(nivelDePeligro(posicionASalvo, estado) == 0 && elemento != 0 &&
                            elemento != 10 && elemento != 11)
                        hayPeligro = false;
                }
                indice++;
            }
            distanciaBusqueda++;
        }
        return posicionASalvo;
    }


    public Stack<Types.ACTIONS> calculaCaminoOptimo(nodoConCoste nodoInicial){

        Stack<Types.ACTIONS> caminoRecorrido = new Stack<>();

        // Genero los contenedores, abiertos y cerrados para los nodos.
        TreeSet<nodoConCoste> abiertos = new TreeSet<nodoConCoste>();
        TreeSet<nodoConCoste> cerrados = new TreeSet<nodoConCoste>();

        // Aniadimos el nodo inicial a abiertos.
        abiertos.add(nodoInicial);

        // Mientras que no se haya alcanzado la posicion destino,
        // Sacamos el primer nodo.

        nodoConCoste nodoActual = abiertos.pollFirst();
        while (nodoActual.posJugador.x != nodoActual.posSiguienteDestino.x || nodoActual.posJugador.y != nodoActual.posSiguienteDestino.y){
            // Generamos los hijos del nodo.
            nodoActual.generarHijos();
            for (nodoConCoste nodoI: nodoActual.nodosHijos){
                if (abiertos.contains(nodoI)){
                    nodoConCoste aux = abiertos.ceiling(nodoI);
                    if ((nodoI.g + nodoI.h) < (aux.g + aux.h))
                        aux.setPadreNuevo(nodoI);
                }
                else if (cerrados.contains(nodoI)){
                    nodoConCoste aux = cerrados.ceiling(nodoI);
                    if ((nodoI.g + nodoI.h) < (aux.g + aux.h))
                        aux.setPadreNuevo(nodoI);
                }
                else
                    abiertos.add(nodoI);
            }
            cerrados.add(nodoActual);
            nodoActual = abiertos.pollFirst();
        }

        nodoActual.getCamino(caminoRecorrido);
        return caminoRecorrido;
    }

    public Agent (StateObservation stateObs, ElapsedCpuTimer elapsedTimer){

        // 1. Calculamos el factor de escala entre mundos (pixeles -> grid)
        fescala = new Vector2d(stateObs.getWorldDimension().width / stateObs.getObservationGrid().length ,
                stateObs.getWorldDimension().height / stateObs.getObservationGrid()[0].length);

        // 2. Calculamos la posicion del portal.
        pos_Portal = stateObs.getPortalsPositions()[0].get(0).position;
        pos_Portal.x = Math.floor(pos_Portal.x / fescala.x);
        pos_Portal.y = Math.floor(pos_Portal.y / fescala.y);

        // Calculamos la posici??n del agente al principio del problema.
        avatar =  new Vector2d(stateObs.getAvatarPosition().x / fescala.x,
                stateObs.getAvatarPosition().y / fescala.y);


        // AGENTE NIVEL 1. NO HAY GEMAS NI ENEMIGOS. ADEM??S DE ESO, COMO EL ENTORNO NO VAR??A, PODEMOS LANZARLO DESDE AQUI.
        if (stateObs.getResourcesPositions() == null && stateObs.getNPCPositions() == null){
            nivel = 1; // Nivel 1.
            nodoConCoste nodoPadre = new nodoConCoste(avatar, stateObs.getAvatarOrientation(), pos_Portal, stateObs);
            deliberativoSimple(nodoPadre);
        }

        // AGENTE NIVEL 2. NO HAY ENEMIGOS, PERO HAY UNA LISTA DE GEMAS POR COGER ANTES DE LLEGAR AL PORTAL.
        else if (stateObs.getResourcesPositions() != null && stateObs.getNPCPositions() == null){
            nivel = 2; // Nivel 2.
        }

        else if (stateObs.getResourcesPositions() == null && stateObs.getNPCPositions() != null){
            nivel = 4; // Nivel 3 y 4.
        }

        else
            nivel = 5;

    }

    /**
     * return the best action to arrive faster to the closest portal
     * @param stateObs Observation of the current state.
     * @param elapsedTimer Timer when the action returned is due.
     * @return best	ACTION to arrive faster to the closest portal
     */

    @Override
    public Types.ACTIONS act(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {

        switch(nivel){
            case 1:
                return caminoRecorrido.pop();

            case 2:
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


            case 4:
                avatar =  new Vector2d(stateObs.getAvatarPosition().x / fescala.x,
                        stateObs.getAvatarPosition().y / fescala.y);

               if (nivelDePeligro(avatar, stateObs) > 0){
                    Vector2d posicionASalvo = buscarPosicionASalvoDeEnemigo(stateObs);
                    nodoConCoste irASalvo = new nodoConCoste(avatar, stateObs.getAvatarOrientation(), posicionASalvo, stateObs);
                    caminoRecorrido = calculaCaminoOptimo(irASalvo);
                    return caminoRecorrido.pop();
                }

                else if (nivelDePeligro(avatar, stateObs) == 0)
                    return Types.ACTIONS.ACTION_NIL;

                break;

            case 5:
                // TODO si el enemigo esta encima de una casilla recalculo.
                // TODO orientacion.
                avatar =  new Vector2d(stateObs.getAvatarPosition().x / fescala.x,
                        stateObs.getAvatarPosition().y / fescala.y);

                Vector2d objetivo;
                if(stateObs.getAvatarResources().isEmpty() || stateObs.getAvatarResources().get(6) < 9){
                    // Calculamos la posici??n de la gema m??s cercana al avatar
                    objetivo = buscarGemaMasCercana(stateObs);
                }
                else
                    objetivo = pos_Portal;

                if(caminoRecorrido.isEmpty() || nivelDePeligro(avatar, stateObs) > 0){
                    if (nivelDePeligro(avatar, stateObs) > 7)
                        objetivo = buscarPosicionASalvoDeEnemigo(stateObs);
                    nodoConCoste nodoInicial = new nodoConCoste(avatar, stateObs.getAvatarOrientation(), objetivo, stateObs);
                    caminoRecorrido = calculaCaminoOptimo(nodoInicial);
                }
                return caminoRecorrido.pop();

        }
        return null;




    }

}
