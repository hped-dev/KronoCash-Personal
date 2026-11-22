/**
 * KronoCash - form-validation.js
 * Sustituye los globos nativos del navegador por notificaciones y mensajes
 * inline integrados con el tema activo (WCAG 2.2 AAA).
 */
(function () {
    'use strict';

    var MESSAGE = 'Revisa los campos marcados: hay datos vacíos o no válidos.';

    function container() {
        var toast = document.querySelector('.theme-toast-container');
        if (toast) {
            return toast;
        }
        toast = document.createElement('div');
        toast.className = 'theme-toast-container';
        toast.setAttribute('aria-live', 'assertive');
        toast.setAttribute('role', 'alert');
        document.body.appendChild(toast);
        return toast;
    }

    function showToast(message) {
        var toast = container();
        toast.textContent = '';
        var item = document.createElement('p');
        item.className = 'theme-toast';
        item.textContent = message;
        toast.appendChild(item);
        window.setTimeout(function () {
            if (item.parentNode) {
                item.parentNode.removeChild(item);
            }
        }, 6000);
    }

    function inlineError(input, message) {
        var existing = input.parentElement.querySelector('.field-error-msg');
        if (existing) {
            existing.remove();
        }
        var error = document.createElement('p');
        error.className = 'field-error-msg';
        error.id = (input.id || 'field') + '-custom-error';
        error.textContent = message || 'Este campo es obligatorio.';
        input.setAttribute('aria-invalid', 'true');
        var describedBy = input.getAttribute('aria-describedby') || '';
        if (describedBy.indexOf(error.id) === -1) {
            input.setAttribute('aria-describedby', (describedBy + ' ' + error.id).trim());
        }
        var host = input.closest('.password-wrapper') || input;
        host.insertAdjacentElement('afterend', error);
    }

    function clearError(input) {
        input.removeAttribute('aria-invalid');
        var host = input.closest('.password-wrapper') || input;
        var error = host.parentElement && host.parentElement.querySelector('.field-error-msg');
        if (error && error.id === (input.id || 'field') + '-custom-error') {
            error.remove();
        }
    }

    function fieldMessage(input) {
        if (input.validity.valueMissing) {
            return 'Este campo es obligatorio.';
        }
        if (input.validity.typeMismatch) {
            return 'Introduce un valor válido (por ejemplo, un correo electrónico).';
        }
        if (input.validity.tooShort) {
            return 'El valor es demasiado corto.';
        }
        if (input.validity.patternMismatch) {
            return 'El formato no es correcto.';
        }
        return 'Revisa este campo.';
    }

    function validate(form) {
        var fields = Array.prototype.slice.call(form.querySelectorAll('input, select, textarea'));
        var firstInvalid = null;
        fields.forEach(function (input) {
            if (input.disabled || input.type === 'hidden' || input.type === 'button') {
                return;
            }
            if (!input.checkValidity()) {
                inlineError(input, fieldMessage(input));
                if (!firstInvalid) {
                    firstInvalid = input;
                }
            } else {
                clearError(input);
            }
        });
        if (firstInvalid) {
            showToast(MESSAGE);
            var bubble = document.getElementById('mascot-message');
            if (bubble) {
                bubble.textContent = 'Hay campos por revisar en el formulario.';
            }
            firstInvalid.focus();
            return false;
        }
        return true;
    }

    function init() {
        // Delegación: también valida formularios creados dinámicamente.
        document.addEventListener('submit', function (event) {
            var form = event.target;
            if (form instanceof HTMLFormElement && form.hasAttribute('novalidate')) {
                if (!validate(form)) {
                    event.preventDefault();
                    event.stopPropagation();
                }
            }
        }, true);

        document.addEventListener('input', function (event) {
            var input = event.target;
            if (input.getAttribute && input.getAttribute('aria-invalid') === 'true' && input.checkValidity()) {
                clearError(input);
            }
        });
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
