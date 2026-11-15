// Smart Banking & ATM Application JavaScript

document.addEventListener('DOMContentLoaded', () => {
    // 1. Live Clock
    const clockEl = document.getElementById('liveClock');
    if (clockEl) {
        const updateClock = () => {
            const now = new Date();
            clockEl.textContent = now.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' });
        };
        updateClock();
        setInterval(updateClock, 1000);
    }

    // 2. Sidebar Mobile Toggle
    const sidebarToggleBtn = document.getElementById('sidebarToggle');
    const sidebar = document.getElementById('appSidebar');
    if (sidebarToggleBtn && sidebar) {
        sidebarToggleBtn.addEventListener('click', () => {
            sidebar.classList.toggle('show');
        });
    }

    // 3. ATM Virtual Keypad logic
    const keypadButtons = document.querySelectorAll('.atm-keypad-btn');
    keypadButtons.forEach(btn => {
        btn.addEventListener('click', (e) => {
            e.preventDefault();
            const val = btn.getAttribute('data-val');
            const targetInputId = btn.closest('.atm-keypad-wrapper')?.getAttribute('data-target') || 'atmPin';
            const input = document.getElementById(targetInputId);
            if (!input) return;

            if (val === 'clear') {
                input.value = '';
            } else if (val === 'back') {
                input.value = input.value.slice(0, -1);
            } else if (input.value.length < 4) {
                input.value += val;
            }
        });
    });

    // 4. Quick Amount Chips
    const amountChips = document.querySelectorAll('.amount-chip');
    amountChips.forEach(chip => {
        chip.addEventListener('click', (e) => {
            e.preventDefault();
            const amt = chip.getAttribute('data-amount');
            const targetInputId = chip.getAttribute('data-target') || 'amount';
            const input = document.getElementById(targetInputId);
            if (input) {
                input.value = amt;
            }
        });
    });

    // 5. Auto-dismiss alerts after 6 seconds
    const alerts = document.querySelectorAll('.auto-dismiss');
    alerts.forEach(alert => {
        setTimeout(() => {
            try {
                const bsAlert = bootstrap.Alert.getOrCreateInstance(alert);
                bsAlert.close();
            } catch (err) {}
        }, 6000);
    });

    // 6. Generic Confirmation Modal
    const confirmTriggers = document.querySelectorAll('[data-confirm]');
    confirmTriggers.forEach(el => {
        el.addEventListener('click', (e) => {
            const message = el.getAttribute('data-confirm');
            if (!confirm(message)) {
                e.preventDefault();
            }
        });
    });
});
