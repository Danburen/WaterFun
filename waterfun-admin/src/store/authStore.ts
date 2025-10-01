import {defineStore} from "pinia";
import {computed, ref} from "vue";

interface accessToken {
    token: string;
    expire: number;
}

export const useAuthStore = defineStore("accessStore",()=>{
    const accessData = ref<accessToken>({
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

    const isValid = computed (() => {
        return accessData.value.token && Date.now() < accessData.value.expire;
    })
    return {
        setToken,
        removeToken,
        accessData,
        isValid,
    }
},{
    persist: {
        storage:  sessionStorage
    }
})