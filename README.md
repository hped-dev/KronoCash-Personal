# KronoCash

**Resumen ejecutivo.** Plataforma web académica full-stack para educación financiera, gestión de ingresos y egresos, metas de ahorro, hábitos diarios y simulación patrimonial. Diseñada con accesibilidad universal **WCAG 2.2 AAA** (tres temas — Minimal, Retro y Cyberpunk — en modo claro y oscuro, escala de fuente 100–200 %, OpenDyslexic, reducción de movimiento y asistente por voz **Krono**). Localizada a México: **MXN**, `es-MX` y `America/Mexico_City`. Proyecto de la materia *Gráficos, Interfaces y Usabilidad* (GIU26B), ExpoDIT; alineada a los ODS 4 y 8.

**Stack:** Java 25 · Spring Boot 4.1.2 · Spring Security 7 · Thymeleaf · JPA/Hibernate · PostgreSQL 15 · Flyway · Docker · Lombok.

## Ejecución rápida

1. Cree un archivo `.env` en la raíz con:

   ```env
   DB_NAME=kronocash
   DB_USER=kronocash
   DB_PASSWORD=cambie-este-valor
   SERVER_PORT=2309
   ```

2. Construya y levante los contenedores:

   ```bash
   docker compose up --build
   ```

3. Abra **http://localhost:2309**. PostgreSQL corre en la red interna `kronocash-net` (sin exposición pública; puerto `5877` opcional para desarrollo).

**Pruebas:** `./mvnw test` (requiere Java 25; usa H2 en modo PostgreSQL)

## Documentación

- [Manual de usuario](docs/MANUAL_USUARIO.md) — registro, accesibilidad, comandos de voz, panel financiero y simulador.
- [Documentación técnica](docs/DOCUMENTACION_TECNICA.md) — arquitectura, base de datos, endpoints, seguridad, despliegue y pruebas.

**Requisitos:** Docker con Docker Compose; Java 25 y Maven Wrapper para ejecutar pruebas. Puerto de la aplicación: **2309**.
