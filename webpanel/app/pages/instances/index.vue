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
import { Activity, AlertTriangle, Clock, SquareStack } from "lucide-vue-next"
import type { Instance, InstanceState } from "#shared/types/Instance"
import { columns, type InstanceRow } from "~/components/app/instances/columns"

const brainApi = useBrainApi()

const instances = ref<Instance[]>([])
const loading = ref(true)
const loadError = ref<string | null>(null)

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

const extractErrorMessage = (error: unknown, fallback: string) => {
  if (!error || typeof error !== "object") return fallback
  const record = error as { data?: { message?: string }; message?: string }
  return record.data?.message || record.message || fallback
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

await loadInstances()
</script>

<style scoped>

</style>
