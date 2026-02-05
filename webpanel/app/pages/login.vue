<script setup lang="ts">
import { Button } from "@/components/ui/button"
import { Card, CardContent } from "@/components/ui/card"
import {
  Field,
  FieldGroup,
  FieldLabel,
} from "@/components/ui/field"
import { Input } from "@/components/ui/input"
import { useAuthStore } from "~/store/auth"

definePageMeta({
  layout: "authentification",
  auth: false,
})

const auth = useAuthStore()
const route = useRoute()
const username = ref("")
const password = ref("")
const errorMessage = ref<string | null>(null)
const isSubmitting = ref(false)
const c = ref();

async function onSubmit() {
  if (isSubmitting.value) return
  errorMessage.value = null
  isSubmitting.value = true

    await auth.login(username.value.trim(), password.value)
    const redirect =
      typeof route.query.redirect === "string" ? route.query.redirect : "/"
    await navigateTo(redirect)
  try {
  } catch (error) {
    const status = (error as { response?: Response }).response?.status
    errorMessage.value =
      status === 401
        ? "Invalid username or password."
        : "Login failed. Please try again."
  } finally {
    isSubmitting.value = false
  }
}

onMounted(() => {
  c.value = Math.floor(Math.random() * 15) + 1
})
</script>

<template>
  <div class="flex flex-col gap-6">
    <Card class="overflow-hidden p-0">
      <CardContent class="grid p-0 md:grid-cols-2">
        <form class="px-6 md:px-8 py-24" @submit.prevent="onSubmit">
          <FieldGroup>
            <div class="flex flex-col items-center gap-2 text-center">
              <h1 class="text-2xl font-bold">
                Welcome back
              </h1>
              <p class="text-muted-foreground text-balance">
                Login to your Acme Inc account
              </p>
            </div>
            <Field>
              <FieldLabel for="username">
                Username
              </FieldLabel>
              <Input
                  id="username"
                  type="text"
                  placeholder="muster"
                  required
                  autocomplete="username"
                  v-model="username"
                  :disabled="isSubmitting"
              />
            </Field>
            <Field>
              <FieldLabel for="password">
                Password
              </FieldLabel>
              <Input
                  id="password"
                  type="password"
                  required
                  autocomplete="current-password"
                  v-model="password"
                  :disabled="isSubmitting"
              />
            </Field>
            <Field v-if="errorMessage">
              <p class="text-sm text-destructive">
                {{ errorMessage }}
              </p>
            </Field>
            <Field>
              <Button type="submit" :disabled="isSubmitting">
                {{ isSubmitting ? "Signing in..." : "Login" }}
              </Button>
            </Field>
          </FieldGroup>
        </form>
        <div class="bg-muted relative hidden md:block">
          <img
              v-if="c"
              :src="`/assets/images/patterns/0${c}.svg`"
              alt="Image"
              class="absolute inset-0 h-full w-full object-cover dark:brightness-[0.8]"
          >
        </div>
      </CardContent>
    </Card>
  </div>
</template>
