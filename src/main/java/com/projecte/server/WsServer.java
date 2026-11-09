package com.projecte.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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
        BaseDades.tancar();
        System.out.println("Servidor aturat de manera neta.");
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
            //processarCompra(conn, peticio); // Llamamos al nuevo método SQL
            processarCompraSQL(conn, peticio); // Llamamos al nuevo método SQL
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
        //File fitxer = new File("data", "esdeveniments.xml");
        //cataleg = leer_catalogo.carregarCataleg(fitxer);
        
        
        //URL provisional para luego cambiarla por la bueno
        BaseDades.conectar();

        cataleg = leer_catalogo.carregar_SQL(BaseDades.getConnexio());
        
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

    private void processarCompraSQL (WebSocket conn, JSONObject peticio){
        String nom = peticio.optString("nom");
        String correu = peticio.optString("correu");
        JSONArray entrades = peticio.optJSONArray("entrades");

        if (entrades == null){
            enviarError(conn, "No hi ha entrades a la cistella");
            return;
        }

        for(int i = 0; i < entrades.length(); i++){
            JSONObject linia = entrades.getJSONObject(i);
            Esdeveniment esdeveniment = cataleg.getEsdeveniment(linia.optString("idEsdeveniment"));
            TipusEntrada tipusEntrada = null;
            if (esdeveniment != null){
                tipusEntrada = esdeveniment.getTipusEntrada(linia.optString("idTipus"));
            }
            if (tipusEntrada == null || linia.optInt("quantitat") > tipusEntrada.getQuota()){
                enviarError(conn, "No es poden revervar les entrades (quota insuficient)");
                return;
            }
        }

        String sqlCompra = "INSERT INTO compres (localitzador_compra, nom_comprador, correu) VALUES (?, ?, ?)";
        String sqlGetTipusId = "SELECT t.tipus_entrada_id, t.preu FROM tipus_entrada t " +
                           "JOIN esdeveniments e ON t.esdeveniment_id = e.esdeveniment_id " +
                           "WHERE e.codi = ? AND t.codi = ?";
        String sqlInsertEntrada = "INSERT INTO entrades (compra_id, tipus_entrada_id, localitzador_entrada, preu_pagat) VALUES (?, ?, ?, ?)";

        Connection connection = null;
        JSONArray localitzadors = new JSONArray();

        try {
            connection = BaseDades.getConnexio();
            
            // Desactivamos autoCommit para que sea una transacción unificada y segura
            connection.setAutoCommit(false);
            
            // 1. Generamos un localizador único general para toda la compra (ej: LC-34829104)
            String localitzadorCompra = "LC-" + (10000000 + random.nextInt(90000000));

            // 2. Preparamos el Statement de la compra pidiéndole que nos retorne la clave primaria generada (compra_id)
            PreparedStatement psCompra = connection.prepareStatement(sqlCompra, Statement.RETURN_GENERATED_KEYS);
            psCompra.setString(1, localitzadorCompra);
            psCompra.setString(2, nom);
            psCompra.setString(3, correu);
            psCompra.executeUpdate();

            // 3. Recuperamos el ID autoincremental que SQLite le ha asignado a esta compra
            ResultSet rsIdCompra = psCompra.getGeneratedKeys();
            int idCompraGenerat = 0;
            if (rsIdCompra.next()){
                idCompraGenerat = rsIdCompra.getInt(1);
            }

            rsIdCompra.close();
            psCompra.close();

            // 4. Preparamos los statements para las consultas internas del bucle
            PreparedStatement psGetTipus = connection.prepareStatement(sqlGetTipusId);
            PreparedStatement psEntrada = connection.prepareStatement(sqlInsertEntrada);

            // 5. Recorremos cada línea de la cistella
            for (int i = 0; i < entrades.length(); i++){
                JSONObject linia = entrades.getJSONObject(i);
                String idEsdeveniment = linia.getString("idEsdeveniment");
                String idTipus = linia.getString("idTipus");
                int quantitat = linia.getInt("quantitat");

                // Restamos la cuota en la memoria del servidor (catálogo)
                TipusEntrada tipusMemoria = cataleg.getEsdeveniment(idEsdeveniment).getTipusEntrada(idTipus);
                tipusMemoria.restarQuota(quantitat);

                // 6. Consultamos la base de datos para obtener el ID numérico interno y el precio real
                psGetTipus.setString(1, idEsdeveniment);
                psGetTipus.setString(2, idTipus);

                ResultSet rsTipusId = psGetTipus.executeQuery();

                int tipusEntradesIdIntern = 0;
                double preuPagat = 0.0;

                if (rsTipusId.next()){
                    tipusEntradesIdIntern = rsTipusId.getInt("tipus_entrada_id");
                    preuPagat = rsTipusId.getDouble("preu");
                }
                rsTipusId.close();

                // 7. Creamos tantos registros de entradas individuales como indique la 'quantitat'
                for (int j = 0; j < quantitat; j++){
                    String localitzadorEntrada = idEsdeveniment + "-" + (10000000 + random.nextInt(90000000));
                    localitzadors.put(localitzadorEntrada);

                    psEntrada.setInt(1, idCompraGenerat);
                    psEntrada.setInt(2, tipusEntradesIdIntern);
                    psEntrada.setString(3, localitzadorEntrada);
                    psEntrada.setDouble(4, preuPagat);
                    psEntrada.executeUpdate();
                }

            }

            psGetTipus.close();
            psEntrada.close();

            // Si todo termina con éxito, guardamos de golpe en la base de datos
            connection.commit();

            // Retornamos los localizadores a la APP
            JSONObject resposta = new JSONObject();
            resposta.put("type", "compraOk");
            resposta.put("localitzadors", localitzadors);
            conn.send(resposta.toString());


        } catch (Exception e) {
            if (connection != null){
                        // Si hay cualquier error de base de datos, deshacemos todo para no dejar registros basura
                try {connection.rollback();} catch(SQLException ex){ex.printStackTrace();}
            }
            System.err.println("Error SQL en procesar la compra: " +  e.getMessage());
            enviarError(conn, "No s'han pogut enregistrar les entrades a la base de dades");
        } finally{
            // Siempre restauramos el comportamiento por defecto de la conexión al terminar
            if (connection != null){
                try { connection.setAutoCommit(true);} catch (SQLException ex) {ex.printStackTrace();}
            }
        }
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