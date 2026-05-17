import { defineStore } from "pinia";
import { ref } from "vue";
import { getUserInfo } from "@/api/userApi";
import type { CloudResourceUrlResp } from "@waterfun/web-core/src/types/api/response";
interface UserInfo {
  username: string;
  uid: string;
  nickname: string;
  avatar: CloudResourceUrlResp;
  accountStatus: string;
  createAt: string;
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
    createAt: '',
    passwordHash: false,
  });

  const updateUserInfo = (data: Partial<UserInfo>) => {
    userInfo.value = { ...userInfo.value, ...data };
  };

  const clearUserInfo = () => {
    userInfo.value = { username: '', uid: '', nickname: '', avatar: { url: '', expireAt: 0 }, accountStatus: '', createAt: '', passwordHash: false };
  };

  const fetchAndUpdateUserInfo = async() =>{
    const userInfoRes = await getUserInfo();
      updateUserInfo({
        ...userInfoRes.data
    });
    if(userInfoRes.data.avatar){
        useUserProfileStore().updateAvatar(
            userInfoRes.data.avatar.url, 
            userInfoRes.data.avatar.expireAt
        );
    }
  }

  return { userInfo, updateUserInfo, clearUserInfo, fetchAndUpdateUserInfo };
}, {
  persist: process.client ? {
        storage: sessionStorage
  } : false
});