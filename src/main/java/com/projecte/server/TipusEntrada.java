package com.projecte.server;

// Tipus d'entrada d'un esdeveniment (<tipusEntrada> del XML).
// La quota són les entrades disponibles: baixa cada cop que se'n venen.
public class TipusEntrada {

    private String id;
    private String nom;
    private String zona; // id de la zona del recinte
    private double preu;
    private int quota;

    public TipusEntrada(String id, String nom, String zona, double preu, int quota) {
        this.id = id;
        this.nom = nom;
        this.zona = zona;
        this.preu = preu;
        this.quota = quota;
    }

    public String getId() { return id; }
    public String getNom() { return nom; }
    public String getZona() { return zona; }
    public double getPreu() { return preu; }
    public int getQuota() { return quota; }

    // Resta entrades venudes de la quota.
    // Retorna false (i no resta res) si no n'hi ha prou.
    public boolean restarQuota(int quantitat) {
        if (quantitat <= 0 || quantitat > quota) {
            return false;
        }
        quota -= quantitat;
        return true;
    }
}