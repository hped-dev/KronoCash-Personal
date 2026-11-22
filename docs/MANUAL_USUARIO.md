# Manual de usuario de KronoCash

KronoCash es una plataforma web de educación financiera, hábitos y simulación patrimonial. Todos los importes se muestran en pesos mexicanos (**MXN**), la interfaz está en **español de México** y las fechas usan la zona horaria **America/Mexico_City**. El sitio cumple **WCAG 2.2 nivel AAA**: puede usar solo el teclado, ampliar la letra hasta 200 %, cambiar de tema y navegar sin barreras.

## 1. Acceder a la plataforma

### Crear una cuenta
En la página de inicio (`/`) pulse **Crear cuenta gratis**, o vaya a `/register`. Llene tres campos:

- **Nombre de usuario**: de 3 a 50 caracteres.
- **Correo electrónico**: un correo válido (máximo 100 caracteres).
- **Contraseña**: de 8 a 128 caracteres.

**No hay CAPTCHA** ni pruebas cognitivas: puede pegar y usar su gestor de contraseñas (`autocomplete="username"` y `autocomplete="new-password"`). No se pide «confirmar contraseña». La casilla **Mostrar contraseña** cambia el campo a texto; al salir, Krono la oculta automáticamente. Si el usuario o correo ya existen, se le indicará en el propio campo.

### Iniciar sesión
En `/login` escriba su **nombre de usuario o correo electrónico** y su contraseña (`autocomplete="current-password"`). También puede usar **Mostrar contraseña**. Pulse **Entrar**. Si algo falla aparece el aviso «El usuario o la contraseña no son correctos». Para terminar, use **Cerrar sesión** en la barra superior.

> **Privacidad de su contraseña:** mientras escribe en un campo de contraseña, Krono **cierra los ojos** y muestra el mensaje «Recuerda que tu contraseña debe ser secreta, no la compartas con nadie». No habla ni repite su contraseña en voz alta. Al terminar, vuelve a abrir los ojos y oculta la clave.

## 2. Widget flotante de accesibilidad

Está en la **esquina inferior izquierda**, con el botón **Accesibilidad**. Ábralo con `Enter` o `Espacio`; el panel recibe el foco automáticamente y **Escape** lo cierra devolviendo el foco al botón. Los cambios se aplican al instante:

| Control | Qué hace |
|---|---|
| **Tema visual** | Elige Retro, Minimal o Cyberpunk. |
| **Modo oscuro** | Alterna entre modo claro y oscuro. |
| **Escala de fuente** | Botones `−`, `+` y `100%`; va de 100 % a 200 % en pasos de 25 %. |
| **Fuente OpenDyslexic** | Tipografía de lectura fácil. |
| **Espaciado de lectura ampliado** | Más interlineado y separación entre palabras. |
| **Reducir movimiento** | Detiene transiciones y animaciones de la mascota. |
| **Resaltar enlaces** | Subrayado permanente en todos los enlaces. |
| **Resaltar puntero del mouse** | Muestra un anillo guía alrededor del cursor (se desactiva con movimiento reducido). |

Cada cambio se anuncia en un mensaje accesible (`aria-live`) y se guarda: en la cuenta si inició sesión, o en este dispositivo si aún no. En **Ajustes** (`/settings`) puede guardar tema, modo claro/oscuro, movimiento y escala directamente en su perfil.

## 3. Krono, el asistente por voz

Krono está a la **derecha** de la pantalla. El botón **Micrófono** activa/desactiva la escucha y el botón **Voz** silencia o reactiva la lectura en voz alta. Trabaja siempre en **es-MX**. Diga una frase y espere un momento: Krono la evalúa y responde por voz y en pantalla. Si el navegador no tiene micrófono o reconocimiento de voz, los mensajes siguen disponibles como texto.

**Navegación:** «iniciar sesión» / «entrar», «registro» / «crear cuenta», «panel» / «finanzas», «simulador» / «proyección», «ajustes» / «configuración» / «perfil», «inicio», «cerrar sesión» / «salir».

**Temas y apariencia:** «tema retro», «tema minimal», «tema cyberpunk», «modo oscuro», «modo claro», «cambiar modo».

**Accesibilidad:** «leer pantalla», «aumentar letra», «reducir letra», «letra normal», «fuente dislexia», «resaltar puntero», «reducir movimiento», «activar animaciones».

**Otros:** «cambiar voz» / «otra voz», «quién eres», «ayuda» / «comandos» y «minimizar» (para volver a mostrarlo, diga «quién eres»).

## 4. Panel financiero (`/dashboard`)

**Resumen:** muestra balance total, total apartado en metas, **balance disponible** (total menos lo apartado), ingresos y egresos del mes, resultado mensual, número de movimientos y periodo. El resultado negativo se marca como «Déficit mensual».

**Registrar un ingreso o gasto:** elija **Ingreso** o **Egreso**, escriba categoría, importe (mayor que cero), fecha (no puede ser futura) y descripción opcional. El sistema valida el saldo: un egreso que supere el dinero disponible se rechaza con una notificación. Apartar dinero en una meta **no** cuenta como gasto.

**Filtrar historial:** use los campos **Tipo**, **Categoría** (búsqueda por texto), **Desde** y **Hasta**, y pulse **Aplicar filtros**; con **Ver todos los movimientos** se limpian. El historial es una tabla accesible con encabezados, importes en formato `$1,234.50 MXN` y botón **Eliminar**. El número de resultados se anuncia automáticamente.

**Metas de ahorro:** cree una con título, importe objetivo y fecha límite opcional. Cada meta muestra cuánto lleva, el porcentaje completado y lo que resta. Con **+ Añadir** y **− Retirar** puede mover fondos (se valida que el importe sea positivo, que no supere el saldo disponible al añadir ni lo acumulado al retirar). **Eliminar meta** la borra.

**Hábitos diarios:** cree hábitos con título y frecuencia (Diaria, Semanal o Mensual). Pulse **Completar** para registrar el día; la **racha** sube una sola vez al día. **Desmarcar** revierte el registro y descuenta la racha. Cada medianoche (hora de México) se preparan los hábitos para el nuevo día.

## 5. Simulador financiero (`/simulator`)

Ajuste los parámetros con el **deslizador** o escriba el valor exacto en el campo numérico vinculado:

- **Ingreso mensual**: 0 a 25,000 MXN.
- **Porcentaje de ahorro**: 0 a 100 %.
- **Recorte de gastos hormiga**: ahorro extra mensual.
- **Horizonte en meses**: 1 a 120.
- **Meta de ahorro (opcional):** al elegir una meta activa se calcula el tiempo necesario para alcanzarla y se desactiva el horizonte manual.

Pulse **Calcular proyección**. La proyección es lineal: ahorro base mensual = ingreso × porcentaje ÷ 100; se suma el recorte de gastos y se acumula mes con mes (sin interés compuesto). Verá el ahorro base, el extra, el total acumulado y el tiempo estimado. La **gráfica de crecimiento** va acompañada de una **tabla con exactamente los mismos datos mes a mes**, para lectores de pantalla.

Con **Guardar escenario** lo conserva en su cuenta; en **Escenarios guardados** puede **Cargar**, **Editar** o **Eliminar** cada uno. Todo el simulador funciona también sin JavaScript.

## 6. Ajustes del perfil (`/settings`)

Consulte su **nombre de usuario**, **correo** y **miembro desde**. En «Preferencias visuales» puede cambiar tema, modo oscuro, reducción de movimiento y escala de fuente (100 %, 125 %, 150 %, 175 %, 200 %) y pulsar **Guardar preferencias**. Las opciones cognitivas (OpenDyslexic, espaciado, enlaces subrayados) se controlan desde el widget y se guardan en su dispositivo.
