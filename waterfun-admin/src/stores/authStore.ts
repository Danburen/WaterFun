import { LoginRequest } from '@waterfun/web-core/src/types/api/auth';
import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import { login } from '@/api/auth';
interface access {
    token: string;
    expire: number;
}

const STORAGE_KEY = 'accessStore'

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

    const restore = () => {
        const stored = localStorage.getItem(STORAGE_KEY)
        if (stored) {
            try {
                const parsed = JSON.parse(stored)
                if (parsed?.accessData?.token) {
                    accessData.value = {
                        token: parsed.accessData.token,
                        expire: parsed.accessData.expire || 0
                    }
                }
            } catch (e) {
                console.error('Auth restore failed:', e)
                localStorage.removeItem(STORAGE_KEY)
            }
        }
    }

    const removeToken = () => {
        accessData.value = {
            token: '',
            expire: 0,
        }
    }

    const isTokenValid = computed(() => {
        return !!accessData.value.token && Date.now() < accessData.value.expire
    })

    const tryLogin = async (data: LoginRequest) => {
        return login(data).then(res => {
            console.log(res);
            setToken(
                res.data.accessToken, 
                Date.now() + res.data.exp * 1000    
            );
            return res;
        });
    }

    return {
        setToken,
        removeToken,
        tryLogin,
        accessData,
        isValid: isTokenValid,
        restore,
    }
},{    persist: {
        storage: localStorage
    }
})

