import { defineStore } from "pinia";
import { ref } from "vue";
import { getUserInfo } from "@/api/userApi";
import type { CloudResourceUrlResp } from "@waterfun/web-core/src/types/api/response";
import { updateUserNickname as apiUpdateNickname } from "@/api/userApi";
interface UserInfo {
  username: string;
  uid: string;
  nickname: string;
  avatar: CloudResourceUrlResp;
  accountStatus: string;
  createdAt: string;
  passwordHash: boolean;
}

export const useUserInfoStore = defineStore('userInfoStore', () => {
  const userInfo = ref<UserInfo>({
    username: '',
    uid: '',
    nickname: '',
    avatar: {
        url: '',
        expireAt: 0,
    },
    accountStatus: '',
    createdAt: '',
    passwordHash: false,
  });

  const updateUserInfo = (data: Partial<UserInfo>) => {
    userInfo.value = { ...userInfo.value, ...data };
  };

  const clearUserInfo = () => {
    userInfo.value = { username: '', uid: '', nickname: '', avatar: { url: '', expireAt: 0 }, accountStatus: '', createdAt: '', passwordHash: false };
  };

  const fetchAndUpdateUserInfo = async() =>{
    const userInfoRes = await getUserInfo();
    updateUserInfo({
        ...userInfoRes.data
    });
    if(userInfoRes.data.avatar){
        const { useUserProfileStore } = await import('./userProfileStore');
        const expireParam = userInfoRes.data.avatar.expireAt;
        const expireTime = typeof expireParam === 'string' ? new Date(expireParam).getTime() : expireParam;
        useUserProfileStore().updateAvatar(
            userInfoRes.data.avatar.url, 
            expireTime
        );
    }
  }

  const updateNickname = async (nickname: string) => {
    await apiUpdateNickname(nickname);
    userInfo.value.nickname = nickname;
  }

  return { userInfo, updateUserInfo, clearUserInfo, fetchAndUpdateUserInfo, updateNickname };
}, {
  persist: process.client ? {
        storage: localStorage
  } : false
});