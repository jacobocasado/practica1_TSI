package src_Casado_deGracia_Jacobo;

public class nodoConCoste {

    int g;
    int h;
    nodoConCoste nodoPadre;

    public nodoConCoste() {
        this.nodoPadre = null;
        this.g = 0;
    }

    public nodoConCoste(nodoConCoste nodoPadre, int g) {
        this.nodoPadre = nodoPadre;
        this.g = g;
    }



}

