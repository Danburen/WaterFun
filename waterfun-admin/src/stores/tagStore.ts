import { defineStore } from 'pinia'
import {computed, ref} from 'vue';
import {TagNavItemType} from "~/types/ui/tagNav";
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
        }else{
            const index = tags.value.findIndex((currentTag) => currentTag.name === tag.name)
            if (index !== -1) {
                // Keep tag unique by name; refresh latest route params/meta in-place.
                tags.value[index] = tag
            }
        }
    }

    /**
     * Remove a tag from tag store
     * @param tagName
     */
    const removeTag = (tagName: string) => {
        if(!cache.value.has(tagName)) return
        tags.value = tags.value.filter((tag) => tag.name !== tagName)
        cache.value = new Set(tags.value.map((tag) => tag.name))
    }

    const updateTags = (updatedTags: TagNavItemType[]) => {
        tags.value = updatedTags
        cache.value = new Set(updatedTags.map((tag) => tag.name))
    }

    const hasTag = (name: string):boolean => cache.value.has(name)

    const getTags = computed(()=>[...tags.value])

    return {
        hasTag,getTags,addTag,removeTag,updateTags,
    }
})