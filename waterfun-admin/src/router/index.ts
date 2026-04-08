import {createRouter, createWebHistory, RouteRecordRaw} from "vue-router";
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
                component: () => import("@/views/System/user/UserList.vue")
            },
            {
                path: 'user/:uid',
                name: 'userDetail',
                component: () => import('@/views/System/user/UserDetail.vue'),
                props: true,
                meta: {
                    locale: 'user.detail',
                    isDetail: true,
                }
            },
            {
                path: 'user/:uid/role-assign',
                name: 'userRoleAssign',
                component: () => import('~/views/System/user/UserAssignRole.vue'),
                props: true,
                meta: {
                    locale: 'userAssignRole',
                    isDetail: true,
                }
            },
            {
                path: 'user/:uid/permission-assign',
                name: 'userPermissionAssign',
                component: () => import('~/views/System/user/UserAssignPerm.vue'),
                props: true,
                meta: {
                    locale: 'userAssignPermission',
                    isDetail: true,
                }
            },
            {
                path: 'role',
                name: 'roleManager',
                meta: { locale:'system.role'},
                component: () => import("~/views/System/role/RoleList.vue")
            },
            {
                path: 'role/:id',
                name: 'roleDetail',
                component: () => import('~/views/System/role/RoleDetail.vue'),
                props: true,
                meta: {
                    locale: 'role.detail',
                    isDetail: true,
                }
            },
            {
                path: 'role/:id/permission-assign',
                name: 'rolePermissionAssign',
                component: () => import('~/views/System/role/RoleAssignPerm.vue'),
                props: true,
                meta: {
                    locale: 'roleAssignPermission',
                    isDetail: true,
                }
            },
            {
                path: 'role/:id/user-assign',
                name: 'roleUserAssign',
                component: () => import('~/views/System/role/RoleAssignUser.vue'),
                props: true,
                meta: {
                    locale: 'roleAssignUser',
                    isDetail: true,
                }
            },
            {
                path: 'permission',
                name: 'permissionManager',
                meta: { locale:'system.permission' },
                component: () => import("~/views/System/perm/PermList.vue")
            },
            {
                path: 'permission/:id',
                name: 'permissionDetail',
                component: () => import('~/views/System/perm/PermDetail.vue'),
                props: true,
                meta: {
                    locale: 'permission.detail',
                    isDetail: true,
                }
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