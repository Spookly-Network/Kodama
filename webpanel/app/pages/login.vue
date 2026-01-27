<script setup lang="ts">
import { Button } from "@/components/ui/button"
import { Card, CardContent } from "@/components/ui/card"
import {
  Field,
  FieldGroup,
  FieldLabel,
} from "@/components/ui/field"
import { Input } from "@/components/ui/input"

import pattern from "@/assets/images/patterns/01.svg"

definePageMeta({
  layout: "authentification"
})
</script>

<template>
  <div class="flex flex-col gap-6">
    <Card class="overflow-hidden p-0">
      <CardContent class="grid p-0 md:grid-cols-2">
        <form class="px-6 md:px-8 py-24">
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
              />
            </Field>
            <Field>
              <FieldLabel for="password">
                Password
              </FieldLabel>
              <Input id="password" type="password" required />
            </Field>
            <Field>
              <Button type="submit">
                Login
              </Button>
            </Field>
          </FieldGroup>
        </form>
        <div class="bg-muted relative hidden md:block">
          <img
              :src="pattern"
              alt="Image"
              class="absolute inset-0 h-full w-full object-cover dark:brightness-[0.5]"
          >
        </div>
      </CardContent>
    </Card>
  </div>
</template>
