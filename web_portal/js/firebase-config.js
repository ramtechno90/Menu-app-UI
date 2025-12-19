// firebase-config.js
let firebaseConfig = {
  apiKey: "YOUR_API_KEY",
  authDomain: "YOUR_PROJECT_ID.firebaseapp.com",
  projectId: "YOUR_PROJECT_ID",
  storageBucket: "YOUR_PROJECT_ID.appspot.com",
  messagingSenderId: "YOUR_SENDER_ID",
  appId: "YOUR_APP_ID"
};

const savedConfig = localStorage.getItem('firebase_config');
if (savedConfig) {
    try {
        const parsed = JSON.parse(savedConfig);
        if (parsed.apiKey && parsed.apiKey !== "YOUR_API_KEY") {
            firebaseConfig = parsed;
        }
    } catch(e) {
        console.error("Failed to parse saved config", e);
    }
}

let db = null;
let auth = null;

if (firebaseConfig.apiKey === "YOUR_API_KEY") {
    window.isConfigMissing = true;
    console.warn("Firebase Config missing. Waiting for user input.");
} else {
    try {
        firebase.initializeApp(firebaseConfig);
        db = firebase.firestore();
        auth = firebase.auth();
    } catch (e) {
        console.error("Firebase init failed", e);
        // Fallback to setup if init fails (e.g. bad keys)
        window.isConfigMissing = true;
    }
}
