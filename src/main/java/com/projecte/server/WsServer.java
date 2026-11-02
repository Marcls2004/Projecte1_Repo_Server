package com.projecte.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.json.JSONException;
import org.json.JSONObject;

public class WsServer extends WebSocketServer {

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

        if (type.equals("ping")) {
            JSONObject resposta = new JSONObject();
            resposta.put("type", "pong");
            conn.send(resposta.toString());
        } else if (type.equals("bounce")) {
            conn.send(peticio.toString());
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
        System.out.println("Escriu 'exit' per aturar el servidor");
        setConnectionLostTimeout(100);
    }
}