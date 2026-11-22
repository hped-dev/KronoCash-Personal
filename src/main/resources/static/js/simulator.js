/**
 * KronoCash - simulator.js (JavaScript Vanilla, sin frameworks)
 *
 * - Pares deslizador/campo numérico sincronizados en ambos sentidos.
 * - POST /api/simulator/calculate para obtener la proyección.
 * - Gráfica SVG responsiva: todos sus colores se resuelven desde las
 *   variables CSS del tema activo (--accent-color, --text-main,
 *   --bg-secondary) mediante clases definidas en base.css.
 * - Tabla con aria-label y los MISMOS datos mes a mes, actualizada en cada
 *   cálculo (SC 1.1.1 / 4.1.2).
 */
(function () {
    'use strict';

    var form = document.getElementById('simulator-form');
    if (!form) {
        return;
    }

    var SVG_NS = 'http://www.w3.org/2000/svg';
    var PAIRS = [
        {range: 'monthlyIncome', number: 'monthlyIncomeNumber', output: 'monthlyIncomeValue', kind: 'money'},
        {range: 'savingsPercentage', number: 'savingsPercentageNumber', output: 'savingsPercentageValue', kind: 'percent'},
        {range: 'antExpensesCut', number: 'antExpensesCutNumber', output: 'antExpensesCutValue', kind: 'money'},
        {range: 'monthsHorizon', number: 'monthsHorizonNumber', output: 'monthsHorizonValue', kind: 'months'}
    ];

    var moneyFormatter = new Intl.NumberFormat('es-MX', {
        style: 'currency', currency: 'MXN', minimumFractionDigits: 2, maximumFractionDigits: 2
    });
    var compactFormatter = new Intl.NumberFormat('es-MX', {notation: 'compact', maximumFractionDigits: 1});
    var timer = null;
    var lastResult = null;
    var goalSelect = byId('goalSelect');

    function byId(id) {
        return document.getElementById(id);
    }

    function money(amount) {
        return moneyFormatter.format(Number(amount) || 0);
    }

    function clamp(value, input) {
        var number = Number(value);
        var min = Number(input.min);
        var max = Number(input.max);
        if (!isFinite(number)) {
            number = min;
        }
        return Math.min(max, Math.max(min, number));
    }

    function readNumber(pair) {
        var numberInput = byId(pair.number);
        if (numberInput.value === '') {
            return Number(byId(pair.range).value);
        }
        return clamp(numberInput.value, byId(pair.range));
    }

    function updateOutput(pair) {
        var value = readNumber(pair);
        var text;
        if (pair.kind === 'money') {
            text = money(value);
        } else if (pair.kind === 'percent') {
            text = value + ' %';
        } else {
            text = value + ' meses';
        }
        byId(pair.output).textContent = text;
    }

    function rangeChanged(event) {
        PAIRS.forEach(function (pair) {
            if (pair.range === event.target.id) {
                byId(pair.number).value = event.target.value;
                updateOutput(pair);
            }
        });
        scheduleCalculation();
    }

    function numberChanged(event) {
        PAIRS.forEach(function (pair) {
            if (pair.number === event.target.id) {
                if (event.target.value !== '') {
                    var value = clamp(event.target.value, byId(pair.range));
                    event.target.value = String(value);
                    byId(pair.range).value = String(value);
                }
                updateOutput(pair);
            }
        });
        scheduleCalculation();
    }

    function numberCommitted(event) {
        PAIRS.forEach(function (pair) {
            if (pair.number === event.target.id && event.target.value === '') {
                event.target.value = byId(pair.range).value;
                updateOutput(pair);
            }
        });
    }

    function monthlySavings() {
        var income = readNumber(PAIRS[0]);
        var percentage = readNumber(PAIRS[1]);
        var cut = readNumber(PAIRS[2]);
        return income * percentage / 100 + cut;
    }

    function selectedGoalRemaining() {
        if (!goalSelect || !goalSelect.value) {
            return null;
        }
        var option = goalSelect.options[goalSelect.selectedIndex];
        return Number(option.getAttribute('data-remaining')) || 0;
    }

    /** Meses necesarios para alcanzar la meta con el ahorro mensual proyectado. */
    function goalMonths() {
        var remaining = selectedGoalRemaining();
        if (remaining === null) {
            return null;
        }
        var monthly = monthlySavings();
        if (monthly <= 0) {
            return 120;
        }
        return Math.min(120, Math.max(1, Math.ceil(remaining / monthly)));
    }

    function updateGoalMode() {
        var active = selectedGoalRemaining() !== null;
        ['monthsHorizon', 'monthsHorizonNumber'].forEach(function (id) {
            var element = byId(id);
            if (element) {
                element.disabled = active;
            }
        });
        var months = goalMonths();
        if (months !== null) {
            byId('monthsHorizon').value = String(months);
            byId('monthsHorizonNumber').value = String(months);
            updateOutput(PAIRS[3]);
        }
    }

    function payload() {
        var request = {scenarioName: byId('scenarioName').value};
        PAIRS.forEach(function (pair) {
            request[pair.range] = readNumber(pair);
        });
        var months = goalMonths();
        if (months !== null) {
            request.monthsHorizon = months;
        }
        return request;
    }

    function status(message) {
        byId('simulator-status').textContent = message;
    }

    function svgElement(name, attributes) {
        var element = document.createElementNS(SVG_NS, name);
        Object.keys(attributes).forEach(function (key) {
            element.setAttribute(key, attributes[key]);
        });
        return element;
    }

    function addLabel(svg, x, y, text, anchor) {
        var label = svgElement('text', {
            x: x, y: y, 'class': 'chart-label', 'text-anchor': anchor || 'start'
        });
        label.textContent = text;
        svg.appendChild(label);
    }

    /** Gráfica SVG: usa SOLO clases que consumen las variables del tema. */
    function renderChart(result) {
        var svg = byId('grafica');
        svg.textContent = '';
        var rows = result.breakdown || [];
        if (!rows.length) {
            return;
        }

        var width = 640;
        var height = 260;
        var left = 64;
        var right = 16;
        var top = 16;
        var bottom = 36;
        var plotWidth = width - left - right;
        var plotHeight = height - top - bottom;
        var baseY = top + plotHeight;
        var max = Number(result.projectedTotal) || 1;
        var stepX = rows.length > 1 ? plotWidth / (rows.length - 1) : 0;

        for (var i = 0; i <= 4; i++) {
            var y = top + (plotHeight / 4) * i;
            svg.appendChild(svgElement('line', {
                x1: left, y1: y, x2: width - right, y2: y, 'class': 'chart-grid'
            }));
            addLabel(svg, left - 8, y + 4, compactFormatter.format(max * (4 - i) / 4), 'end');
        }

        var points = rows.map(function (row, index) {
            var x = left + stepX * index;
            var y = baseY - (Number(row.accumulatedTotal) / max) * plotHeight;
            return {x: x, y: y, row: row};
        });

        var linePath = points.map(function (point, index) {
            return (index === 0 ? 'M ' : 'L ') + point.x.toFixed(1) + ' ' + point.y.toFixed(1);
        }).join(' ');

        svg.appendChild(svgElement('path', {
            d: linePath + ' L ' + points[points.length - 1].x.toFixed(1) + ' ' + baseY
                    + ' L ' + points[0].x.toFixed(1) + ' ' + baseY + ' Z',
            'class': 'chart-area'
        }));
        svg.appendChild(svgElement('path', {d: linePath, 'class': 'chart-line'}));

        if (points.length <= 24) {
            points.forEach(function (point) {
                svg.appendChild(svgElement('circle', {
                    cx: point.x.toFixed(1), cy: point.y.toFixed(1), r: 3, 'class': 'chart-point'
                }));
            });
        }

        svg.appendChild(svgElement('line', {
            x1: left, y1: baseY, x2: width - right, y2: baseY, 'class': 'chart-axis'
        }));
        addLabel(svg, left, height - 10, 'Mes 1', 'start');
        addLabel(svg, left + plotWidth / 2, height - 10, 'Mes ' + Math.ceil(rows.length / 2), 'middle');
        addLabel(svg, width - right, height - 10, 'Mes ' + rows.length, 'end');
    }

    /** Tabla sincronizada: mismos datos que la gráfica, en texto. */
    function renderTable(result) {
        var body = byId('desglose-body');
        body.textContent = '';
        (result.breakdown || []).forEach(function (row) {
            var tr = document.createElement('tr');
            var values = [
                row.month,
                money(row.accumulatedBaseSavings),
                money(row.accumulatedExtraSavings),
                money(row.accumulatedTotal)
            ];
            values.forEach(function (value, index) {
                var cell = document.createElement(index === 0 ? 'th' : 'td');
                if (index === 0) {
                    cell.setAttribute('scope', 'row');
                }
                cell.textContent = String(value);
                tr.appendChild(cell);
            });
            body.appendChild(tr);
        });
    }

    function render(result) {
        lastResult = result;
        byId('res-base').textContent = money(result.baseMonthlySavings);
        byId('res-extra').textContent = money(result.extraMonthlySavings);
        byId('res-total').textContent = money(result.projectedTotal);
        var remaining = selectedGoalRemaining();
        var goalTime = byId('res-goal-time');
        if (remaining !== null && remaining > 0) {
            goalTime.textContent = result.monthsHorizon + ' meses para completar la meta (faltan '
                    + money(remaining) + ').';
        } else {
            goalTime.textContent = 'Sin meta seleccionada (horizonte manual).';
        }
        renderChart(result);
        renderTable(result);
        status('Proyección calculada a ' + result.monthsHorizon + ' meses. Total acumulado: '
                + money(result.projectedTotal) + '. La tabla contiene el desglose mes a mes.');
    }

    function post(url, body) {
        var token = document.querySelector('meta[name="_csrf"]');
        var header = document.querySelector('meta[name="_csrf_header"]');
        var headers = {'Content-Type': 'application/json', 'Accept': 'application/json'};
        if (token && header) {
            headers[header.content] = token.content;
        }
        return fetch(url, {
            method: 'POST',
            headers: headers,
            credentials: 'same-origin',
            body: JSON.stringify(body)
        }).then(function (response) {
            if (!response.ok) {
                throw new Error('HTTP ' + response.status);
            }
            return response.json();
        });
    }

    function calculate() {
        post('/api/simulator/calculate', payload())
                .then(render)
                .catch(function () {
                    status('No se pudo calcular la proyección. Inténtelo de nuevo.');
                });
    }

    function scheduleCalculation() {
        if (timer !== null) {
            window.clearTimeout(timer);
        }
        timer = window.setTimeout(function () {
            timer = null;
            calculate();
        }, 250);
    }

    PAIRS.forEach(function (pair) {
        updateOutput(pair);
        byId(pair.range).addEventListener('input', rangeChanged);
        byId(pair.number).addEventListener('input', numberChanged);
        byId(pair.number).addEventListener('change', numberCommitted);
    });

    if (goalSelect) {
        goalSelect.addEventListener('change', function () {
            updateGoalMode();
            calculate();
        });
        updateGoalMode();
    }

    /** Carga un escenario guardado en los controles actuales. */
    document.querySelectorAll('.load-scenario').forEach(function (link) {
        link.addEventListener('click', function (event) {
            event.preventDefault();
            var data = link.dataset;
            byId('scenarioName').value = data.name;
            ['income', 'percentage', 'cut', 'months'].forEach(function (key, index) {
                var pair = PAIRS[index];
                byId(pair.range).value = data[key];
                byId(pair.number).value = data[key];
                updateOutput(pair);
            });
            if (goalSelect) {
                goalSelect.value = '';
                updateGoalMode();
            }
            calculate();
            status('Escenario «' + data.name + '» cargado en el simulador.');
        });
    });

    var lastEditOpener = null;

    function closeEditForm() {
        var container = document.getElementById('scenario-edit-form');
        if (container) {
            container.remove();
        }
        if (lastEditOpener) {
            lastEditOpener.focus();
            lastEditOpener = null;
        }
    }

    /** Formulario de edición accesible: Cancelar, Escape y foco gestionado. */
    document.querySelectorAll('.edit-scenario').forEach(function (link) {
        link.addEventListener('click', function (event) {
            event.preventDefault();
            var data = link.dataset;
            closeEditForm();
            var container = document.createElement('section');
            container.className = 'card';
            container.id = 'scenario-edit-form';
            container.setAttribute('aria-labelledby', 'editar-escenario-titulo');

            var heading = document.createElement('h3');
            heading.id = 'editar-escenario-titulo';
            heading.textContent = 'Editar escenario';
            container.appendChild(heading);

            var editForm = document.createElement('form');
            editForm.method = 'post';
            editForm.action = '/simulator/scenarios/' + encodeURIComponent(data.id) + '/edit';
            editForm.noValidate = true;

            var fields = [
                ['scenarioName', 'Nombre', 'text', data.name, '100'],
                ['monthlyIncome', 'Ingreso mensual (MXN)', 'number', data.income, null, '0', '25000', '0.01'],
                ['savingsPercentage', 'Porcentaje de ahorro', 'number', data.percentage, null, '0', '100', '0.01'],
                ['antExpensesCut', 'Recorte de gastos hormiga (MXN)', 'number', data.cut, null, '0', null, '0.01'],
                ['monthsHorizon', 'Horizonte en meses', 'number', data.months, null, '1', '120', '1']
            ];

            fields.forEach(function (field, index) {
                var id = 'edit-' + field[0] + '-js';
                var group = document.createElement('div');
                var label = document.createElement('label');
                label.htmlFor = id;
                label.textContent = field[1];
                var input = document.createElement('input');
                input.id = id;
                input.name = field[0];
                input.type = field[2];
                input.value = field[3];
                input.required = true;
                if (field[4]) {
                    input.maxLength = Number(field[4]);
                }
                if (field[5]) {
                    input.min = field[5];
                }
                if (field[6]) {
                    input.max = field[6];
                }
                if (field[7]) {
                    input.step = field[7];
                }
                group.appendChild(label);
                group.appendChild(input);
                editForm.appendChild(group);
                if (index === 0) {
                    input.setAttribute('data-edit-first', 'true');
                }
            });

            var save = document.createElement('button');
            save.type = 'submit';
            save.className = 'touch-target';
            save.textContent = 'Guardar cambios';
            var cancel = document.createElement('button');
            cancel.type = 'button';
            cancel.className = 'touch-target';
            cancel.textContent = 'Cancelar';
            cancel.addEventListener('click', closeEditForm);
            var actions = document.createElement('div');
            actions.className = 'form-actions';
            actions.appendChild(save);
            cancel.className = 'btn-secondary touch-target';
            actions.appendChild(cancel);
            editForm.appendChild(actions);
            container.appendChild(editForm);

            var section = document.getElementById('seccion-escenarios');
            section.appendChild(container);
            lastEditOpener = link;
            container.querySelector('[data-edit-first]').focus();
        });
    });

    document.addEventListener('keydown', function (event) {
        if (event.key === 'Escape' && document.getElementById('scenario-edit-form')) {
            closeEditForm();
        }
    });

    form.addEventListener('submit', function (event) {
        event.preventDefault();
        calculate();
    });

    byId('saveBtn').addEventListener('click', function (event) {
        event.preventDefault();
        post('/api/simulator/save', payload())
                .then(function () {
                    status('Escenario guardado correctamente en su cuenta.');
                })
                .catch(function () {
                    status('No se pudo guardar el escenario. Inténtelo de nuevo.');
                });
    });

    // Si el usuario cambia de tema, la gráfica vuelve a resolverse con los
    // valores vigentes de las variables CSS.
    document.addEventListener('kronocash:preferences', function () {
        if (lastResult) {
            renderChart(lastResult);
        }
    });

    calculate();
})();
