<script setup lang="ts">
import { mockNodes, useNodesStore } from "~/store/nodes"

callOnce(() => {
  useNodesStore().upsertMany(mockNodes)
})

onMounted(() => {
  document.body.classList.add('dark')
})
</script>
<template>
  <div class="dark">
    <NuxtRouteAnnouncer />
    <NuxtLayout>
      <NuxtPage />
    </NuxtLayout>
  </div>
</template>
