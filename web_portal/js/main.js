// Main App Orchestrator

const App = {
    // State
    currentUser: null,
    currentRole: null, // 'admin' | 'staff'
    viewState: 'landing', // 'landing', 'login-admin', 'login-staff', 'dashboard-admin', 'dashboard-staff'

    init: function() {
        // Listen to Auth
        if (typeof auth === 'undefined') {
            console.error('Firebase Auth not initialized');
            return;
        }

        auth.onAuthStateChanged(user => {
            this.currentUser = user;
            if (user) {
                this.determineRoleAndRedirect(user);
            } else {
                // Not logged in.
                // If we are on a dashboard, kick to landing.
                // If we are on a login screen, stay there.
                // If we are on landing, stay there.
                if (this.viewState.startsWith('dashboard')) {
                    this.showLanding();
                }
            }
        });
    },

    determineRoleAndRedirect: function(user) {
        if (typeof db === 'undefined') {
            console.error('Firestore not initialized');
            return;
        }

        // Check Admin
        db.collection('admins').doc(user.uid).get().then(doc => {
            if (doc.exists || user.email === 'admin@pizzaparadize.com') {
                this.currentRole = 'admin';
                this.showAdminDashboard();
            } else {
                // Check Staff
                db.collection('delivery_staff').doc(user.uid).get().then(doc => {
                    if (doc.exists) {
                        this.currentRole = 'staff';
                        this.showDeliveryDashboard(doc.data().name);
                    } else {
                        alert('Unknown User Role');
                        auth.signOut();
                    }
                });
            }
        }).catch(err => {
            console.error(err);
            auth.signOut();
        });
    },

    // Helper for safe DOM access
    safeDisplay: function(id, display) {
        const el = document.getElementById(id);
        if (el) {
            el.style.display = display;
        } else {
            console.error(`Element with id '${id}' not found.`);
        }
    },

    // Navigation / View Switching
    hideAllViews: function() {
        document.querySelectorAll('.view-container').forEach(el => el.style.display = 'none');
        // Also cleanup logic
        if (typeof AdminApp !== 'undefined') AdminApp.cleanup();
        if (typeof DeliveryApp !== 'undefined') DeliveryApp.cleanup();
    },

    showLanding: function() {
        this.hideAllViews();
        this.viewState = 'landing';
        this.safeDisplay('landing-container', 'block');
    },

    showAdminLogin: function() {
        if (this.currentUser) return; // Should have redirected already
        this.hideAllViews();
        this.viewState = 'login-admin';
        this.safeDisplay('admin-login-container', 'block');
    },

    showDeliveryLogin: function() {
        if (this.currentUser) return;
        this.hideAllViews();
        this.viewState = 'login-staff';
        this.safeDisplay('delivery-login-container', 'block');
    },

    showAdminDashboard: function() {
        this.hideAllViews();
        this.viewState = 'dashboard-admin';
        this.safeDisplay('admin-dashboard-container', 'block');
        if (typeof AdminApp !== 'undefined') AdminApp.init();
    },

    showDeliveryDashboard: function(staffName) {
        this.hideAllViews();
        this.viewState = 'dashboard-staff';
        this.safeDisplay('delivery-dashboard-container', 'block');
        if (typeof DeliveryApp !== 'undefined') DeliveryApp.init(staffName);
    },

    // Actions
    login: function(role) {
        const emailId = role === 'admin' ? 'admin-email' : 'delivery-email';
        const passId = role === 'admin' ? 'admin-password' : 'delivery-password';
        const errId = role === 'admin' ? 'admin-login-error' : 'delivery-login-error';

        const emailEl = document.getElementById(emailId);
        const passEl = document.getElementById(passId);

        if (!emailEl || !passEl) {
            console.error('Login inputs not found');
            return;
        }

        const email = emailEl.value;
        const pass = passEl.value;

        auth.signInWithEmailAndPassword(email, pass).catch(err => {
            const errEl = document.getElementById(errId);
            if (errEl) errEl.innerText = err.message;
        });
    },

    logout: function() {
        auth.signOut().then(() => {
            this.showLanding();
        });
    }
};

// Start
App.init();
