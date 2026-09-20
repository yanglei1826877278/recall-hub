import axios from 'axios'

export const api = axios.create({
  baseURL: '/api/v1',
  withCredentials: true,
  withXSRFToken: true,
  xsrfCookieName: 'XSRF-TOKEN',
  xsrfHeaderName: 'X-XSRF-TOKEN',
  timeout: 20_000,
})

api.interceptors.response.use(
  response => response,
  error => {
    const message = error.response?.data?.message || error.message || '请求失败'
    error.userMessage = message
    if (error.response?.status === 401 && !location.pathname.startsWith('/login')) {
      location.href = '/login'
    }
    return Promise.reject(error)
  },
)

export async function unwrap<T>(promise: Promise<{ data: { data: T } }>): Promise<T> {
  const response = await promise
  return response.data.data
}

