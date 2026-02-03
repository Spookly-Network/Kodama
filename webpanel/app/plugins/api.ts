import { useAuthStore } from "~/store/auth";

export default defineNuxtPlugin(() => {
    const config = useRuntimeConfig()
    const baseURL = config.public.brainBaseUrl?.trim() || undefined

    const api = $fetch.create({
        baseURL,
        onRequest({ options }) {
            const auth = useAuthStore()
            const header = auth.authHeader
            if (!header) return

            const headers = new Headers(options.headers as HeadersInit | undefined)
            headers.set('Authorization', header)
            options.headers = headers
        },
        onResponseError({ response }) {
            if (response?.status === 401) {
                const auth = useAuthStore()
                auth.logout()
            }
        },
    })

    return {
        provide: { api },
    }
})
