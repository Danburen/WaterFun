import {
    createRouter, 
    createWebHistory, 
    RouteRecordRaw
} from "vue-router";
import { useAuthStore } from '~/stores/authStore';

export const routes: RouteRecordRaw[] = [
    {
        path: '/login',
        component: () => import('@/layouts/EmptyLayout.vue'),
        children: [{ path: '', component: () => import('@/views/LoginView.vue') }]
    },
    {
        path: '/',
        component: () => import('@/layouts/MainLayout.vue'),
        children: [
            {
                path: '', redirect: '/dashboard',
            },
            {
                path: '/dashboard', component: () => import("~/views/DashBoardView.vue"),
                name: "dashboard",
                meta: {
                    locale: '仪表盘',
                    icon: 'dashboard',
                }
            },
            {
                path: '/system',
                component: () => import('@/layouts/EmptyLayout.vue'),
                meta: { locale:'系统管理', icon: 'system' },
                children: [
                    {
                        path: 'user',
                        name: 'userManager',
                        meta: { locale:'用户管理', icon: 'users' },
                        component: () => import("@/views/System/user/UserList.vue")
                    },
                    {
                        path: 'user/:uid',
                        name: 'userDetail',
                        component: () => import('@/views/System/user/UserDetail.vue'),
                        props: true,
                        meta: {
                            locale: '用户详情',
                            isDetail: true,
                        }
                    },
                    {
                        path: 'user/:uid/role-assign',
                        name: 'userRoleAssign',
                        component: () => import('~/views/System/user/UserAssignRole.vue'),
                        props: true,
                        meta: {
                            locale: '分配角色',
                            isDetail: true,
                        }
                    },
                    {
                        path: 'user/:uid/permission-assign',
                        name: 'userPermissionAssign',
                        component: () => import('~/views/System/user/UserAssignPerm.vue'),
                        props: true,
                        meta: {
                            locale: '分配权限',
                            isDetail: true,
                        }
                    },
                    {
                        path: 'role',
                        name: 'roleManager',
                        meta: { locale:'角色管理', icon: 'user-tag' },
                        component: () => import("~/views/System/role/RoleList.vue")
                    },
                    {
                        path: 'role/:id',
                        name: 'roleDetail',
                        component: () => import('~/views/System/role/RoleDetail.vue'),
                        props: true,
                        meta: {
                            locale: '角色详情',
                            isDetail: true,
                        }
                    },
                    {
                        path: 'role/:id/permission-assign',
                        name: 'rolePermissionAssign',
                        component: () => import('~/views/System/role/RoleAssignPerm.vue'),
                        props: true,
                        meta: {
                            locale: '分配权限',
                            isDetail: true,
                        }
                    },
                    {
                        path: 'role/:id/user-assign',
                        name: 'roleUserAssign',
                        component: () => import('~/views/System/role/RoleAssignUser.vue'),
                        props: true,
                        meta: {
                            locale: '分配用户',
                            isDetail: true,
                        }
                    },
                    {
                        path: 'permission',
                        name: 'permissionManager',
                        meta: { locale:'权限管理', icon: 'key' },
                        component: () => import("~/views/System/perm/PermList.vue")
                    },
                    {
                        path: 'permission/:id',
                        name: 'permissionDetail',
                        component: () => import('~/views/System/perm/PermDetail.vue'),
                        props: true,
                        meta: {
                            locale: '权限详情',
                            isDetail: true,
                        }
                    }
                ]
            },
            {
                path: '/monitor',
                component: () => import('@/layouts/EmptyLayout.vue'),
                meta: { locale: '系统监控' ,icon: 'monitor' },
                children: [
                    {
                        path: 'online-users',
                        name: 'online-users',
                        meta: { locale: '在线用户', icon: 'user-clock' },
                        component: () => import("@/views/System/OnlineMonitorView.vue")
                    },
                    {
                        path: 'status',
                        name: 'status',
                        meta: { locale: '系统状态', icon: 'heart-pulse' },
                        component: () => import("@/views/System/StatusMonitorView.vue")
                    },
                    {
                        path: 'global',
                        name: 'global',
                        meta: { locale: '全局视图', icon: 'globe' },
                        component: () => import('@/views/System/GlobalViewMonitorView.vue')
                    }
                ]
            },
            {
                path: '/moderation',
                component: () => import('@/layouts/EmptyLayout.vue'),
                meta: { locale: '内容审核', icon: 'moderation' },
                children: [
                    {
                        path: 'workbench',
                        name: 'moderationWorkbench',
                        meta: { locale: '审核工作台', icon: 'clipboard-check' },
                        component: () => import('@/views/Moderation/ContentModerationWorkbench.vue')
                    },
                    {
                        path: 'tasks/:taskId',
                        name: 'moderationTaskDetail',
                        component: () => import('@/views/Moderation/ModerationDetail.vue'),
                        props: true,
                        meta: {
                            locale: '任务详情',
                            isDetail: true,
                        }
                    },
                    {
                        path: 'tasks/:taskId/resources/:resourceUuid',
                        name: 'moderationResourceDetail',
                        component: () => import('@/views/Moderation/ModerationResourceDetail.vue'),
                        props: true,
                        meta: {
                            locale: '资源详情',
                            isDetail: true,
                        }
                    },
                    {
                        path: 'resources',
                        name: 'moderationResourceList',
                        meta: { locale: '资源列表', icon: 'cubes' },
                        component: () => import('@/views/Moderation/ResourceModerationList.vue')
                    }
                ]
            },
            {
                path: '/content',
                component: () => import('@/layouts/EmptyLayout.vue'),
                meta: { locale: '内容管理', icon: 'content' },
                children: [
                    {
                        path: 'post',
                        name: 'contentPostList',
                        meta: { locale: '帖子管理', icon: 'file-lines' },
                        component: () => import('@/views/Content/PostList.vue')
                    },
                    {
                        path: 'post/:id',
                        name: 'contentPostDetail',
                        component: () => import('@/views/Content/PostDetail.vue'),
                        props: true,
                        meta: {
                            locale: '帖子详情',
                            isDetail: true,
                        }
                    },
                    {
                        path: 'tag',
                        name: 'contentTagList',
                        meta: { locale: '标签管理', icon: 'tags' },
                        component: () => import('@/views/Content/TagList.vue')
                    },
                    {
                        path: 'tag/:id',
                        name: 'contentTagDetail',
                        component: () => import('@/views/Content/TagDetail.vue'),
                        props: true,
                        meta: {
                            locale: '标签详情',
                            isDetail: true,
                        }
                    },
                    {
                        path: 'category',
                        name: 'contentCategoryList',
                        meta: { locale: '分类管理', icon: 'folder-tree' },
                        component: () => import('@/views/Content/CategoryList.vue')
                    },
                    {
                        path: 'category/:id',
                        name: 'contentCategoryDetail',
                        component: () => import('@/views/Content/CategoryDetail.vue'),
                        props: true,
                        meta: {
                            locale: '分类详情',
                            isDetail: true,
                        }
                    },
                    {
                        path: 'banner',
                        name: 'contentBannerList',
                        meta: { locale: '横幅管理', icon: 'flag' },
                        component: () => import('@/views/Content/BannerList.vue')
                    },
                    {
                        path: 'banner/:id',
                        name: 'contentBannerDetail',
                        component: () => import('@/views/Content/BannerDetail.vue'),
                        props: true,
                        meta: {
                            locale: '横幅详情',
                            isDetail: true,
                        }
                    }
                ]
            }
        ]
    }
]

export const router = createRouter({
        history:createWebHistory(),
    routes
})

router.beforeEach((to, from, next) => {
    console.log('Router beforeEach:', { to, from });
    const authStore = useAuthStore();
    const whiteList = ['/login', '/register', '/forgot-password'];
    console.log('Is user authenticated?', authStore.isValid);
    if (whiteList.includes(to.path)) {
        if (authStore.isValid) {
            return next('/dashboard')
        }
        return next()
    }
    if (!authStore.isValid) {
        return next(`/login?redirect=${encodeURIComponent(to.fullPath)}`)
    }
    next()
})

function extractMenuRoutes(routes: RouteRecordRaw[]): RouteRecordRaw[] {
    const result: RouteRecordRaw[] = []
    
    for (const route of routes) {
        if (route.meta?.public === true) continue
        if (route.meta?.isDetail === true) continue
            const hasLocale = !!route.meta?.locale
        
        if (route.children && route.children.length > 0) {
            const childMenus = extractMenuRoutes(route.children)
            
            if (hasLocale) {
                result.push({
                    ...route,
                    children: childMenus
                })
            } else {
                result.push(...childMenus)
            }
        } else if (hasLocale) {
            result.push(route)
        }
    }
    
    return result
}
export const menuRoutes = extractMenuRoutes(routes);

export type routeType = typeof routes[number];

export default router
