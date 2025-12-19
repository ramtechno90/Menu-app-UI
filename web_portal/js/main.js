// Main App Orchestrator

const App = {
    // State
    currentUser: null,
    currentRole: null, // 'admin' | 'staff'
    viewState: 'landing', // 'landing', 'login-admin', 'login-staff', 'dashboard-admin', 'dashboard-staff'

    init: function() {
        if (window.isConfigMissing) {
            this.showSetup();
            return;
        }

        // Listen to Auth
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

    showSetup: function() {
        this.hideAllViews();
        document.getElementById('setup-container').style.display = 'block';
    },

    saveSetup: function() {
        const jsonStr = document.getElementById('config-input').value;
        try {
            // Allow loose JSON (e.g. copied from JS object) by using a regex or just strict JSON.
            // Strict JSON is safer.
            const config = JSON.parse(jsonStr);
            if (!config.apiKey) throw new Error("Missing apiKey in JSON");

            localStorage.setItem('firebase_config', JSON.stringify(config));
            location.reload();
        } catch(e) {
            alert("Invalid JSON configuration: " + e.message);
        }
    },

    determineRoleAndRedirect: function(user) {
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

    // Navigation / View Switching
    hideAllViews: function() {
        document.querySelectorAll('.view-container').forEach(el => el.style.display = 'none');
        // Also cleanup logic
        AdminApp.cleanup();
        DeliveryApp.cleanup();
    },

    showLanding: function() {
        this.hideAllViews();
        this.viewState = 'landing';
        document.getElementById('landing-container').style.display = 'block';
    },

    showAdminLogin: function() {
        if (this.currentUser) return; // Should have redirected already
        this.hideAllViews();
        this.viewState = 'login-admin';
        document.getElementById('admin-login-container').style.display = 'block';
    },

    showDeliveryLogin: function() {
        if (this.currentUser) return;
        this.hideAllViews();
        this.viewState = 'login-staff';
        document.getElementById('delivery-login-container').style.display = 'block';
    },

    showAdminDashboard: function() {
        this.hideAllViews();
        this.viewState = 'dashboard-admin';
        document.getElementById('admin-dashboard-container').style.display = 'block';
        AdminApp.init();
    },

    showDeliveryDashboard: function(staffName) {
        this.hideAllViews();
        this.viewState = 'dashboard-staff';
        document.getElementById('delivery-dashboard-container').style.display = 'block';
        DeliveryApp.init(staffName);
    },

    // Actions
    login: function(role) {
        const emailId = role === 'admin' ? 'admin-email' : 'delivery-email';
        const passId = role === 'admin' ? 'admin-password' : 'delivery-password';
        const errId = role === 'admin' ? 'admin-login-error' : 'delivery-login-error';

        const email = document.getElementById(emailId).value;
        const pass = document.getElementById(passId).value;

        auth.signInWithEmailAndPassword(email, pass).catch(err => {
            document.getElementById(errId).innerText = err.message;
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
