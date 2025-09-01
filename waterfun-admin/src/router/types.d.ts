import 'vue-router'
export {}

declare module 'vue-router' {
    interface RouteMeta {
        locale?: string
        icon?: string
    }
}