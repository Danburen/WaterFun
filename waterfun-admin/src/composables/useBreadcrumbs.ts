import { computed } from 'vue';
import { useRoute } from 'vue-router'
import type { RouteLocationMatched, RouteLocationNormalized } from 'vue-router';
import type {BreadNavItemType} from "@/layouts/types.js";

export const useBreadcrumbs = () => {
    const route = useRoute();
    const breadcrumbs = computed(() => {
        const matchedRoutes = getMatchedRoutes(route);
        return matchedRoutes.map((matchedRoute, index) => {
            if (matchedRoute.path === '/') {
                return {
                    name: 'dashboard',
                    to: '/',
                    locale: 'nav.dashboard',
                } as BreadNavItemType;
            }

            const isLast = index === matchedRoutes.length - 1;
            const path = isLast
                ? route.path
                : matchedRoute.path;

            return {
                name: matchedRoute.name,
                to: matchedRoute.name ? path: null,
                locale: `nav.${matchedRoute.meta?.locale}`,
            } as BreadNavItemType
        })
    })
    return {
        breadcrumbs,
    }
}



export const getMatchedRoutes = (route: RouteLocationNormalized): RouteLocationMatched[] =>{
    return route.matched
        .filter(item => item.meta && (item.meta.locale))
        .map(item => ({
            ...item,
            path: item.path,
            name: item.name as string,
            meta: item.meta
        }))
}

