import { defineStore } from "pinia";
import { ref } from "vue";
import { useUserInfoStore } from './userInfoStore';
import { getAvatar, getUserProfile, updateUserProfile as apiUpdateProfile } from '../api/userApi';
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
  });

  /** 更新本地 profile 状态（不调接口） */
  const updateLocalProfile = (data: Partial<UserProfile>) => {
    userProfile.value = { ...userProfile.value, ...data };
  };

  /** 更新 profile → 调后端 API + 同步本地状态 */
  const updateUserProfile = async (data: Partial<UserProfile>) => {
    const payload: { bio?: string; gender?: string; birthday?: string; residence?: string } = {};
    if (data.bio !== undefined) payload.bio = data.bio;
    if (data.gender !== undefined) payload.gender = data.gender;
    if (data.birthday != null) {
      payload.birthday = typeof data.birthday === 'object' && 'toISOString' in data.birthday
        ? data.birthday.toISOString().split('T')[0]
        : String(data.birthday);
    }
    if (data.residence !== undefined) payload.residence = data.residence;
    await apiUpdateProfile(payload);
    // sync local after successful API call
    userProfile.value = { ...userProfile.value, ...data };
  };

  const updateAvatar = (avatarUrl: string, expiresAt: number) => {
    avatarCache.value = {
      expiresAt: expiresAt || Date.now() + 1000 * 60 * 60,
      lastAccess: Date.now(),
      presignedUrl: avatarUrl,
    }
  };

  const clearUserProfile = () => {
    userProfile.value = { bio: '', gender: '', birthday: null, residence: '' };
    avatarCache.value = { expiresAt: 0, lastAccess: 0, presignedUrl: '' };
  };

  /**
   * Get user avatar url from memory
   * @returns avatar presigned url, empty string if user is not logged in
   */
  const getAvatarUrl = async (): Promise<string> => {
    const uid = useUserInfoStore().userInfo.uid;
    if (!uid) return '';
    
    if (avatarCache.value.presignedUrl && Date.now() < avatarCache.value.expiresAt) {
      avatarCache.value.lastAccess = Date.now();
      return avatarCache.value.presignedUrl;
    }
    
    try {
      const response = await getAvatar();
      const expireParam = response.data.expireAt;
      const expireTime = typeof expireParam === 'string' ? new Date(expireParam).getTime() : (expireParam || 0);

      avatarCache.value = {
        expiresAt: expireTime || Date.now() + 1000 * 60 * 60,
        lastAccess: Date.now(),
        presignedUrl: response.data.url || ''
      };
      return avatarCache.value.presignedUrl;
    } catch (error) {
      console.error('Failed to get avatar from API:', error);
      return avatarCache.value.presignedUrl || '';
    }
  };

  const fetchAndUpdateUserProfile = async() =>{
    const userProfileRes = await getUserProfile();
    updateLocalProfile({
      bio: userProfileRes.data.bio,
      gender: userProfileRes.data.gender,
      birthday: userProfileRes.data.birthday ? new Date(userProfileRes.data.birthday) : null,
      residence: userProfileRes.data.residence,
    });
    if(userProfileRes.data.avatar){
      const expireParam = userProfileRes.data.avatar.expireAt;
      const expireTime = typeof expireParam === 'string' ? new Date(expireParam).getTime() : (expireParam || 0);
      updateAvatar(userProfileRes.data.avatar.url, expireTime);
    }
  }

  return { userProfile, avatarCache, updateUserProfile, updateLocalProfile, updateAvatar, clearUserProfile, getAvatarUrl, fetchAndUpdateUserProfile };
}, {
  persist: process.client ? {
    storage: localStorage,
    pick: ['avatarCache']
  } : false
});
