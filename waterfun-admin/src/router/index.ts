import {createRouter, createMemoryHistory, createWebHistory, RouteRecordRaw} from "vue-router";
export const routes: RouteRecordRaw[] = [
    {
        path: '/', redirect: '/dashboard',
    },
    {
        path: '/dashboard' , component: ()=> import("~/views/DashBoardView.vue"),
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
            layout: false,
        }
    },
    {
        path: '/system',
        component: () => import('@/layouts/EmptyLayout.vue'),
        meta: { locale:'system.title', icon: 'el-Edit' },
        children: [
            {
                path: 'user',
                name: 'userManager',
                meta: { locale:'system.user' },
                component: () => import("@/views/System/user/UserManagerView.vue")
            },
            {
                path: 'user/:uid/edit',
                name: 'userEdit',
                component: () => import('@/views/System/user/UserEdit.vue'),
                props: true,
                meta: { 
                    locale: 'user.edit',
                    isDetail: true,
                }
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
        component: () => import('@/layouts/EmptyLayout.vue'),
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
    },{
        path: '/post',
        component: () => import('@/layouts/EmptyLayout.vue'),
        meta: { locale: 'post.title' ,icon: 'el-Document' },
        children: [
            {
                path: 'list',
                name: 'postList',
                meta: { locale: 'post.list' },
                component: () => import("@/views/Post/Post.vue")
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