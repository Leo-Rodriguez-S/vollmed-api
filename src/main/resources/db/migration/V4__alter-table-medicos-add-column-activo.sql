ALTER TABLE medicos ADD activo tinyint;
UPDATE medicos SET activo = 1;
ALTER TABLE medicos MODIFY COLUMN activo tinyint NOT NULL;
