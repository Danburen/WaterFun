import { LoginRequest } from '@waterfun/web-core/src/types/api/auth';
import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import { login } from '@/api/auth';
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

    const isValid = computed(() => {
        return accessData.value.token && Date.now() < accessData.value.expire;
    });

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
        isValid,
    }
},{    persist: {
        storage: localStorage
    }
})

