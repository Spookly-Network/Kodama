<template>
  <template v-if="tooltipText">
    <TooltipProvider>
      <Tooltip>
        <TooltipTrigger>
            <Badge :class="statusStyles[variant]">
              <slot></slot>
            </Badge>
        </TooltipTrigger>
        <TooltipContent>
          <p>{{tooltipText}}</p>
        </TooltipContent>
      </Tooltip>
    </TooltipProvider>
  </template>
  <template v-else>
    <Badge :class="statusStyles[variant]">
      <slot></slot>
    </Badge>
  </template>


</template>

<script setup lang="ts">
import {Badge} from "~/components/ui/badge";

const { variant = 'ONLINE' } = defineProps<{ variant?: keyof typeof statusStyles, tooltipText?: string }>()
</script>

<script lang="ts">
export const statusStyles = {
  'ONLINE': 'bg-green-500/10 text-green-500',
  'OFFLINE': 'bg-red-500/10 text-red-500',
  'UNKNOWN': 'bg-gray-500/10 text-gray-500'
}
</script>