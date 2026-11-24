package com.projecte.server;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;


// Recinte amb les seves zones (<recinte> del XML).
public class Recinte {

    private String id;
    private String nom;
    private Map<String, Zona> zones = new LinkedHashMap<>(); // clau = id de la zona

    public Recinte(String id, String nom) {
        this.id = id;
        this.nom = nom;
    }

    public void afegirZona(Zona zona) {
        zones.put(zona.getId(), zona);
    }

    // Retorna la zona amb aquest id, o null si no existeix.
    public Zona getZona(String idZona) {
        return zones.get(idZona);
    }

    public Collection<Zona> getZones() {
        return zones.values();
    }

    public String getId() { return id; }
    public String getNom() { return nom; }
}