import {createRouter , createWebHashHistory ,RouterView} from 'vue-router'
const router = createRouter({
    history: createWebHashHistory(),
    routes: [
        { path: '/', redirect: '/login' },
        { path:'/home', component: ()=>import( '../client/views/Home.vue') },
        { path: '/login',
            component: ()=>import( '../client/views/Login.vue') ,
            meta: { hideHeader: true } },
        { path: '/home', component: ()=>import( '../client/views/Home.vue') },
        { path: '/resource', component: ()=>import( '../client/views/Resource.vue') },
        { path: '/community', component: ()=>import( '../client/views/Community.vue') },
    ]
})

export default router