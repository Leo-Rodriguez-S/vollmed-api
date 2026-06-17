CREATE TABLE usuarios
(
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    login         VARCHAR(100) NOT NULL UNIQUE,
    contrasena    VARCHAR(255) NOT NULL,

    PRIMARY KEY (id)
);