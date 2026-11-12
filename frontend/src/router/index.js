import { createRouter, createWebHistory } from 'vue-router'
import BackendLayout from '@/components/BackendLayout.vue'
import AuthLayout from '@/components/AuthLayout.vue'
import { getCurrentUser } from '@/api/admin'
import { useAdminStore } from '@/stores/admin'
// 路由配置
const routes = [
  { path:'/public', component:() => import('@/views/PublicView.vue') },
  {
      path: '/',
      redirect: '/back'
  },
  {
      path: '/back',
      meta: { requiresAuth: true },
      component: BackendLayout,
      redirect: '/back/dashboard',
      children: [
        {
          path: 'dashboard',
          component: () => import('@/views/DashboardView.vue'),
          meta: {
            title: '家族总览',
            icon: 'PieChart'
          }
        },
        {
          path: 'knowledge',
          component: () => import('@/views/TreeView.vue'),
          meta: {
            title: '族谱关系图',
            icon: 'ChatLineSquare'
          }        
        },
        {
          path: 'consultations',
          component: () => import('@/views/MembersView.vue'),
          meta: {
            title: '成员档案',
            icon: 'Message'
          }
        },
        {
          path: 'emotional',
          component: () => import('@/views/EventsView.vue'),
          meta: {
            title: '家族大事记',  
            icon: 'User'
          }
        },
        { path:'album', component:() => import('@/views/AlbumView.vue'), meta:{ title:'相册与文献', icon:'Picture' } },
        { path:'settings', component:() => import('@/views/SettingsView.vue'), meta:{ title:'审核与设置', icon:'Setting' } }
      ]
  },
  {
    path: '/auth',
    component: AuthLayout,
    children: [
      {
        path: 'login',
      component: () => import('@/views/login.vue'),
      meta: {
        title: '登录'
        }
      },
      {
        path: 'register',
        component: () => import('@/views/register.vue'),
        meta: {
          title: '注册'
        }
      }
  ]
    
  }
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

router.beforeEach(async (to) => {
  if (!to.meta.requiresAuth) return true

  const adminStore = useAdminStore()

  try {
    adminStore.user = await getCurrentUser()
    return true
  } catch (error) {
    adminStore.user = null
    return { path: '/auth/login', replace: true }
  }
})

export default router
