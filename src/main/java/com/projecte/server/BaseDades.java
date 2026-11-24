package com.projecte.server;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class BaseDades {
    private static Connection conn = null;

    public static void conectar(){
        try {
            conn = DriverManager.getConnection("jdbc:sqlite:data/platea3.db");

            Statement stmt = conn.createStatement();
            stmt.execute("PRAGMA foreign_keys = ON;");
            stmt.close();

            System.out.println("Conexio correcte!");

            crearTaules();
        } catch (Exception e) {
            System.out.println("Error de la conexio de la base de dades: " + e.getMessage());
        }
    }

    private static void crearTaules(){
        executarScript("BD/crear_bd.sql");
    }

    // Executa totes les sentències d'un fitxer .sql (separades per ;)
    public static void executarScript(String ruta){
        try {
            String contingut = Files.readString(Path.of(ruta));
            
            String[] consultes = contingut.split(";");

            for (String consulta : consultes) {
                consulta = consulta.trim();
                if (!consulta.isEmpty()){
                    Statement stmt = conn.createStatement();
                    stmt.execute(consulta);
                    stmt.close();
                }
            }
            afegirColumnaEstat();
        }catch (IOException e){
            System.out.println("Error en trobar l'arxiu: " + e.getMessage());
        }catch (SQLException e){
            System.out.println("Error en script SQL: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Error de lectura de la base de dades: " + e.getMessage());
        }
    }

    public static Connection getConnexio(){
        return conn;
    }

    public static void tancar(){
        try {
            if ((conn != null) && (!conn.isClosed())){
                conn.close();
            }
        } catch (Exception e) {
        }
    }
        // Afegeix la columna 'estat' a entrades si no existeix (valors: ACTIVA / VALIDADA)
    private static void afegirColumnaEstat() {
        try {
            boolean existeix = false;
            try (Statement st = conn.createStatement();
                 java.sql.ResultSet rs = st.executeQuery("PRAGMA table_info(entrades)")) {
                while (rs.next()) {
                    if ("estat".equals(rs.getString("name"))) existeix = true;
                }
            }
            if (!existeix) {
                try (Statement st = conn.createStatement()) {
                    st.execute("ALTER TABLE entrades ADD COLUMN estat TEXT NOT NULL DEFAULT 'ACTIVA'");
                }
                System.out.println("Columna 'estat' afegida a entrades");
            }
        } catch (SQLException e) {
            System.out.println("Error afegint la columna estat: " + e.getMessage());
        }
    }
}

