<script setup lang="ts">
import { Copy, MoreHorizontal, Pencil } from "lucide-vue-next"
import type { NodeRow } from "~/components/app/nodes/columns"
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
  node: NodeRow
  onEdit?: () => void
}>()

function copy(value: string) {
  navigator.clipboard.writeText(value)
}
</script>

<template>
  <DropdownMenu>
    <DropdownMenuTrigger as-child>
      <Button variant="ghost" class="h-8 w-8 p-0">
        <span class="sr-only">Open actions</span>
        <MoreHorizontal class="h-4 w-4" />
      </Button>
    </DropdownMenuTrigger>
    <DropdownMenuContent align="end">
      <DropdownMenuLabel>Actions</DropdownMenuLabel>
      <DropdownMenuItem :disabled="!onEdit" @click="onEdit?.()">
        <Pencil class="h-4 w-4" />
        Edit node
      </DropdownMenuItem>
      <DropdownMenuSeparator />
      <DropdownMenuItem @click="copy(props.node.id)">
        <Copy class="h-4 w-4" />
        Copy node ID
      </DropdownMenuItem>
      <DropdownMenuItem v-if="props.node.baseUrl" @click="copy(props.node.baseUrl)">
        <Copy class="h-4 w-4" />
        Copy base URL
      </DropdownMenuItem>
    </DropdownMenuContent>
  </DropdownMenu>
</template>
