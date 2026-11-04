package com.projecte.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Random;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class WsServer extends WebSocketServer {

    private Cataleg cataleg;
    private Random random = new Random();
    
    public static void main(String[] args) throws InterruptedException, IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        int port = 80;
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
            System.out.println("Mensaje recibido: " + peticio.toString());
        } catch (JSONException e) {
            enviarError(conn, "El missatge no és JSON valid");
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
            if (cataleg == null) {
                enviarError(conn, "El catàleg no s'ha carregat correctament");
            } else {
                resposta.put("type", "esdeveniments");
                resposta.put("data", construirLlistaEsdeveniments());
                conn.send(resposta.toString());
            }
        } else if (type.equals("comprar")) {
            processarCompra(conn, peticio);
        } else {
            enviarError(conn, "Tipus desconegut: " + type);
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
        File fitxer = new File("data", "esdeveniments.xml");
        cataleg = leer_catalogo.carregarCataleg(fitxer);
        if (cataleg == null) {
            System.out.println("ERROR: no s'ha pogut carregar el cataleg");
        } else {
            System.out.println("Catàleg carregat: " + cataleg.getEsdeveniments().size() + " esdeveniments");
        }

        System.out.println("Escriu 'exit' per aturar el servidor");
        setConnectionLostTimeout(100);
    }

    private JSONArray construirLlistaEsdeveniments() {
        JSONArray jLlistaEsdeveniments = new JSONArray();
 
        for (Esdeveniment esde : cataleg.getEsdeveniments()) {
            JSONObject jEsde = new JSONObject();
            jEsde.put("id", esde.getId());
            jEsde.put("nom", esde.getNom());
            jEsde.put("dataHora", esde.getDataHora());
            jEsde.put("categoria", esde.getCategoria());
            jEsde.put("descripcio", esde.getDescripcio());
 
            JSONArray jTipusEntrades = new JSONArray();
            for (TipusEntrada tipus : esde.getTipusEntrades()) {
                JSONObject jTipus = new JSONObject();
                jTipus.put("idTipus", tipus.getId());
                jTipus.put("nomTipus", tipus.getNom());
                jTipus.put("preuTipus", tipus.getPreu());
                jTipus.put("quotaTipus", tipus.getQuota());   // entrades disponibles
 
                // Tipus de zona (general / numerada) perquè l'APP pugui filtrar
                Zona zona = cataleg.getZonaDe(esde, tipus);
                if (zona != null) {
                    jTipus.put("tipusZona", zona.getTipus());
                }
 
                jTipusEntrades.put(jTipus);
            }
            jEsde.put("tipusEntrades", jTipusEntrades);
 
            jLlistaEsdeveniments.put(jEsde);
        }
        return jLlistaEsdeveniments;
    }

    private void processarCompra(WebSocket conn, JSONObject peticio) {
        String nom = peticio.optString("nom");
        String correu = peticio.optString("correu");
        JSONArray entrades = peticio.optJSONArray("entrades");
 
        if (entrades == null) {
            enviarError(conn, "No hi ha entrades a la cistella");
            return;
        }
 
        // Comprovem que totes les entrades existeixen i que en queden prou
        for (int i = 0; i < entrades.length(); i++) {
            JSONObject linia = entrades.getJSONObject(i);
            Esdeveniment esde = cataleg.getEsdeveniment(linia.optString("idEsdeveniment"));
            TipusEntrada tipus = null;

            if (esde != null) {
                tipus = esde.getTipusEntrada(linia.optString("idTipus"));
            }
 
            if (tipus == null || linia.optInt("quantitat") > tipus.getQuota()) {
                enviarError(conn, "No es poden reservar les entrades");
                return;
            }
        }
 
        // Reservem: un localitzador per entrada, desat a <id_esdeveniment>.json
        JSONArray localitzadors = new JSONArray();
        try {
            for (int i = 0; i < entrades.length(); i++) {
                JSONObject linia = entrades.getJSONObject(i);
                String idEsde = linia.getString("idEsdeveniment");
                TipusEntrada tipus = cataleg.getEsdeveniment(idEsde).getTipusEntrada(linia.getString("idTipus"));
                int quantitat = linia.getInt("quantitat");
 
                tipus.restarQuota(quantitat);
 
                for (int j = 0; j < quantitat; j++) {
                    String localitzador = idEsde + "-" + (10000000 + random.nextInt(90000000));
                    localitzadors.put(localitzador);
 
                    JSONObject reserva = new JSONObject();
                    reserva.put("idTipus", tipus.getId());
                    reserva.put("localitzador", localitzador);
                    reserva.put("nom", nom);
                    reserva.put("correu", correu);
                    desarReserva(idEsde, reserva);
                }
            }
        } catch (IOException e) {
            enviarError(conn, "No s'han pogut enregistrar les entrades");
            return;
        }
 
        // Retornem els localitzadors a l'APP
        JSONObject resposta = new JSONObject();
        resposta.put("type", "compraOk");
        resposta.put("localitzadors", localitzadors);
        conn.send(resposta.toString());
    }
 
    // Afegeix una reserva a data/<id_esdeveniment>.json (el crea si no existeix)
    private void desarReserva(String idEsde, JSONObject reserva) throws IOException {
        File fitxer = new File("data", idEsde + ".json");
        JSONArray reserves = new JSONArray();
        if (fitxer.exists()) {
            reserves = new JSONArray(Files.readString(fitxer.toPath(), StandardCharsets.UTF_8));
        }
        reserves.put(reserva);
        Files.writeString(fitxer.toPath(), reserves.toString(2), StandardCharsets.UTF_8);
    }

    private void enviarError(WebSocket conn, String text) {
        JSONObject error = new JSONObject();
        error.put("type", "error");
        error.put("message", text);
        conn.send(error.toString());
    }
}