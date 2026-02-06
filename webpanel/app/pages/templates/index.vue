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
        <template #number>{{ versionedTemplates }}</template>
        <template #label>Versioned templates</template>
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
          <Dialog v-model:open="importDialogOpen">
            <DialogTrigger>
              <Button variant="secondary" :disabled="loading || templates.length === 0">
                Import Template
              </Button>
            </DialogTrigger>
            <DialogContent class="sm:max-w-2xl">
              <DialogHeader>
                <DialogTitle>Import template version</DialogTitle>
                <DialogDescription>
                  Register a new template version from an uploaded artifact.
                </DialogDescription>
              </DialogHeader>
              <form class="space-y-6" @submit.prevent="submitImport">
                <FieldGroup class="grid gap-5 sm:grid-cols-2">
                  <Field class="sm:col-span-2">
                    <FieldLabel for="import-template-id">Template</FieldLabel>
                    <select
                      id="import-template-id"
                      v-model="importForm.templateId"
                      :class="inputClasses"
                      :disabled="importSubmitting"
                    >
                      <option value="" disabled>Select a template</option>
                      <option v-for="template in templates" :key="template.id" :value="template.id">
                        {{ template.name }}
                      </option>
                    </select>
                    <FieldError v-if="importSubmitted" :errors="importErrors.templateId" />
                  </Field>
                  <Field>
                    <FieldLabel for="import-version">Version</FieldLabel>
                    <Input
                      id="import-version"
                      v-model="importForm.version"
                      autocomplete="off"
                      placeholder="v1.0.0"
                      :disabled="importSubmitting"
                      :aria-invalid="importSubmitted && !!importErrors.version?.length"
                    />
                    <FieldError v-if="importSubmitted" :errors="importErrors.version" />
                  </Field>
                  <Field>
                    <FieldLabel for="import-checksum">Checksum</FieldLabel>
                    <Input
                      id="import-checksum"
                      v-model="importForm.checksum"
                      autocomplete="off"
                      placeholder="sha256:..."
                      :disabled="importSubmitting"
                      :aria-invalid="importSubmitted && !!importErrors.checksum?.length"
                    />
                    <FieldError v-if="importSubmitted" :errors="importErrors.checksum" />
                  </Field>
                  <Field class="sm:col-span-2">
                    <FieldLabel for="import-s3">S3 key</FieldLabel>
                    <Input
                      id="import-s3"
                      v-model="importForm.s3Key"
                      autocomplete="off"
                      placeholder="templates/hytale/survival-v1.0.0.tar.gz"
                      :disabled="importSubmitting"
                      :aria-invalid="importSubmitted && !!importErrors.s3Key?.length"
                    />
                    <FieldError v-if="importSubmitted" :errors="importErrors.s3Key" />
                  </Field>
                  <Field class="sm:col-span-2">
                    <FieldLabel for="import-metadata">Metadata JSON (optional)</FieldLabel>
                    <textarea
                      id="import-metadata"
                      v-model="importForm.metadataJson"
                      rows="4"
                      :class="inputClasses"
                      :disabled="importSubmitting"
                      :aria-invalid="importSubmitted && !!importErrors.metadataJson?.length"
                    ></textarea>
                    <FieldDescription>Raw JSON string stored with the version.</FieldDescription>
                    <FieldError v-if="importSubmitted" :errors="importErrors.metadataJson" />
                  </Field>
                </FieldGroup>
                <div v-if="importError" class="rounded-lg border border-destructive/40 bg-destructive/10 p-3 text-sm text-destructive">
                  {{ importError }}
                </div>
                <DialogFooter class="gap-2">
                  <DialogClose as-child>
                    <Button type="button" variant="outline" :disabled="importSubmitting">
                      Cancel
                    </Button>
                  </DialogClose>
                  <Button type="submit" :disabled="importSubmitting">
                    {{ importSubmitting ? "Importing..." : "Import version" }}
                  </Button>
                </DialogFooter>
              </form>
            </DialogContent>
          </Dialog>
          <Dialog v-model:open="createDialogOpen">
            <DialogTrigger>
              <Button>
                <Plus class="size-4" />
                New Template
              </Button>
            </DialogTrigger>
            <DialogContent class="sm:max-w-2xl">
              <DialogHeader>
                <DialogTitle>Create template</DialogTitle>
                <DialogDescription>
                  Define a new template before publishing versions.
                </DialogDescription>
              </DialogHeader>
              <form class="space-y-6" @submit.prevent="submitCreate">
                <FieldGroup class="grid gap-5 sm:grid-cols-2">
                  <Field class="sm:col-span-2">
                    <FieldLabel for="create-name">Name</FieldLabel>
                    <Input
                      id="create-name"
                      v-model="createForm.name"
                      autocomplete="off"
                      placeholder="Hytale Survival Core"
                      :disabled="createSubmitting"
                      :aria-invalid="createSubmitted && !!createErrors.name?.length"
                    />
                    <FieldError v-if="createSubmitted" :errors="createErrors.name" />
                  </Field>
                  <Field class="sm:col-span-2">
                    <FieldLabel for="create-description">Description</FieldLabel>
                    <textarea
                      id="create-description"
                      v-model="createForm.description"
                      rows="3"
                      :class="inputClasses"
                      :disabled="createSubmitting"
                      :aria-invalid="createSubmitted && !!createErrors.description?.length"
                    ></textarea>
                    <FieldError v-if="createSubmitted" :errors="createErrors.description" />
                  </Field>
                  <Field>
                    <FieldLabel for="create-type">Type</FieldLabel>
                    <select
                      id="create-type"
                      v-model="createForm.type"
                      :class="inputClasses"
                      :disabled="createSubmitting"
                    >
                      <option value="CUSTOM">CUSTOM</option>
                    </select>
                  </Field>
                </FieldGroup>
                <div v-if="createError" class="rounded-lg border border-destructive/40 bg-destructive/10 p-3 text-sm text-destructive">
                  {{ createError }}
                </div>
                <DialogFooter class="gap-2">
                  <DialogClose as-child>
                    <Button type="button" variant="outline" :disabled="createSubmitting">
                      Cancel
                    </Button>
                  </DialogClose>
                  <Button type="submit" :disabled="createSubmitting">
                    {{ createSubmitting ? "Creating..." : "Create template" }}
                  </Button>
                </DialogFooter>
              </form>
            </DialogContent>
          </Dialog>
        </div>
      </div>

      <div class="grid gap-4 lg:grid-cols-[2fr_1fr]">
        <Card class="border bg-muted/30">
          <CardHeader class="flex flex-row items-start justify-between gap-4">
            <div class="space-y-1">
              <CardTitle>Template library</CardTitle>
              <CardDescription>
                Most recently updated templates with usage and owners.
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
                  </TableRow>
                </TableHeader>
                <TableBody>
                <template v-if="loading">
                  <TableRow v-for="row in 5" :key="row">
                    <TableCell v-for="cell in 6" :key="cell">
                      <Skeleton class="h-4 w-full" />
                    </TableCell>
                  </TableRow>
                </template>
                <template v-else-if="templateRows.length">
                  <TableRow v-for="template in templateRows" :key="template.id">
                    <TableCell class="w-[320px]">
                      <div class="space-y-2">
                        <div class="font-medium">{{ template.name }}</div>
                        <div class="text-xs text-muted-foreground">
                          {{ template.description }}
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
                          Created by
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
                  </TableRow>
                </template>
                <template v-else>
                  <TableRow>
                    <TableCell :colspan="6" class="h-24 text-center text-muted-foreground">
                      No templates found.
                    </TableCell>
                  </TableRow>
                </template>
                </TableBody>
              </Table>
            </div>
          <div v-if="loadError" class="mt-4 rounded-lg border border-destructive/40 bg-destructive/10 p-3 text-sm text-destructive">
            {{ loadError }}
            <Button variant="secondary" size="sm" class="ml-3" @click="loadTemplates">
              Retry
            </Button>
          </div>
          <div v-else-if="loadWarning" class="mt-4 rounded-lg border border-amber-500/40 bg-amber-500/10 p-3 text-sm text-amber-200">
            {{ loadWarning }}
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
              <div v-if="!recentActivity.length && !loading" class="rounded-lg border border-dashed p-3 text-sm text-muted-foreground">
                No template activity yet.
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
              <div v-if="!adoptionLeaders.length && !loading" class="rounded-lg border border-dashed p-3 text-sm text-muted-foreground">
                No template usage detected yet.
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

type TemplateDto = {
  id: string
  name: string
  description: string
  type: string
  createdAt: string
  createdBy: string
}

type TemplateVersionDto = {
  id: string
  templateId: string
  version: string
  checksum: string
  s3Key: string
  metadataJson?: string | null
  createdAt: string
}

type InstanceTemplateLayerDto = {
  id: string
  templateVersionId: string
  orderIndex: number
}

type InstanceSummaryDto = {
  id: string
  templateLayers: InstanceTemplateLayerDto[]
}

type TemplateRow = {
  id: string
  name: string
  description: string
  type: string
  versions: number
  instances: number
  latestVersion: string
  owner: string
  updatedAt: string
  updatedAtLabel: string
  createdAtLabel: string
}

type ActivityItem = {
  id: string
  template: string
  summary: string
  actor: string
  when: string
  whenRaw: string
}

const brainApi = useBrainApi()

const templates = ref<TemplateDto[]>([])
const templateVersions = ref<Record<string, TemplateVersionDto[]>>({})
const instances = ref<InstanceSummaryDto[]>([])
const loading = ref(true)
const loadError = ref<string | null>(null)
const loadWarning = ref<string | null>(null)
const createDialogOpen = ref(false)
const importDialogOpen = ref(false)
const createSubmitting = ref(false)
const importSubmitting = ref(false)
const createSubmitted = ref(false)
const importSubmitted = ref(false)
const createError = ref<string | null>(null)
const importError = ref<string | null>(null)

const createForm = reactive({
  name: "",
  description: "",
  type: "CUSTOM",
})

const importForm = reactive({
  templateId: "",
  version: "",
  checksum: "",
  s3Key: "",
  metadataJson: "",
})

const inputClasses =
  "file:text-foreground placeholder:text-muted-foreground selection:bg-primary selection:text-primary-foreground dark:bg-input/30 border-input w-full min-w-0 rounded-md border bg-transparent px-3 py-2 text-base shadow-xs transition-[color,box-shadow] outline-none file:inline-flex file:h-7 file:border-0 file:bg-transparent file:text-sm file:font-medium disabled:pointer-events-none disabled:cursor-not-allowed disabled:opacity-50 md:text-sm focus-visible:border-ring focus-visible:ring-ring/50 focus-visible:ring-[3px] aria-invalid:ring-destructive/20 dark:aria-invalid:ring-destructive/40 aria-invalid:border-destructive"

const isValidUuid = (value: string) =>
  /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(value)

const createErrors = computed(() => {
  const next: Record<string, string[]> = {}
  if (!createForm.name.trim()) next.name = ["Name is required."]
  if (!createForm.description.trim()) next.description = ["Description is required."]
  return next
})

const importErrors = computed(() => {
  const next: Record<string, string[]> = {}
  if (!importForm.templateId.trim()) next.templateId = ["Template is required."]
  if (!importForm.version.trim()) next.version = ["Version is required."]
  if (!importForm.checksum.trim()) next.checksum = ["Checksum is required."]
  if (!importForm.s3Key.trim()) next.s3Key = ["S3 key is required."]
  if (importForm.metadataJson.trim()) {
    try {
      JSON.parse(importForm.metadataJson)
    } catch {
      next.metadataJson = ["Metadata must be valid JSON."]
    }
  }
  return next
})

const dateFormatter = new Intl.DateTimeFormat("en-US", {
  month: "short",
  day: "numeric",
  year: "numeric",
})

const formatDate = (value: string) => {
  const parsed = Date.parse(value)
  if (Number.isNaN(parsed)) return "—"
  return dateFormatter.format(parsed)
}

const formatOwner = (value: string) => {
  if (!value) return "—"
  if (value.length <= 12) return value
  return value
}

const extractErrorMessage = (error: unknown, fallback: string) => {
  if (!error || typeof error !== "object") return fallback
  const record = error as { data?: { message?: string }; message?: string }
  return record.data?.message || record.message || fallback
}

const loadTemplates = async () => {
  loading.value = true
  loadError.value = null
  loadWarning.value = null
  try {
    const templateList = await brainApi<TemplateDto[]>("/api/templates")
    templates.value = templateList

    const instancesResult = await brainApi<InstanceSummaryDto[]>("/api/instances").catch(() => {
      loadWarning.value = "Instance usage data is unavailable."
      return []
    })
    instances.value = instancesResult

    if (templateList.length === 0) {
      templateVersions.value = {}
      return
    }

    const versionResults = await Promise.allSettled(
      templateList.map((template) =>
        brainApi<TemplateVersionDto[]>(`/api/templates/${template.id}/versions`),
      ),
    )

    const nextVersions: Record<string, TemplateVersionDto[]> = {}
    let versionsFailed = false
    versionResults.forEach((result, index) => {
      const templateId = templateList[index].id
      if (result.status === "fulfilled") {
        nextVersions[templateId] = result.value
      } else {
        nextVersions[templateId] = []
        versionsFailed = true
      }
    })
    templateVersions.value = nextVersions
    if (versionsFailed) {
      loadWarning.value = "Some template versions could not be loaded."
    }
  } catch (error) {
    loadError.value = "Unable to load templates. Check your session and Brain API connectivity."
    templates.value = []
    templateVersions.value = {}
    instances.value = []
  } finally {
    loading.value = false
  }
}

await loadTemplates()

watch(
  () => templates.value,
  (next) => {
    if (!importForm.templateId && next.length > 0) {
      importForm.templateId = next[0].id
    }
  },
  { immediate: true },
)

watch(createDialogOpen, (open) => {
  if (!open) {
    resetCreateForm()
  }
})

watch(importDialogOpen, (open) => {
  if (!open) {
    resetImportForm()
  }
})

const versionIdToTemplate = computed(() => {
  const mapping = new Map<string, string>()
  for (const [templateId, versions] of Object.entries(templateVersions.value)) {
    for (const version of versions) {
      mapping.set(version.id, templateId)
    }
  }
  return mapping
})

const usageCounts = computed(() => {
  const counts = new Map<string, number>()
  for (const instance of instances.value) {
    const templateIds = new Set<string>()
    for (const layer of instance.templateLayers ?? []) {
      const templateId = versionIdToTemplate.value.get(layer.templateVersionId)
      if (templateId) {
        templateIds.add(templateId)
      }
    }
    for (const templateId of templateIds) {
      counts.set(templateId, (counts.get(templateId) ?? 0) + 1)
    }
  }
  return counts
})

const templateRows = computed<TemplateRow[]>(() => {
  return templates.value
    .map((template) => {
      const versions = templateVersions.value[template.id] ?? []
      const latestVersion = versions[0]
      const updatedAt = latestVersion?.createdAt ?? template.createdAt
      return {
        id: template.id,
        name: template.name,
        description: template.description,
        type: template.type,
        versions: versions.length,
        instances: usageCounts.value.get(template.id) ?? 0,
        latestVersion: latestVersion?.version ?? "—",
        owner: formatOwner(template.createdBy),
        updatedAt,
        updatedAtLabel: formatDate(updatedAt),
        createdAtLabel: formatDate(template.createdAt),
      }
    })
    .sort((first, second) => Date.parse(second.updatedAt) - Date.parse(first.updatedAt))
})

const totalTemplates = computed(() => templateRows.value.length)
const versionedTemplates = computed(
  () => templateRows.value.filter((template) => template.versions > 0).length,
)
const templatesInUse = computed(
  () => templateRows.value.filter((template) => template.instances > 0).length,
)
const totalVersions = computed(() =>
  templateRows.value.reduce((total, template) => total + template.versions, 0),
)

const maxInstances = computed(() => {
  const values = templateRows.value.map((template) => template.instances)
  return Math.max(1, ...values)
})

const adoptionLeaders = computed(() => {
  return [...templateRows.value]
    .sort((first, second) => second.instances - first.instances)
    .filter((template) => template.instances > 0)
    .slice(0, 4)
})

const recentActivity = computed<ActivityItem[]>(() => {
  const items: ActivityItem[] = []
  for (const template of templates.value) {
    const versions = templateVersions.value[template.id] ?? []
    for (const version of versions) {
      items.push({
        id: version.id,
        template: template.name,
        summary: `Published version ${version.version}.`,
        actor: formatOwner(template.createdBy),
        when: formatDate(version.createdAt),
        whenRaw: version.createdAt,
      })
    }
  }
  return items
    .sort((first, second) => Date.parse(second.whenRaw) - Date.parse(first.whenRaw))
    .slice(0, 3)
})

const usagePercentFor = (instances: number) => {
  return Math.round((instances / maxInstances.value) * 100)
}

function resetCreateForm() {
  createForm.name = ""
  createForm.description = ""
  createForm.type = "CUSTOM"
  createSubmitted.value = false
  createError.value = null
}

function resetImportForm() {
  importForm.version = ""
  importForm.checksum = ""
  importForm.s3Key = ""
  importForm.metadataJson = ""
  importSubmitted.value = false
  importError.value = null
}

const submitCreate = async () => {
  createSubmitted.value = true
  if (Object.keys(createErrors.value).length > 0) return
  createSubmitting.value = true
  createError.value = null
  try {
    await brainApi<TemplateDto>("/api/templates", {
      method: "POST",
      body: {
        name: createForm.name.trim(),
        description: createForm.description.trim(),
        type: createForm.type
      },
    })
    resetCreateForm()
    createDialogOpen.value = false
    await loadTemplates()
  } catch (error) {
    createError.value = extractErrorMessage(error, "Unable to create template.")
  } finally {
    createSubmitting.value = false
  }
}

const submitImport = async () => {
  importSubmitted.value = true
  if (Object.keys(importErrors.value).length > 0) return
  importSubmitting.value = true
  importError.value = null
  const metadata = importForm.metadataJson.trim()
  try {
    await brainApi<TemplateVersionDto>(`/api/templates/${importForm.templateId}/versions`, {
      method: "POST",
      body: {
        version: importForm.version.trim(),
        checksum: importForm.checksum.trim(),
        s3Key: importForm.s3Key.trim(),
        ...(metadata ? { metadataJson: metadata } : {}),
      },
    })
    resetImportForm()
    importDialogOpen.value = false
    await loadTemplates()
  } catch (error) {
    importError.value = extractErrorMessage(error, "Unable to import template version.")
  } finally {
    importSubmitting.value = false
  }
}
</script>

<style scoped></style>
