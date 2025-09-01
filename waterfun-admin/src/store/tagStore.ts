import { defineStore } from 'pinia'
import type {TagNavItemType} from "@/layouts/types.d.ts";
import { readonly } from 'vue';
import {computed, ref} from 'vue';
export const useTagStore = defineStore('tagStore', ()=>{
    const tags= ref<TagNavItemType[]>([])
    const cache = ref<Set<string>>(new Set())

    /**
     * Add a tag to tag store
     * @param tag
     */
    const addTag = (tag: TagNavItemType) => {
        if(!cache.value.has(tag.name)) {
            cache.value.add(tag.name)
            tags.value.push(tag)
        }
    }

    /**
     * Remove a tag from tag store
     * @param tagName
     */
    const removeTag = (tagName: string) => {
        if(cache.value.has(tagName)) {
            cache.value.delete(tagName)
            tags.value.splice(tags.value.findIndex(tag => tag.name === tagName),1)
        }
    }

    const updateTags = (updatedTags: TagNavItemType[]) => {
        tags.value = updatedTags
    }

    const hasTag = (name: string):boolean => cache.value.has(name)

    const getTags = computed(()=>[...tags.value])

    return {
        hasTag,getTags,addTag,removeTag,updateTags,
    }
})