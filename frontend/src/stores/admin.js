import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useAdminStore = defineStore('admin', () => {
    const isCollapse = ref(false)
    const user = ref(null)

    const toggleCollapse = () => {
        isCollapse.value = !isCollapse.value
    }

    return {
        isCollapse,
        user,
        toggleCollapse
    }
})