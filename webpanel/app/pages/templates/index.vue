<template>
  <div class="flex flex-1 flex-col gap-10 p-4 pt-0">
    <div class="grid auto-rows-min gap-4 md:grid-cols-4">
      <AppStatsCard variant="blue">
        <template #icon><Package /></template>
        <template #number>{{ totalTemplates }}</template>
        <template #label>Total templates</template>
      </AppStatsCard>
      <AppStatsCard variant="green">
        <template #icon><Sparkles /></template>
        <template #number>{{ activeTemplates }}</template>
        <template #label>Active templates</template>
      </AppStatsCard>
      <AppStatsCard variant="amber">
        <template #icon><SquareStack /></template>
        <template #number>{{ templatesInUse }}</template>
        <template #label>Templates in use</template>
      </AppStatsCard>
      <AppStatsCard variant="violet">
        <template #icon><Layers /></template>
        <template #number>{{ totalVersions }}</template>
        <template #label>Published versions</template>
      </AppStatsCard>
    </div>

    <section class="text-foreground space-y-4">
      <div class="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
        <div>
          <h2 class="text-2xl font-semibold">Templates</h2>
          <div class="text-muted-foreground">
            Overview of created templates, versions, and current adoption.
          </div>
        </div>
        <div class="flex flex-wrap gap-2">
          <Button variant="secondary">Import Template</Button>
          <Button>
            <Plus class="size-4" />
            New Template
          </Button>
        </div>
      </div>

      <div class="grid gap-4 lg:grid-cols-[2fr_1fr]">
        <Card class="border bg-muted/30">
          <CardHeader class="flex flex-row items-start justify-between gap-4">
            <div class="space-y-1">
              <CardTitle>Template library</CardTitle>
              <CardDescription>
                Most recently updated templates with status, usage, and owners.
              </CardDescription>
            </div>
            <Badge variant="secondary">{{ totalTemplates }} total</Badge>
          </CardHeader>
          <CardContent>
            <div class="overflow-hidden rounded-lg border">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Template</TableHead>
                    <TableHead>Type</TableHead>
                    <TableHead>Versions</TableHead>
                    <TableHead>Instances</TableHead>
                    <TableHead>Owner</TableHead>
                    <TableHead>Updated</TableHead>
                    <TableHead class="text-right">Status</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  <TableRow v-for="template in templates" :key="template.id">
                    <TableCell class="w-[320px]">
                      <div class="space-y-2">
                        <div class="font-medium">{{ template.name }}</div>
                        <div class="text-xs text-muted-foreground">
                          {{ template.description }}
                        </div>
                        <div class="flex flex-wrap gap-1">
                          <Badge
                            v-for="tag in template.tags"
                            :key="tag"
                            variant="secondary"
                            class="text-xs"
                          >
                            {{ tag }}
                          </Badge>
                        </div>
                      </div>
                    </TableCell>
                    <TableCell>
                      <Badge variant="outline">{{ template.type }}</Badge>
                    </TableCell>
                    <TableCell>
                      <div class="space-y-1">
                        <div class="font-medium">{{ template.versions }}</div>
                        <div class="text-xs text-muted-foreground">
                          Latest {{ template.latestVersion }}
                        </div>
                      </div>
                    </TableCell>
                    <TableCell>
                      <div class="space-y-2">
                        <div class="font-medium">{{ template.instances }}</div>
                        <div class="h-2 rounded-full bg-muted">
                          <div
                            class="h-2 rounded-full bg-emerald-500"
                            :style="{ width: `${usagePercentFor(template.instances)}%` }"
                          ></div>
                        </div>
                      </div>
                    </TableCell>
                    <TableCell>
                      <div class="space-y-1">
                        <div class="font-medium">{{ template.owner }}</div>
                        <div class="text-xs text-muted-foreground">
                          {{ template.runtime }}
                        </div>
                      </div>
                    </TableCell>
                    <TableCell>
                      <div class="space-y-1">
                        <div class="font-medium">{{ template.updatedAtLabel }}</div>
                        <div class="text-xs text-muted-foreground">
                          Created {{ template.createdAtLabel }}
                        </div>
                      </div>
                    </TableCell>
                    <TableCell class="text-right">
                      <Badge variant="outline" :class="statusStyles[template.status]">
                        {{ template.status }}
                      </Badge>
                    </TableCell>
                  </TableRow>
                </TableBody>
              </Table>
            </div>
          </CardContent>
        </Card>

        <div class="grid gap-4">
          <Card class="border bg-muted/30">
            <CardHeader class="space-y-1">
              <CardTitle>Recent activity</CardTitle>
              <CardDescription>Latest template changes and releases.</CardDescription>
            </CardHeader>
            <CardContent class="space-y-4">
              <div
                v-for="activity in recentActivity"
                :key="activity.id"
                class="flex items-start justify-between gap-4 rounded-lg border border-dashed p-3"
              >
                <div class="space-y-1">
                  <div class="font-medium">{{ activity.template }}</div>
                  <div class="text-xs text-muted-foreground">
                    {{ activity.summary }}
                  </div>
                </div>
                <div class="text-xs text-muted-foreground text-right">
                  <div class="font-medium text-foreground">
                    {{ activity.actor }}
                  </div>
                  <div>{{ activity.when }}</div>
                </div>
              </div>
            </CardContent>
          </Card>

          <Card class="border bg-muted/30">
            <CardHeader class="space-y-1">
              <CardTitle>Adoption snapshot</CardTitle>
              <CardDescription>Templates with the most instance traffic.</CardDescription>
            </CardHeader>
            <CardContent class="space-y-4">
              <div v-for="template in adoptionLeaders" :key="template.id" class="space-y-2">
                <div class="flex items-center justify-between text-sm">
                  <span class="font-medium">{{ template.name }}</span>
                  <span class="text-muted-foreground">
                    {{ template.instances }} instances
                  </span>
                </div>
                <div class="h-2 rounded-full bg-muted">
                  <div
                    class="h-2 rounded-full bg-sky-500"
                    :style="{ width: `${usagePercentFor(template.instances)}%` }"
                  ></div>
                </div>
              </div>
              <div class="rounded-lg border border-dashed p-3 text-sm text-muted-foreground">
                {{ draftsCount }} drafts and {{ archivedCount }} archived templates are waiting for updates.
              </div>
            </CardContent>
          </Card>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { Layers, Package, Plus, Sparkles, SquareStack } from "lucide-vue-next"

type TemplateStatus = "ACTIVE" | "DRAFT" | "ARCHIVED"

type TemplateRow = {
  id: string
  name: string
  description: string
  type: string
  status: TemplateStatus
  versions: number
  instances: number
  latestVersion: string
  owner: string
  runtime: string
  updatedAtLabel: string
  createdAtLabel: string
  tags: string[]
}

type ActivityItem = {
  id: string
  template: string
  summary: string
  actor: string
  when: string
}

const statusStyles: Record<TemplateStatus, string> = {
  ACTIVE: "bg-emerald-500/10 text-emerald-500",
  DRAFT: "bg-amber-500/10 text-amber-500",
  ARCHIVED: "bg-slate-500/10 text-slate-400",
}

const templates = ref<TemplateRow[]>([
  {
    id: "tmpl_01J9A9FQY2V0A1B2C3D4E5F6G7",
    name: "Hytale Survival Core",
    description: "Base survival server with economy and starter kit.",
    type: "CUSTOM",
    status: "ACTIVE",
    versions: 5,
    instances: 12,
    latestVersion: "v1.4.2",
    owner: "admin",
    runtime: "hytale-1.4",
    updatedAtLabel: "Feb 3, 2026",
    createdAtLabel: "Jan 5, 2026",
    tags: ["survival", "public", "economy"],
  },
  {
    id: "tmpl_01J9A9FQY2V0A1B2C3D4E5F6G8",
    name: "Lobby Hub",
    description: "Entry point with matchmaking and cosmetic store.",
    type: "CUSTOM",
    status: "ACTIVE",
    versions: 8,
    instances: 6,
    latestVersion: "v2.0.1",
    owner: "admin",
    runtime: "hytale-1.4",
    updatedAtLabel: "Feb 2, 2026",
    createdAtLabel: "Dec 20, 2025",
    tags: ["hub", "critical", "entry"],
  },
  {
    id: "tmpl_01J9A9FQY2V0A1B2C3D4E5F6G9",
    name: "Minigames Rotation",
    description: "Fast matchmaking and rotating maps for weekly events.",
    type: "CUSTOM",
    status: "ACTIVE",
    versions: 4,
    instances: 9,
    latestVersion: "v1.3.0",
    owner: "operator",
    runtime: "hytale-1.3",
    updatedAtLabel: "Jan 31, 2026",
    createdAtLabel: "Jan 10, 2026",
    tags: ["minigames", "events"],
  },
  {
    id: "tmpl_01J9A9FQY2V0A1B2C3D4E5F6H0",
    name: "Creative Plots",
    description: "Dedicated build worlds with permissions and plot tools.",
    type: "CUSTOM",
    status: "DRAFT",
    versions: 2,
    instances: 0,
    latestVersion: "v0.9.5",
    owner: "viewer",
    runtime: "hytale-1.2",
    updatedAtLabel: "Jan 26, 2026",
    createdAtLabel: "Jan 12, 2026",
    tags: ["creative", "private"],
  },
  {
    id: "tmpl_01J9A9FQY2V0A1B2C3D4E5F6H1",
    name: "Seasonal Events",
    description: "Limited time event template with seasonal progression.",
    type: "CUSTOM",
    status: "ACTIVE",
    versions: 3,
    instances: 4,
    latestVersion: "v1.1.0",
    owner: "operator",
    runtime: "hytale-1.4",
    updatedAtLabel: "Jan 25, 2026",
    createdAtLabel: "Nov 30, 2025",
    tags: ["events", "seasonal"],
  },
  {
    id: "tmpl_01J9A9FQY2V0A1B2C3D4E5F6H2",
    name: "Legacy Modpack",
    description: "Deprecated template awaiting migration plan.",
    type: "CUSTOM",
    status: "ARCHIVED",
    versions: 7,
    instances: 0,
    latestVersion: "v0.8.4",
    owner: "admin",
    runtime: "hytale-1.1",
    updatedAtLabel: "Jan 14, 2026",
    createdAtLabel: "Oct 2, 2025",
    tags: ["legacy", "migration"],
  },
])

const recentActivity = ref<ActivityItem[]>([
  {
    id: "act_01",
    template: "Hytale Survival Core",
    summary: "Published version v1.4.2 with updated loot tables.",
    actor: "admin",
    when: "2 hours ago",
  },
  {
    id: "act_02",
    template: "Lobby Hub",
    summary: "Updated matchmaking configs and health checks.",
    actor: "admin",
    when: "Yesterday",
  },
  {
    id: "act_03",
    template: "Minigames Rotation",
    summary: "Enabled weekly map rotation for February.",
    actor: "operator",
    when: "3 days ago",
  },
])

const totalTemplates = computed(() => templates.value.length)
const activeTemplates = computed(
  () => templates.value.filter((template) => template.status === "ACTIVE").length,
)
const templatesInUse = computed(
  () => templates.value.filter((template) => template.instances > 0).length,
)
const totalVersions = computed(() =>
  templates.value.reduce((total, template) => total + template.versions, 0),
)
const draftsCount = computed(
  () => templates.value.filter((template) => template.status === "DRAFT").length,
)
const archivedCount = computed(
  () => templates.value.filter((template) => template.status === "ARCHIVED").length,
)
const maxInstances = computed(() => {
  const values = templates.value.map((template) => template.instances)
  return Math.max(1, ...values)
})

const adoptionLeaders = computed(() => {
  return [...templates.value]
    .sort((first, second) => second.instances - first.instances)
    .slice(0, 4)
})

const usagePercentFor = (instances: number) => {
  return Math.round((instances / maxInstances.value) * 100)
}
</script>

<style scoped></style>
