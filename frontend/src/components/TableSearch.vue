<template>
  <el-form :model="formData">
    <el-row :gutter="24">
      <template v-for="item in formItemAttrs" :key="item.prop">
        <el-col v-bind="item.col">
          <el-form-item :label="item.label" :prop="item.prop">
        <el-input
          v-if="item.comp === 'input'"
          v-model="formData[item.prop]"
          :placeholder="item.placeholder"
        />
        <el-select
          v-else-if="item.comp === 'select'"
          v-model="formData[item.prop]"
          :placeholder="item.placeholder"
        >
          <el-option
            v-for="opt in item.options || []"
            :key="opt.value"
            :label="opt.label"
            :value="opt.value"
          />
        </el-select>
       </el-form-item>
    </el-col>
    </template>
    </el-row>
    <el-row>
      <el-button type="primary" @click="handleSearch">查询</el-button>
      <el-button @click="handleReset">重置</el-button>
    </el-row>
  </el-form>
</template>
<script setup>
import { ref,reactive, computed } from 'vue'

const props = defineProps({
    formItem: {
        type: Array,
        default: () => []
    }
})

const emit = defineEmits(['search', 'reset'])

const formItemAttrs = computed(() => {
  const { formItem } = props
  formItem.forEach(item => {
    item.col= { xs: 24, sm: 12, md: 8, lg: 6, xl: 6 }
  })
  return formItem
})
// 表单数据（key 为各表单项的 prop），输入时自动收集
const formData = reactive({})

const handleSearch = () => {
  console.log('搜索条件:', { ...formData })
  emit('search', { ...formData })
}
// 重置：清空所有表单项，并通知父组件
const handleReset = () => {
  Object.keys(formData).forEach(key => { formData[key] = '' })
  console.log('表单已重置')
  emit('reset')
}
</script>
