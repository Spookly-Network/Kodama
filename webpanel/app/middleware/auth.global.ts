import { useAuthStore } from "~/store/auth"

export default defineNuxtRouteMiddleware((to) => {
  const auth = useAuthStore()
  auth.initFromCookie()

  if (to.meta.auth === false) {
    if (to.path === '/login' && auth.isAuthenticated) {
      return navigateTo('/')
    }
    return
  }

  if (auth.isAuthenticated) return

  return navigateTo({
    path: '/login',
    query: { redirect: to.fullPath },
  })
})
