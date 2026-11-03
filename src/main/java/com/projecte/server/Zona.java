package com.projecte.server;


// Zona d'un recinte (<zona> del XML).
// Pot ser "numerada" o "general".
public class Zona {

    private String id;
    private String nom;
    private String tipus; // "numerada" o "general"
    private int aforament;

    public Zona(String id, String nom, String tipus, int aforament) {
        this.id = id;
        this.nom = nom;
        this.tipus = tipus;
        this.aforament = aforament;
    }

    public String getId() { return id; }
    public String getNom() { return nom; }
    public String getTipus() { return tipus; }
    public int getAforament() { return aforament; }

    // Retorna true si és una zona d'entrada general (sense numerar).
    public boolean esGeneral() {
        return "general".equalsIgnoreCase(tipus);
    }
}