<script setup lang="ts">
import { computed } from "vue"
import { Copy, MoreHorizontal, Play, Square, Trash2 } from "lucide-vue-next"
import type { InstanceRow } from "~/components/app/instances/columns"
import { Button } from "@/components/ui/button"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"

const props = defineProps<{
  instance: InstanceRow
  busy?: boolean
  onStart?: () => void
  onStop?: () => void
  onDestroy?: () => void
  onCopy?: () => void
}>()

const hasNode = computed(() => !!props.instance.nodeId)
const canStart = computed(() =>
  hasNode.value && (props.instance.state === "REQUESTED" || props.instance.state === "STOPPED")
)
const canStop = computed(() => hasNode.value && props.instance.state === "RUNNING")
const canDestroy = computed(() =>
  hasNode.value && (props.instance.state === "STOPPED" || props.instance.state === "STOPPING")
)
</script>

<template>
  <DropdownMenu>
    <DropdownMenuTrigger as-child>
      <Button variant="ghost" class="h-8 w-8 p-0" :disabled="busy">
        <span class="sr-only">Open actions</span>
        <MoreHorizontal class="h-4 w-4" />
      </Button>
    </DropdownMenuTrigger>
    <DropdownMenuContent align="end">
      <DropdownMenuLabel>Quick actions</DropdownMenuLabel>
      <DropdownMenuItem :disabled="!canStart || busy" @click="onStart?.()">
        <Play class="h-4 w-4" />
        Start
      </DropdownMenuItem>
      <DropdownMenuItem :disabled="!canStop || busy" @click="onStop?.()">
        <Square class="h-4 w-4" />
        Stop
      </DropdownMenuItem>
      <DropdownMenuItem variant="destructive" :disabled="!canDestroy || busy" @click="onDestroy?.()">
        <Trash2 class="h-4 w-4" />
        Destroy
      </DropdownMenuItem>
      <DropdownMenuSeparator />
      <DropdownMenuItem :disabled="busy" @click="onCopy?.()">
        <Copy class="h-4 w-4" />
        Copy to new instance
      </DropdownMenuItem>
    </DropdownMenuContent>
  </DropdownMenu>
</template>
