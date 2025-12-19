// firebase-config.js
const firebaseConfig = {
  apiKey: "AIzaSyCLYNZwZMZQVNrQbVWOpNm_sMbXgS1pfww",
  authDomain: "strategic-haven-471117-j1.firebaseapp.com",
  projectId: "strategic-haven-471117-j1",
  storageBucket: "strategic-haven-471117-j1.firebasestorage.app",
  messagingSenderId: "855839483192",
  appId: "1:855839483192:web:320c12657f59509744cdcd",
  measurementId: "G-F6WFYRFLB0"
};

firebase.initializeApp(firebaseConfig);
const db = firebase.firestore();
const auth = firebase.auth();
