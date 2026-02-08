<script setup lang="ts">
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import {
  Field,
  FieldDescription,
  FieldError,
  FieldGroup,
  FieldLabel,
  FieldContent,
} from "@/components/ui/field"
import { default as AppStatusBadge } from "~/components/app/AppStatusBadge.vue"
import type { NodeDto } from "#shared/types/Node"
import type { NodeUpdatePayload } from "~/store/nodes"

const props = withDefaults(defineProps<{
  node: NodeDto
  isSubmitting?: boolean
}>(), {
  isSubmitting: false,
})

const emit = defineEmits<{
  (event: "submit", payload: NodeUpdatePayload): void
  (event: "cancel"): void
}>()

const form = reactive({
  name: "",
  region: "",
  capacitySlots: 1,
  nodeVersion: "",
  devMode: false,
  tags: "",
  baseUrl: "",
})

const hasSubmitted = ref(false)

function resetForm(node: NodeDto) {
  form.name = node.name ?? ""
  form.region = node.region ?? ""
  form.capacitySlots = node.capacitySlots ?? 1
  form.nodeVersion = node.nodeVersion ?? ""
  form.devMode = node.devMode ?? false
  form.tags = node.tags ?? ""
  form.baseUrl = node.baseUrl ?? ""
  hasSubmitted.value = false
}

watch(
  () => props.node,
  (node) => {
    if (node) resetForm(node)
  },
  { immediate: true }
)

function isValidUrl(value: string) {
  try {
    new URL(value)
    return true
  } catch {
    return false
  }
}

const errors = computed(() => {
  const next: Record<string, string[]> = {}

  if (!form.region.trim()) next.region = ["Region is required."]
  if (!form.nodeVersion.trim()) next.nodeVersion = ["Node version is required."]
  if (!Number.isFinite(form.capacitySlots) || form.capacitySlots < 1) {
    next.capacitySlots = ["Capacity slots must be at least 1."]
  }
  if (Number.isFinite(form.capacitySlots) && form.capacitySlots < props.node.usedSlots) {
    next.capacitySlots = [
      ...(next.capacitySlots ?? []),
      `Capacity slots must be at least ${props.node.usedSlots}.`,
    ]
  }
  if (form.baseUrl.trim() && !isValidUrl(form.baseUrl.trim())) {
    next.baseUrl = ["Base URL must be a valid URL."]
  }

  return next
})

const isValid = computed(() => Object.keys(errors.value).length === 0)

function buildPayload(): NodeUpdatePayload {
  const payload: NodeUpdatePayload = {
    region: form.region.trim(),
    capacitySlots: Number(form.capacitySlots),
    nodeVersion: form.nodeVersion.trim(),
    devMode: form.devMode,
  }

  const tags = form.tags.trim()
  payload.tags = tags ? tags : null

  const baseUrl = form.baseUrl.trim()
  payload.baseUrl = baseUrl ? baseUrl : null

  return payload
}

function onSubmit() {
  hasSubmitted.value = true
  if (!isValid.value) return
  emit("submit", buildPayload())
}
</script>

<template>
  <form class="space-y-6" @submit.prevent="onSubmit">
    <div class="rounded-lg border bg-muted/30 p-4 text-sm">
      <div class="flex flex-wrap items-center justify-between gap-4">
        <div class="space-y-1">
          <div class="text-xs text-muted-foreground">Node ID</div>
          <div class="font-mono text-xs">{{ props.node.id }}</div>
        </div>
        <AppStatusBadge :variant="props.node.status">
          {{ props.node.status }}
        </AppStatusBadge>
      </div>
    </div>

    <FieldGroup class="grid gap-5 sm:grid-cols-2">
      <Field class="sm:col-span-2">
        <FieldLabel for="edit-node-name">Name</FieldLabel>
        <Input
          id="edit-node-name"
          v-model="form.name"
          autocomplete="off"
          disabled
        />
        <FieldDescription>Name is managed by the node agent.</FieldDescription>
      </Field>

      <Field>
        <FieldLabel for="edit-node-region">Region<span class="text-red-500">*</span></FieldLabel>
        <Input
          id="edit-node-region"
          v-model="form.region"
          autocomplete="off"
          placeholder="eu-central"
          :disabled="isSubmitting"
          :aria-invalid="hasSubmitted && !!errors.region?.length"
        />
        <FieldError v-if="hasSubmitted" :errors="errors.region" />
      </Field>

      <Field>
        <FieldLabel for="edit-node-capacity">Capacity slots<span class="text-red-500">*</span></FieldLabel>
        <Input
          id="edit-node-capacity"
          v-model.number="form.capacitySlots"
          type="number"
          min="1"
          step="1"
          :disabled="isSubmitting"
          :aria-invalid="hasSubmitted && !!errors.capacitySlots?.length"
        />
        <FieldDescription>Must be at least the current used slots ({{ props.node.usedSlots }}).</FieldDescription>
        <FieldError v-if="hasSubmitted" :errors="errors.capacitySlots" />
      </Field>

      <Field>
        <FieldLabel for="edit-node-version">Node version<span class="text-red-500">*</span></FieldLabel>
        <Input
          id="edit-node-version"
          v-model="form.nodeVersion"
          autocomplete="off"
          placeholder="1.4.2"
          :disabled="isSubmitting"
          :aria-invalid="hasSubmitted && !!errors.nodeVersion?.length"
        />
        <FieldError v-if="hasSubmitted" :errors="errors.nodeVersion" />
      </Field>

      <Field>
        <FieldLabel for="edit-node-base-url">Base URL</FieldLabel>
        <Input
          id="edit-node-base-url"
          v-model="form.baseUrl"
          type="url"
          autocomplete="off"
          placeholder="https://node-fra-1.kodama.internal"
          :disabled="isSubmitting"
          :aria-invalid="hasSubmitted && !!errors.baseUrl?.length"
        />
        <FieldDescription>Used by the control plane to reach the node.</FieldDescription>
        <FieldError v-if="hasSubmitted" :errors="errors.baseUrl" />
      </Field>

      <Field>
        <FieldLabel for="edit-node-tags">Tags</FieldLabel>
        <Input
          id="edit-node-tags"
          v-model="form.tags"
          autocomplete="off"
          placeholder="prod,ssd,hetzner"
          :disabled="isSubmitting"
        />
        <FieldDescription>Comma-separated labels for scheduling and filtering.</FieldDescription>
      </Field>

      <Field orientation="horizontal" class="sm:col-span-2">
        <Checkbox
          id="edit-node-dev-mode"
          v-model="form.devMode"
          type="checkbox"
          class="h-4 w-4 rounded border-input text-primary focus-visible:ring-ring/50 focus-visible:ring-[3px]"
          :disabled="isSubmitting"
        />
        <FieldContent>
          <FieldLabel for="edit-node-dev-mode">Enable dev mode</FieldLabel>
          <FieldDescription>Marked for development-only scheduling.</FieldDescription>
        </FieldContent>
      </Field>
    </FieldGroup>

    <div class="flex flex-col gap-3 sm:flex-row sm:justify-end">
      <Button type="button" variant="outline" :disabled="isSubmitting" @click="emit('cancel')">
        Cancel
      </Button>
      <Button type="submit" :disabled="isSubmitting">
        {{ isSubmitting ? "Saving..." : "Save changes" }}
      </Button>
    </div>
  </form>
</template>
