package com.projecte.server;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class Cataleg {
    
    private Map<String, Recinte> recintes = new LinkedHashMap<>();
    private Map<String, Esdeveniment> esdeveniments = new LinkedHashMap<>();

    public void afegirRecinte(Recinte recinte) {
        recintes.put(recinte.getId(), recinte);
    }

    public void afegirEsdeveniment(Esdeveniment esdeveniment) {
        esdeveniments.put(esdeveniment.getId(), esdeveniment);
    }

    public Recinte getRecinte(String idRecinte) {
        return recintes.get(idRecinte); // null si no existeix
    }

    public Esdeveniment getEsdeveniment(String idEsdeveniment) {
        return esdeveniments.get(idEsdeveniment);
    }

    public Collection<Esdeveniment> getEsdeveniments() {
        return esdeveniments.values(); // per recórrer-los tots
    }

    // Retorna la zona on és un tipus d'entrada d'un esdeveniment,
    // o null si el recinte o la zona no existeixen.
    public Zona getZonaDe(Esdeveniment esdeveniment, TipusEntrada tipus) {
        Recinte recinte = getRecinte(esdeveniment.getRecinte());
        if (recinte == null) {
            return null;
        }
        return recinte.getZona(tipus.getZona());
    }
}
