import service from '@/utils/request'

export function login(data) {
    return service.post('/auth/login', data)
}

export function getCurrentUser() {
    return service.get('/auth/me')
}

export function logout() {
    return service.post('/auth/logout')
}

export function categoryTree() {
    return service.get('/knowledge/catagory/tree')
}

export function register(data) {
    return service.post('/auth/register', data)
}