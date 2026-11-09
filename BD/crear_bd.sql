CREATE TABLE IF NOT EXISTS recintes (
    recinte_id INTEGER PRIMARY KEY AUTOINCREMENT, 
    codi TEXT UNIQUE NOT NULL, 
    nom TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS zones (
    zona_id INTEGER PRIMARY KEY AUTOINCREMENT, 
    recinte_id INTEGER NOT NULL, 
    codi TEXT NOT NULL,
    nom TEXT NOT NULL, 
    tipus TEXT NOT NULL CHECK (tipus IN ('numerada', 'general')), 
    aforament INTEGER NOT NULL, 
    num_files INTEGER NULL, 
    seients_per_fila INTEGER NULL, 
    fila_inici INTEGER NULL, 
    columna_inici INTEGER NULL,
    CONSTRAINT fk_zones_recintes FOREIGN KEY (recinte_id) REFERENCES recintes(recinte_id) ON DELETE CASCADE,
    CONSTRAINT uq_zona_recinte UNIQUE (recinte_id, codi)
);

CREATE TABLE IF NOT EXISTS esdeveniments (
    esdeveniment_id INTEGER PRIMARY KEY AUTOINCREMENT, 
    recinte_id INTEGER NOT NULL, 
    codi TEXT NOT NULL, 
    nom TEXT NOT NULL, 
    data_esdeveniment TEXT NOT NULL, -- SQLite almacena fechas como TEXT, REAL o INTEGER
    categoria TEXT NOT NULL, 
    descripcio TEXT NOT NULL, 
    CONSTRAINT fk_esdeveniments_recintes FOREIGN KEY (recinte_id) REFERENCES recintes(recinte_id) ON DELETE CASCADE,
    CONSTRAINT uq_esdeveniment_recinte UNIQUE (recinte_id, codi)
);

CREATE TABLE IF NOT EXISTS tipus_entrada (
    tipus_entrada_id INTEGER PRIMARY KEY AUTOINCREMENT, 
    esdeveniment_id INTEGER NOT NULL, 
    zona_id INTEGER NOT NULL, 
    codi TEXT NOT NULL,
    nom TEXT NOT NULL, 
    quota INTEGER NOT NULL, 
    preu REAL NOT NULL, -- SQLite usa REAL para valores decimales/numÃ©ricos
    CONSTRAINT fk_tipus_entrada_zones FOREIGN KEY (zona_id) REFERENCES zones(zona_id) ON DELETE CASCADE, 
    CONSTRAINT fk_tipus_entrada_esdeveniments FOREIGN KEY (esdeveniment_id) REFERENCES esdeveniments(esdeveniment_id) ON DELETE CASCADE,
    CONSTRAINT uq_tipus_entrada_esdeveniment UNIQUE (esdeveniment_id, codi)
);

CREATE TABLE IF NOT EXISTS compres (
    compra_id INTEGER PRIMARY KEY AUTOINCREMENT, 
    localitzador_compra TEXT UNIQUE NOT NULL, 
    nom_comprador TEXT NOT NULL,
    correu TEXT NOT NULL, 
    data_compra TEXT DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS entrades (
    entrada_id INTEGER PRIMARY KEY AUTOINCREMENT, 
    compra_id INTEGER NOT NULL, 
    tipus_entrada_id INTEGER NOT NULL, 
    localitzador_entrada TEXT UNIQUE NOT NULL,
    fila INTEGER NULL, 
    seient INTEGER NULL, 
    preu_pagat REAL NOT NULL, 
    CONSTRAINT fk_entrades_compres FOREIGN KEY (compra_id) REFERENCES compres(compra_id) ON DELETE CASCADE,
    CONSTRAINT fk_entrades_tipus_entrada FOREIGN KEY (tipus_entrada_id) REFERENCES tipus_entrada(tipus_entrada_id) ON DELETE CASCADE
);
