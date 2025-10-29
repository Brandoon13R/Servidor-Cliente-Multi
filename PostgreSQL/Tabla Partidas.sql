CREATE TABLE Partidas (
    partida_id SERIAL PRIMARY KEY,
    jugador1_usr VARCHAR(100) REFERENCES Usuarios(Usuario),
    jugador2_usr VARCHAR(100) REFERENCES Usuarios(Usuario),
    ganador_usr VARCHAR(100) REFERENCES Usuarios(Usuario) 
);