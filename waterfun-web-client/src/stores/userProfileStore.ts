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

  const avatarCaches = ref<Record<string, avatar>>({});

  const updateLocalProfile = (data: Partial<UserProfile>) => {
    userProfile.value = { ...userProfile.value, ...data };
  };

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
    userProfile.value = { ...userProfile.value, ...data };
  };

  const updateAvatar = (avatarUrl: string, expiresAt: number, uid?: string) => {
    const key = uid || useUserInfoStore().userInfo.uid;
    if (!key) return;
    avatarCaches.value[key] = {
      expiresAt: expiresAt || Date.now() + 1000 * 60 * 60,
      lastAccess: Date.now(),
      presignedUrl: avatarUrl,
    }
  };

  const clearUserProfile = () => {
    userProfile.value = { bio: '', gender: '', birthday: null, residence: '' };
    avatarCaches.value = {};
  };

  const getAvatarUrl = async (uid?: string): Promise<string> => {
    const key = uid || useUserInfoStore().userInfo.uid;
    if (!key) return '';

    const cache = avatarCaches.value[key];
    if (cache?.presignedUrl && Date.now() < cache.expiresAt) {
      cache.lastAccess = Date.now();
      return cache.presignedUrl;
    }

    try {
      const response = await getAvatar();
      const expireParam = response.data.expireAt;
      const expireTime = typeof expireParam === 'string' ? new Date(expireParam).getTime() : (expireParam || 0);

      avatarCaches.value[key] = {
        expiresAt: expireTime || Date.now() + 1000 * 60 * 60,
        lastAccess: Date.now(),
        presignedUrl: response.data.url || ''
      };
      return avatarCaches.value[key].presignedUrl;
    } catch (error) {
      console.error('Failed to get avatar from API:', error);
      return cache?.presignedUrl || '';
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

  return { userProfile, avatarCaches, updateUserProfile, updateLocalProfile, updateAvatar, clearUserProfile, getAvatarUrl, fetchAndUpdateUserProfile };
}, {
  persist: process.client ? {
    storage: localStorage,
    pick: ['avatarCaches']
  } : false
});
