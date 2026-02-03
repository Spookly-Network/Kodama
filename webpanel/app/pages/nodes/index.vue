<template>
    <div class="flex flex-1 flex-col gap-10 p-4 pt-0">
      <div class="grid auto-rows-min gap-4 md:grid-cols-4">
        <AppStatsCard variant="green">
          <template #icon><Earth/></template>
          <template #number>{{ amountNodesOnline }}</template>
          <template #label>Online nodes</template>
        </AppStatsCard>

        <AppStatsCard variant="blue">
          <template #icon><CircleDashed/></template>
          <template #number>{{ amountSlotsOpen }}</template>
          <template #label>Open slots</template>
        </AppStatsCard>

        <AppStatsCard variant="orange">
          <template #icon><CircleDot/></template>
          <template #number>{{ amountSlotsOpen }}</template>
          <template #label>Used slots</template>
        </AppStatsCard>
        <AppStatsCard v-for="i in 1" :key="i" variant="blue"/>
      </div>
      <section class="text-foreground space-y-4">
        <div class="flex justify-between">
          <div>
            <h2 class="text-2xl font-semibold">Nodes</h2>
            <div class="text-muted-foreground">Penis 123</div>
          </div>
          <div>
            <AppNodesCreateDialog />
          </div>

        </div>
        <div class="bg-muted/50 min-h-screen flex-1 rounded-xl md:min-h-min border">
          <AppNodesTable :data="rows" :columns="columns"/>
        </div>
      </section>

    </div>
</template>

<script lang="ts" setup>
import {columns} from "~/components/app/nodes/columns";
import {useNodesStore} from "~/store/nodes";
import {Earth, CircleDashed, CircleDot, Plus} from "lucide-vue-next";
import AppNodesCreateDialog from "~/components/app/nodes/AppNodesCreateDialog.vue";

const nodesStore = useNodesStore()
// TODO enable when prod
await nodesStore.ensureFresh() // nodes likely already loaded at login

const amountNodesOnline = computed(() => {
  return Object.values(nodesStore.byId).filter(node => node.status === NodeStatus.ONLINE).length
})

const amountSlotsOpen = computed(() => {
  return 12
})

const rows = computed(() => {
  return Object.values(nodesStore.byId)
})
</script>

<style lang="scss" scoped>

</style>