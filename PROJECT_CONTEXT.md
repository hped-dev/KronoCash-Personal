# Contexto del Proyecto: KronoCash (AI Agent Reference)

Este documento define las reglas técnicas, la arquitectura, las restricciones de accesibilidad y las convenciones del proyecto **KronoCash** para su uso como contexto base por agentes de desarrollo asistido por IA en terminal.

## 1. Descripción General

* **Nombre del Proyecto:** KronoCash

* **Naturaleza:** Plataforma web académica full-stack para educación financiera, gestión de hábitos y simulación patrimonial.

* **Materia:** Gráficos, Interfaces y Usabilidad (Ingeniería en Ciencias Computacionales).

* **Alineación ODS:** ODS 4 (Educación de Calidad) y ODS 8 (Trabajo Decente y Crecimiento Económico).

* **Restricción Crítica:** Cumplimiento nativo y estricto de las pautas de accesibilidad **WCAG 2.2 Nivel AAA**.

## 2. Stack Tecnológico e Infraestructura

* **Lenguaje:** Java 25

* **Framework Principal:** Spring Boot 4.1.2

* **Motor de Plantillas (Frontend):** Thymeleaf (Server-Side Rendering) + HTML5 Semántico + CSS3 nativo + JavaScript Vanilla (ES6+). *No se utiliza React, Angular ni Vue.*

* **Seguridad:** Spring Security (autenticación basada en sesiones/formularios compatible con gestores de contraseñas).

* **Persistencia:** Spring Data JPA / Hibernate (`ddl-auto=validate`).

* **Base de Datos:** PostgreSQL 15 Alpine.

* **Migraciones SQL:** Flyway (`src/main/resources/db/migration/`).

* **Contenerización:** Docker & Docker Compose (compilación multietapa con Maven Wrapper).

### Configuración de Red y Puertos (Seguridad)

* **Puerto de la Aplicación (Host -> Contenedor):** `2309:8080` (Acceso local en `http://localhost:2309`).

* **Puerto de PostgreSQL:** Blindado en la red interna `kronocash-net` (sin exposición pública hacia el host por defecto; puerto alternativo configurado para desarrollo si se requiere: `5877`).

* **Gestión de Secretos:** Mediante archivo `.env` en la raíz (ignorado en Git). Variables: `DB_NAME`, `DB_USER`, `DB_PASSWORD`, `SERVER_PORT`.

## 3. Estructura de Directorios y Paquetes

```
KronoCash/
├── Dockerfile
├── docker-compose.yml
├── .env                                    # Secretos locales (no versionado)
├── pom.xml
└── src/main/
    ├── java/com/GIU26B/KronoCash/
    │   ├── KronoCashApplication.java
    │   ├── config/                         # SecurityConfig.java (Spring Security + WCAG auth)
    │   ├── controller/                     # Controladores MVC (@Controller) y REST (@RestController)
    │   ├── dto/                            # Data Transfer Objects para formularios y simulaciones
    │   ├── model/                          # Entidades JPA (@Entity)
    │   ├── repository/                     # Interfaces JpaRepository
    │   └── service/                        # Lógica de negocio y proyecciones matemáticas
    └── resources/
        ├── application.properties
        ├── db/migration/                   # Scripts Flyway estrictamente versionados
        │   ├── V1__users_and_preferences.sql
        │   ├── V2__transactions_and_habits.sql
        │   └── V3__simulation_scenarios.sql
        ├── static/
        │   ├── css/
        │   │   ├── base.css                # Reset, tipografía fluida, foco 3px y variables abstractas
        │   │   ├── theme-retro.css         # Skin 1: Retro-Gaming / Pixel Art
        │   │   ├── theme-minimal.css       # Skin 2: Minimalista Editorial
        │   │   └── theme-cyberpunk.css     # Skin 3: Neón / Cyberpunk Accesible
        │   ├── js/
        │   │   ├── accessibility.js        # Cambio dinámico de tema, escala de fuente y motion
        │   │   ├── simulator.js            # Lógica de sliders, gráficas y tablas sincronizadas
        │   │   └── mascot-voice.js         # Web Speech API (SpeechRecognition + speechSynthesis)
        │   └── img/
        └── templates/
            ├── fragments/                  # Fragmentos Thymeleaf compartidos (th:fragment)
            │   ├── head.html
            │   ├── navbar.html
            │   └── mascot.html
            ├── index.html
            └── public/
                ├── login.html
                ├── register.html
                ├── dashboard.html
                ├── simulator.html
                └── settings.html

```

## 4. Arquitectura de Estilos y Temas (CSS Custom Properties)

**Regla obligatoria para el agente:** Nunca utilizar colores hexadecimales, RGB o fuentes hardcodeadas dentro de los componentes HTML o reglas específicas de vistas. Todo estilo debe consumir variables CSS definidas en `base.css`:

* `--bg-primary`, `--bg-secondary`

* `--text-main`, `--text-muted`

* `--accent-color`, `--accent-hover`

* `--border-radius`, `--border-color`

* `--font-family`

El cambio de tema se controla mediante el atributo `data-theme` en la etiqueta raíz (`<html data-theme="retro|minimal|cyberpunk">`), persistido en la tabla `user_preferences` de PostgreSQL.

## 5. Reglas Inquebrantables de Accesibilidad (WCAG 2.2 Nivel AAA)

Cualquier código HTML, CSS o JavaScript generado por el agente **DEBE** cumplir estrictamente con estos criterios:

1. **Contraste de Color (Criterio 1.4.6):** Relación de contraste mínima de **7:1** entre texto normal y fondo en los 3 temas y modos (claro/oscuro).

2. **Navegación por Teclado y Foco (Criterios 2.1.1, 2.1.2, 2.4.11, 2.4.13):**

   * Cero trampas de teclado. Orden de tabulación lógico.

   * Todos los elementos interactivos (`a`, `button`, `input`, `select`, sliders) deben tener un indicador de foco visible con contraste alto y grosor mínimo de `3px` (`outline: 3px solid var(--accent-color); outline-offset: 2px;`).

3. **Reducción de Movimiento (Criterio 2.3.3):**

   * Respetar `@media (prefers-reduced-motion: reduce)`.

   * Soportar un atributo global (`data-reduce-motion="true"`) que detenga todas las transiciones CSS y las animaciones de la mascota asistente.

4. **Escalabilidad y Reflow (Criterios 1.4.4, 1.4.10):**

   * Uso de unidades relativas (`rem`, `em`, `%`, `clamp()`). Prohibido usar `px` fijos para tamaños de fuente o contenedores rígidos.

   * Debe soportar zoom del **200% al 400%** sin scroll horizontal ni superposición de textos.

5. **Alternativas Textuales a Gráficas (Criterio 1.1.1):**

   * Toda gráfica visual generada en el Simulador Financiero debe contar con una tabla HTML semántica (`<table>`, `<thead>`, `<th scope="col">`) sincronizada en tiempo real con los mismos datos.

6. **Autenticación Accesible (Criterio 3.3.8):**

   * No bloquear el pegado (`paste`) ni el autocompletado de gestores de contraseñas en formularios de login/registro.

   * Todos los inputs deben estar vinculados explícitamente a un `<label for="...">` visible.

## 6. Módulos y División de Responsabilidades (Equipo de 3)

* **Módulo 1 (Identidad, Skins y Accesibilidad):** `V1__users_and_preferences.sql`, `SecurityConfig`, registro/login, `base.css`, los 3 archivos `theme-*.css`, `fragments/head.html`, `fragments/navbar.html` y `settings.html`.

* **Módulo 2 (Gestión Financiera y Hábitos):** `V2__transactions_and_habits.sql`, entidades/servicios de ingresos, egresos, metas de ahorro y hábitos diarios, `dashboard.html` y tablas filtrables de historial.

* **Módulo 3 (Simulador y Mascota IA):** `V3__simulation_scenarios.sql`, motor de proyección financiera en `SimulatorService`, endpoints REST (`/api/simulator/**`), `simulator.html` (sliders + gráfica + tabla accesible) y `mascot-voice.js` (Web Speech API).

## 7. Instrucciones de Comportamiento para el Agente IA

1. **Respeto a Flyway:** Nunca modificar un archivo de migración SQL (`V1`, `V2`, `V3`) que ya haya sido ejecutado si rompe el checksum; si se requieren cambios de esquema en desarrollo, coordinar la actualización del script asignado al módulo correspondiente.

2. **Validación de Dependencias:** Utilizar anotaciones de **Lombok** (`@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@RequiredArgsConstructor`) en entidades, DTOs y servicios para mantener el código limpio.

3. **Thymeleaf Estándar:** Usar expresiones estándar de Thymeleaf (`th:text`, `th:href`, `th:object`, `th:field`, `th:replace`) asegurando que los atributos ARIA (`aria-label`, `aria-live`, `aria-describedby`) se mantengan íntegros en el HTML renderizado.

4. **Comandos de Ejecución:** El entorno se levanta y prueba exclusivamente mediante `docker compose up --build`.