import Vue from 'vue'
import App from './App.vue'
import router from './router'
import firebase from 'firebase'

Vue.config.productionTip = false

const firebaseConfig = {
  apiKey: "AIzaSyCEWHmdSXcs7MeFT-Uzcr1O50x7wl8RPoU",
  authDomain: "email-service-47524.firebaseapp.com",
  projectId: "email-service-47524",
  storageBucket: "email-service-47524.appspot.com",
  messagingSenderId: "96746777014",
  appId: "1:96746777014:web:01a3836a55a34b1e4cf917",
  measurementId: "G-9XBWVC39NB"
};
// Initialize Firebase
firebase.initializeApp(firebaseConfig);

new Vue({
  router,
  render: h => h(App)
}).$mount('#app')
