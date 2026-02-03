<script setup lang="ts">
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import {
  Field,
  FieldDescription,
  FieldError,
  FieldGroup,
  FieldLabel,
} from "@/components/ui/field"
import { NodeStatus } from "#shared/types/Node"
import {useNodesStore} from "~/store/nodes";

export interface NodeCreatePayload {
  name: string
  region: string
  capacitySlots: number
  nodeVersion: string
  devMode: boolean
  tags?: string | null
  baseUrl?: string | null
  status?: NodeStatus
}

const nodeStore = useNodesStore();

const props = withDefaults(defineProps<{
  isSubmitting?: boolean
  initialValue?: Partial<NodeCreatePayload>
}>(), {
  isSubmitting: false,
  initialValue: () => ({}),
})

const emit = defineEmits<{
  (event: "submit", payload: NodeCreatePayload): void
  (event: "cancel"): void
}>()

const form = reactive({
  name: props.initialValue.name ?? "",
  region: props.initialValue.region ?? "",
  capacitySlots: props.initialValue.capacitySlots ?? 1,
  nodeVersion: props.initialValue.nodeVersion ?? "",
  devMode: props.initialValue.devMode ?? false,
  tags: props.initialValue.tags ?? "",
  baseUrl: props.initialValue.baseUrl ?? "",
  statusOverride: props.initialValue.status ?? "",
})

const hasSubmitted = ref(false)

const inputClasses =
  "file:text-foreground placeholder:text-muted-foreground selection:bg-primary selection:text-primary-foreground dark:bg-input/30 border-input h-9 w-full min-w-0 rounded-md border bg-transparent px-3 py-1 text-base shadow-xs transition-[color,box-shadow] outline-none file:inline-flex file:h-7 file:border-0 file:bg-transparent file:text-sm file:font-medium disabled:pointer-events-none disabled:cursor-not-allowed disabled:opacity-50 md:text-sm focus-visible:border-ring focus-visible:ring-ring/50 focus-visible:ring-[3px] aria-invalid:ring-destructive/20 dark:aria-invalid:ring-destructive/40 aria-invalid:border-destructive"

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

  if (!form.name.trim()) next.name = ["Name is required."]
  if (!form.region.trim()) next.region = ["Region is required."]
  if (!form.nodeVersion.trim()) next.nodeVersion = ["Node version is required."]
  if (!Number.isFinite(form.capacitySlots) || form.capacitySlots < 1) {
    next.capacitySlots = ["Capacity slots must be at least 1."]
  }
  if (form.baseUrl.trim() && !isValidUrl(form.baseUrl.trim())) {
    next.baseUrl = ["Base URL must be a valid URL."]
  }

  return next
})

const isValid = computed(() => Object.keys(errors.value).length === 0)

function buildPayload(): NodeCreatePayload {
  const payload: NodeCreatePayload = {
    name: form.name.trim(),
    region: form.region.trim(),
    capacitySlots: Number(form.capacitySlots),
    nodeVersion: form.nodeVersion.trim(),
    devMode: form.devMode,
  }

  const tags = form.tags.trim()
  if (tags) payload.tags = tags

  const baseUrl = form.baseUrl.trim()
  if (baseUrl) payload.baseUrl = baseUrl

  if (form.statusOverride) {
    payload.status = form.statusOverride as NodeStatus
  }

  return payload
}

function onSubmit() {
  hasSubmitted.value = true
  if (!isValid.value) return
  nodeStore.create(buildPayload());
  emit("submit", buildPayload())
}
</script>

<template>
  <form class="space-y-6" @submit.prevent="onSubmit">
    <FieldGroup class="gap-5 grid grid-cols-2 items-center">
      <Field>
        <FieldLabel for="node-name">Name<span class="text-red-500">*</span></FieldLabel>
        <Input
          id="node-name"
          v-model="form.name"
          autocomplete="off"
          placeholder="fra-1"
          :disabled="isSubmitting"
          :aria-invalid="hasSubmitted && !!errors.name?.length"
        />
        <FieldError v-if="hasSubmitted" :errors="errors.name" />
      </Field>

      <Field>
        <FieldLabel for="node-region">Region</FieldLabel>
        <Input
          id="node-region"
          v-model="form.region"
          autocomplete="off"
          placeholder="eu-central"
          :disabled="isSubmitting"
          :aria-invalid="hasSubmitted && !!errors.region?.length"
        />
        <FieldError v-if="hasSubmitted" :errors="errors.region" />
      </Field>

      <Field>
        <FieldLabel for="node-capacity">Capacity slots<span class="text-red-500">*</span></FieldLabel>
        <Input
          id="node-capacity"
          v-model.number="form.capacitySlots"
          type="number"
          min="1"
          step="1"
          :disabled="isSubmitting"
          :aria-invalid="hasSubmitted && !!errors.capacitySlots?.length"
        />
        <FieldDescription>Maximum concurrent instances this node can host.</FieldDescription>
        <FieldError v-if="hasSubmitted" :errors="errors.capacitySlots" />
      </Field>

      <Field>
        <FieldLabel for="node-version">Node version</FieldLabel>
        <Input
          id="node-version"
          v-model="form.nodeVersion"
          autocomplete="off"
          placeholder="1.4.2"
          :disabled="isSubmitting"
          :aria-invalid="hasSubmitted && !!errors.nodeVersion?.length"
        />
        <FieldError v-if="hasSubmitted" :errors="errors.nodeVersion" />
      </Field>

      <Field>
        <FieldLabel for="node-base-url">Base URL<span class="text-red-500">*</span></FieldLabel>
        <Input
          id="node-base-url"
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
        <FieldLabel for="node-tags">Tags</FieldLabel>
        <Input
          id="node-tags"
          v-model="form.tags"
          autocomplete="off"
          placeholder="prod,ssd,hetzner"
          :disabled="isSubmitting"
        />
        <FieldDescription>Comma-separated labels for scheduling and filtering.</FieldDescription>
      </Field>

      <Field>
        <FieldLabel for="node-status">Initial status</FieldLabel>
        <select
          id="node-status"
          v-model="form.statusOverride"
          :class="inputClasses"
          :disabled="isSubmitting"
        >
          <option value="">Default (Online)</option>
          <option :value="NodeStatus.ONLINE">Online</option>
          <option :value="NodeStatus.OFFLINE">Offline</option>
          <option :value="NodeStatus.UNKNOWN">Unknown</option>
        </select>
        <FieldDescription>Leave default unless you need to override.</FieldDescription>
      </Field>

      <Field orientation="horizontal">
        <Checkbox
          id="node-dev-mode"
          v-model="form.devMode"
          type="checkbox"
          class="h-4 w-4 rounded border-input text-primary focus-visible:ring-ring/50 focus-visible:ring-[3px]"
          :disabled="isSubmitting"
        />
        <FieldContent>
          <FieldLabel for="node-dev-mode">Enable dev mode</FieldLabel>
        </FieldContent>
      </Field>
    </FieldGroup>

    <div class="flex flex-col gap-3 sm:flex-row sm:justify-end">
      <Button type="button" variant="outline" :disabled="isSubmitting" @click="emit('cancel')">
        Cancel
      </Button>
      <Button type="submit" :disabled="isSubmitting" @click="onSubmit">
        Create node
      </Button>
    </div>
  </form>
</template>
