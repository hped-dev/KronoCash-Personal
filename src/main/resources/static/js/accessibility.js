/**
 * KronoCash - accessibility.js (JavaScript Vanilla, sin frameworks)
 *
 * Aplica en tiempo real: tema visual, modo claro/oscuro, reducción de
 * movimiento y escala base de fuente (100%-200%). Sincroniza cada cambio con
 * el backend mediante fetch a POST /api/preferences:
 *   - sesión HTTP cuando aún no hay login (primera visita);
 *   - user_preferences (PostgreSQL) cuando hay usuario autenticado.
 *
 * El estado inicial viene del servidor en <html data-theme data-dark-mode
 * data-reduce-motion data-font-scale>, por lo que no hay "flash" de tema.
 * Los colores NUNCA se manipulan aquí: solo se escriben atributos y la
 * variable --font-scale; las skins CSS resuelven el resto.
 */
(function () {
    'use strict';

    var root = document.documentElement;
    var endpoint = root.dataset.preferencesEndpoint || '/api/preferences';
    var STORAGE_KEY = 'kronocash.accessibility.v1';
    var VALID_THEMES = ['retro', 'minimal', 'cyberpunk'];
    var MIN_SCALE = 100;
    var MAX_SCALE = 200;
    var syncTimer = null;

    var serverState = {
        theme: root.getAttribute('data-theme'),
        darkMode: root.getAttribute('data-dark-mode'),
        reduceMotion: root.getAttribute('data-reduce-motion'),
        fontScale: root.getAttribute('data-font-scale') || root.style.getPropertyValue('--font-scale'),
        dyslexicFont: root.getAttribute('data-dyslexic-font'),
        wideSpacing: root.getAttribute('data-wide-spacing'),
        underlineLinks: root.getAttribute('data-underline-links'),
        cursorRing: root.getAttribute('data-cursor-ring')
    };

    var state = {
        theme: sanitizeTheme(serverState.theme || fromCache('theme') || 'minimal'),
        darkMode: parseBoolean(serverState.darkMode, fromCache('darkMode')),
        reduceMotion: parseBoolean(serverState.reduceMotion,
                fromCache('reduceMotion') !== null ? fromCache('reduceMotion') : prefersReducedMotion()),
        fontScale: clampScale(parseInt(serverState.fontScale, 10) || parseInt(fromCache('fontScale'), 10) || 100),
        dyslexicFont: parseBoolean(serverState.dyslexicFont, fromCache('dyslexicFont')),
        wideSpacing: parseBoolean(serverState.wideSpacing, fromCache('wideSpacing')),
        underlineLinks: parseBoolean(serverState.underlineLinks, fromCache('underlineLinks')),
        cursorRing: parseBoolean(serverState.cursorRing, fromCache('cursorRing'))
    };

    var cursorRingElement = null;

    function sanitizeTheme(value) {
        return VALID_THEMES.indexOf(value) !== -1 ? value : 'minimal';
    }

    function clampScale(value) {
        var number = Number(value);
        if (!isFinite(number)) {
            return 100;
        }
        return Math.min(MAX_SCALE, Math.max(MIN_SCALE, Math.round(number / 25) * 25));
    }

    function parseBoolean(value, fallback) {
        if (value === null || value === undefined || value === '') {
            return Boolean(fallback);
        }
        return value === true || value === 'true';
    }

    function prefersReducedMotion() {
        return !!(window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches);
    }

    function fromCache(key) {
        try {
            var cached = JSON.parse(window.localStorage.getItem(STORAGE_KEY) || '{}');
            return Object.prototype.hasOwnProperty.call(cached, key) ? cached[key] : null;
        } catch (ignored) {
            return null;
        }
    }

    function saveCache() {
        try {
            window.localStorage.setItem(STORAGE_KEY, JSON.stringify(state));
        } catch (ignored) {
            // Modo privado o almacenamiento lleno: el backend sigue siendo fuente de verdad.
        }
    }

    function announce(message) {
        var region = document.getElementById('a11y-status');
        if (!region) {
            region = document.createElement('p');
            region.id = 'a11y-status';
            region.className = 'sr-only';
            region.setAttribute('role', 'status');
            region.setAttribute('aria-live', 'polite');
            document.body.appendChild(region);
        }
        region.textContent = message;
    }

    function updateCursorRing() {
        if (!cursorRingElement) {
            cursorRingElement = document.createElement('div');
            cursorRingElement.className = 'a11y-cursor-ring';
            cursorRingElement.setAttribute('aria-hidden', 'true');
            document.body.appendChild(cursorRingElement);

            document.addEventListener('pointermove', function (event) {
                if (!state.cursorRing || state.reduceMotion) {
                    return;
                }
                cursorRingElement.style.left = event.clientX + 'px';
                cursorRingElement.style.top = event.clientY + 'px';
            });
            document.addEventListener('mouseleave', function () {
                cursorRingElement.style.display = 'none';
            });
            document.addEventListener('mouseenter', function () {
                cursorRingElement.style.display = '';
            });
            window.addEventListener('blur', function () {
                cursorRingElement.style.display = 'none';
            });
        }
        cursorRingElement.style.display = (state.cursorRing && !state.reduceMotion) ? '' : 'none';
    }

    /** Aplica el estado al DOM y mantiene espejados los controles del formulario. */
    function applyState() {
        root.setAttribute('data-theme', state.theme);
        root.setAttribute('data-dark-mode', String(state.darkMode));
        root.setAttribute('data-reduce-motion', String(state.reduceMotion));
        root.setAttribute('data-dyslexic-font', String(state.dyslexicFont));
        root.setAttribute('data-wide-spacing', String(state.wideSpacing));
        root.setAttribute('data-underline-links', String(state.underlineLinks));
        root.setAttribute('data-cursor-ring', String(state.cursorRing));
        root.style.setProperty('--font-scale', state.fontScale + '%');
        updateCursorRing();

        var scaleLabel = document.getElementById('a11y-scale-value');
        if (scaleLabel) {
            scaleLabel.textContent = state.fontScale + '%';
        }

        document.querySelectorAll('[data-a11y-control]').forEach(function (control) {
            var kind = control.dataset.a11yControl;
            if (kind === 'theme') {
                if (control.type === 'radio') {
                    control.checked = control.value === state.theme;
                } else {
                    control.value = state.theme;
                }
            } else if (kind === 'darkMode') {
                control.checked = state.darkMode;
            } else if (kind === 'reduceMotion') {
                control.checked = state.reduceMotion;
            } else if (kind === 'fontScale') {
                control.value = String(state.fontScale);
            } else if (kind === 'dyslexicFont') {
                control.checked = state.dyslexicFont;
            } else if (kind === 'wideSpacing') {
                control.checked = state.wideSpacing;
            } else if (kind === 'underlineLinks') {
                control.checked = state.underlineLinks;
            } else if (kind === 'cursorRing') {
                control.checked = state.cursorRing;
            }
        });

        // Otros módulos (mascota, simulador) pueden pausar sus animaciones/habla.
        document.dispatchEvent(new CustomEvent('kronocash:preferences', { detail: currentState() }));
    }

    function currentState() {
        return {
            theme: state.theme,
            darkMode: state.darkMode,
            reduceMotion: state.reduceMotion,
            fontScale: state.fontScale,
            dyslexicFont: state.dyslexicFont,
            wideSpacing: state.wideSpacing,
            underlineLinks: state.underlineLinks,
            cursorRing: state.cursorRing
        };
    }

    function csrfHeader() {
        var token = document.querySelector('meta[name="_csrf"]');
        var header = document.querySelector('meta[name="_csrf_header"]');
        return (token && header) ? {[header.content]: token.content} : {};
    }

    function syncWithBackend() {
        var headers = Object.assign({
            'Content-Type': 'application/json',
            'Accept': 'application/json'
        }, csrfHeader());
        fetch(endpoint, {
            method: 'POST',
            headers: headers,
            credentials: 'same-origin',
            body: JSON.stringify({
                themeStyle: state.theme,
                darkMode: state.darkMode,
                reduceMotion: state.reduceMotion,
                fontScale: state.fontScale
            })
        }).then(function (response) {
            if (!response.ok) {
                throw new Error('HTTP ' + response.status);
            }
            announce('Preferencias de accesibilidad guardadas.');
        }).catch(function () {
            announce('No se pudieron guardar las preferencias en el servidor; se aplicaron solo en este dispositivo.');
        });
    }

    function scheduleSync() {
        if (syncTimer !== null) {
            window.clearTimeout(syncTimer);
        }
        syncTimer = window.setTimeout(function () {
            syncTimer = null;
            syncWithBackend();
        }, 300);
    }

    function commit(message) {
        applyState();
        saveCache();
        scheduleSync();
        if (message) {
            announce(message);
        }
    }

    function onControlChange(event) {
        var control = event.currentTarget;
        var kind = control.dataset.a11yControl;

        if (kind === 'theme') {
            if (control.type === 'radio' && !control.checked) {
                return;
            }
            state.theme = sanitizeTheme(control.value);
            commit('Tema ' + state.theme + ' aplicado.');
        } else if (kind === 'darkMode') {
            state.darkMode = control.checked;
            commit(state.darkMode ? 'Modo oscuro activado.' : 'Modo claro activado.');
        } else if (kind === 'reduceMotion') {
            state.reduceMotion = control.checked;
            commit(state.reduceMotion ? 'Animaciones pausadas.' : 'Animaciones activadas.');
        } else if (kind === 'fontScale') {
            state.fontScale = clampScale(control.value);
            commit('Tamaño de fuente: ' + state.fontScale + '%.');
        } else if (kind === 'dyslexicFont') {
            state.dyslexicFont = control.checked;
            commit(state.dyslexicFont ? 'Tipografía de lectura fácil activada.' : 'Tipografía estándar activada.');
        } else if (kind === 'wideSpacing') {
            state.wideSpacing = control.checked;
            commit(state.wideSpacing ? 'Espaciado ampliado activado.' : 'Espaciado normal activado.');
        } else if (kind === 'underlineLinks') {
            state.underlineLinks = control.checked;
            commit(state.underlineLinks ? 'Enlaces siempre subrayados.' : 'Subrayado de enlaces desactivado.');
        } else if (kind === 'cursorRing') {
            state.cursorRing = control.checked;
            commit(state.cursorRing ? 'Resaltado del puntero activado.' : 'Resaltado del puntero desactivado.');
        }
    }

    function bindControls() {
        document.querySelectorAll('[data-a11y-control]').forEach(function (control) {
            var eventName = control.dataset.a11yControl === 'fontScale' ? 'input' : 'change';
            control.addEventListener(eventName, onControlChange);
        });
    }

    /** Widget flotante: apertura/cierre, Escape y botones de escala. */
    function setPanel(open, toggle, panel) {
        toggle.setAttribute('aria-expanded', String(open));
        if (open) {
            panel.removeAttribute('hidden');
            var first = panel.querySelector('select, input, button');
            if (first) {
                first.focus();
            }
        } else {
            panel.setAttribute('hidden', '');
        }
    }

    function bindWidget() {
        var toggle = document.getElementById('a11y-toggle-btn');
        var panel = document.getElementById('a11y-panel');
        if (!toggle || !panel) {
            return;
        }

        toggle.addEventListener('click', function () {
            setPanel(toggle.getAttribute('aria-expanded') !== 'true', toggle, panel);
        });

        document.addEventListener('keydown', function (event) {
            if (event.key === 'Escape' && toggle.getAttribute('aria-expanded') === 'true') {
                setPanel(false, toggle, panel);
                toggle.focus();
            }
        });

        document.querySelectorAll('[data-a11y-scale]').forEach(function (button) {
            button.addEventListener('click', function () {
                var action = button.dataset.a11yScale;
                var next = action === 'decrease' ? state.fontScale - 25
                        : action === 'increase' ? state.fontScale + 25 : 100;
                state.fontScale = clampScale(next);
                commit('Tamaño de fuente: ' + state.fontScale + '%.');
            });
        });
    }

    /** Casilla clásica «Mostrar contraseña». */
    function bindPasswordToggles() {
        var checkbox = document.getElementById('show-password-toggle');
        var input = document.getElementById('password');
        if (!checkbox || !input) {
            return;
        }
        checkbox.addEventListener('change', function () {
            input.type = checkbox.checked ? 'text' : 'password';
        });
    }

    function init() {
        applyState();
        bindControls();
        bindWidget();
        bindPasswordToggles();

        // SC 2.3.3: si el sistema pide reducir movimiento y el usuario no ha
        // guardado una preferencia explícita, se respeta automáticamente.
        if (window.matchMedia) {
            var query = window.matchMedia('(prefers-reduced-motion: reduce)');
            var listener = function (event) {
                if (event.matches && serverState.reduceMotion === null && fromCache('reduceMotion') === null) {
                    state.reduceMotion = true;
                    commit('Movimiento reducido según la preferencia del sistema.');
                }
            };
            if (query.addEventListener) {
                query.addEventListener('change', listener);
            } else if (query.addListener) {
                query.addListener(listener);
            }
        }
    }

    /** API pública para el resto de módulos. */
    window.KronoCashAccessibility = {
        getState: currentState,
        apply: applyState,
        setTheme: function (value) {
            state.theme = sanitizeTheme(value);
            commit('Tema ' + state.theme + ' aplicado.');
        },
        setDarkMode: function (value) {
            state.darkMode = Boolean(value);
            commit(state.darkMode ? 'Modo oscuro activado.' : 'Modo claro activado.');
        },
        setReduceMotion: function (value) {
            state.reduceMotion = Boolean(value);
            commit(state.reduceMotion ? 'Animaciones pausadas.' : 'Animaciones activadas.');
        },
        setFontScale: function (value) {
            state.fontScale = clampScale(value);
            commit('Tamaño de fuente: ' + state.fontScale + '%.');
        },
        setDyslexicFont: function (value) {
            state.dyslexicFont = Boolean(value);
            commit(state.dyslexicFont ? 'Tipografía de lectura fácil activada.' : 'Tipografía estándar activada.');
        },
        setWideSpacing: function (value) {
            state.wideSpacing = Boolean(value);
            commit(state.wideSpacing ? 'Espaciado ampliado activado.' : 'Espaciado normal activado.');
        },
        setUnderlineLinks: function (value) {
            state.underlineLinks = Boolean(value);
            commit(state.underlineLinks ? 'Enlaces siempre subrayados.' : 'Subrayado de enlaces desactivado.');
        },
        setCursorRing: function (value) {
            state.cursorRing = Boolean(value);
            commit(state.cursorRing ? 'Resaltado del puntero activado.' : 'Resaltado del puntero desactivado.');
        }
    };

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
