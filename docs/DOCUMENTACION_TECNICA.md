# Documentación técnica de KronoCash

## 1. Arquitectura y stack

Aplicación web full-stack de renderizado en servidor (SSR), empaquetada en contenedores y orientada a educación financiera accesible (ODS 4 y 8).

| Capa | Tecnología |
|---|---|
| Lenguaje / build | **Java 25**, Maven Wrapper (`./mvnw`), Lombok |
| Framework | **Spring Boot 4.1.2** (`spring-boot-starter-webmvc`) |
| Seguridad | **Spring Security 7** (`spring-boot-starter-security`), sesiones formulario, BCrypt |
| Vista | **Thymeleaf** + `thymeleaf-extras-springsecurity6`, HTML5 semántico, CSS3, JS Vanilla ES6+ |
| Datos | **Spring Data JPA / Hibernate** (`ddl-auto=validate`), **PostgreSQL 15 Alpine**, **Flyway** |
| Validación | Jakarta Validation (`spring-boot-starter-validation`) |
| Pruebas | JUnit 5, Mockito, H2 (`MODE=PostgreSQL`), starters `*-test` |
| Infraestructura | **Docker** multietapa (Temurin 25 JDK/JRE Alpine), Docker Compose, `springboot4-dotenv` |

Paquete raíz: `com.GIU26B.KronoCash`, con los paquetes `config`, `controller`, `dto`, `model`, `repository` y `service`. La clase de arranque `KronoCashApplication` usa `@EnableScheduling` y fija la zona horaria por defecto a `America/Mexico_City` en un `@PostConstruct`.

El fragmento `fragments/head.html` inyecta los metadatos CSRF (`_csrf`, `_csrf_header`), carga `base.css` y las tres skins, aplica preferencias cognitivas desde `localStorage` antes del `body` y carga los scripts con `defer`. `PreferencesControllerAdvice` expone `theme`, `darkMode`, `reduceMotion`, `fontScale`, `authenticated` y `currentPath` en **todas** las vistas, de modo que `<html data-theme=...>` se renderiza en servidor sin parpadeo.

## 2. Modelo entidad-relación y diccionario de datos

`users` es la entidad raíz. Relaciones: `users 1—1 user_preferences` (PK/FK compartida, `@MapsId`, `ON DELETE CASCADE`); `users 1—N transactions`, `savings_goals`, `habits`, `simulation_scenarios`, `achievements` (todas con `ON DELETE CASCADE`).

**V1 — `V1__users_and_preferences.sql`**

| Tabla | Columna | Tipo / restricción |
|---|---|---|
| `users` | `id`, `username`, `email`, `password_hash`, `created_at` | `BIGSERIAL PK`; `VARCHAR(50) UNIQUE NOT NULL`; `VARCHAR(100) UNIQUE NOT NULL`; `VARCHAR(255) NOT NULL`; `TIMESTAMP DEFAULT CURRENT_TIMESTAMP` |
| `user_preferences` | `user_id`, `theme_style`, `dark_mode`, `reduce_motion`, `font_scale` | `BIGINT PK REFERENCES users(id)`; `VARCHAR(20) DEFAULT 'minimal'` con `CHECK IN ('retro','minimal','cyberpunk')`; `BOOLEAN DEFAULT FALSE`; `BOOLEAN DEFAULT FALSE`; `INTEGER DEFAULT 100` con `CHECK BETWEEN 100 AND 200` |

El enum `ThemeStyle` y el conversor `ThemeStyleConverter` persisten los valores en minúsculas para cumplir el `CHECK`. `password_hash` lleva `@JsonIgnore` y se excluye de `toString()`.

**V2 — `V2__transactions_and_habits.sql`**

| Tabla | Columnas relevantes |
|---|---|
| `transactions` | `id BIGSERIAL PK`, `user_id FK`, `type VARCHAR(10) CHECK IN ('INCOME','EXPENSE')`, `category VARCHAR(50)`, `amount NUMERIC(12,2) CHECK (>0)`, `description VARCHAR(255)`, `transaction_date DATE DEFAULT CURRENT_DATE` |
| `savings_goals` | `id`, `user_id FK`, `title VARCHAR(100)`, `target_amount NUMERIC(12,2) CHECK (>0)`, `current_amount NUMERIC(12,2) DEFAULT 0`, `deadline DATE` |
| `habits` | `id`, `user_id FK`, `title VARCHAR(100)`, `frequency VARCHAR(20) DEFAULT 'DAILY'`, `completed_today BOOLEAN DEFAULT FALSE`, `streak_count INTEGER DEFAULT 0` |

Índices creados: `idx_transactions_user_id`, `idx_transactions_date`, `idx_transactions_user_date` (compuesto), `idx_savings_goals_user_id`, `idx_habits_user_id`.

**V3 — `V3__simulation_scenarios.sql`**

| Tabla | Columnas relevantes |
|---|---|
| `simulation_scenarios` | `id`, `user_id FK`, `scenario_name VARCHAR(100)`, `monthly_income NUMERIC(12,2)`, `savings_percentage NUMERIC(5,2) CHECK (0–100)`, `ant_expenses_cut NUMERIC(12,2) DEFAULT 0`, `months_horizon INTEGER CHECK (1–120)`, `projected_total NUMERIC(14,2)`, `created_at TIMESTAMP` |
| `achievements` | `id`, `user_id FK`, `code VARCHAR(50)`, `title VARCHAR(100)`, `unlocked_at TIMESTAMP` |

**V4 — `V4__optimistic_locking.sql`**: añade `version INTEGER NOT NULL DEFAULT 0` a `transactions`, `savings_goals` y `habits`; las entidades lo mapean con `@Version`.

## 3. Controladores y endpoints

| Controlador | Rutas |
|---|---|
| `HomeController` | `GET /` → `index` (selector de tema para invitados) |
| `AuthController` | `GET /login`, `GET /register`, `POST /register` (valida duplicados, redirige a `/login?registered`) |
| `SettingsController` | `GET /settings`, `POST /settings/preferences` → `redirect:/settings?saved` |
| `DashboardController` | `GET /dashboard` (filtros `type`, `category`, `from`, `to`); `POST /dashboard/transactions`, `/dashboard/transactions/{id}/delete`, `/dashboard/goals`, `/dashboard/goals/{id}/deposit`, `/withdraw`, `/delete`, `/dashboard/habits`, `/dashboard/habits/{id}/complete`, `/{id}/undo`; `GET /api/dashboard/transactions`, `GET /api/dashboard/summary` |
| `SimulatorController` | `GET /simulator` (`load`, `edit`); `POST /simulator/calculate`, `/simulator/save`, `/api/simulator/calculate`, `/api/simulator/save`, `/simulator/scenarios/{id}/edit`, `/{id}/delete` |
| `PreferencesApiController` | `POST /api/preferences` (JSON, CSRF activo; guarda en BD si hay usuario o en sesión HTTP si es invitado) |

Los formularios `POST /simulator/calculate` y `/simulator/save` son el fallback sin JavaScript; el resto usa `fetch` con el token CSRF leído de `<meta>`. `SecurityConfig` permite `/`, `/css/**`, `/js/**`, `/img/**`, `/register`, `/login`, `/error` y `/api/preferences`; `anyRequest().authenticated()`.

## 4. Servicios

- **`UserService`**: registro con BCrypt y fila de preferencias por defecto en la misma transacción; `getOrCreatePreferences`, `updatePreferences`, `getPreferencesSnapshot`.
- **`FinanceService`**: `getSummary` (balance total, available = total − apartado, totales mensuales, progreso de metas), `searchTransactions` con `Specification`/JPA Criteria (sin concatenación SQL; escape de `%`, `_` y `\`), `addTransaction` (rechaza egresos sobre saldo disponible y fechas futuras), `deleteTransaction` (impide eliminar ingresos que dejen saldo negativo), `findActiveGoals`.
- **`SavingsGoalService`**: `totalBalance`, `totalSavedInGoals`, `availableBalance`, `deposit`, `withdraw`, `delete`; valida montos `> 0` y propiedad del recurso.
- **`HabitService`**: alta, `completeHabit` y `undoCompletion` idempotentes por día, racha que no baja de cero y reinicio programado `@Scheduled(cron = "0 0 0 * * *", zone = "America/Mexico_City")`.
- **`SimulatorService`**: `calculate` (proyección lineal con `BigDecimal` y `RoundingMode.HALF_UP`), `validate` (defensa en profundidad: ingreso 0–25,000, porcentaje 0–100, horizonte 1–120), `save`, `update`, `findById`, `findByUser`, `delete`.
- **Auxiliares**: `AuthenticationHelper`, `GuestPreferencesService` (preferencias de invitado en sesión), `CustomUserDetailsService` (acepta usuario o correo, sin CAPTCHA), `InsufficientBalanceException`.

## 5. Arquitectura de temas y WCAG 2.2 AAA

`base.css` define los tokens abstractos (`--bg-primary`, `--bg-secondary`, `--text-main`, `--text-muted`, `--accent-color`, `--accent-hover`, `--error-color`, `--border-color`, `--border-radius`, `--font-family`, `--font-scale`, etc.). Las skins `theme-minimal.css`, `theme-retro.css` y `theme-cyberpunk.css` los sobrescriben mediante selectores `html[data-theme="..."][data-dark-mode="true|false"]`, cubriendo **6 variantes** (3 temas × claro/oscuro). Los componentes nunca usan colores hardcodeados.

Medidas verificadas: contraste **≥ 7:1** (ratios documentados en los archivos de tema), foco visible de **3 px** con `outline-offset`, objetivos táctiles `.touch-target` de **2.75 rem ≈ 44 px**, `clamp()` y unidades relativas para reflow al 200–400 %, `@media (prefers-reduced-motion: reduce)` y `html[data-reduce-motion="true"]` para detener animaciones, fuente **OpenDyslexic** (`fonts.cdnfonts.com`) con `data-dyslexic-font`, `data-wide-spacing`, `data-underline-links` y anillo de cursor `data-cursor-ring`. Toda gráfica SVG lleva una `<table>` sincronizada (`<th scope="col">`), y los formularios usan `<label for>` visible, `aria-live`, `aria-invalid` y enlace de salto al contenido principal.

## 6. Seguridad implementada

- **CSRF activo** en formularios y en `/api/**`; `head.html` expone `_csrf` / `_csrf_header` y `accessibility.js` envía la cabecera en cada `fetch`.
- **BCryptPasswordEncoder**; contraseñas nunca en logs ni en DTOs de salida (`ProfileDTO` solo expone usuario, correo y fecha).
- **Control de propietario (anti-IDOR)**: `findOwned` en metas, hábitos y escenarios, y filtro `user.id == userId` en movimientos.
- **`@Version`** para control optimista de concurrencia (migración V4).
- **Validación DTO** en backend (`@Valid`, `@DecimalMin`, `@DecimalMax`, `@PastOrPresent`, `@Pattern`).
- **Cabeceras HTTP**: CSP (con `frame-ancestors 'none'`), HSTS (`maxAgeInSeconds=31536000`, `includeSubDomains`), `X-Frame-Options: DENY`, `X-Content-Type-Options`.
- **Cookies** de sesión `HttpOnly` y `SameSite=Lax`; `server.error.include-stacktrace=never` e `include-message=never`.
- **Secretos** externalizados en `.env` (`DB_NAME`, `DB_USER`, `DB_PASSWORD`, `SERVER_PORT`), ignorado por Git. JPA con `ddl-auto=validate` y consultas parametrizadas (`@Param`, Specifications).

## 7. Localización

`es-MX` en la interfaz y en la voz de Krono (`SpeechRecognition`/`speechSynthesis` con `lang='es-MX'`). Moneda **MXN** con formato `#numbers.formatDecimal` en el historial. Zona horaria `America/Mexico_City` en JVM, Jackson, Hibernate/JDBC, contenedor y tareas programadas.

## 8. Despliegue, pruebas y auditoría QA

**Requisitos:** Docker con Compose; Java 25 y Maven Wrapper para ejecutar pruebas localmente.

**Puesta en marcha:** crear `.env` con `DB_NAME`, `DB_USER`, `DB_PASSWORD` y `SERVER_PORT=2309`; ejecutar `docker compose up --build`. La app queda en `http://localhost:2309` (mapeo `2309:2309`); PostgreSQL corre en la red interna `kronocash-net` (puerto `5877` opcional sólo para desarrollo). El `Dockerfile` compila con `./mvnw package` (omite tests), usa JRE Alpine, usuario sin privilegios y `TZ=America/Mexico_City`. Si se requiere un respaldo de configuración, puede ubicarse en `C:\Users\hpede\AppData\Local\Temp\opencode`.

**Pruebas:** `./mvnw test` con perfil `test` (H2 en modo PostgreSQL, Flyway desactivado). **11 pruebas** en 4 clases: `KronoCashApplicationTests` (1, carga de contexto), `FinanceServiceTest` (3: egreso con saldo reservado, fecha futura, borrado de ingreso que deja saldo negativo), `SavingsGoalServiceTest` (4: depósito sobre saldo, retiro sobre acumulado, acceso a meta ajena, depósito válido), `SimulatorServiceTest` (3: ingreso máximo 25,000 → 60,600.00, rechazo por encima del máximo, recorte negativo).

**Auditoría QA aplicada:** bajo el flujo human-in-the-loop de `AGENTS.md`, se auditaron accesibilidad, navegación y responsividad, seguridad (SQLi, IDOR, CSRF, secretos) e integridad financiera. Las correcciones aprobadas quedan reflejadas en las secciones 5 y 6.
