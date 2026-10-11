/**
 * Personal Finance Management Platform - Frontend Interactivity & Charts
 * Stack: Chart.js (CDN) + Vanilla JS
 */

document.addEventListener('DOMContentLoaded', function () {
    // 1. Mobile Sidebar Drawer Logic
    const sidebar = document.getElementById('appSidebar');
    const toggleBtn = document.getElementById('sidebarToggleBtn');
    const backdrop = document.getElementById('sidebarBackdrop');

    if (toggleBtn && sidebar && backdrop) {
        toggleBtn.addEventListener('click', function () {
            sidebar.classList.toggle('show');
            backdrop.classList.toggle('show');
        });

        backdrop.addEventListener('click', function () {
            sidebar.classList.remove('show');
            backdrop.classList.remove('show');
        });
    }

    // 2. Initialize Charts if canvases exist on current page
    initCategoryDoughnut();
    initSpendingLineChart();
    initAdvisorProgressChart();

    // 3. Admin Settings Category Weights Live Validator
    initWeightValidator();

    // 4. Form Submission Interactivity & Success Toast
    initPreviewForms();

    // 5. Advice Card Actions
    initAdviceCardInteractions();
});

/**
 * Chart 1: Doughnut Chart of Spending by Category (Section 7 numbers)
 * Food: Rs. 1,200.00 | Savings: Rs. 600.00 | Emergency: Rs. 450.00 | Entertainment: Rs. 300.00 | Other: Rs. 450.00
 */
function initCategoryDoughnut() {
    const canvas = document.getElementById('categoryDoughnutChart');
    if (!canvas) return;

    const ctx = canvas.getContext('2d');
    
    // Check if dynamic JSP data attributes are provided
    let labels = ['Food (40%)', 'Savings (20%)', 'Emergency (15%)', 'Entertainment (10%)', 'Other (15%)'];
    let dataValues = [1200.00, 600.00, 450.00, 300.00, 450.00];

    if (canvas.dataset.labels && canvas.dataset.values) {
        try {
            labels = JSON.parse(canvas.dataset.labels);
            dataValues = JSON.parse(canvas.dataset.values);
        } catch (e) {
            console.warn("Using default doughnut chart values due to parse exception:", e);
        }
    }

    const colors = ['#4f46e5', '#0d9488', '#f59e0b', '#ec4899', '#64748b', '#8b5cf6', '#06b6d4'];

    new Chart(ctx, {
        type: 'doughnut',
        data: {
            labels: labels,
            datasets: [{
                data: dataValues,
                backgroundColor: colors.slice(0, labels.length),
                borderWidth: 2,
                borderColor: '#ffffff',
                hoverOffset: 6
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            cutout: '72%',
            plugins: {
                legend: {
                    position: 'bottom',
                    labels: {
                        boxWidth: 12,
                        padding: 14,
                        font: { family: "'Plus Jakarta Sans', sans-serif", size: 12, weight: 600 },
                        color: '#475569'
                    }
                },
                tooltip: {
                    callbacks: {
                        label: function (context) {
                            const val = Number(context.raw).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
                            return ' ' + context.label + ': Rs. ' + val;
                        }
                    }
                }
            }
        }
    });
}

/**
 * Chart 2: Daily Spending vs Daily Limit Line Chart
 * Food daily limit benchmark = Rs. 40.00
 */
function initSpendingLineChart() {
    const canvas = document.getElementById('dailySpendingLineChart');
    if (!canvas) return;

    const ctx = canvas.getContext('2d');
    const days = Array.from({ length: 15 }, (_, i) => 'Oct ' + (i + 1));
    const actualSpending = [34.50, 42.00, 28.00, 39.50, 45.00, 31.00, 38.00, 40.00, 52.00, 30.00, 35.50, 48.00, 26.00, 38.00, 44.00];
    const dailyLimitBenchmark = Array(15).fill(40.00);

    const gradient = ctx.createLinearGradient(0, 0, 0, 300);
    gradient.addColorStop(0, 'rgba(79, 70, 229, 0.25)');
    gradient.addColorStop(1, 'rgba(79, 70, 229, 0.00)');

    new Chart(ctx, {
        type: 'line',
        data: {
            labels: days,
            datasets: [
                {
                    label: 'Actual Daily Spend (Rs.)',
                    data: actualSpending,
                    borderColor: '#4f46e5',
                    backgroundColor: gradient,
                    fill: true,
                    tension: 0.35,
                    borderWidth: 2.5,
                    pointBackgroundColor: '#4f46e5',
                    pointRadius: 3.5,
                    pointHoverRadius: 6
                },
                {
                    label: 'Pace Limit (Rs. 40.00 / day)',
                    data: dailyLimitBenchmark,
                    borderColor: '#ef4444',
                    borderWidth: 2,
                    borderDash: [6, 4],
                    pointRadius: 0,
                    fill: false
                }
            ]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            interaction: { mode: 'index', intersect: false },
            scales: {
                y: {
                    beginAtZero: true,
                    grid: { color: '#f1f5f9' },
                    ticks: {
                        callback: function (val) { return 'Rs. ' + val; },
                        font: { family: "'Plus Jakarta Sans', sans-serif", size: 11 }
                    }
                },
                x: {
                    grid: { display: false },
                    ticks: { font: { family: "'Plus Jakarta Sans', sans-serif", size: 11 } }
                }
            },
            plugins: {
                legend: {
                    position: 'top',
                    align: 'end',
                    labels: {
                        boxWidth: 14,
                        font: { family: "'Plus Jakarta Sans', sans-serif", size: 12, weight: 600 }
                    }
                },
                tooltip: {
                    callbacks: {
                        label: function (ctx) {
                            return ' ' + ctx.dataset.label + ': Rs. ' + ctx.raw.toFixed(2);
                        }
                    }
                }
            }
        }
    });
}

/**
 * Chart 3: Advisor Dashboard Horizontal Bar Progress Chart
 */
function initAdvisorProgressChart() {
    const canvas = document.getElementById('advisorClientProgressChart');
    if (!canvas) return;

    const ctx = canvas.getContext('2d');
    let clients = ['John Doe (User #3)', 'Jane Smith (User #4)', 'Rahul Verma (User #7)', 'Priya Patel (User #8)'];
    let budgetConsumption = [68, 92, 115, 45]; // % used so far

    if (canvas.dataset.labels && canvas.dataset.values) {
        try {
            const parsedLabels = JSON.parse(canvas.dataset.labels);
            const parsedValues = JSON.parse(canvas.dataset.values);
            if (parsedLabels && parsedLabels.length > 0) {
                clients = parsedLabels;
                budgetConsumption = parsedValues;
            }
        } catch (e) {
            console.warn('Could not parse advisor chart data attributes', e);
        }
    }

    new Chart(ctx, {
        type: 'bar',
        data: {
            labels: clients,
            datasets: [{
                label: '% Budget Consumed',
                data: budgetConsumption,
                backgroundColor: budgetConsumption.map(val => val > 100 ? '#ef4444' : (val > 80 ? '#f59e0b' : '#10b981')),
                borderRadius: 6,
                barThickness: 18
            }]
        },
        options: {
            indexAxis: 'y',
            responsive: true,
            maintainAspectRatio: false,
            scales: {
                x: {
                    min: 0,
                    max: 140,
                    grid: { color: '#f1f5f9' },
                    ticks: { callback: function (v) { return v + '%'; } }
                },
                y: {
                    grid: { display: false }
                }
            },
            plugins: {
                legend: { display: false },
                tooltip: {
                    callbacks: {
                        label: function (ctx) {
                            return ' ' + ctx.raw + '% of Monthly Disposable Envelope spent';
                        }
                    }
                }
            }
        }
    });
}

/**
 * Toast Notification Utility
 */
function showToast(title, message, type = 'success') {
    let container = document.querySelector('.toast-container-custom');
    if (!container) {
        container = document.createElement('div');
        container.className = 'toast-container-custom';
        document.body.appendChild(container);
    }

    const toastEl = document.createElement('div');
    toastEl.className = 'toast toast-clean show mb-2';
    toastEl.setAttribute('role', 'alert');
    toastEl.setAttribute('aria-live', 'assertive');
    toastEl.setAttribute('aria-atomic', 'true');

    const badgeClass = type === 'success' ? 'bg-success' : (type === 'danger' ? 'bg-danger' : 'bg-primary');
    const icon = type === 'success' ? '✓' : (type === 'danger' ? '✕' : 'ℹ');

    toastEl.innerHTML = `
        <div class="toast-header border-bottom-0 pb-1">
            <span class="badge ${badgeClass} me-2 rounded-circle px-2">${icon}</span>
            <strong class="me-auto text-dark" style="font-size: 0.9rem;">${title}</strong>
            <small class="text-muted">Just now</small>
            <button type="button" class="btn-close ms-2" onclick="this.closest('.toast').remove()"></button>
        </div>
        <div class="toast-body pt-0 text-secondary" style="font-size: 0.85rem;">
            ${message}
        </div>
    `;

    container.appendChild(toastEl);
    setTimeout(() => {
        toastEl.classList.remove('show');
        setTimeout(() => toastEl.remove(), 300);
    }, 4000);
}

/**
 * Live Category Weights Validator for Admin Settings
 * Live check that Category Weights sum to exactly 100%
 */
function initWeightValidator() {
    const weightInputs = document.querySelectorAll('.weight-input');
    const sumDisplay = document.getElementById('weightSumDisplay');
    const sumProgressBar = document.getElementById('weightSumProgress');
    const submitBtn = document.getElementById('saveSettingsBtn');
    const validationAlert = document.getElementById('weightValidationAlert');

    if (!weightInputs.length || !sumDisplay) return;

    function recalculateWeights() {
        let total = 0;
        weightInputs.forEach(input => {
            const val = parseFloat(input.value) || 0;
            total += val;
        });

        sumDisplay.textContent = total + '%';
        if (sumProgressBar) {
            sumProgressBar.style.width = Math.min(total, 100) + '%';
        }

        if (total === 100) {
            sumDisplay.className = 'badge bg-success fs-6';
            if (sumProgressBar) sumProgressBar.className = 'progress-bar bg-success';
            if (validationAlert) {
                validationAlert.className = 'alert alert-success d-flex align-items-center py-2 px-3 small';
                validationAlert.innerHTML = '<strong>✓ Valid:</strong> Category allocations equal exactly 100%.';
            }
            if (submitBtn) submitBtn.disabled = false;
        } else {
            sumDisplay.className = 'badge bg-danger fs-6';
            if (sumProgressBar) sumProgressBar.className = 'progress-bar bg-danger';
            if (validationAlert) {
                validationAlert.className = 'alert alert-danger d-flex align-items-center py-2 px-3 small';
                validationAlert.innerHTML = `<strong>⚠️ Attention:</strong> Total must equal exactly 100% (currently ${total}%).`;
            }
            if (submitBtn) submitBtn.disabled = true;
        }
    }

    weightInputs.forEach(input => {
        input.addEventListener('input', recalculateWeights);
    });

    recalculateWeights();
}

/**
 * Static preview forms interceptor to provide realistic feedback
 */
function initPreviewForms() {
    const forms = document.querySelectorAll('form[data-preview-handler]');
    forms.forEach(form => {
        form.addEventListener('submit', function (e) {
            e.preventDefault();
            const actionName = form.getAttribute('data-preview-action') || 'Saved';
            showToast('Success', `${actionName} successfully! (Preview Mode)`);

            // If inside a modal, close it
            const modalEl = form.closest('.modal');
            if (modalEl && window.bootstrap) {
                const modalInstance = bootstrap.Modal.getInstance(modalEl);
                if (modalInstance) modalInstance.hide();
            }
        });
    });
}

/**
 * Advice Card Interactive Actions
 */
function initAdviceCardInteractions() {
    document.querySelectorAll('[data-advice-action]').forEach(btn => {
        btn.addEventListener('click', function () {
            const card = this.closest('.advice-card');
            const action = this.getAttribute('data-advice-action');
            const title = card ? card.querySelector('.fw-bold').textContent.trim() : 'Advice';

            if (action === 'accept') {
                showToast('Advice Accepted', `Applied recommendation for ${title}. Daily limits updated!`);
            } else if (action === 'ignore') {
                showToast('Dismissed', `Dismissed alert for ${title}.`);
            } else if (action === 'edit') {
                showToast('Adjusting Target', `Opening allocation adjustment for ${title}...`);
            }

            if (card) {
                card.style.opacity = '0.5';
                card.style.pointerEvents = 'none';
                setTimeout(() => card.remove(), 400);
            }
        });
    });
}
