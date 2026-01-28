<template>
  <Card>
    <CardContent class="flex items-center gap-5">
      <div :class="variantStyles[variant]" class="icon p-3 rounded-xl h-fit">
        <span class="size-6">
          <slot name="icon">
            <Bookmark/>
          </slot>
        </span>
      </div>
      <div class="content text-foreground grid gap-1">
        <span class="text-4xl font-bold">
          <slot name="number">
            0
          </slot>
        </span>
        <span class="text-muted-foreground">
          <slot name="label">
            Online instances
          </slot>
        </span>
      </div>
    </CardContent>
  </Card>
</template>

<script lang="ts" setup>
import {Bookmark} from "lucide-vue-next";
const variantStyles: Record<string, string> = {
  primary: 'bg-primary/10 text-primary',
  orange: 'bg-orange-500/10 text-orange-500',
  amber: 'bg-amber-500/10 text-amber-500',
  yellow: 'bg-yellow-500/10 text-yellow-500',
  lime: 'bg-lime-500/10 text-lime-500',
  green: 'bg-green-500/10 text-green-500',
  emerald: 'bg-emerald-500/10 text-emerald-500',
  teal: 'bg-teal-500/10 text-teal-500',
  cyan: 'bg-cyan-500/10 text-cyan-500',
  sky: 'bg-sky-500/10 text-sky-500',
  blue: 'bg-blue-500/10 text-blue-500',
  indigo: 'bg-indigo-500/10 text-indigo-500',
  violet: 'bg-violet-500/10 text-violet-500',
  fuchsia: 'bg-fuchsia-500/10 text-fuchsia-500',
  pink: 'bg-pink-500/10 text-pink-500',
  rose: 'bg-rose-500/10 text-rose-500',
}

const { variant = "primary" } = defineProps<{
  variant?: keyof typeof variantStyles
}>();
</script>

<style lang="scss" scoped>

</style>