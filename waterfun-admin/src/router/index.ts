import { createRouter ,createMemoryHistory } from "vue-router";
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
        path: '/system',
        meta: { locale:'system.title', icon: 'el-Edit' },
        children: [
            {
                path: 'user',
                name: 'userManager',
                meta: { locale:'system.user' },
                component: () => import("@/views/UserManagerView.vue")
            },
            {
                path: 'role',
                name: 'roleManager',
                meta: { locale:'system.role'},
                component: () => import("@/views/RoleManagerView.vue")
            },
            {
                path: 'permission',
                name: 'permissionManager',
                meta: { locale:'system.permission' },
                component: () => import("@/views/PermManagerView.vue")
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
                component: () => import("@/views/OnlineMonitorView.vue")
            },
            {
                path: 'status',
                name: 'status',
                meta: { locale: 'monitor.status' },
                component: () => import("@/views/StatusMonitorView.vue")
            },
            {
                path: 'global',
                name: 'global',
                meta: { locale: 'monitor.globalView' },
                component: () => import('@/views/GlobalViewMonitorView.vue')
            }
        ]
    }
]

const router = createRouter({
    history:createMemoryHistory(),
    routes
})

export type routeType = typeof routes[number];

export default router