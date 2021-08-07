import Vue from 'vue'
import VueRouter from 'vue-router'
import Home from '../components/Home.vue'
import Signup from '../components/Signup.vue'
import Emails from '../components/Emails.vue'



Vue.use(VueRouter)

const routes = [
  {
    path: '/',
    name: 'Home',
    component: Home
  },
  {
    path: '/signup',
    name: 'Signup',
    component: Signup
  },
  {
    path: '/emails',
    name: 'Emails',
    component: Emails
  }
]

const router = new VueRouter({
  mode: 'history',
  base: process.env.BASE_URL,
  routes
})

export default router
