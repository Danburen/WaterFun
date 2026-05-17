import { defineStore } from "pinia";
import { ref } from "vue";
import { useCacheStore } from './cacheStore';
import { useUserInfoStore } from './userInfoStore';
import { getAvatar, getUserProfile } from '../api/userApi';
import type CacheItem from '@waterfun/web-core/src/cache/types';


interface UserProfile {
  bio: string;
  gender: string;
  birthday: Date | null;
  residence: string;
}

interface avatar extends CacheItem {}
export const useUserProfileStore = defineStore('userProfileStore', () => {
  const userProfile = ref<UserProfile>({
    bio: '',
    gender: '',
    birthday: null,
    residence: '',
  });

  const avatarCache = ref<avatar>({
    expiresAt: 0,
    lastAccess: 0,
    presignedUrl: '',
  })
  
  const cacheStore = useCacheStore();

  const updateUserProfile = (data: Partial<UserProfile>) => {
    userProfile.value = { ...userProfile.value, ...data };
  };

  const updateAvatar = (avatarUrl: string, expiresAt: number) => {
    avatarCache.value = {
      expiresAt: expiresAt || Date.now() + 1000 * 60 * 60,
      lastAccess: Date.now(),
      presignedUrl: avatarUrl,
    }
    const uid = useUserInfoStore().userInfo.uid;
    cacheStore.updateCacheItem(`avatar_${uid || 'anonymous'}`, 
      avatarCache.value);
  };

  const clearUserProfile = () => {
    userProfile.value = { bio: '', gender: '', birthday: null, residence: '' };
  };

  /**
   * Get user avatar url from cache
   * @returns avatar presigned url, undefined if user is not logged in (uid is empty)
   */
  const getAvatarUrl = (): Promise<string> => {
    const uid = useUserInfoStore().userInfo.uid;
    if (!uid) return Promise.resolve('');
    
    const cacheKey = `avatar_${uid}`;
    
    return cacheStore.load(cacheKey, async (path) => {
      try {
        const response = await getAvatar();
        avatarCache.value = {
          expiresAt: response.data.expireAt || Date.now() + 1000 * 60 * 60, // 默认1小时过期
          lastAccess: Date.now(),
          presignedUrl: response.data.url || ''
        };
        return avatarCache.value;
      } catch (error) {
        console.error('Failed to get avatar from API:', error);
        return avatarCache.value;
      }
    }).catch((error) => {
      console.error('Failed to get avatar from cacheStore:', error);
      return avatarCache.value.presignedUrl || '';
    });
  };

  const fetchAndUpdateUserProfile = async() =>{
    const userProfileRes = await getUserProfile();
    updateUserProfile({
      bio: userProfileRes.data.bio,
      gender: userProfileRes.data.gender,
      birthday: userProfileRes.data.birthday ? new Date(userProfileRes.data.birthday) : null,
      residence: userProfileRes.data.residence,
    });
    if(userProfileRes.data.avatar){
      updateAvatar(userProfileRes.data.avatar
            .url, userProfileRes.data.avatar.expireAt);
    }
  }

  return { userProfile, updateUserProfile, updateAvatar, clearUserProfile, getAvatarUrl, fetchAndUpdateUserProfile };
}, {
  persist: process.client ? {
        storage: sessionStorage
  } : false
});