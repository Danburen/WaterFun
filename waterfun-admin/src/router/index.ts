import {createRouter, createMemoryHistory, createWebHistory} from "vue-router";
export const routes = [
    {
        path: '/' , component: ()=> import("@/views/HomeView.vue"),
        name: "dashboard",
        meta: {
            locale: 'dashboard',
            icon: 'DashBoard',
        }
    },
    {
        path: '/login', component: ()=> import("@/views/LoginView.vue"),
        name: "login",
        meta: {
            public: true,
        }
    },
    {
        path: '/system',
        meta: { locale:'system.title', icon: 'el-Edit' },
        children: [
            {
                path: 'user',
                name: 'userManager',
                meta: { locale:'system.user' },
                component: () => import("@/views/System/UserManagerView.vue")
            },
            {
                path: 'role',
                name: 'roleManager',
                meta: { locale:'system.role'},
                component: () => import("@/views/System/RoleManagerView.vue")
            },
            {
                path: 'permission',
                name: 'permissionManager',
                meta: { locale:'system.permission' },
                component: () => import("@/views/System/PermManagerView.vue")
            }
        ]
    },
    {
        path: '/monitor',
        meta: { locale: 'monitor.title' ,icon: 'el-VideoCamera' },
        children: [
            {
                path: 'online-users',
                name: 'online-users',
                meta: { locale: 'monitor.online' },
                component: () => import("@/views/System/OnlineMonitorView.vue")
            },
            {
                path: 'status',
                name: 'status',
                meta: { locale: 'monitor.status' },
                component: () => import("@/views/System/StatusMonitorView.vue")
            },
            {
                path: 'global',
                name: 'global',
                meta: { locale: 'monitor.globalView' },
                component: () => import('@/views/System/GlobalViewMonitorView.vue')
            }
        ]
    }
]

const router = createRouter({
        history:createWebHistory(),
    routes
})

export const menuRoutes = routes.filter(route => route.meta && route.meta.public !== true );

export type routeType = typeof routes[number];

export default router