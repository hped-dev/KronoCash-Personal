/**
 * KronoCash - mascot-voice.js (JavaScript Vanilla)
 *
 * Web Speech API robusta: solo evalúa resultados isFinal (con feedback
 * interim y debounce de silencio), degrada a texto y respeta la privacidad
 * de los campos de contraseña.
 */
(function () {
    'use strict';

    var widget = document.querySelector('[data-mascot-state]');
    if (!widget) {
        return;
    }

    var kronoSvg = document.getElementById('krono-svg');
    var micButton = document.getElementById('mascot-mic');
    var speakButton = document.getElementById('mascot-speak');
    var bubble = document.getElementById('mascot-message');
    var bubbleText = document.getElementById('mascot-message-text');
    var mobileToggle = document.getElementById('krono-mobile-toggle');
    var panel = document.getElementById('krono-floating-panel');
    var panelClose = document.getElementById('krono-panel-close');
    var bubbleClose = document.getElementById('mascot-bubble-close');
    var SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
    var synthesis = window.speechSynthesis || null;

    var GREETING = '¡Hola! Soy Krono. Si necesitas apoyo por voz, haz clic en el botón Micrófono. '
            + 'Si no necesitas que hable, haz clic en el botón Voz.';
    var PRIVACY_MESSAGE = 'Recuerda que tu contraseña debe ser secreta, no la compartas con nadie.';
    var DEFAULT_MESSAGE = bubbleText ? bubbleText.textContent.trim() : '';

    // Localización mexicana: voz y formato siempre en es-MX.
    var language = 'es-MX';
    var spanishVoices = [];
    var voiceIndex = 0;
    var recognition = null;
    var listening = false;
    var muted = false;
    var finalTranscript = '';
    var silenceTimer = null;
    var evaluated = false;
    var pendingGreeting = false;
    var greetingStarted = false;
    var privacyActive = false;
    var messageBeforePrivacy = DEFAULT_MESSAGE;

    function isCompact() {
        return window.matchMedia && window.matchMedia('(max-width: 900px)').matches;
    }

    function setPanelOpen(open, moveFocus) {
        if (!panel) {
            return;
        }
        panel.hidden = !open;
        panel.classList.toggle('is-open', open);
        if (mobileToggle) {
            mobileToggle.setAttribute('aria-expanded', String(open));
            mobileToggle.setAttribute('aria-label', open ? 'Cerrar asistente' : 'Abrir asistente');
        }
        if (open && moveFocus && panelClose) {
            panelClose.focus();
        }
    }

    function setBubbleOpen(open, moveFocus) {
        if (bubble) {
            bubble.classList.toggle('is-open', open);
        }
        if (isCompact()) {
            setPanelOpen(open, moveFocus);
        } else if (open && moveFocus && bubbleClose) {
            bubbleClose.focus();
        }
    }

    function messageText() {
        return bubbleText ? bubbleText.textContent : (bubble ? messageText() : DEFAULT_MESSAGE);
    }

    function setState(state) {
        widget.setAttribute('data-mascot-state', state);
        if (state === 'speaking' && isCompact()) {
            setBubbleOpen(true, false);
        }
    }

    function say(message) {
        if (bubbleText) {
            bubbleText.textContent = message;
        } else if (bubble) {
            bubble.textContent = message;
        }
    }

    function setLabel(button, text) {
        var span = button.querySelector('span');
        if (span) {
            span.textContent = text;
        } else {
            button.textContent = text;
        }
    }

    function updateMicButton(active) {
        micButton.setAttribute('aria-pressed', String(active));
        micButton.setAttribute('aria-label', active ? 'Desactivar micrófono' : 'Activar micrófono');
        setLabel(micButton, active ? 'Escuchando' : 'Micrófono');
    }

    function updateSpeakButton() {
        speakButton.setAttribute('aria-pressed', String(muted));
        speakButton.setAttribute('aria-label', muted
                ? 'Activar lectura en voz alta'
                : 'Silenciar lectura en voz alta');
        setLabel(speakButton, muted ? 'Silenciada' : 'Voz');
    }

    /** Quita artículos para que "leer la pantalla" y "leer pantalla" coincidan. */
    function normalize(text) {
        return text.toLowerCase()
                .normalize('NFD')
                .replace(/[\u0300-\u036f]/g, '')
                .replace(/[.,!?;:"'()¿¡]/g, ' ')
                .replace(/\b(a la|al|la|el)\s+/g, ' ')
                .replace(/\s+/g, ' ')
                .trim();
    }

    function has(command, words) {
        return words.some(function (word) {
            return command.indexOf(word) !== -1;
        });
    }

    function preference(kind, value) {
        var api = window.KronoCashAccessibility;
        if (api) {
            if (kind === 'darkMode') {
                api.setDarkMode(value);
            } else if (kind === 'theme') {
                api.setTheme(value);
            } else if (kind === 'reduceMotion') {
                api.setReduceMotion(value);
            } else if (kind === 'fontScale') {
                api.setFontScale(value);
            } else if (kind === 'dyslexicFont') {
                api.setDyslexicFont(value);
            } else if (kind === 'cursorRing') {
                api.setCursorRing(value);
            }
            return;
        }
        var attribute = kind === 'darkMode' ? 'data-dark-mode' : 'data-theme';
        document.documentElement.setAttribute(attribute, kind === 'darkMode' ? String(value) : value);
    }

    function currentFontScale() {
        var api = window.KronoCashAccessibility;
        if (api && api.getState) {
            return Number(api.getState().fontScale) || 100;
        }
        return parseInt(document.documentElement.style.getPropertyValue('--font-scale'), 10) || 100;
    }

    function loadVoices() {
        if (!synthesis || typeof synthesis.getVoices !== 'function') {
            return;
        }
        spanishVoices = (synthesis.getVoices() || []).filter(function (voice) {
            return /^es([-_]|$)/i.test(voice.lang);
        }).sort(function (a, b) {
            var naturalA = /natural|premium|neural/i.test(a.name) ? 1 : 0;
            var naturalB = /natural|premium|neural/i.test(b.name) ? 1 : 0;
            return naturalB - naturalA;
        });
    }

    function speak(message) {
        if (muted) {
            say(message);
            return null;
        }
        if (!synthesis || typeof window.SpeechSynthesisUtterance === 'undefined') {
            say(message + ' (Lectura en voz alta no disponible; el mensaje queda en pantalla.)');
            return null;
        }
        synthesis.cancel();
        var utterance = new window.SpeechSynthesisUtterance(message);
        utterance.lang = language;
        if (spanishVoices.length) {
            utterance.voice = spanishVoices[voiceIndex % spanishVoices.length];
            utterance.lang = utterance.voice.lang;
        }
        utterance.rate = 1.0;
        utterance.pitch = 1.05;
        utterance.onstart = function () {
            greetingStarted = true;
            setState('speaking');
        };
        utterance.onend = function () {
            setState(listening ? 'listening' : 'idle');
        };
        utterance.onerror = function () {
            setState(listening ? 'listening' : 'idle');
        };
        synthesis.speak(utterance);
        return utterance;
    }

    function rotateVoice() {
        if (!spanishVoices.length) {
            say('Solo hay una voz en español disponible en este navegador.');
            return;
        }
        voiceIndex = (voiceIndex + 1) % spanishVoices.length;
        var name = spanishVoices[voiceIndex].name;
        say('Voz cambiada: ' + name + '.');
        speak('Voz cambiada.');
    }

    function navigate(path, message) {
        say(message);
        speak(message);
        window.location.assign(path);
    }

    function logout() {
        var form = document.querySelector('form[action$="/logout"]');
        if (!form) {
            say('No se encontró el formulario de cierre de sesión.');
            return;
        }
        say('Cerrando sesión.');
        speak('Cerrando sesión.');
        if (form.requestSubmit) {
            form.requestSubmit();
        } else {
            form.submit();
        }
    }

    function readScreen() {
        var heading = document.querySelector('main h1') || document.querySelector('h1');
        var main = document.querySelector('main');
        var bodyText = main ? (main.innerText || main.textContent) : (document.body.textContent || '');
        var clean = bodyText.replace(/\s+/g, ' ').trim().slice(0, 700);
        var message = (heading ? heading.textContent.trim() + '. ' : '') + clean;
        if (!message.trim()) {
            message = 'No hay contenido legible en esta página.';
        }
        say(message);
        speak(message);
    }

    function helpDetails() {
        return 'Entiendo: inicio de sesión, registro, inicio, panel, simulador, ajustes, cerrar sesión, '
                + 'leer pantalla, temas retro/minimal/cyberpunk, modo oscuro o claro, reducir movimiento, '
                + 'aumentar o reducir letra, fuente dislexia, resaltar puntero, cambiar voz, ayuda y quién eres.';
    }

    function helpSpoken() {
        return 'Puedo ayudarte a navegar, cambiar el tema o leer la pantalla. La lista completa está en pantalla.';
    }

    function handleCommand(raw) {
        var command = normalize(raw);
        if (!command) {
            return;
        }

        if (has(command, ['cerrar sesion', 'salir'])) {
            logout();
            return;
        }
        if (has(command, ['inicio de sesion', 'iniciar sesion', 'login', 'entrar'])) {
            navigate('/login', 'Abriendo el inicio de sesión.');
            return;
        }
        if (has(command, ['registro', 'registrar', 'crear cuenta'])) {
            navigate('/register', 'Abriendo el registro.');
            return;
        }
        if (has(command, ['panel', 'dashboard', 'finanzas', 'gastos'])) {
            navigate('/dashboard', 'Abriendo el panel financiero.');
            return;
        }
        if (has(command, ['simulador', 'simular', 'proyeccion'])) {
            navigate('/simulator', 'Abriendo el simulador.');
            return;
        }
        if (has(command, ['ajustes', 'configuracion', 'accesibilidad', 'perfil'])) {
            navigate('/settings', 'Abriendo los ajustes.');
            return;
        }
        if (has(command, ['inicio', 'bienvenida', 'home', 'principal'])) {
            navigate('/', 'Abriendo la página de inicio.');
            return;
        }

        if (has(command, ['leer pantalla', 'lee pantalla', 'leer resumen', 'leer contenido',
                'que dice aqui', 'que hay en pantalla', 'que hay aqui'])) {
            readScreen();
            return;
        }

        if (command.indexOf('desactivar modo oscuro') !== -1 || command.indexOf('modo claro') !== -1) {
            preference('darkMode', false);
            say('Modo claro activado.');
            speak(messageText());
            return;
        }
        if (has(command, ['activar modo oscuro', 'modo oscuro', 'cambiar modo'])) {
            if (command.indexOf('cambiar modo') !== -1) {
                var isDark = document.documentElement.getAttribute('data-dark-mode') === 'true';
                preference('darkMode', !isDark);
                say(isDark ? 'Modo claro activado.' : 'Modo oscuro activado.');
            } else {
                preference('darkMode', true);
                say('Modo oscuro activado.');
            }
            speak(messageText());
            return;
        }

        var themeMatch = command.match(/\b(retro|minimal|minimalista|cyberpunk|ciberpunk)\b/);
        if (themeMatch) {
            var theme = themeMatch[1] === 'minimalista' ? 'minimal'
                    : themeMatch[1] === 'ciberpunk' ? 'cyberpunk' : themeMatch[1];
            preference('theme', theme);
            say('Tema ' + theme + ' aplicado.');
            speak(messageText());
            return;
        }

        if (has(command, ['activar dislexia', 'fuente dislexia'])) {
            preference('dyslexicFont', true);
            say('Fuente de lectura fácil activada.');
            speak(messageText());
            return;
        }
        if (has(command, ['resaltar puntero', 'resaltar cursor'])) {
            preference('cursorRing', true);
            say('Resaltado del puntero activado.');
            speak(messageText());
            return;
        }
        if (has(command, ['reducir movimiento', 'quitar animaciones'])) {
            preference('reduceMotion', true);
            say('Animaciones desactivadas.');
            speak(messageText());
            return;
        }
        if (command.indexOf('activar animaciones') !== -1) {
            preference('reduceMotion', false);
            say('Animaciones activadas.');
            speak(messageText());
            return;
        }
        if (has(command, ['aumentar letra', 'hacer zoom'])) {
            var bigger = Math.min(200, currentFontScale() + 25);
            preference('fontScale', bigger);
            say('Tamaño de letra: ' + bigger + '%.');
            speak(messageText());
            return;
        }
        if (has(command, ['reducir letra'])) {
            var smaller = Math.max(100, currentFontScale() - 25);
            preference('fontScale', smaller);
            say('Tamaño de letra: ' + smaller + '%.');
            speak(messageText());
            return;
        }
        if (has(command, ['letra normal'])) {
            preference('fontScale', 100);
            say('Tamaño de letra normal: 100%.');
            speak(messageText());
            return;
        }
        if (has(command, ['cambiar voz', 'otra voz'])) {
            rotateVoice();
            return;
        }
        if (has(command, ['ocultar krono', 'minimizar'])) {
            widget.classList.add('krono-minimized');
            say('Krono minimizado. Di «quien eres» para mostrarlo de nuevo.');
            return;
        }
        if (has(command, ['quien eres', 'quién eres'])) {
            widget.classList.remove('krono-minimized');
            var intro = 'Soy Krono, tu asistente virtual para la educación financiera accesible.';
            say(intro);
            speak(intro);
            return;
        }
        if (has(command, ['ayuda', 'comandos'])) {
            say(helpDetails());
            speak(helpSpoken());
            return;
        }

        var fallback = 'No reconocí el comando. Di «ayuda» para ver la lista completa.';
        say(fallback);
        speak(fallback);
    }

    function recognitionError(error) {
        if (error === 'not-allowed' || error === 'service-not-allowed' || error === 'denied') {
            return 'Permiso de micrófono denegado. Puede usar los botones y leer los mensajes aquí.';
        }
        if (error === 'no-speech') {
            return 'No se detectó voz. Intente de nuevo o use los botones del asistente.';
        }
        if (error === 'network') {
            return 'Sin conexión para reconocer la voz. El asistente sigue disponible por texto.';
        }
        return 'El micrófono no está disponible ahora. Puede usar los botones del asistente.';
    }

    function clearSilenceTimer() {
        if (silenceTimer !== null) {
            window.clearTimeout(silenceTimer);
            silenceTimer = null;
        }
    }

    function finishListening() {
        clearSilenceTimer();
        if (evaluated) {
            return;
        }
        evaluated = true;
        listening = false;
        updateMicButton(false);
        var command = finalTranscript.trim();
        if (!command) {
            say('No se detectó una frase completa. Di «ayuda» para ver los comandos.');
            setState('idle');
            return;
        }
        handleCommand(command);
    }

    function onResult(event) {
        var interim = '';
        for (var i = 0; i < event.results.length; i++) {
            var result = event.results[i];
            if (result.isFinal) {
                finalTranscript += result[0].transcript + ' ';
            } else {
                interim += result[0].transcript + ' ';
            }
        }
        if (interim.trim()) {
            say('Escuchando: ' + interim.trim());
        } else if (finalTranscript.trim()) {
            say('Escuchando: ' + finalTranscript.trim());
        }
        clearSilenceTimer();
        silenceTimer = window.setTimeout(finishListening, 1200);
    }

    function initRecognition() {
        if (!SpeechRecognition) {
            micButton.disabled = true;
            micButton.setAttribute('aria-label', 'Micrófono no disponible en este navegador');
            say('El reconocimiento de voz no está disponible en este navegador. Puede usar los botones y leer los mensajes aquí.');
            return;
        }
        recognition = new SpeechRecognition();
        recognition.lang = language;
        recognition.continuous = false;
        recognition.interimResults = true;
        recognition.maxAlternatives = 1;
        recognition.onresult = onResult;
        recognition.onerror = function (event) {
            clearSilenceTimer();
            listening = false;
            evaluated = true;
            updateMicButton(false);
            setState('idle');
            say(recognitionError(event.error));
        };
        recognition.onend = function () {
            clearSilenceTimer();
            listening = false;
            updateMicButton(false);
            if (!evaluated) {
                finishListening();
            } else if (!synthesis || !synthesis.speaking) {
                setState('idle');
            }
        };
    }

    function toggleMicrophone() {
        if (synthesis && synthesis.speaking) {
            synthesis.cancel();
        }
        setState('idle');
        if (!recognition) {
            say('El micrófono no está disponible en este navegador; los mensajes se muestran como texto.');
            return;
        }
        if (listening) {
            evaluated = true;
            clearSilenceTimer();
            recognition.stop();
            listening = false;
            updateMicButton(false);
            if (!synthesis || !synthesis.speaking) {
                setState('idle');
            }
            say('Micrófono desactivado.');
            return;
        }
        finalTranscript = '';
        evaluated = false;
        try {
            recognition.start();
            listening = true;
            updateMicButton(true);
            setState('listening');
            say('Escuchando… diga un comando, por ejemplo «ayuda».');
        } catch (ignored) {
            listening = false;
            updateMicButton(false);
            setState('idle');
            say('No se pudo iniciar el micrófono. Puede usar los botones del asistente.');
        }
    }

    function toggleSpeech() {
        muted = !muted;
        updateSpeakButton();
        if (muted) {
            if (synthesis) {
                synthesis.cancel();
            }
            setState(listening ? 'listening' : 'idle');
            say('Lectura en voz alta silenciada. Los mensajes seguirán visibles aquí.');
        } else {
            say('Voz alta activada.');
            speak('Voz alta activada.');
        }
    }

    /** Krono cierra los ojos mientras se escribe una contraseña (sin hablar). */
    /** Foco dentro del grupo sensible: campo de contraseña o su casilla. */
    function isSensitiveFocus(target) {
        return !!(target && (target.id === 'password'
                || target.id === 'show-password-toggle'
                || (target.getAttribute && target.getAttribute('data-sensitive') === 'true')));
    }

    function setPrivacy(active) {
        privacyActive = active;
        if (kronoSvg) {
            kronoSvg.classList.toggle('state-privacy', active);
        }
        if (active) {
            say(PRIVACY_MESSAGE);
            if (isCompact()) {
                setBubbleOpen(true, false);
            }
        } else {
            say(messageBeforePrivacy || DEFAULT_MESSAGE);
        }
    }

    function syncPrivacyWithFocus(target) {
        var active = target || document.activeElement;
        if (isSensitiveFocus(active)) {
            if (!privacyActive) {
                messageBeforePrivacy = bubble ? messageText() : DEFAULT_MESSAGE;
                setPrivacy(true);
            }
            return;
        }
        if (privacyActive) {
            // El foco salió al botón «Entrar», a otro campo o al fondo:
            // abrimos los ojos, restauramos el mensaje y ocultamos la clave.
            setPrivacy(false);
            var checkbox = document.getElementById('show-password-toggle');
            var input = document.getElementById('password');
            if (checkbox && input && input.type === 'text') {
                checkbox.checked = false;
                input.type = 'password';
            }
        }
    }

    function bindSensitiveFields() {
        // Revisamos el foco de forma global: focusin/focusout y clics en el
        // fondo de la página (donde el foco puede pasar a <body>).
        document.addEventListener('focusin', function (event) {
            syncPrivacyWithFocus(event.target);
        });
        document.addEventListener('focusout', function (event) {
            window.setTimeout(function () {
                syncPrivacyWithFocus(event.relatedTarget || document.activeElement);
            }, 0);
        });
        document.addEventListener('click', function () {
            window.setTimeout(syncPrivacyWithFocus, 0);
        });
        document.addEventListener('input', function (event) {
            if (isSensitiveFocus(event.target) && !privacyActive) {
                messageBeforePrivacy = bubble ? messageText() : DEFAULT_MESSAGE;
                setPrivacy(true);
            }
        });
    }

    function queueGreeting() {
        try {
            if (window.sessionStorage.getItem('krono_welcomed')) {
                return;
            }
            window.sessionStorage.setItem('krono_welcomed', '1');
        } catch (ignored) {
            // sessionStorage no disponible: se muestra igualmente el saludo.
        }
        say(GREETING);
        speak(GREETING);
        window.setTimeout(function () {
            if (!greetingStarted && !muted) {
                pendingGreeting = true;
            }
        }, 800);
    }

    function bindFirstInteraction() {
        function replay() {
            if (!pendingGreeting) {
                return;
            }
            pendingGreeting = false;
            speak(GREETING);
        }
        document.addEventListener('click', replay, {once: false});
        document.addEventListener('keydown', replay, {once: false});
    }

    function init() {
        loadVoices();
        if (synthesis && typeof synthesis.addEventListener === 'function') {
            synthesis.addEventListener('onvoiceschanged', function () {
                loadVoices();
            });
        } else if (synthesis) {
            synthesis.onvoiceschanged = loadVoices;
        }

        updateMicButton(false);
        updateSpeakButton();
        initRecognition();
        micButton.addEventListener('click', toggleMicrophone);
        speakButton.addEventListener('click', toggleSpeech);

        setPanelOpen(!isCompact(), false);

        if (mobileToggle) {
            mobileToggle.addEventListener('click', function () {
                var open = panel && panel.hidden;
                setPanelOpen(open, false);
            });
        }
        if (panelClose) {
            panelClose.addEventListener('click', function () {
                setPanelOpen(false, false);
                if (mobileToggle) {
                    mobileToggle.focus();
                }
            });
        }
        if (bubbleClose) {
            bubbleClose.addEventListener('click', function () {
                setBubbleOpen(false, false);
                if (isCompact()) {
                    setPanelOpen(false, false);
                    if (mobileToggle) {
                        mobileToggle.focus();
                    }
                }
            });
        }
        document.addEventListener('keydown', function (event) {
            var menuOpen = document.querySelector('.nav-toggle-btn[aria-expanded="true"]');
            if (event.key === 'Escape' && !menuOpen && panel && !panel.hidden && isCompact()) {
                setPanelOpen(false, false);
                if (mobileToggle) {
                    mobileToggle.focus();
                }
            }
        });

        window.addEventListener('resize', function () {
            if (isCompact() && panel && !panel.hidden) {
                setPanelOpen(false, false);
            } else if (!isCompact() && panel && panel.hidden) {
                setPanelOpen(true, false);
            }
        });

        bindSensitiveFields();
        bindFirstInteraction();
        queueGreeting();
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
