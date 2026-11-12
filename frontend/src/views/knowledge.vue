<template>
  <div>
    <PageHead title="族谱关系图">
      <template #buttons>
        <el-button type="primary">新增</el-button>
        <el-button type="primary">编辑</el-button>
      </template>
    </PageHead>
    <TableSearch :formItem="formItem" @search="handleSearch" @reset="handleReset"/>
  </div>
</template>
<script setup>  
import { onMounted, ref, reactive } from 'vue'
import PageHead from '@/components/PageHead.vue'  
import TableSearch from '@/components/TableSearch.vue'
import { categoryTree } from '@/api/admin'

//分类映射
const categoryMap = reactive({})
//分类列表
const categoryies = ref([])

onMounted (async () => {
   try {
      const data = await categoryTree()
      if (!Array.isArray(data)) {
         console.warn('分类树数据格式异常', data)
         return
      }
      // 组装下拉选项，并写入分类映射
      const options = data.map(item => ({
         label: item.categoryName,
         value: item.id
      }))
      options.forEach(o => { categoryMap[o.value] = o.label })
      categoryies.value = options
      if (formItem[1]) formItem[1].options = options
   } catch (e) {
      console.error('加载分类失败', e)
   }
})

const formItem = [
  {comp: 'input', prop: 'title', label: '文章标题', placeholder: '请输入文章标题'},
  {comp: 'select', prop: 'categoryId', label: '分类', placeholder: '请选择分类'},
  {comp: 'select', prop: 'status', label: '状态', placeholder: '请选择状态', options: [{
      label: '草稿',
      value: '0'
  },{
      label: '已发布',
      value: '1'
  },{
      label: '已下线',
      value: '2'
  }]}
]

const handleSearch = (data) => {
  console.log('知识文章-搜索:', data)
}

const handleReset = () => {
  console.log('知识文章-表单已重置')
}
</script>
