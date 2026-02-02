import { useAuthStore } from "~/store/auth";

export default defineNuxtPlugin(() => {
    const auth = useAuthStore()
    const config = useRuntimeConfig()
    const baseURL = config.public.brainBaseUrl?.trim() || undefined

    const api = $fetch.create({
        baseURL,
        onRequest({ options }) {
            const header = auth.authHeader
            if (!header) return

            const headers = new Headers(options.headers as HeadersInit | undefined)
            headers.set('Authorization', header)
            options.headers = headers
        },
        onResponseError({ response }) {
            if (response?.status === 401) {
                auth.logout()
            }
        },
    })

    return {
        provide: { api },
    }
})
