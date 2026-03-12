import { computed } from 'vue';
import { useRoute } from 'vue-router'
import type { RouteLocationMatched, RouteLocationNormalized } from 'vue-router';
import type { BreadNavItemType } from "@/types/ui/tagNav";

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
            let path;
            if (isLast) {
                path = route.path;
            } else {
                path = matchedRoute.path;
            }

            return {
                name: matchedRoute.name,
                to: matchedRoute.name ? { name: matchedRoute.name } : path,
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
        .filter(item => {
            // 保留有locale的meta，或者当前激活的路由（最后一个）
            const isLast = item.path === route.matched[route.matched.length - 1].path;
            return item.meta && (item.meta.locale || isLast);
        })
        .map(item => ({
            ...item,
            path: item.path,
            name: item.name as string,
            meta: {
                ...item.meta,
                // 如果最后一个路由没有locale，使用route的meta.locale作为备选
                locale: item.meta?.locale || (item.path === route.matched[route.matched.length - 1].path ? route.meta?.locale : undefined)
            }
        }))
}

