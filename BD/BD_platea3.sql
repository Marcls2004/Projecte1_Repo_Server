CREATE DATABASE IF NOT EXISTS `platea3`;

USE `platea3`;

CREATE TABLE recintes (recinte_id INT AUTO_INCREMENT PRIMARY KEY, codi VARCHAR(50) UNIQUE NOT NULL, nom VARCHAR(255) NOT NULL);

CREATE TABLE zones (zona_id INT AUTO_INCREMENT PRIMARY KEY, recinte_id INT NOT NULL, codi VARCHAR(50) NOT NULL,nom VARCHAR(255) NOT NULL, 
tipus ENUM('numerada', 'general') NOT NULL, aforament INT NOT NULL, num_files INT NULL, seients_per_fila INT NULL, fila_inici INT NULL, columna_inici INT NULL,
CONSTRAINT fk_zones_recintes FOREIGN KEY (recinte_id) REFERENCES recintes(recinte_id) ON DELETE CASCADE,
CONSTRAINT uq_zona_recinte UNIQUE (recinte_id, codi));

CREATE TABLE esdeveniments (esdeveniment_id INT AUTO_INCREMENT PRIMARY KEY, recinte_id INT NOT NULL, codi VARCHAR(50) NOT NULL, nom VARCHAR(255) NOT NULL, 
data_esdeveniment DATETIME NOT NULL, categoria VARCHAR(255) NOT NULL, descripcio TEXT NOT NULL, 
CONSTRAINT fk_esdeveniments_recintes FOREIGN KEY (recinte_id) REFERENCES recintes(recinte_id) ON DELETE CASCADE,
CONSTRAINT uq_esdeveniment_recinte UNIQUE (recinte_id, codi));

CREATE TABLE tipus_entrada (tipus_entrada_id INT AUTO_INCREMENT PRIMARY KEY, esdeveniment_id INT NOT NULL, zona_id INT NOT NULL, codi VARCHAR(50) NOT NULL,
nom VARCHAR(255) NOT NULL, quota INT NOT NULL, preu DECIMAL(10,2) NOT NULL, 
CONSTRAINT fk_tipus_entrada_zones FOREIGN KEY (zona_id) REFERENCES zones(zona_id) ON DELETE CASCADE, 
CONSTRAINT fk_tipus_entrada_esdeveniments FOREIGN KEY (esdeveniment_id) REFERENCES esdeveniments(esdeveniment_id) ON DELETE CASCADE,
CONSTRAINT uq_tipus_entrada_esdeveniment UNIQUE (esdeveniment_id, codi));

CREATE TABLE compres (compra_id INT AUTO_INCREMENT PRIMARY KEY, localitzador_compra VARCHAR(50) UNIQUE NOT NULL, nom_comprador VARCHAR(255) NOT NULL,
correu VARCHAR(255) NOT NULL, data_compra DATETIME DEFAULT CURRENT_TIMESTAMP);

CREATE TABLE entrades (entrada_id INT AUTO_INCREMENT PRIMARY KEY, compra_id INT NOT NULL, tipus_entrada_id INT NOT NULL, localitzador_entrada VARCHAR(50) UNIQUE NOT NULL,
fila INT NULL, seient INT NULL, preu_pagat DECIMAL(10,2) NOT NULL, CONSTRAINT fk_entrades_compres FOREIGN KEY (compra_id) REFERENCES compres(compra_id) ON DELETE CASCADE,
CONSTRAINT fk_entrades_tipus_entrada FOREIGN KEY (tipus_entrada_id) REFERENCES tipus_entrada(tipus_entrada_id) ON DELETE CASCADE);

