import { defineStore } from "pinia";
import { ref, computed } from "vue";

interface access {
    token: string;
    expire: number;
}

export const useAuthStore = defineStore('accessStore', ()=>{
    const accessData = ref<access>({
        token: '',
        expire: 0,
    })

    const fromPool = ref(false)
    const lastBrowserLoginUid = ref('')

    const setToken = (token:string,expire:number) => {
        accessData.value = {
            token: token,
            expire: expire
        }
    }

    const removeToken = () => {
        accessData.value = {
            token: '',
            expire: 0,
        }
    }

    const isAccess = computed(() => {
        return accessData.value.token && Date.now() < accessData.value.expire;
    });

    return {
        setToken,
        removeToken,
        accessData,
        isAccess,
        fromPool,
        lastBrowserLoginUid,
    }
},{
    persist: process.client ? {
        storage: localStorage,
        pick: ['accessData', 'lastBrowserLoginUid']
    } : false
})

