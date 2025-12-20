// Delivery App Orchestrator

const App = {
    // State
    currentUser: null,
    viewState: 'landing', // 'landing', 'login-staff', 'dashboard-staff'

    init: function() {
        // Listen to Auth
        if (typeof auth === 'undefined') {
            console.error('Firebase Auth not initialized');
            return;
        }

        auth.onAuthStateChanged(user => {
            this.currentUser = user;
            if (user) {
                this.checkStaffAndRedirect(user);
            } else {
                // Not logged in.
                if (this.viewState === 'dashboard-staff') {
                    this.showLanding();
                }
            }
        });
    },

    checkStaffAndRedirect: function(user) {
        if (typeof db === 'undefined') {
            console.error('Firestore not initialized');
            return;
        }

        // Check Staff
        db.collection('delivery_staff').doc(user.uid).get().then(doc => {
            if (doc.exists) {
                this.showDeliveryDashboard(doc.data().name);
            } else {
                alert('Access Denied: You are not a delivery staff member.');
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
        if (typeof DeliveryApp !== 'undefined') DeliveryApp.cleanup();
    },

    showLanding: function() {
        this.hideAllViews();
        this.viewState = 'landing';
        this.safeDisplay('landing-container', 'block');
    },

    showDeliveryLogin: function() {
        if (this.currentUser) return;
        this.hideAllViews();
        this.viewState = 'login-staff';
        this.safeDisplay('delivery-login-container', 'block');
    },

    showDeliveryDashboard: function(staffName) {
        this.hideAllViews();
        this.viewState = 'dashboard-staff';
        this.safeDisplay('delivery-dashboard-container', 'block');
        if (typeof DeliveryApp !== 'undefined') DeliveryApp.init(staffName);
    },

    // Actions
    login: function() {
        const emailId = 'delivery-email';
        const passId = 'delivery-password';
        const errId = 'delivery-login-error';

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

// Navbar Toggle Helper
function toggleNav() {
    const nav = document.getElementById('delivery-nav');
    if (nav) {
        nav.classList.toggle('nav-expanded');
    }
}
