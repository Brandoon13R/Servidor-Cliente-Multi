Create Table Usuarios(
	Nombre VARCHAR(100),
	Usuario VARCHAR(100),
	Contrasena VARCHAR(10)
);

CREATE TABLE bloqueados (
    blocker_username VARCHAR(100) REFERENCES Usuarios(Usuario) ON DELETE CASCADE,
    blocked_username VARCHAR(100) REFERENCES Usuarios(Usuario) ON DELETE CASCADE,
    PRIMARY KEY (blocker_username, blocked_username)
);

ALTER TABLE Usuarios
ADD PRIMARY KEY (Usuario);

Delete Table usuarios;