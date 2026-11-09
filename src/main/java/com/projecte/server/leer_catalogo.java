package com.projecte.server;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;

import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

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

    public static Cataleg carregar_SQL(Connection conn) {        
 
        Cataleg cataleg = new Cataleg();

        String sqlZones = "SELECT codi, nom, tipus, aforament FROM zones WHERE recinte_id = ?";

        String sqlEsdeveniments = "SELECT e.esdeveniment_id, e.codi AS esde_codi, r.codi AS recinte_codi, " +
                            "e.nom, e.data_esdeveniment, e.categoria, e.descripcio " +
                            "FROM esdeveniments e " +
                            "JOIN recintes r ON e.recinte_id = r.recinte_id";

        String sqlTipus = "SELECT t.codi AS tipus_codi, t.nom AS tipus_nom, z.codi AS zona_codi, t.preu, t.quota " +
                    "FROM tipus_entrada t " +
                    "JOIN zones z ON t.zona_id = z.zona_id " +
                    "WHERE t.esdeveniment_id = ?";
 
        try {
            
            // Recintes amb les seves zones
            PreparedStatement psZones = conn.prepareStatement(sqlZones);
            Statement stmtRecintes = conn.createStatement();
            ResultSet rsRecintes = stmtRecintes.executeQuery("SELECT recinte_id, codi, nom FROM recintes");
            
            while (rsRecintes.next()) {
                //Obtener Identificadors
                int recinteIdIntern = rsRecintes.getInt("recinte_id");

                Recinte recinte = new Recinte(
                    rsRecintes.getString("codi"),
                    rsRecintes.getString("nom"));
                
                //Consultar zones
                psZones.setInt(1, recinteIdIntern);
                ResultSet rsZones = psZones.executeQuery();

                while (rsZones.next()) {
                    Zona zona = new Zona(
                        rsZones.getString("codi"), 
                        rsZones.getString("nom"), 
                        rsZones.getString("tipus"), 
                        rsZones.getInt("aforament")
                    );
                    recinte.afegirZona(zona);
                }
                rsZones.close();

                cataleg.afegirRecinte(recinte);
            }
            rsRecintes.close();
            stmtRecintes.close();
 
            // Esdeveniments amb els seus tipus d'entrada
            PreparedStatement psTipus = conn.prepareStatement(sqlTipus);
            Statement stmtEsdeveniments = conn.createStatement();
            ResultSet rsEsdeveniments = stmtEsdeveniments.executeQuery(sqlEsdeveniments);

            while (rsEsdeveniments.next()) {
                int esdevenimentIdIntern = rsEsdeveniments.getInt("esdeveniment_id");

                Esdeveniment esdeveniment = new Esdeveniment(
                    rsEsdeveniments.getString("esde_codi"),
                    rsEsdeveniments.getString("recinte_codi"), 
                    rsEsdeveniments.getString("nom"), 
                    rsEsdeveniments.getString("data_esdeveniment"), 
                    rsEsdeveniments.getString("categoria"), 
                    rsEsdeveniments.getString("descripcio")
                );
                psTipus.setInt(1, esdevenimentIdIntern);
                ResultSet rsTipus = psTipus.executeQuery();

                while (rsTipus.next()) {

                    TipusEntrada tipus = new TipusEntrada(
                        rsTipus.getString("tipus_codi"), 
                        rsTipus.getString("tipus_nom"), 
                        rsTipus.getString("zona_codi"), 
                        rsTipus.getDouble("preu"), 
                        rsTipus.getInt("quota")
                    );

                    esdeveniment.afegirTipusEntrada(tipus);
                }
                rsTipus.close();

                cataleg.afegirEsdeveniment(esdeveniment);
            }

            rsEsdeveniments.close();
            stmtEsdeveniments.close();

            psZones.close();
            psTipus.close();
 
        } catch (SQLException e) {
            System.err.println("Error en carregar el catàleg des de SQL: " + e.getMessage());
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
}
