import { defineStore } from 'pinia'
import { useBrainApi } from '@/composables/useBrainApi'

export type Role = 'ADMIN' | 'OPERATOR' | 'VIEWER'

export interface LoginRequest {
    username: string
    password: string
}

export interface LoginResponse {
    accessToken: string
    tokenType: string // usually "Bearer"
    expiresAt: string // ISO date-time
    roles: Role[]
}

type AuthSession = LoginResponse

function normalizeSession(value: unknown): AuthSession | null {
    if (!value) return null
    if (typeof value === 'string') {
        try {
            value = JSON.parse(value)
        } catch {
            return null
        }
    }
    if (typeof value !== 'object' || value === null) return null
    const parsed = value as AuthSession
    if (!parsed?.accessToken) return null
    return parsed
}

export const useAuthStore = defineStore('auth', () => {
    const brainApi = useBrainApi()
    const sessionCookie = useCookie<AuthSession | string | null>('kodama.session', {
        sameSite: 'lax',
        secure: process.env.NODE_ENV === 'production',
    })

    const session = ref<AuthSession | null>(normalizeSession(sessionCookie.value))

    const accessToken = computed(() => session.value?.accessToken ?? null)
    const tokenType = computed(() => session.value?.tokenType ?? 'Bearer')
    const roles = computed(() => session.value?.roles ?? [])
    const expiresAt = computed(() => session.value?.expiresAt ?? null)

    const isExpired = computed(() => {
        if (!expiresAt.value) return true
        return Date.parse(expiresAt.value) <= Date.now()
    })

    const isAuthenticated = computed(() => !!accessToken.value && !isExpired.value)

    const authHeader = computed(() => {
        if (!accessToken.value || isExpired.value) return null
        return `${tokenType.value} ${accessToken.value}`
    })

    function hasRole(role: Role) {
        return roles.value.includes(role)
    }

    function setSession(next: AuthSession | null) {
        session.value = next
        sessionCookie.value = next
    }

    function initFromCookie() {
        const next = normalizeSession(sessionCookie.value)
        session.value = next
        if (!next && sessionCookie.value) {
            sessionCookie.value = null
        }
    }

    async function login(username: string, password: string) {
        const res = await brainApi<LoginResponse>('/api/auth/login', {
            method: 'POST',
            body: { username, password } satisfies LoginRequest,
        })
        setSession(res)
    }

    function logout() {
        setSession(null)
    }

    return {
        session,
        accessToken,
        tokenType,
        roles,
        expiresAt,
        isExpired,
        isAuthenticated,
        authHeader,
        hasRole,
        initFromCookie,
        login,
        logout,
    }
})
