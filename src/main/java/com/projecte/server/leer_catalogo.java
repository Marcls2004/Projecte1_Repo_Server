package com.projecte.server;

import org.w3c.dom.Node;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;

public class leer_catalogo {
    private final File dataDir;

    public leer_catalogo(File dataDir) {
        this.dataDir = dataDir;
    }

    public static void main(String[] args) {
        String userDir = System.getProperty("user.dir");

        // Canvi realitzat per buscar dins de la carpeta "data"
        File dataDir = new File(userDir, "data");

        leer_catalogo app = new leer_catalogo(dataDir);
        
        // Assegura't que el nom del fitxer coincideixi exactament
        app.processarFitxerXML("esdeveniments.xml"); 
    }

    public void processarFitxerXML(String filename) {
        File inputFile = new File(dataDir, filename);
        
        // Verificació ràpida de si el fitxer existeix físicament en aquesta ruta
        if (!inputFile.exists()) {
            System.err.println("ERROR: El fitxer no existeix a la ruta: " + inputFile.getAbsolutePath());
            return;
        }

        Document doc = parseXML(inputFile);

        if (doc != null) {
            System.out.println("¡Fitxer XML llegit correctament!");

        } else {
            System.err.println("ERROR: No s'ha pogut analitzar (parsejar) el document XML.");
        }
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
