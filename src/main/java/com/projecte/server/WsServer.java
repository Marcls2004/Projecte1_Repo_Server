package com.projecte.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class WsServer extends WebSocketServer {

    private Document catalogo;
    
    public static void main(String[] args) throws InterruptedException, IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        int port = 8888;
        boolean running = true;

        WsServer socket = new WsServer(port);
        socket.start();
        System.out.println("WsServer funciona al port: " + socket.getPort());

        while (running) {
            String line = in.readLine();
            // Aturem el servidor amb 'exit' (o si es tanca la consola)
            if (line == null || line.trim().equalsIgnoreCase("exit")) {
                running = false;
            }
        }

        System.out.println("Aturant WsServer");
        socket.stop(1000);
    }

    public WsServer(int port) {
        super(new InetSocketAddress(port));
    }

    public WsServer(InetSocketAddress address) {
        super(address);
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {

        // Saludem personalment al nou client (en JSON, com tots els missatges)
        JSONObject welcome = new JSONObject();
        welcome.put("type", "welcome");
        welcome.put("message", "Benvingut a WsServer");
        conn.send(welcome.toString());

        // Mostrem per pantalla (servidor) la nova connexió
        String host = conn.getRemoteSocketAddress().getAddress().getHostAddress();
        System.out.println(host + " s'ha connectat");
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {

        // Mostrem per pantalla (servidor) la desconnexió
        String host = conn.getRemoteSocketAddress().getAddress().getHostAddress();
        System.out.println(host + " s'ha desconnectat (codi " + code + ")");
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        JSONObject peticio;
        try {
            peticio = new JSONObject(message);
            System.err.println("Mensaje recibido: " + peticio.toString());
        } catch (JSONException e) {
            JSONObject error = new JSONObject();
            error.put("type", "error");
            error.put("message", "El missatge no és JSON valid");
            conn.send(error.toString());
            return;
        }

        String type = peticio.optString("type", "");

        JSONObject resposta = new JSONObject();

        if (type.equals("ping")) {
            resposta.put("type", "pong");
            conn.send(resposta.toString());
        } else if (type.equals("bounce")) {
            conn.send(peticio.toString());
        } else if (type.equals("getEsdeveniments")) {
            if (catalogo == null) {
                JSONObject error = new JSONObject();
                error.put("type", "error");
                error.put("message", "El catalog no s'ha carregat correctament");
                conn.send(error.toString());
            } else {
                resposta.put("type", "esdeveniments");
                resposta.put("data", construirLlistaEsdeveniments());
                conn.send(resposta.toString());
            }
        } else {
            JSONObject error = new JSONObject();
            error.put("type", "error");
            error.put("message", "Tipus desconegut: " + type);
            conn.send(error.toString());
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {

        // conn és null si l'error és del servidor (p. ex. el port ja està ocupat)
        if (conn == null) {
            System.out.println("Error del servidor: " + ex.getMessage());
        } else {
            ex.printStackTrace();
        }
    }

    @Override
    public void onStart() {
        // S'inicia el servidor
        File fichero = new File("data", "esdeveniments.xml");
        catalogo = leer_catalogo.parseXML(fichero);
        if (catalogo == null) {
            System.out.println("ERROR: no s'ha pogut carregar el cataleg");
        } else {
            System.out.println("Cataleg carregat");
        }

        System.out.println("Escriu 'exit' per aturar el servidor");
        setConnectionLostTimeout(100);
    }

    private JSONArray construirLlistaEsdeveniments() {
        NodeList esdeveniments = catalogo.getElementsByTagName("esdeveniment");
        JSONArray jLlistaEsdeveniments = new JSONArray();

        for (int i = 0; i < esdeveniments.getLength(); i++) {
            Element esde = (Element) esdeveniments.item(i);
            JSONObject jEsde = new JSONObject();
            
            jEsde.put("id", esde.getAttribute("id"));
            jEsde.put("nom", esde.getElementsByTagName("nom").item(0).getTextContent());
            jEsde.put("dataHora", esde.getElementsByTagName("dataHora").item(0).getTextContent());
            jEsde.put("categoria", esde.getElementsByTagName("categoria").item(0).getTextContent());
            jEsde.put("descripcio", esde.getElementsByTagName("descripcio").item(0).getTextContent());
            
            NodeList tipusEntradas = esde.getElementsByTagName("tipusEntrada");
            JSONArray jTipusEntradas = new JSONArray();

            for (int j = 0; j < tipusEntradas.getLength(); j++) {
                Element tipoEntrada = (Element) tipusEntradas.item(j);
                JSONObject jTipusEntrada = new JSONObject();

                jTipusEntrada.put("idTipus", tipoEntrada.getAttribute("id"));
                jTipusEntrada.put("nomTipus", tipoEntrada.getAttribute("nom"));
                jTipusEntrada.put("preuTipus", tipoEntrada.getAttribute("preu"));
                jTipusEntrada.put("quotaTipus", tipoEntrada.getAttribute("quota"));
                
                jTipusEntradas.put(jTipusEntrada);
            }
            jEsde.put("tipusEntrades", jTipusEntradas);
            jLlistaEsdeveniments.put(jEsde);
        }
        return jLlistaEsdeveniments;
    }
}