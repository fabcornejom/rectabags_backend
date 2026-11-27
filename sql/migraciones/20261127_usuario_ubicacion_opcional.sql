-- Bases existentes: alinear la ubicación opcional del usuario con el contrato
-- actual de registro. No modifica datos ni los límites de las columnas.
BEGIN;

ALTER TABLE public.usuario
    ALTER COLUMN region DROP NOT NULL,
    ALTER COLUMN comuna DROP NOT NULL,
    ALTER COLUMN direccion DROP NOT NULL;

COMMIT;
