/**
 * KronoCash - ui-feedback.js
 * Restaura la posición de scroll tras envíos y muestra notificaciones
 * temáticas para los resultados de las acciones (query params).
 */
(function () {
    'use strict';

    function showToast(message, isError) {
        var container = document.querySelector('.theme-toast-container');
        if (!container) {
            container = document.createElement('div');
            container.className = 'theme-toast-container';
            container.setAttribute('role', 'alert');
            container.setAttribute('aria-live', 'assertive');
            document.body.appendChild(container);
        }
        var item = document.createElement('p');
        item.className = 'theme-toast' + (isError ? ' theme-toast-error' : '');
        item.textContent = message;
        container.textContent = '';
        container.appendChild(item);
        window.setTimeout(function () {
            if (item.parentNode) {
                item.parentNode.removeChild(item);
            }
        }, 6000);
    }

    function notifyFromQuery() {
        var params = new URLSearchParams(window.location.search);
        var messages = {
            created: 'Movimiento registrado correctamente.',
            deleted: 'Registro eliminado correctamente.',
            goalCreated: 'Meta de ahorro creada correctamente.',
            goalUpdated: 'Fondos de la meta actualizados correctamente.',
            goalDeleted: 'Meta de ahorro eliminada correctamente.',
            habitCreated: 'Hábito creado correctamente.',
            habitDone: 'Hábito completado; la racha se ha incrementado.',
            habitUndone: 'Hábito desmarcado; la racha se ha ajustado.',
            updated: 'Escenario actualizado correctamente.',
            saved: 'Preferencias guardadas correctamente.'
        };
        var error = params.get('error');
        if (error) {
            var decoded = error;
            try {
                decoded = decodeURIComponent(error);
            } catch (ignored) { /* se muestra tal cual */ }
            var map = {delete: 'No se pudo eliminar el registro.',
                edit: 'No se pudo actualizar el escenario.',
                habit: 'No se pudo actualizar el hábito.'};
            showToast(map[decoded] || decoded, true);
            return;
        }
        Object.keys(messages).forEach(function (key) {
            if (params.has(key)) {
                showToast(messages[key], false);
            }
        });
    }

    function restoreScroll() {
        var key = 'scrollPos_' + window.location.pathname;
        var saved = window.sessionStorage.getItem(key);
        if (saved === null) {
            return;
        }
        window.sessionStorage.removeItem(key);
        if (window.location.hash) {
            var target = document.querySelector(window.location.hash);
            if (target) {
                target.scrollIntoView();
                return;
            }
        }
        window.scrollTo(0, parseInt(saved, 10) || 0);
    }

    /** El toast se coloca justo debajo del navbar, aunque crezca al escalar texto. */
    function trackNavbarHeight() {
        var navbar = document.querySelector('.navbar');
        if (!navbar) {
            return;
        }
        var apply = function () {
            document.documentElement.style.setProperty('--toast-top',
                    (navbar.getBoundingClientRect().height + 8) + 'px');
        };
        apply();
        if (window.ResizeObserver) {
            new ResizeObserver(apply).observe(navbar);
        } else {
            window.addEventListener('resize', apply);
        }
    }

    /** Confirmación temática de dos pasos, sin confirm() nativo. */
    function bindConfirmations() {
        document.addEventListener('submit', function (event) {
            var form = event.target;
            if (!(form instanceof HTMLFormElement) || !form.hasAttribute('data-confirm')) {
                return;
            }
            if (form.dataset.confirmed === 'true') {
                return;
            }
            event.preventDefault();
            form.dataset.confirmed = 'true';
            var button = form.querySelector('button[type="submit"]');
            if (button) {
                button.textContent = 'Confirmar';
                button.focus();
            }
            showToast(form.getAttribute('data-confirm'), true);
        }, true);
    }

    /** Los mensajes aria-live presentes en la carga se anuncian una vez. */
    function announceInitialMessages() {
        window.setTimeout(function () {
            var region = document.getElementById('a11y-status');
            if (!region) {
                return;
            }
            var visible = Array.prototype.slice.call(document.querySelectorAll('[aria-live]'))
                    .filter(function (element) {
                        return element !== region
                                && !element.closest('.theme-toast-container')
                                && element.textContent.trim().length > 0;
                    })[0];
            if (visible) {
                region.textContent = visible.textContent.trim();
            }
        }, 200);
    }

    function init() {
        document.addEventListener('submit', function (event) {
            try {
                window.sessionStorage.setItem('scrollPos_' + window.location.pathname,
                        String(window.scrollY));
            } catch (ignored) { /* almacenamiento no disponible */ }
        });
        trackNavbarHeight();
        restoreScroll();
        notifyFromQuery();
        bindConfirmations();
        announceInitialMessages();
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
