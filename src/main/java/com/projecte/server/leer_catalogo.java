package com.projecte.server;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class leer_catalogo {
    
    // Prova ràpida: carrega el catàleg i mostra algunes dades
    public static void main(String[] args) {
        File fitxer = new File("data", "esdeveniments.xml");
        Cataleg cataleg = carregarCataleg(fitxer);
 
        if (cataleg == null) {
            System.err.println("ERROR: no s'ha pogut carregar el catàleg");
            return;
        }
 
        System.out.println("Esdeveniments carregats: " + cataleg.getEsdeveniments().size());
 
        // Busquem un esdeveniment concret pel seu id
        Esdeveniment e1 = cataleg.getEsdeveniment("E1");
        System.out.println("E1: " + e1.getNom());

        // Recorrem els seus tipus d'entrada
        for (TipusEntrada tipus : e1.getTipusEntrades()) {
            Zona zona = cataleg.getZonaDe(e1, tipus);

            if (zona == null) {
                System.out.println("  " + tipus.getNom() + " -> zona no trobada");
            } else {
                System.out.println("  " + tipus.getNom() + " -> " + zona.getTipus());
            }
        }
    }

    
    // Llegeix l'XML i el carrega en un Cataleg.
    // Retorna null si el fitxer no es pot llegir o té dades incorrectes.
    public static Cataleg carregarCataleg(File fitxer) {
        Document doc = parseXML(fitxer);
        if (doc == null) {
            return null;
        }
 
        Cataleg cataleg = new Cataleg();
 
        try {
            // Recintes amb les seves zones
            NodeList llistaRecintes = doc.getElementsByTagName("recinte");
            for (int i = 0; i < llistaRecintes.getLength(); i++) {
                Element eRecinte = (Element) llistaRecintes.item(i);
                Recinte recinte = new Recinte(
                        eRecinte.getAttribute("id"),
                        eRecinte.getAttribute("nom"));
 
                NodeList llistaZones = eRecinte.getElementsByTagName("zona");   // només les d'aquest recinte
                for (int j = 0; j < llistaZones.getLength(); j++) {
                    Element eZona = (Element) llistaZones.item(j);
                    Zona zona = new Zona(
                            eZona.getAttribute("id"),
                            eZona.getAttribute("nom"),
                            eZona.getAttribute("tipus"),
                            Integer.parseInt(eZona.getAttribute("aforament")));
                    recinte.afegirZona(zona);
                }
                cataleg.afegirRecinte(recinte);
            }
 
            // Esdeveniments amb els seus tipus d'entrada
            NodeList llistaEsdeveniments = doc.getElementsByTagName("esdeveniment");
            for (int i = 0; i < llistaEsdeveniments.getLength(); i++) {
                Element eEsde = (Element) llistaEsdeveniments.item(i);
                Esdeveniment esdeveniment = new Esdeveniment(
                        eEsde.getAttribute("id"),          // atributs
                        eEsde.getAttribute("recinte"),
                        eEsde.getElementsByTagName("nom").item(0).getTextContent(),
                        eEsde.getElementsByTagName("dataHora").item(0).getTextContent(),
                        eEsde.getElementsByTagName("categoria").item(0).getTextContent(),
                        eEsde.getElementsByTagName("descripcio").item(0).getTextContent());
 
                NodeList llistaTipus = eEsde.getElementsByTagName("tipusEntrada");
                for (int j = 0; j < llistaTipus.getLength(); j++) {
                    Element eTipus = (Element) llistaTipus.item(j);
                    TipusEntrada tipus = new TipusEntrada(
                            eTipus.getAttribute("id"),
                            eTipus.getAttribute("nom"),
                            eTipus.getAttribute("zona"),
                            Double.parseDouble(eTipus.getAttribute("preu")),
                            Integer.parseInt(eTipus.getAttribute("quota")));
                    esdeveniment.afegirTipusEntrada(tipus);
                }
                cataleg.afegirEsdeveniment(esdeveniment);
            }
 
        } catch (NumberFormatException e) {
            System.err.println("Error: un número de l'XML no és vàlid: " + e.getMessage());
            return null;
        }
 
        return cataleg;
    }

    public static Document parseXML(File inputFile) {
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inputFile);
            doc.getDocumentElement().normalize();
            return doc;
        } catch (Exception e) {
            System.err.println("Error en analitzar l'XML: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
public static void migrarCatalegABaseDeDades(
        Cataleg cataleg, Connection connexio
) throws SQLException {

    String sqlRecinte = """
        INSERT OR IGNORE INTO recintes (codi, nom)
        VALUES (?, ?)
        """;

    String sqlZona = """
        INSERT OR IGNORE INTO zones
        (recinte_id, codi, nom, tipus, aforament)
        VALUES (?, ?, ?, ?, ?)
        """;

    String sqlEsdeveniment = """
        INSERT OR IGNORE INTO esdeveniments
        (recinte_id, codi, nom, data_esdeveniment, categoria, descripcio)
        VALUES (?, ?, ?, ?, ?, ?)
        """;

    String sqlTipus = """
        INSERT OR IGNORE INTO tipus_entrada
        (esdeveniment_id, zona_id, codi, nom, quota, preu)
        VALUES (?, ?, ?, ?, ?, ?)
        """;

    boolean autoCommitOriginal = connexio.getAutoCommit();

    try {
        connexio.setAutoCommit(false);

        // 1. Migrar recintes i zones
        try (PreparedStatement psRecinte = connexio.prepareStatement(sqlRecinte);
             PreparedStatement psZona = connexio.prepareStatement(sqlZona);
             PreparedStatement psIdRecinte = connexio.prepareStatement(
                     "SELECT recinte_id FROM recintes WHERE codi = ?");
             PreparedStatement psIdZona = connexio.prepareStatement(
                     "SELECT zona_id FROM zones WHERE recinte_id = ? AND codi = ?")) {

            for (Recinte recinte : cataleg.getRecintes()) {

                psRecinte.setString(1, recinte.getId());
                psRecinte.setString(2, recinte.getNom());
                psRecinte.executeUpdate();

                psIdRecinte.setString(1, recinte.getId());

                int idRecinte;
                try (ResultSet rs = psIdRecinte.executeQuery()) {
                    if (!rs.next()) {
                        throw new SQLException("No s'ha trobat el recinte: " + recinte.getId());
                    }
                    idRecinte = rs.getInt("recinte_id");
                }

                for (Zona zona : recinte.getZones()) {
                    psZona.setInt(1, idRecinte);
                    psZona.setString(2, zona.getId());
                    psZona.setString(3, zona.getNom());
                    psZona.setString(4, zona.getTipus());
                    psZona.setInt(5, zona.getAforament());
                    psZona.executeUpdate();
                }
            }
        }

        // 2. Migrar esdeveniments i tipus d'entrada
        try (PreparedStatement psEsdeveniment = connexio.prepareStatement(sqlEsdeveniment);
             PreparedStatement psTipus = connexio.prepareStatement(sqlTipus);
             PreparedStatement psIdRecinte = connexio.prepareStatement(
                     "SELECT recinte_id FROM recintes WHERE codi = ?");
             PreparedStatement psIdEsdeveniment = connexio.prepareStatement(
                     "SELECT esdeveniment_id FROM esdeveniments WHERE recinte_id = ? AND codi = ?");
             PreparedStatement psIdZona = connexio.prepareStatement(
                     "SELECT zona_id FROM zones WHERE recinte_id = ? AND codi = ?")) {

            for (Esdeveniment esdeveniment : cataleg.getEsdeveniments()) {

                psIdRecinte.setString(1, esdeveniment.getRecinte());

                int idRecinte;
                try (ResultSet rs = psIdRecinte.executeQuery()) {
                    if (!rs.next()) {
                        throw new SQLException("No s'ha trobat el recinte de l'esdeveniment: "
                                + esdeveniment.getId());
                    }
                    idRecinte = rs.getInt("recinte_id");
                }

                psEsdeveniment.setInt(1, idRecinte);
                psEsdeveniment.setString(2, esdeveniment.getId());
                psEsdeveniment.setString(3, esdeveniment.getNom());
                psEsdeveniment.setString(4, esdeveniment.getDataHora());
                psEsdeveniment.setString(5, esdeveniment.getCategoria());
                psEsdeveniment.setString(6, esdeveniment.getDescripcio());
                psEsdeveniment.executeUpdate();

                psIdEsdeveniment.setInt(1, idRecinte);
                psIdEsdeveniment.setString(2, esdeveniment.getId());

                int idEsdeveniment;
                try (ResultSet rs = psIdEsdeveniment.executeQuery()) {
                    if (!rs.next()) {
                        throw new SQLException("No s'ha pogut recuperar l'esdeveniment: "
                                + esdeveniment.getId());
                    }
                    idEsdeveniment = rs.getInt("esdeveniment_id");
                }

                for (TipusEntrada tipus : esdeveniment.getTipusEntrades()) {

                    psIdZona.setInt(1, idRecinte);
                    psIdZona.setString(2, tipus.getZona());

                    int idZona;
                    try (ResultSet rs = psIdZona.executeQuery()) {
                        if (!rs.next()) {
                            throw new SQLException("No s'ha trobat la zona: "
                                    + tipus.getZona());
                        }
                        idZona = rs.getInt("zona_id");
                    }

                    psTipus.setInt(1, idEsdeveniment);
                    psTipus.setInt(2, idZona);
                    psTipus.setString(3, tipus.getId());
                    psTipus.setString(4, tipus.getNom());
                    psTipus.setInt(5, tipus.getQuota());
                    psTipus.setDouble(6, tipus.getPreu());
                    psTipus.executeUpdate();
                }
            }
        }

        connexio.commit();

    } catch (SQLException e) {
        connexio.rollback();
        throw e;

    } finally {
        connexio.setAutoCommit(autoCommitOriginal);
    }
}
}