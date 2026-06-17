-- Objetivo:
-- 1. Ajustar tamaños de columnas
-- 2. Garantizar integridad mediante constraints (CHECK, UNIQUE)

-- ============================================================
-- 1. MODIFICACIÓN DE COLUMNAS
-- ============================================================
ALTER TABLE pacientes
    MODIFY codigo_postal VARCHAR(5) NOT NULL, -- 5 dígitos exactos
    MODIFY estado VARCHAR(100) NOT NULL, -- cambio de CHAR a VARCHAR
    MODIFY telefono VARCHAR(8) NOT NULL; -- 8 dígitos exactos


-- ============================================================
-- 2. CONSTRAINTS DE INTEGRIDAD
-- ============================================================
-- ------------------------------------------------------------
-- 2.1 NOMBRE
-- ------------------------------------------------------------
-- Solo letras (incluye tildes y espacios)
ALTER TABLE pacientes
    ADD CONSTRAINT chk_pacientes_nombre_formato
        CHECK (nombre REGEXP '^[A-Za-zÁÉÍÓÚáéíóúÑñ ]+$');

-- ------------------------------------------------------------
-- 2.2 DOCUMENTO
-- ------------------------------------------------------------
ALTER TABLE pacientes
    ADD CONSTRAINT chk_pacientes_documento_formato
        CHECK (documento REGEXP '^[0-9]{9,12}$');

-- ------------------------------------------------------------
-- 2.3 CÓDIGO POSTAL
-- ------------------------------------------------------------
ALTER TABLE pacientes
    ADD CONSTRAINT chk_pacientes_codigo_postal_formato
        CHECK (codigo_postal REGEXP '^[0-9]{5}$');

-- ------------------------------------------------------------
-- 2.4 ESTADO
-- ------------------------------------------------------------
ALTER TABLE pacientes
    ADD CONSTRAINT chk_pacientes_estado_formato
        CHECK (estado REGEXP '^[A-Za-zÁÉÍÓÚáéíóúÑñ ]+$');

-- ------------------------------------------------------------
-- 2.5 CIUDAD
-- ------------------------------------------------------------
ALTER TABLE pacientes
    ADD CONSTRAINT chk_pacientes_ciudad_formato
        CHECK (ciudad REGEXP '^[A-Za-zÁÉÍÓÚáéíóúÑñ ]+$');

-- ------------------------------------------------------------
-- 2.6 TELÉFONO
-- ------------------------------------------------------------
-- Evitar duplicados
ALTER TABLE pacientes
    ADD CONSTRAINT uk_pacientes_telefono UNIQUE (telefono);

-- Validar formato: exactamente 8 dígitos numéricos
ALTER TABLE pacientes
    ADD CONSTRAINT chk_pacientes_telefono_formato
        CHECK (telefono REGEXP '^[0-9]{8}$');


-- ============================================================
-- 3. EVITAR GUARDAR CAMPOS VACÍOS
-- ============================================================
ALTER TABLE pacientes
    ADD CONSTRAINT chk_pacientes_nombre_no_vacio
        CHECK (TRIM(nombre) <> '');

ALTER TABLE pacientes
    ADD CONSTRAINT chk_pacientes_email_no_vacio
        CHECK (TRIM(email) <> '');

ALTER TABLE pacientes
    ADD CONSTRAINT chk_pacientes_documento_no_vacio
        CHECK (TRIM(documento) <> '');

ALTER TABLE pacientes
    ADD CONSTRAINT chk_pacientes_calle_no_vacio
        CHECK (TRIM(calle) <> '');

ALTER TABLE pacientes
    ADD CONSTRAINT chk_pacientes_barrio_no_vacio
        CHECK (TRIM(barrio) <> '');

ALTER TABLE pacientes
    ADD CONSTRAINT chk_pacientes_codigo_postal_no_vacio
        CHECK (TRIM(codigo_postal) <> '');

ALTER TABLE pacientes
    ADD CONSTRAINT chk_pacientes_estado_no_vacio
        CHECK (TRIM(estado) <> '');

ALTER TABLE pacientes
    ADD CONSTRAINT chk_pacientes_ciudad_no_vacio
        CHECK (TRIM(ciudad) <> '');

ALTER TABLE pacientes
    ADD CONSTRAINT chk_pacientes_telefono_no_vacio
        CHECK (TRIM(telefono) <> '');