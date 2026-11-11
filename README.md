# PlantSync Backend

Guía vigente para publicar: [salida a Render y Netlify](docs/salida-render-netlify.md).

Configuración vigente: Gemini `gemini-flash-latest`, Rabby Wallet, IoT simulado, Render y Netlify. Consultar [guía de configuración y despliegue](docs/configuracion-y-despliegue.md). La clave Gemini se introduce exclusivamente en `.env.local` (perfil local) o Render Environment. Nunca en frontend ni Git.

API Spring Boot / Java 21 para el frontend Vue de PlantSync. Conserva las funciones anteriores e incorpora análisis con Gemini, propuestas de acciones IoT y medallas en Ethereum Sepolia.

## Inicio local

Requisitos: JDK 21 y acceso a Maven Central en la primera ejecución.

```powershell
.\scripts\start-local.ps1
```

El script inicia en `http://127.0.0.1:8080`, usa H2 persistente en `.data/` y genera un secreto JWT temporal si no existe `JWT_SECRET`. Para conservar sesiones entre reinicios, define un secreto propio de al menos 32 bytes en el entorno. La base H2 es solo para desarrollo. Nunca subir `.data`, secretos o logs al repositorio.

En el repositorio frontend, ejecutar `npm ci` y `npm run dev`. Vite redirige `/api/v1` al backend local; `BACKEND_PROXY_TARGET` permite cambiar el destino. Registro público crea un usuario BASIC y su perfil. No existe una contraseña administrativa predeterminada: el alta inicial de administrador requiere `BOOTSTRAP_ADMIN_EMAIL` y `BOOTSTRAP_ADMIN_PASSWORD` (mínimo 16 caracteres), que deben retirarse después.

`.env.example` documenta las variables. El perfil local importa exclusivamente `.env.local`; en Render se configuran mediante Environment o un gestor de secretos. Gemini, dispositivos y Sepolia permanecen deshabilitados sin su configuración. No se sustituyen por respuestas ficticias.

## Verificación y entrega

```powershell
.\mvnw.cmd test
.\mvnw.cmd verify
# Con Docker iniciado: también prueba las migraciones sobre MySQL 8.4 real.
.\mvnw.cmd -Dmysql.it=true clean verify
```

Con el servidor local iniciado, `powershell -File scripts/smoke-local.ps1` comprueba registro, login, identidad, perfil y denegación del directorio global; crea una cuenta descartable local y no imprime credenciales.

Verificación del 11 de noviembre: **90 pruebas backend aprobadas sin omisiones**, incluidas MySQL 8.4, y **85,83 % de cobertura de instrucciones**. `verify` pasa con el umbral original del 80 %. La imagen `plantsync-api:local` se construyó con ese mismo control. Esto habilita la preparación de staging, no certifica integraciones externas ni producción.

El perfil `render` ejecuta las migraciones Flyway V1/V2 sobre una base nueva y valida el esquema con Hibernate. Consultar [migraciones y pruebas](docs/migraciones-mysql.md) antes de conectar una base existente. `scripts/smoke-docker.ps1` verifica el contenedor y la persistencia con MySQL desechable sin cargar secretos externos.

Consultar [contrato de integración vigente](docs/contrato-integracion-vigente.md) para rutas, seguridad, configuración de proveedores, evidencia y pendientes. OpenAPI local: `/v3/api-docs`; Swagger: `/swagger-ui/index.html`. El contrato Solidity y su compilación están en `contracts/` (`npm ci`, `npm run build`). El contrato todavía no está desplegado en Sepolia.
