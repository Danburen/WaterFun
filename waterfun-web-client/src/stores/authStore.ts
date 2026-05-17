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
    }
},{
    persist: process.client ? {
        storage: sessionStorage
    } : false
})

