package com.projecte.server;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

// Esdeveniment del catàleg amb els seus tipus d'entrada (<esdeveniment> del XML).
public class Esdeveniment {

    private String id;
    private String recinte; // id del recinte on es fa
    private String nom;
    private String dataHora;
    private String categoria;
    private String descripcio;

    // LinkedHashMap: manté l'ordre del XML (T1, T2, T3...)
    private Map<String, TipusEntrada> tipusEntrades = new LinkedHashMap<>();

    public Esdeveniment(String id, String recinte, String nom, String dataHora,
                        String categoria, String descripcio) {
        this.id = id;
        this.recinte = recinte;
        this.nom = nom;
        this.dataHora = dataHora;
        this.categoria = categoria;
        this.descripcio = descripcio;
    }

    public void afegirTipusEntrada(TipusEntrada tipus) {
        tipusEntrades.put(tipus.getId(), tipus);
    }

    // Retorna el tipus d'entrada amb aquest id, o null si no existeix.
    public TipusEntrada getTipusEntrada(String idTipus) {
        return tipusEntrades.get(idTipus);
    }

    public Collection<TipusEntrada> getTipusEntrades() {
        return tipusEntrades.values();
    }

    public String getId() { return id; }
    public String getRecinte() { return recinte; }
    public String getNom() { return nom; }
    public String getDataHora() { return dataHora; }
    public String getCategoria() { return categoria; }
    public String getDescripcio() { return descripcio; }
}