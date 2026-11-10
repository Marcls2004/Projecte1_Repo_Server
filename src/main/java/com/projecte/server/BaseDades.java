package com.projecte.server;

import java.sql.Statement;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

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
        try {
            String contingut = Files.readString(Path.of("BD/crear_bd.sql"));
            
            String[] consultes = contingut.split(";");

            for (String consulta : consultes) {
                consulta = consulta.trim();
                if (!consulta.isEmpty()){
                    Statement stmt = conn.createStatement();
                    stmt.execute(consulta);
                    stmt.close();
                }
            }
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
}
