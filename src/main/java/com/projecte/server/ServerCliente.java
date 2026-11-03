package com.projecte.server;
import java.net.URI;
import java.util.Scanner;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

public class ServerCliente {
    
    public static void main(String[] args) {
        try {
            // URL del servidor WebSocket local en el puerto 8888
            String serverUri = "ws://localhost:8888";
            
            WebSocketClient client = new WebSocketClient(new URI(serverUri)) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    System.out.println("[SRV-CLI] ¡Conectado con éxito al servidor WebSocket!");
                    System.out.println("Escribe un comando (ej: ping, bounce, getEsdeveniments) o 'exit' para salir:");
                }

                @Override
                public void onMessage(String message) {
                    System.out.println("\nRESPUESTA DEL SERVIDOR:");
                    System.out.println(message);
                    System.out.print("\nEscribe el siguiente comando: ");
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    System.out.println("[SRV-CLI] Conexión cerrada: " + reason);
                }

                @Override
                public void onError(Exception ex) {
                    System.err.println("[SRV-CLI] Error: " + ex.getMessage());
                }
            };

            // Conectamos con el servidor
            client.connect();

            // Lector para la terminal
            Scanner scanner = new Scanner(System.in);
            while (true) {
                String input = scanner.nextLine();
                if (input.equalsIgnoreCase("exit")) {
                    client.close();
                    break;
                }

                // Construimos el JSON según lo que pide el enunciado
                String jsonToSend = "";
                if (input.equals("ping") || input.equals("getEsdeveniments") || input.equals("bounce")) {
                    jsonToSend = "{\"type\":\"" + input + "\"}";
                } else {
                    // Por defecto envía el tipo personalizado o texto libre
                    jsonToSend = "{\"type\":\"" + input + "\"}";
                }

                if (client.isOpen()) {
                    client.send(jsonToSend);
                } else {
                    System.out.println(" El cliente no está conectado al servidor.");
                }
            }

            scanner.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}