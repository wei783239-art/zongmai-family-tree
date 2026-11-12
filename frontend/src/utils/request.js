import axios from 'axios'
import { ElMessage } from 'element-plus'
import 'element-plus/es/components/message/style/css'

const service = axios.create({
    baseURL: '/api',
    timeout: 5000
})

// 写请求先获取当前会话的 CSRF 令牌。
service.interceptors.request.use(
    async (config) => {
        const method = config.method?.toLowerCase()

        if (['post', 'put', 'patch', 'delete'].includes(method)) {
            const { data: csrf } = await axios.get('/auth/csrf', {
                baseURL: service.defaults.baseURL,
                timeout: service.defaults.timeout
            })

            config.headers[csrf.headerName] = csrf.token
        }

        return config
    },
    (error) => Promise.reject(error)
)

// 成功时返回响应数据，失败时统一显示错误。
service.interceptors.response.use(
    (response) => response.data,
    (error) => {
        const status = error.response?.status
        if (status === 401 && error.config?.url === '/auth/me') {
            return Promise.reject(error)
        }
        const message =
            error.response?.data?.message || '请求失败，请稍后重试'

        ElMessage.error(message)

        if (
            status === 401 &&
            error.config?.url !== '/auth/login'
        ) {
            window.location.href = '/auth/login'
        }

        return Promise.reject(error)
    }
)

export default service