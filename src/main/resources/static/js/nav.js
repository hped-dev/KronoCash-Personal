/**
 * KronoCash - nav.js
 * Menú accesible para móvil/tablet (<= 900px).
 */
(function () {
    'use strict';

    var toggle = document.querySelector('.nav-toggle-btn');
    var menu = document.getElementById('primary-nav-menu');
    if (!toggle || !menu) {
        return;
    }

    var list = menu.querySelector('.navbar-list');
    var label = toggle.querySelector('.nav-toggle-label');

    function setOpen(open) {
        toggle.setAttribute('aria-expanded', String(open));
        toggle.setAttribute('aria-label', open ? 'Cerrar menú' : 'Abrir menú');
        if (label) {
            label.textContent = open ? 'Cerrar' : 'Menú';
        }
        list.classList.toggle('is-open', open);
        if (open) {
            var first = list.querySelector('a, button');
            if (first) {
                first.focus();
            }
        }
    }

    toggle.addEventListener('click', function () {
        setOpen(toggle.getAttribute('aria-expanded') !== 'true');
    });

    list.addEventListener('click', function (event) {
        if (event.target.closest('a, button')) {
            setOpen(false);
        }
    });

    document.addEventListener('keydown', function (event) {
        if (event.key === 'Escape' && toggle.getAttribute('aria-expanded') === 'true') {
            event.stopImmediatePropagation();
            setOpen(false);
            toggle.focus();
        }
    });

    window.addEventListener('resize', function () {
        if (window.innerWidth > 900 && toggle.getAttribute('aria-expanded') === 'true') {
            setOpen(false);
        }
    });
})();
