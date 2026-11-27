-- Ampliaciones autorizadas según el instructivo. No modifica los datos
-- existentes, la nulabilidad ni los límites de región y comuna.
BEGIN;

ALTER TABLE public.producto
    ALTER COLUMN descripcion TYPE varchar(500);

ALTER TABLE public.usuario
    ALTER COLUMN apellido TYPE varchar(100),
    ALTER COLUMN direccion TYPE varchar(300);

ALTER TABLE public.boleta
    ALTER COLUMN apellido_cliente TYPE varchar(100),
    ALTER COLUMN direccion_cliente TYPE varchar(300);

COMMIT;
