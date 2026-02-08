<template>
  <div class="flex flex-1 flex-col gap-10 p-4 pt-0">
    <div class="grid auto-rows-min gap-4 md:grid-cols-4">
      <AppStatsCard variant="blue">
        <template #icon><SquareStack /></template>
        <template #number>{{ totalInstancesLabel }}</template>
        <template #label>Total instances</template>
      </AppStatsCard>
      <AppStatsCard variant="green">
        <template #icon><Activity /></template>
        <template #number>{{ runningInstancesLabel }}</template>
        <template #label>Running</template>
      </AppStatsCard>
      <AppStatsCard variant="amber">
        <template #icon><Clock /></template>
        <template #number>{{ startingInstancesLabel }}</template>
        <template #label>Starting</template>
      </AppStatsCard>
      <AppStatsCard variant="red">
        <template #icon><AlertTriangle /></template>
        <template #number>{{ failedInstancesLabel }}</template>
        <template #label>Failed</template>
      </AppStatsCard>
    </div>

    <section class="text-foreground space-y-4">
      <div class="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
        <div>
          <h2 class="text-2xl font-semibold">Instances</h2>
          <div class="text-muted-foreground">
            Monitor orchestration state, node assignment, and template coverage.
          </div>
        </div>
        <div class="flex flex-wrap items-center gap-2">
          <Badge variant="secondary">{{ unassignedInstancesLabel }} unassigned</Badge>
          <Dialog v-model:open="createDialogOpen">
            <DialogTrigger>
              <Button :disabled="createSubmitting">
                <Plus class="size-4" />
                Create instance
              </Button>
            </DialogTrigger>
            <DialogContent class="sm:max-w-3xl">
              <DialogHeader>
                <DialogTitle>Create instance</DialogTitle>
                <DialogDescription>
                  Provision a new instance using one or more template layers.
                </DialogDescription>
              </DialogHeader>
              <form class="space-y-6" @submit.prevent="submitCreate">
                <FieldGroup class="grid gap-5 sm:grid-cols-2">
                  <Field class="sm:col-span-2">
                    <FieldLabel for="instance-name">Name</FieldLabel>
                    <Input
                      id="instance-name"
                      v-model="createForm.name"
                      autocomplete="off"
                      placeholder="hytale-survival-001"
                      :disabled="createSubmitting"
                      :aria-invalid="createSubmitted && !!createErrors.name?.length"
                    />
                    <FieldError v-if="createSubmitted" :errors="createErrors.name" />
                  </Field>
                  <Field class="sm:col-span-2">
                    <FieldLabel for="instance-display-name">Display name</FieldLabel>
                    <Input
                      id="instance-display-name"
                      v-model="createForm.displayName"
                      autocomplete="off"
                      placeholder="Hytale Survival #1"
                      :disabled="createSubmitting"
                      :aria-invalid="createSubmitted && !!createErrors.displayName?.length"
                    />
                    <FieldError v-if="createSubmitted" :errors="createErrors.displayName" />
                  </Field>
                  <Field>
                    <FieldLabel for="instance-node-id">Preferred node ID</FieldLabel>
                    <Input
                      id="instance-node-id"
                      v-model="createForm.nodeId"
                      autocomplete="off"
                      placeholder="Leave blank for auto-assign"
                      :disabled="createSubmitting"
                    />
                    <FieldDescription>Optional override to schedule on a specific node.</FieldDescription>
                  </Field>
                  <Field>
                    <FieldLabel for="instance-region">Region</FieldLabel>
                    <Input
                      id="instance-region"
                      v-model="createForm.region"
                      autocomplete="off"
                      placeholder="eu-central"
                      :disabled="createSubmitting"
                    />
                  </Field>
                  <Field class="sm:col-span-2">
                    <FieldLabel for="instance-tags">Tags</FieldLabel>
                    <Input
                      id="instance-tags"
                      v-model="createForm.tags"
                      autocomplete="off"
                      placeholder="prod,ssd,low-latency"
                      :disabled="createSubmitting"
                    />
                    <FieldDescription>Comma-separated tags used for node preference.</FieldDescription>
                  </Field>
                  <Field orientation="horizontal" class="sm:col-span-2">
                    <Checkbox
                      id="instance-dev-mode"
                      v-model="createForm.devModeAllowed"
                      type="checkbox"
                      class="h-4 w-4 rounded border-input text-primary focus-visible:ring-ring/50 focus-visible:ring-[3px]"
                      :disabled="createSubmitting"
                    />
                    <FieldContent>
                      <FieldLabel for="instance-dev-mode">Allow dev mode</FieldLabel>
                      <FieldDescription>Enables development tooling in this instance.</FieldDescription>
                    </FieldContent>
                  </Field>
                </FieldGroup>

                <div class="space-y-3">
                  <div class="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
                    <div>
                      <div class="text-sm font-medium">Template layers</div>
                      <div class="text-xs text-muted-foreground">
                        At least one layer is required.
                      </div>
                    </div>
                    <Button type="button" variant="outline" size="sm" :disabled="createSubmitting" @click="addLayer">
                      <Plus class="size-4" />
                      Add layer
                    </Button>
                  </div>
                  <FieldError v-if="createSubmitted" :errors="createErrors.templateLayers" />
                  <div v-if="templatesError" class="rounded-lg border border-destructive/40 bg-destructive/10 p-3 text-sm text-destructive">
                    {{ templatesError }}
                  </div>
                  <div v-else-if="templatesLoading" class="text-xs text-muted-foreground">
                    Loading templates...
                  </div>

                  <div
                    v-for="(layer, index) in createForm.templateLayers"
                    :key="layer.key"
                    class="rounded-lg border bg-muted/30 p-4"
                  >
                    <div class="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
                      <div class="grid flex-1 gap-4 sm:grid-cols-3">
                        <Field class="sm:col-span-2">
                          <FieldLabel :for="`instance-template-${layer.key}`">Template ID</FieldLabel>
                          <Input
                            :id="`instance-template-${layer.key}`"
                            v-model="layer.templateId"
                            autocomplete="off"
                            placeholder="Template UUID"
                            :disabled="createSubmitting"
                            list="instance-template-options"
                            :aria-invalid="createSubmitted && !!layerErrors[index]?.templateId?.length"
                          />
                          <FieldError v-if="createSubmitted" :errors="layerErrors[index]?.templateId" />
                        </Field>
                        <Field>
                          <FieldLabel :for="`instance-priority-${layer.key}`">Priority</FieldLabel>
                          <Input
                            :id="`instance-priority-${layer.key}`"
                            v-model="layer.priority"
                            type="number"
                            min="0"
                            step="1"
                            placeholder="Auto"
                            :disabled="createSubmitting"
                            :aria-invalid="createSubmitted && !!layerErrors[index]?.priority?.length"
                          />
                          <FieldError v-if="createSubmitted" :errors="layerErrors[index]?.priority" />
                        </Field>
                        <Field class="sm:col-span-2">
                          <FieldLabel :for="`instance-version-${layer.key}`">Template version ID</FieldLabel>
                          <Input
                            :id="`instance-version-${layer.key}`"
                            v-model="layer.templateVersionId"
                            autocomplete="off"
                            placeholder="Optional version UUID"
                            :disabled="createSubmitting"
                          />
                          <FieldDescription>Leave blank to use the latest version.</FieldDescription>
                        </Field>
                      </div>
                      <Button
                        type="button"
                        variant="ghost"
                        size="sm"
                        class="text-muted-foreground"
                        :disabled="createSubmitting || createForm.templateLayers.length === 1"
                        @click="removeLayer(index)"
                      >
                        Remove
                      </Button>
                    </div>
                  </div>
                </div>

                <datalist id="instance-template-options">
                  <option
                    v-for="template in templates"
                    :key="template.id"
                    :value="template.id"
                    :label="template.name"
                  />
                </datalist>

                <div v-if="createError" class="rounded-lg border border-destructive/40 bg-destructive/10 p-3 text-sm text-destructive">
                  {{ createError }}
                </div>
                <DialogFooter class="gap-2">
                  <DialogClose as-child>
                    <Button type="button" variant="outline" :disabled="createSubmitting">
                      Cancel
                    </Button>
                  </DialogClose>
                  <Button type="submit" :disabled="createSubmitting">
                    {{ createSubmitting ? "Creating..." : "Create instance" }}
                  </Button>
                </DialogFooter>
              </form>
            </DialogContent>
          </Dialog>
          <Button variant="secondary" :disabled="loading" @click="loadInstances">
            {{ loading ? "Refreshing..." : "Refresh" }}
          </Button>
        </div>
      </div>

      <Card class="border bg-muted/30">
        <CardHeader class="flex flex-row items-start justify-between gap-4">
          <div class="space-y-1">
            <CardTitle>Instance fleet</CardTitle>
            <CardDescription>
              Latest status, template layers, and lifecycle timestamps.
            </CardDescription>
          </div>
          <Badge variant="secondary">{{ totalInstancesLabel }} total</Badge>
        </CardHeader>
        <CardContent>
          <div class="overflow-hidden rounded-lg border">
            <AppInstancesTable
              :data="instanceRows"
              :columns="columns"
              :loading="loading"
              empty-label="No instances found."
            />
          </div>
          <div
            v-if="actionError"
            class="mt-4 rounded-lg border border-destructive/40 bg-destructive/10 p-3 text-sm text-destructive"
          >
            {{ actionError }}
          </div>
          <div
            v-if="loadError"
            class="mt-4 rounded-lg border border-destructive/40 bg-destructive/10 p-3 text-sm text-destructive"
          >
            {{ loadError }}
            <Button variant="secondary" size="sm" class="ml-3" @click="loadInstances">
              Retry
            </Button>
          </div>
        </CardContent>
      </Card>
    </section>
  </div>
</template>

<script setup lang="ts">
import { Activity, AlertTriangle, Clock, Plus, SquareStack } from "lucide-vue-next"
import type { Instance, InstanceState } from "#shared/types/Instance"
import { buildColumns, type InstanceRow } from "~/components/app/instances/columns"

type TemplateSummary = {
  id: string
  name: string
}

type TemplateAssignmentRequest = {
  templateId: string
  templateVersionId?: string | null
  priority?: number | null
}

type CreateInstanceRequest = {
  name: string
  displayName: string
  nodeId?: string | null
  region?: string | null
  tags?: string | null
  devModeAllowed?: boolean | null
  templateLayers: TemplateAssignmentRequest[]
}

type CreateInstanceLayer = {
  key: number
  templateId: string
  templateVersionId: string
  priority: string
}

type CreateInstanceForm = {
  name: string
  displayName: string
  nodeId: string
  region: string
  tags: string
  devModeAllowed: boolean
  templateLayers: CreateInstanceLayer[]
}

const brainApi = useBrainApi()

const instances = ref<Instance[]>([])
const loading = ref(true)
const loadError = ref<string | null>(null)
const actionError = ref<string | null>(null)
const createDialogOpen = ref(false)
const createSubmitting = ref(false)
const createSubmitted = ref(false)
const createError = ref<string | null>(null)
const templates = ref<TemplateSummary[]>([])
const templatesLoading = ref(false)
const templatesError = ref<string | null>(null)
const actionSubmitting = reactive<Record<string, boolean>>({})
let nextLayerKey = 0

const buildLayer = (): CreateInstanceLayer => ({
  key: nextLayerKey++,
  templateId: "",
  templateVersionId: "",
  priority: "",
})

const createForm = reactive<CreateInstanceForm>({
  name: "",
  displayName: "",
  nodeId: "",
  region: "",
  tags: "",
  devModeAllowed: false,
  templateLayers: [buildLayer()],
})

const pendingStates = new Set<InstanceState>([
  "REQUESTED",
  "PREPARING",
  "PREPARED",
  "STARTING",
])

const totalInstances = computed(() => instances.value.length)
const runningInstances = computed(() => instances.value.filter((instance) => instance.state === "RUNNING").length)
const startingInstances = computed(() => instances.value.filter((instance) => pendingStates.has(instance.state)).length)
const failedInstances = computed(() => instances.value.filter((instance) => instance.state === "FAILED").length)
const unassignedInstances = computed(() => instances.value.filter((instance) => !instance.nodeId).length)

const totalInstancesLabel = computed(() => (loading.value ? "--" : totalInstances.value))
const runningInstancesLabel = computed(() => (loading.value ? "--" : runningInstances.value))
const startingInstancesLabel = computed(() => (loading.value ? "--" : startingInstances.value))
const failedInstancesLabel = computed(() => (loading.value ? "--" : failedInstances.value))
const unassignedInstancesLabel = computed(() => (loading.value ? "--" : unassignedInstances.value))

const dateFormatter = new Intl.DateTimeFormat("en-US", {
  month: "short",
  day: "numeric",
  year: "numeric",
})

const formatDate = (value: string) => {
  const parsed = Date.parse(value)
  if (Number.isNaN(parsed)) return "--"
  return dateFormatter.format(parsed)
}

const formatOptionalDate = (value: string | null) => {
  if (!value) return null
  return formatDate(value)
}

const formatStateLabel = (state: InstanceState) =>
  state
    .toLowerCase()
    .split("_")
    .map((segment) => segment.charAt(0).toUpperCase() + segment.slice(1))
    .join(" ")

const formatNodeLabel = (nodeId: string | null) => {
  if (!nodeId) return "Unassigned"
  return nodeId.length > 8 ? `${nodeId.slice(0, 8)}...` : nodeId
}

const parseTags = (value: string | null) => {
  if (!value) return []
  return value
    .split(",")
    .map((tag) => tag.trim())
    .filter(Boolean)
}

const instanceRows = computed<InstanceRow[]>(() =>
  instances.value.map((instance) => {
    const templateLayers = instance.templateLayers ?? []
    const groupLayerCount = templateLayers.filter((layer) => layer.source === "GROUP").length
    return {
      id: instance.id,
      name: instance.name,
      displayName: instance.displayName,
      state: instance.state,
      stateLabel: formatStateLabel(instance.state),
      nodeId: instance.nodeId ?? null,
      nodeLabel: formatNodeLabel(instance.nodeId ?? null),
      regionLabel: instance.region ?? "--",
      tags: parseTags(instance.tags ?? null),
      devModeAllowed: instance.devModeAllowed ?? null,
      templateLayerCount: templateLayers.length,
      groupLayerCount,
      updatedAtLabel: formatDate(instance.updatedAt),
      createdAtLabel: formatDate(instance.createdAt),
      startedAtLabel: formatOptionalDate(instance.startedAt ?? null),
      stoppedAtLabel: formatOptionalDate(instance.stoppedAt ?? null),
      failureReason: instance.failureReason ?? null,
    }
  })
)

const isActionBusy = (instanceId: string) => Boolean(actionSubmitting[instanceId])

const findInstanceById = (instanceId: string) =>
  instances.value.find((instance) => instance.id === instanceId) ?? null

const applyCreateFormFromInstance = (instance: Instance) => {
  nextLayerKey = 0
  createForm.name = instance.name
  createForm.displayName = instance.displayName
  createForm.nodeId = instance.nodeId ?? ""
  createForm.region = instance.region ?? ""
  createForm.tags = instance.tags ?? ""
  createForm.devModeAllowed = instance.devModeAllowed ?? false

  const instanceLayers = (instance.templateLayers ?? []).filter((layer) => layer.source === "INSTANCE")
  if (instanceLayers.length) {
    createForm.templateLayers = instanceLayers.map((layer) => ({
      key: nextLayerKey++,
      templateId: layer.templateId,
      templateVersionId: layer.templateVersionId,
      priority: String(layer.priority),
    }))
  } else {
    createForm.templateLayers = [buildLayer()]
  }

  createSubmitted.value = false
  createError.value = null
}

const extractErrorMessage = (error: unknown, fallback: string) => {
  if (!error || typeof error !== "object") return fallback
  const record = error as { data?: { message?: string }; message?: string }
  return record.data?.message || record.message || fallback
}

const resetCreateForm = () => {
  nextLayerKey = 0
  createForm.name = ""
  createForm.displayName = ""
  createForm.nodeId = ""
  createForm.region = ""
  createForm.tags = ""
  createForm.devModeAllowed = false
  createForm.templateLayers = [buildLayer()]
  createSubmitted.value = false
  createError.value = null
}

const layerErrors = computed(() =>
  createForm.templateLayers.map((layer) => {
    const errors: Record<string, string[]> = {}
    if (!layer.templateId.trim()) errors.templateId = ["Template ID is required."]
    const priorityValue = layer.priority.trim()
    if (priorityValue) {
      const parsed = Number(priorityValue)
      if (!Number.isInteger(parsed) || parsed < 0) {
        errors.priority = ["Priority must be 0 or greater."]
      }
    }
    return errors
  })
)

const createErrors = computed(() => {
  const next: Record<string, string[]> = {}
  if (!createForm.name.trim()) next.name = ["Name is required."]
  if (!createForm.displayName.trim()) next.displayName = ["Display name is required."]
  if (!createForm.templateLayers.length) {
    next.templateLayers = ["At least one template layer is required."]
  }
  return next
})

const isCreateValid = computed(
  () =>
    Object.keys(createErrors.value).length === 0 &&
    layerErrors.value.every((errors) => Object.keys(errors).length === 0)
)

const addLayer = () => {
  createForm.templateLayers.push(buildLayer())
}

const removeLayer = (index: number) => {
  if (createForm.templateLayers.length <= 1) return
  createForm.templateLayers.splice(index, 1)
}

const buildCreatePayload = (): CreateInstanceRequest => {
  const payload: CreateInstanceRequest = {
    name: createForm.name.trim(),
    displayName: createForm.displayName.trim(),
    templateLayers: createForm.templateLayers.map((layer) => {
      const assignment: TemplateAssignmentRequest = {
        templateId: layer.templateId.trim(),
      }
      const versionId = layer.templateVersionId.trim()
      if (versionId) assignment.templateVersionId = versionId
      const priorityValue = layer.priority.trim()
      if (priorityValue) assignment.priority = Number(priorityValue)
      return assignment
    }),
  }

  const nodeId = createForm.nodeId.trim()
  if (nodeId) payload.nodeId = nodeId

  const region = createForm.region.trim()
  if (region) payload.region = region

  const tags = createForm.tags.trim()
  if (tags) payload.tags = tags

  if (createForm.devModeAllowed) {
    payload.devModeAllowed = true
  }

  return payload
}

const submitInstanceAction = async (
  instanceId: string,
  action: "start" | "stop" | "destroy",
  fallbackMessage: string
) => {
  if (actionSubmitting[instanceId]) return
  actionSubmitting[instanceId] = true
  actionError.value = null
  try {
    await brainApi<Instance>(`/api/instances/${instanceId}/${action}`, { method: "POST" })
    await loadInstances()
  } catch (error) {
    actionError.value = extractErrorMessage(error, fallbackMessage)
  } finally {
    actionSubmitting[instanceId] = false
  }
}

const handleStart = (row: InstanceRow) =>
  submitInstanceAction(row.id, "start", "Unable to start instance.")

const handleStop = (row: InstanceRow) =>
  submitInstanceAction(row.id, "stop", "Unable to stop instance.")

const handleDestroy = (row: InstanceRow) => {
  const confirmed = window.confirm(`Destroy ${row.displayName || row.name}? This cannot be undone.`)
  if (!confirmed) return
  return submitInstanceAction(row.id, "destroy", "Unable to destroy instance.")
}

const handleCopy = (row: InstanceRow) => {
  const instance = findInstanceById(row.id)
  if (!instance) return
  applyCreateFormFromInstance(instance)
  createDialogOpen.value = true
}

const loadInstances = async () => {
  loading.value = true
  loadError.value = null
  try {
    const result = await brainApi<Instance[]>("/api/instances")
    instances.value = Array.isArray(result) ? result : []
  } catch (error) {
    loadError.value = extractErrorMessage(error, "Unable to load instances.")
    instances.value = []
  } finally {
    loading.value = false
  }
}

const loadTemplates = async () => {
  if (templatesLoading.value) return
  templatesLoading.value = true
  templatesError.value = null
  try {
    const result = await brainApi<TemplateSummary[]>("/api/templates")
    templates.value = Array.isArray(result) ? result : []
  } catch (error) {
    templatesError.value = extractErrorMessage(error, "Unable to load templates.")
    templates.value = []
  } finally {
    templatesLoading.value = false
  }
}

const submitCreate = async () => {
  createSubmitted.value = true
  createError.value = null
  if (!isCreateValid.value) return
  createSubmitting.value = true
  try {
    const payload = buildCreatePayload()
    await brainApi<Instance>("/api/instances", {
      method: "POST",
      body: payload satisfies CreateInstanceRequest,
    })
    createDialogOpen.value = false
    resetCreateForm()
    await loadInstances()
  } catch (error) {
    createError.value = extractErrorMessage(error, "Unable to create instance.")
  } finally {
    createSubmitting.value = false
  }
}

const columns = buildColumns({
  onStart: handleStart,
  onStop: handleStop,
  onDestroy: handleDestroy,
  onCopy: handleCopy,
  isBusy: (row) => isActionBusy(row.id),
})

watch(createDialogOpen, (open) => {
  if (open && !templates.value.length) {
    loadTemplates()
  }
  if (!open) {
    resetCreateForm()
  }
})

await loadInstances()
</script>

<style scoped>

</style>
