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
                    locale: 'user.roleAssign',
                    isDetail: true,
                }
            },
            {
                path: 'user/:uid/permission-assign',
                name: 'userPermissionAssign',
                component: () => import('~/views/System/user/UserAssignPerm.vue'),
                props: true,
                meta: {
                    locale: 'user.permissionAssign',
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
    },
    {
        path: '/moderation',
        component: () => import('@/layouts/EmptyLayout.vue'),
        meta: { locale: 'moderation.title', icon: 'el-Check' },
        children: [
            {
                path: 'workbench',
                name: 'moderationWorkbench',
                meta: { locale: 'moderation.workbench' },
                component: () => import('@/views/Moderation/ContentModerationWorkbench.vue')
            },
            {
                path: 'resources/:resourceId',
                name: 'moderationResourceDetail',
                component: () => import('@/views/Moderation/ModerationResourceDetail.vue'),
                props: true,
                meta: {
                    locale: 'moderation.resourceDetail',
                    isDetail: true,
                }
            }
        ]
    },
    {
        path: '/content',
        component: () => import('@/layouts/EmptyLayout.vue'),
        meta: { locale: 'content.title', icon: 'el-Document' },
        children: [
            {
                path: 'post',
                name: 'contentPostList',
                meta: { locale: 'content.post.title' },
                component: () => import('@/views/Content/PostList.vue')
            },
            {
                path: 'post/:id',
                name: 'contentPostDetail',
                component: () => import('@/views/Content/PostDetail.vue'),
                props: true,
                meta: {
                    locale: 'content.post.detail',
                    isDetail: true,
                }
            },
            {
                path: 'tag',
                name: 'contentTagList',
                meta: { locale: 'content.tag.title' },
                component: () => import('@/views/Content/TagList.vue')
            },
            {
                path: 'tag/:id',
                name: 'contentTagDetail',
                component: () => import('@/views/Content/TagDetail.vue'),
                props: true,
                meta: {
                    locale: 'content.tag.detail',
                    isDetail: true,
                }
            },
            {
                path: 'category',
                name: 'contentCategoryList',
                meta: { locale: 'content.category.title' },
                component: () => import('@/views/Content/CategoryList.vue')
            },
            {
                path: 'category/:id',
                name: 'contentCategoryDetail',
                component: () => import('@/views/Content/CategoryDetail.vue'),
                props: true,
                meta: {
                    locale: 'content.category.detail',
                    isDetail: true,
                }
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