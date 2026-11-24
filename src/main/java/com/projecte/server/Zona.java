package com.projecte.server;


// Zona d'un recinte (<zona> del XML).
// Pot ser "numerada" o "general".
public class Zona {

    private String id;
    private String nom;
    private String tipus; // "numerada" o "general"
    private int aforament;

    // Només per a les zones numerades (rectangle de files x seients dins la sala)
    private Integer numFiles;
    private Integer seientsPerFila;
    private Integer filaInici;
    private Integer columnaInici;

    public Zona(String id, String nom, String tipus, int aforament) {
        this.id = id;
        this.nom = nom;
        this.tipus = tipus;
        this.aforament = aforament;
    }

    public void setGraella(int numFiles, int seientsPerFila, int filaInici, int columnaInici) {
        this.numFiles = numFiles;
        this.seientsPerFila = seientsPerFila;
        this.filaInici = filaInici;
        this.columnaInici = columnaInici;
    }

    public Integer getNumFiles() { return numFiles; }
    public Integer getSeientsPerFila() { return seientsPerFila; }
    public Integer getFilaInici() { return filaInici; }
    public Integer getColumnaInici() { return columnaInici; }

    public String getId() { return id; }
    public String getNom() { return nom; }
    public String getTipus() { return tipus; }
    public int getAforament() { return aforament; }

    // Retorna true si és una zona d'entrada general (sense numerar).
    public boolean esGeneral() {
        return "general".equalsIgnoreCase(tipus);
    }
}