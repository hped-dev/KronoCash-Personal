/**
 * KronoCash - birthday.js
 * Melodía sintetizada con Web Audio API (sin archivos externos, sin
 * reproducción automática). El usuario controla inicio y detención.
 */
(function () {
    'use strict';

    var card = document.getElementById('birthday-card');
    if (!card) {
        return;
    }

    var button = document.getElementById('birthday-music-btn');
    var closeButton = document.getElementById('birthday-close');
    var AudioContextClass = window.AudioContext || window.webkitAudioContext;
    var context = null;
    var oscillators = [];
    var playing = false;
    var endTimer = null;

    // "Feliz cumpleaños" en Do mayor. MIDI -> Hz = 440 * 2^((n-69)/12).
    var melody = [
        [67, 0.5], [67, 0.5], [69, 1], [67, 1], [72, 1], [71, 2],
        [67, 0.5], [67, 0.5], [69, 1], [67, 1], [74, 1], [72, 2],
        [67, 0.5], [67, 0.5], [79, 1], [76, 1], [72, 1], [71, 1], [69, 2],
        [77, 0.5], [77, 0.5], [76, 1], [72, 1], [74, 1], [72, 2]
    ];
    var BEAT_SECONDS = 0.42;
    var MAX_GAIN = 0.12;

    function frequency(midi) {
        return 440 * Math.pow(2, (midi - 69) / 12);
    }

    function setPlaying(active) {
        playing = active;
        if (button) {
            button.setAttribute('aria-pressed', String(active));
            button.textContent = active ? 'Detener música' : 'Dale clic';
        }
    }

    function stopMusic() {
        if (endTimer !== null) {
            window.clearTimeout(endTimer);
            endTimer = null;
        }
        oscillators.forEach(function (oscillator) {
            try {
                oscillator.stop();
            } catch (ignored) { /* ya detenido */ }
        });
        oscillators = [];
        if (context && context.state === 'running') {
            context.suspend();
        }
        setPlaying(false);
    }

    function playMusic() {
        if (!AudioContextClass) {
            announce('Tu navegador no permite generar audio; la felicitación sigue visible en texto.');
            return;
        }
        if (!context) {
            context = new AudioContextClass();
        }
        if (context.state === 'suspended') {
            context.resume();
        }

        var start = context.currentTime + 0.08;
        var cursor = start;
        melody.forEach(function (note, index) {
            var duration = note[1] * BEAT_SECONDS;
            var oscillator = context.createOscillator();
            var gain = context.createGain();
            oscillator.type = index % 2 === 0 ? 'triangle' : 'sine';
            oscillator.frequency.setValueAtTime(frequency(note[0]), cursor);

            gain.gain.setValueAtTime(0.0001, cursor);
            gain.gain.linearRampToValueAtTime(MAX_GAIN, cursor + 0.04);
            gain.gain.exponentialRampToValueAtTime(0.0001, cursor + Math.max(0.12, duration * 0.9));

            oscillator.connect(gain);
            gain.connect(context.destination);
            oscillator.start(cursor);
            oscillator.stop(cursor + duration);
            oscillators.push(oscillator);
            cursor += duration;
        });

        setPlaying(true);
        endTimer = window.setTimeout(stopMusic, (cursor - start + 0.4) * 1000);
    }

    function announce(message) {
        var region = document.getElementById('a11y-status');
        if (region) {
            region.textContent = message;
        }
    }

    function closeCard() {
        stopMusic();
        card.hidden = true;
        var heading = document.getElementById('perfil-titulo');
        if (heading) {
            heading.setAttribute('tabindex', '-1');
            heading.focus();
        }
    }

    if (button) {
        if (!AudioContextClass) {
            button.disabled = true;
            button.textContent = 'Audio no disponible';
        } else {
            button.addEventListener('click', function () {
                if (playing) {
                    stopMusic();
                    announce('Música detenida.');
                } else {
                    playMusic();
                    announce('Reproduciendo música de cumpleaños.');
                }
            });
        }
    }

    if (closeButton) {
        closeButton.addEventListener('click', closeCard);
    }

    document.addEventListener('keydown', function (event) {
        if (event.key === 'Escape' && !card.hidden) {
            closeCard();
        }
    });

    window.addEventListener('pagehide', stopMusic);
    window.addEventListener('beforeunload', stopMusic);
})();
