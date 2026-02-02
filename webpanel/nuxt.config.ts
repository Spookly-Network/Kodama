// https://nuxt.com/docs/api/configuration/nuxt-config
import {defineNuxtConfig} from 'nuxt/config'
import tailwindcss from "@tailwindcss/vite";

export default defineNuxtConfig({
  compatibilityDate: '2025-07-15',
  devtools: { enabled: true },
  css: ['~/assets/css/tailwind.css'],
  runtimeConfig: {
    public: {
      brainBaseUrl: process.env.NUXT_PUBLIC_BRAIN_BASE_URL || '',
    },
  },
  vite: {
    plugins: [
        tailwindcss()
    ],
  },

  modules: ['shadcn-nuxt', '@pinia/nuxt'],

  shadcn: {
    prefix: '',
    componentDir: '@/components/ui'
  }
})
