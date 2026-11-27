# Backend de RectaBags

Requiere Java 17 o compatible, PostgreSQL y una base `rectabags` con su usuario configurado.

## Configuración local

La aplicación necesita `DB_PASSWORD` y `JWT_SECRET`. No tienen valores compartidos por defecto.
Proporcionarlos mediante variables de entorno o copiar
`config/application-local.properties.example` como `config/application-local.properties`
y definir allí ambas propiedades. Ese archivo particular está ignorado por Git.

Para generar una clave JWT aleatoria en PowerShell:

```powershell
[Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(48))
```

Guardar el resultado como `JWT_SECRET` en la configuración particular, sin publicarlo.
`DB_PASSWORD` debe coincidir con la contraseña propia de PostgreSQL.
Revisar también URL y usuario de la base en `src/main/resources/application.properties`.

Desde esta carpeta, ejecutar `./mvnw.cmd spring-boot:run` y abrir
`http://localhost:8080/swagger-ui/index.html`.

Si la base se creó con la versión que exigía ubicación al registrarse,
ejecutar en esa base `sql/migraciones/20261127_usuario_ubicacion_opcional.sql`
desde pgAdmin o psql. La migración permite omitir región, comuna y dirección,
como el registro actual, sin borrar datos ni ampliar columnas. En bases nuevas,
las entidades ya definen estos campos como opcionales.

Para actualizar los máximos de bases anteriores al instructivo, ejecutar
`sql/migraciones/20261127_limites_instructivo.sql`: descripción de producto
hasta 500 caracteres, apellido de usuario/boleta hasta 100 y dirección hasta
300. Las ampliaciones conservan los datos y región/comuna siguen en 30.

## Pruebas

Ejecutar `./mvnw.cmd test` con PostgreSQL disponible y `DB_PASSWORD` configurada.
Los tests de Spring activan el perfil `test` y generan una clave JWT efímera;
no utilizan ni publican la clave del entorno normal.

Si una credencial estuvo versionada, retirarla del archivo actual no la elimina
del historial: debe renovarse. Cambiar la clave JWT invalida los tokens anteriores
y exige iniciar sesión nuevamente. No modificar el historial compartido sin coordinar al equipo.
