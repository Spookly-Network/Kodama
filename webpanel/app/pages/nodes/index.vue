<template>
  <div class="flex flex-1 flex-col gap-10 p-4 pt-0">
    <div class="grid auto-rows-min gap-4 md:grid-cols-4">
      <AppStatsCard variant="blue">
        <template #icon><Server /></template>
        <template #number>{{ totalNodesLabel }}</template>
        <template #label>Total nodes</template>
      </AppStatsCard>

      <AppStatsCard variant="green">
        <template #icon><Globe /></template>
        <template #number>{{ amountNodesOnlineLabel }}</template>
        <template #label>Nodes online</template>
      </AppStatsCard>

      <AppStatsCard variant="red">
        <template #icon><GlobeX /></template>
        <template #number>{{ amountNodesOfflineLabel }}</template>
        <template #label>Nodes offline</template>
      </AppStatsCard>

      <AppStatsCard variant="amber">
        <template #icon><CircleDot /></template>
        <template #number>{{ slotsUsedLabel }}</template>
        <template #label>Slots used</template>
      </AppStatsCard>
    </div>

    <section class="text-foreground space-y-4">
      <div class="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
        <div>
          <h2 class="text-2xl font-semibold">Nodes</h2>
          <div class="text-muted-foreground">
            Manage node metadata, capacity, and heartbeat health.
          </div>
        </div>
        <div class="flex flex-wrap items-center gap-2">
          <Badge variant="secondary">{{ slotsOpenLabel }} slots open</Badge>
          <Badge variant="secondary">{{ utilizationLabel }} utilization</Badge>
          <Badge variant="secondary">{{ amountNodesUnknownLabel }} unknown</Badge>
          <Button variant="secondary" :disabled="loading" @click="refreshNodes">
            <RefreshCw class="h-4 w-4" />
            {{ loading ? "Refreshing..." : "Refresh" }}
          </Button>
        </div>
      </div>

      <Card class="border bg-muted/30">
        <CardHeader class="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
          <div class="space-y-1">
            <CardTitle>Node fleet</CardTitle>
            <CardDescription>Heartbeat status, capacity usage, and operator metadata.</CardDescription>
          </div>
          <div class="text-xs text-muted-foreground">
            Last refresh: {{ lastRefreshLabel }}
          </div>
        </CardHeader>
        <CardContent class="p-0">
          <AppNodesTable :data="rows" :columns="columns" />
        </CardContent>
      </Card>
    </section>

    <Dialog v-model:open="editDialogOpen">
      <DialogContent class="sm:max-w-4xl">
        <DialogHeader>
          <DialogTitle>Edit node</DialogTitle>
          <DialogDescription>
            Update operator-controlled metadata and capacity values.
          </DialogDescription>
        </DialogHeader>
        <div v-if="editError" class="rounded-lg border border-destructive/40 bg-destructive/10 p-3 text-sm text-destructive">
          {{ editError }}
        </div>
        <AppNodesEditForm
          v-if="editNode"
          :node="editNode"
          :is-submitting="editSubmitting"
          @submit="submitEdit"
          @cancel="closeEdit"
        />
      </DialogContent>
    </Dialog>
  </div>
</template>

<script lang="ts" setup>
import { buildColumns, type NodeRow } from "~/components/app/nodes/columns"
import AppNodesEditForm from "~/components/app/nodes/AppNodesEditForm.vue"
import { useNodesStore, type NodeUpdatePayload } from "~/store/nodes"
import { NodeStatus, type NodeDto } from "#shared/types/Node"
import { CircleDot, Globe, GlobeX, RefreshCw, Server } from "lucide-vue-next"

const nodesStore = useNodesStore()

const nodes = computed(() => Object.values(nodesStore.byId))
const loading = computed(() => nodesStore.loading)

const amountNodesOnline = computed(
  () => nodes.value.filter((node) => node.status === NodeStatus.ONLINE).length
)
const amountNodesOffline = computed(
  () => nodes.value.filter((node) => node.status === NodeStatus.OFFLINE).length
)
const amountNodesUnknown = computed(
  () => nodes.value.filter((node) => node.status === NodeStatus.UNKNOWN).length
)
const totalNodes = computed(() => nodes.value.length)

const totalCapacity = computed(() =>
  nodes.value.reduce((total, node) => total + node.capacitySlots, 0)
)
const totalUsed = computed(() =>
  nodes.value.reduce((total, node) => total + node.usedSlots, 0)
)
const slotsOpen = computed(() => Math.max(totalCapacity.value - totalUsed.value, 0))
const utilization = computed(() =>
  totalCapacity.value > 0 ? Math.round((totalUsed.value / totalCapacity.value) * 100) : 0
)

const totalNodesLabel = computed(() => (loading.value ? "--" : totalNodes.value))
const amountNodesOnlineLabel = computed(() => (loading.value ? "--" : amountNodesOnline.value))
const amountNodesOfflineLabel = computed(() => (loading.value ? "--" : amountNodesOffline.value))
const amountNodesUnknownLabel = computed(() => (loading.value ? "--" : amountNodesUnknown.value))
const slotsUsedLabel = computed(() => (loading.value ? "--" : totalUsed.value))
const slotsOpenLabel = computed(() => (loading.value ? "--" : slotsOpen.value))
const utilizationLabel = computed(() => (loading.value ? "--" : `${utilization.value}%`))

const dateTimeFormatter = new Intl.DateTimeFormat("en-US", {
  month: "short",
  day: "numeric",
  year: "numeric",
  hour: "2-digit",
  minute: "2-digit",
})

const formatDateTime = (value: string) => {
  const parsed = Date.parse(value)
  if (Number.isNaN(parsed)) return "--"
  return dateTimeFormatter.format(parsed)
}

const formatHeartbeatAge = (value: string) => {
  const parsed = Date.parse(value)
  if (Number.isNaN(parsed)) return "--"
  const diffMs = Date.now() - parsed
  if (diffMs < 0) return "just now"
  const seconds = Math.floor(diffMs / 1000)
  if (seconds < 30) return "just now"
  if (seconds < 60) return `${seconds}s ago`
  const minutes = Math.floor(seconds / 60)
  if (minutes < 60) return `${minutes}m ago`
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours}h ago`
  const days = Math.floor(hours / 24)
  return `${days}d ago`
}

const parseTags = (value: string | null) => {
  if (!value) return []
  return value
    .split(",")
    .map((tag) => tag.trim())
    .filter(Boolean)
}

const rows = computed<NodeRow[]>(() => {
  return [...nodes.value]
    .sort((a, b) => a.name.localeCompare(b.name))
    .map((node) => ({
      id: node.id,
      name: node.name,
      region: node.region,
      status: node.status,
      devMode: node.devMode,
      capacitySlots: node.capacitySlots,
      usedSlots: node.usedSlots,
      lastHeartbeatAt: node.lastHeartbeatAt,
      nodeVersion: node.nodeVersion,
      tags: parseTags(node.tags ?? null),
      baseUrl: node.baseUrl ?? null,
      heartbeatLabel: formatDateTime(node.lastHeartbeatAt),
      heartbeatAgoLabel: formatHeartbeatAge(node.lastHeartbeatAt),
      usagePercent: node.capacitySlots > 0
        ? Math.min(100, Math.round((node.usedSlots / node.capacitySlots) * 100))
        : 0,
    }))
})

const lastRefreshLabel = computed(() => {
  if (!nodesStore.lastLoadedAt) return "--"
  return dateTimeFormatter.format(nodesStore.lastLoadedAt)
})

const editDialogOpen = ref(false)
const editNode = ref<NodeDto | null>(null)
const editSubmitting = ref(false)
const editError = ref<string | null>(null)

const columns = buildColumns({
  onEdit: (node) => openEdit(node),
})

watch(editDialogOpen, (isOpen) => {
  if (!isOpen) {
    editNode.value = null
    editError.value = null
  }
})

function openEdit(node: NodeRow) {
  const found = nodesStore.get(node.id)
  if (!found) return
  editNode.value = found
  editError.value = null
  editDialogOpen.value = true
}

function closeEdit() {
  editDialogOpen.value = false
  editNode.value = null
  editError.value = null
}

async function submitEdit(payload: NodeUpdatePayload) {
  if (!editNode.value) return
  editSubmitting.value = true
  editError.value = null
  try {
    await nodesStore.update(editNode.value.id, payload)
    editDialogOpen.value = false
  } catch (error) {
    editError.value = resolveErrorMessage(error)
  } finally {
    editSubmitting.value = false
  }
}

function resolveErrorMessage(error: unknown) {
  if (typeof error === "string") return error
  if (error && typeof error === "object") {
    const anyError = error as { data?: { message?: string }; message?: string }
    return anyError.data?.message || anyError.message || "Failed to update node."
  }
  return "Failed to update node."
}

async function refreshNodes() {
  await nodesStore.refresh()
}
</script>

<style lang="scss" scoped>

</style>
