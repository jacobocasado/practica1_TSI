package src_Casado_deGracia_Jacobo;

import core.game.Observation;
import core.game.StateObservation;
import core.player.AbstractPlayer;
import ontology.Types;
import tools.ElapsedCpuTimer;
import tools.Vector2d;
import tools.pathfinder.AStar;

import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Stack;
import java.util.TreeSet;
import java.util.Vector;


public class Agent extends AbstractPlayer {
    Vector2d avatar;
    Vector2d fescala;
    Vector2d pos_Portal;
    int nivel;
    int gemasObtenidas = 0;
    public Stack<Types.ACTIONS> caminoRecorrido =  new Stack<Types.ACTIONS>();

    public void deliberativoSimple(nodoConCoste nodoInicial){
        calculaCaminoOptimo(nodoInicial);
    }

    private int distanciaManhattan(Vector2d pos1, Vector2d pos2){
        return (int) (Math.abs(pos1.x - pos2.x) + Math.abs(pos1.y-pos2.y));
    }

    private int obtenerTipo (Vector2d posicion, StateObservation estado){

        if (posicion.x > 0 && posicion.x < estado.getObservationGrid().length && posicion.y > 0 &&  posicion.y < estado.getObservationGrid().length){
            // Guardamos la posicion en un vector de Observation y, si es suelo, devolvemos -1, y si no es suelo, devuelve el tipo, que es otro entero.

            ArrayList<Observation> aux = estado.getObservationGrid()[(int)posicion.x][(int)posicion.y];
            // Si es vacio, es suelo, por lo tanto, podemos andar sin problemas, y devolvemos -1.
            if(!aux.isEmpty()){
                return aux.get(0).itype;
            }
        }
        return -1;
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

    boolean estoyEnPeligro(StateObservation estado, Vector2d posicion){

        boolean estoyEnPeligro = false;

        ArrayList<Observation> enemigos = new ArrayList<Observation>(estado.getNPCPositions()[0]);
        ArrayList<Vector2d> enemigosEscalados = new ArrayList<>();

        for (Observation d: enemigos){
            Vector2d posEnemigo = new Vector2d();
            posEnemigo = d.position;
            posEnemigo.x = Math.floor(posEnemigo.x / fescala.x);
            posEnemigo.y = Math.floor(posEnemigo.y / fescala.y);
            enemigosEscalados.add(posEnemigo);
        }

        ArrayList<Vector2d> posicionesDeCalor = new ArrayList<>();
        posicionesDeCalor.add(enemigosEscalados.get(0));

        for (int i = -2; i < 3; ++i){
            posicionesDeCalor.add(new Vector2d(enemigosEscalados.get(0).x + i, enemigosEscalados.get(0).y));

            if (i == -1 || i == 1 ) {
                posicionesDeCalor.add(new Vector2d(enemigosEscalados.get(0).x + i, enemigosEscalados.get(0).y - 1));
                posicionesDeCalor.add(new Vector2d(enemigosEscalados.get(0).x + i, enemigosEscalados.get(0).y + 1));
            }

            if (i == 0) {
                posicionesDeCalor.add(new Vector2d(enemigosEscalados.get(0).x + i, enemigosEscalados.get(0).y - 1));
                posicionesDeCalor.add(new Vector2d(enemigosEscalados.get(0).x + i, enemigosEscalados.get(0).y + 1));
                posicionesDeCalor.add(new Vector2d(enemigosEscalados.get(0).x + i, enemigosEscalados.get(0).y - 2));
                posicionesDeCalor.add(new Vector2d(enemigosEscalados.get(0).x + i, enemigosEscalados.get(0).y + 2));
            }
        }

        if (posicionesDeCalor.contains(posicion)){
            estoyEnPeligro = true;
        }

        return estoyEnPeligro;

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

        ArrayList<Vector2d> posicionesASalvo = new ArrayList<>();
        Vector2d posicionEnemigo = new Vector2d(enemigosEscalados.get(0));

        for (int i = -1; i < 2; ++i){
            if (!hayMuro(new Vector2d(avatar.x + i, avatar.y), estado) && !estoyEnPeligro(estado, new Vector2d(avatar.x + i, avatar.y)))
                posicionesASalvo.add(new Vector2d(avatar.x + i, avatar.y));
            if (!hayMuro(new Vector2d(avatar.x + i, avatar.y + 1), estado) && !estoyEnPeligro(estado, new Vector2d(avatar.x + i, avatar.y + 1)))
                posicionesASalvo.add(new Vector2d(avatar.x - i, avatar.y));
            if (!hayMuro(new Vector2d(avatar.x + i, avatar.y - 1), estado) && !estoyEnPeligro(estado, new Vector2d(avatar.x + i, avatar.y - 1)))
                posicionesASalvo.add(new Vector2d(avatar.x - i, avatar.y));
        }

        int casillaMasLejana = 0;
        int mayorDistancia = distanciaManhattan(posicionEnemigo, posicionesASalvo.get(0));

        for (int i = 0; i < posicionesASalvo.size(); ++i){
            if (distanciaManhattan(posicionEnemigo, posicionesASalvo.get(i)) > mayorDistancia){
                mayorDistancia = distanciaManhattan(posicionEnemigo, posicionesASalvo.get(i));
                casillaMasLejana = i;
            }
        }
        return posicionesASalvo.get(casillaMasLejana);
    }


    public Stack<Types.ACTIONS> calculaCaminoOptimo(nodoConCoste nodoInicial){

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
        return nodoActual.getCamino(caminoRecorrido);
    }

    public Agent (StateObservation stateObs, ElapsedCpuTimer elapsedTimer){

        // 1. Calculamos el factor de escala entre mundos (pixeles -> grid)
        fescala = new Vector2d(stateObs.getWorldDimension().width / stateObs.getObservationGrid().length ,
                stateObs.getWorldDimension().height / stateObs.getObservationGrid()[0].length);

        // 2. Calculamos la posicion del portal.
        pos_Portal = stateObs.getPortalsPositions()[0].get(0).position;
        pos_Portal.x = Math.floor(pos_Portal.x / fescala.x);
        pos_Portal.y = Math.floor(pos_Portal.y / fescala.y);

        // Calculamos la posición del agente al principio del problema.
        avatar =  new Vector2d(stateObs.getAvatarPosition().x / fescala.x,
                stateObs.getAvatarPosition().y / fescala.y);


        // AGENTE NIVEL 1. NO HAY GEMAS NI ENEMIGOS. ADEMÁS DE ESO, COMO EL ENTORNO NO VARÍA, PODEMOS LANZARLO DESDE AQUI.
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
            nivel = 3; // Nivel 3.
        }

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
                        calculaCaminoOptimo(caminoHastaSalida);
                    }

                    else{
                        Vector2d posicion_diamante = buscarGemaMasCercana(stateObs);
                        nodoConCoste buscaDiamante = new nodoConCoste(avatar, stateObs.getAvatarOrientation(), posicion_diamante, stateObs);
                        calculaCaminoOptimo(buscaDiamante);
                        gemasObtenidas ++;
                    }

                }
                return caminoRecorrido.pop();

            case 3:


               /* if (estoyEnPeligro(stateObs, avatar) && caminoRecorrido.isEmpty()){
                    System.out.println("Estoy en peligro.");
                    avatar =  new Vector2d(stateObs.getAvatarPosition().x / fescala.x,
                            stateObs.getAvatarPosition().y / fescala.y);
                    Vector2d posicionASalvo = buscarPosicionASalvoDeEnemigo(stateObs);
                    nodoConCoste irASalvo = new nodoConCoste(avatar, stateObs.getAvatarOrientation(), posicionASalvo, stateObs);
                    calculaCaminoOptimo(irASalvo);
                    caminoRecorrido.pop();
                }

                if (!caminoRecorrido.isEmpty())
                    caminoRecorrido.pop();

                else if (!estoyEnPeligro(stateObs, avatar))
                    return Types.ACTIONS.ACTION_NIL;*/

        }
        return null;
    }

}
