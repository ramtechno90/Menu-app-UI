// Admin App Orchestrator

const App = {
    // State
    currentUser: null,
    viewState: 'landing', // 'landing', 'login-admin', 'dashboard-admin'

    init: function() {
        // Listen to Auth
        if (typeof auth === 'undefined') {
            console.error('Firebase Auth not initialized');
            return;
        }

        auth.onAuthStateChanged(user => {
            this.currentUser = user;
            if (user) {
                this.checkAdminAndRedirect(user);
            } else {
                // Not logged in.
                if (this.viewState === 'dashboard-admin') {
                    this.showLanding();
                }
            }
        });
    },

    checkAdminAndRedirect: function(user) {
        if (typeof db === 'undefined') {
            console.error('Firestore not initialized');
            return;
        }

        // Check Admin
        db.collection('admins').doc(user.uid).get().then(doc => {
            if (doc.exists || user.email === 'admin@pizzaparadize.com') {
                this.showAdminDashboard();
            } else {
                alert('Access Denied: You are not an administrator.');
                auth.signOut();
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

    showAdminDashboard: function() {
        this.hideAllViews();
        this.viewState = 'dashboard-admin';
        this.safeDisplay('admin-dashboard-container', 'block');
        if (typeof AdminApp !== 'undefined') AdminApp.init();
    },

    // Actions
    login: function() {
        const emailId = 'admin-email';
        const passId = 'admin-password';
        const errId = 'admin-login-error';

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
