<template>
  <Badge :class="statusStyles[variant]">
    <slot></slot>
  </Badge>
</template>

<script setup lang="ts">
import {Badge} from "~/components/ui/badge";

const { variant = 'ONLINE' } = defineProps<{ variant?: keyof typeof statusStyles }>()
</script>

<script lang="ts">
export const statusStyles = {
  'ONLINE': 'bg-green-500/10 text-green-500',
  'OFFLINE': 'bg-red-500/10 text-red-500',
  'UNKNOWN': 'bg-gray-500/10 text-gray-500'
}
</script>