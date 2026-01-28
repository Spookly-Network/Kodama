<template>
    <div class="flex flex-1 flex-col gap-10 p-4 pt-0">
      <div class="grid auto-rows-min gap-4 md:grid-cols-4">

        <AppStatsCard v-for="i in 4" :key="i" variant="blue"/>
      </div>
      <section class="text-foreground space-y-4">
        <div>
          <h2 class="text-2xl font-semibold">Penis</h2>
          <div class="text-muted-foreground">Penis 123</div>
        </div>
        <div class="bg-muted/50 min-h-screen flex-1 rounded-xl md:min-h-min">
          <AppInstancesTable :data="instances" :columns="columns"/>
        </div>
      </section>

    </div>
</template>

<script lang="ts" setup>
import type {Instance} from "~~/types/Instance";
import {columns} from "~/components/app/instances/columns";

const instances: Ref<Instance[]> = ref([])
</script>

<style lang="scss" scoped>

</style>