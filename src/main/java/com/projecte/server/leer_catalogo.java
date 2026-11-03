package com.projecte.server;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
 
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;

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
}
