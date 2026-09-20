# Servicio de compilación

API Spring Boot 3.3.0 y Java 17. Envía código a Judge0, compara la salida con casos de prueba y escucha en el puerto `8083`. No usa base de datos.

## Configuración y ejecución

Se requieren Java 17, Maven 3.9 o el wrapper y acceso a Judge0. `judge0.url` apunta por defecto a `https://judge0-ce.p.rapidapi.com`. Configure `JUDGE0_API_KEY` para esa URL; sin ella el cliente no envía cabeceras de RapidAPI. El cliente utiliza `/submissions?base64_encoded=false&wait=true`.

Desde este directorio: `./mvnw spring-boot:run` y `./mvnw test`; en Windows use `mvnw.cmd`.

## API

| Método | Ruta | Función |
| --- | --- | --- |
| POST | `/api/compiler/run` | Ejecuta código con entrada estándar |
| POST | `/api/compiler/execute` | Compara casos de prueba |
| GET | `/api/compiler/languages` | Java, JavaScript y Python |

Swagger: `http://localhost:8083/swagger-ui.html`; OpenAPI: `http://localhost:8083/v3/api-docs`.

Mngr llama a `/api/compiler/execute` al calificar código. La API no configura autenticación propia; restrinja el acceso antes de publicarla.
