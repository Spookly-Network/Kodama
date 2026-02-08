<template>
  <div class="flex flex-1 flex-col gap-10 p-4 pt-0">
    <div class="grid auto-rows-min gap-4 md:grid-cols-4">
      <AppStatsCard variant="blue">
        <template #icon><Users /></template>
        <template #number>{{ totalGroupsLabel }}</template>
        <template #label>Total groups</template>
      </AppStatsCard>
      <AppStatsCard variant="green">
        <template #icon><Layers /></template>
        <template #number>{{ groupsWithAssignmentsLabel }}</template>
        <template #label>With templates</template>
      </AppStatsCard>
      <AppStatsCard variant="amber">
        <template #icon><Link2 /></template>
        <template #number>{{ totalAssignmentsLabel }}</template>
        <template #label>Assignments</template>
      </AppStatsCard>
      <AppStatsCard variant="violet">
        <template #icon><Clock /></template>
        <template #number>{{ latestUpdatedLabel }}</template>
        <template #label>Latest update</template>
      </AppStatsCard>
    </div>

    <section class="text-foreground space-y-4">
      <div class="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
        <div>
          <h2 class="text-2xl font-semibold">Instance groups</h2>
          <div class="text-muted-foreground">
            Organize instances with shared template assignments.
          </div>
        </div>
        <div class="flex flex-wrap gap-2">
          <Badge variant="secondary">{{ totalAssignmentsLabel }} assignments</Badge>
          <Dialog v-model:open="createDialogOpen">
            <DialogTrigger>
              <Button :disabled="createSubmitting">
                <Plus class="size-4" />
                New group
              </Button>
            </DialogTrigger>
            <DialogContent class="sm:max-w-xl">
              <DialogHeader>
                <DialogTitle>Create instance group</DialogTitle>
                <DialogDescription>
                  Define a new group for shared template layers.
                </DialogDescription>
              </DialogHeader>
              <form class="space-y-6" @submit.prevent="submitCreate">
                <FieldGroup class="grid gap-5">
                  <Field>
                    <FieldLabel for="group-name">Name</FieldLabel>
                    <Input
                      id="group-name"
                      v-model="createForm.name"
                      autocomplete="off"
                      placeholder="Survival EU"
                      :disabled="createSubmitting"
                      :aria-invalid="createSubmitted && !!createErrors.name?.length"
                    />
                    <FieldError v-if="createSubmitted" :errors="createErrors.name" />
                  </Field>
                  <Field>
                    <FieldLabel for="group-description">Description</FieldLabel>
                    <textarea
                      id="group-description"
                      v-model="createForm.description"
                      rows="3"
                      :class="inputClasses"
                      :disabled="createSubmitting"
                    ></textarea>
                    <FieldDescription>Optional context for operators.</FieldDescription>
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
                    {{ createSubmitting ? "Creating..." : "Create group" }}
                  </Button>
                </DialogFooter>
              </form>
            </DialogContent>
          </Dialog>
          <Button variant="secondary" :disabled="loading" @click="loadGroups">
            {{ loading ? "Refreshing..." : "Refresh" }}
          </Button>
        </div>
      </div>

      <div class="grid gap-4 lg:grid-cols-[2fr_1fr]">
        <Card class="border bg-muted/30">
          <CardHeader class="flex flex-row items-start justify-between gap-4">
            <div class="space-y-1">
              <CardTitle>Group catalog</CardTitle>
              <CardDescription>
                Select a group to inspect template assignments.
              </CardDescription>
            </div>
            <Badge variant="secondary">{{ totalGroupsLabel }} total</Badge>
          </CardHeader>
          <CardContent>
            <div class="overflow-hidden rounded-lg border">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Group</TableHead>
                    <TableHead>Assignments</TableHead>
                    <TableHead>Updated</TableHead>
                    <TableHead>Created</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  <template v-if="loading">
                    <TableRow v-for="row in 5" :key="row">
                      <TableCell v-for="cell in 4" :key="cell">
                        <Skeleton class="h-4 w-full" />
                      </TableCell>
                    </TableRow>
                  </template>
                  <template v-else-if="groupRows.length">
                    <TableRow
                      v-for="group in groupRows"
                      :key="group.id"
                      class="cursor-pointer transition-colors"
                      :class="group.id === selectedGroupId ? 'bg-muted/50' : 'hover:bg-muted/30'"
                      @click="selectGroup(group.id)"
                    >
                      <TableCell class="w-[320px]">
                        <div class="space-y-2">
                          <div class="font-medium">{{ group.name }}</div>
                          <div class="text-xs text-muted-foreground">
                            {{ group.description || "No description." }}
                          </div>
                        </div>
                      </TableCell>
                      <TableCell>
                        <div class="space-y-1">
                          <div class="font-medium">{{ assignmentCountFor(group.id) }}</div>
                          <div class="text-xs text-muted-foreground">Template layers</div>
                        </div>
                      </TableCell>
                      <TableCell>
                        <div class="space-y-1">
                          <div class="font-medium">{{ formatDate(group.updatedAt) }}</div>
                          <div class="text-xs text-muted-foreground">Latest change</div>
                        </div>
                      </TableCell>
                      <TableCell>
                        <div class="space-y-1">
                          <div class="font-medium">{{ formatDate(group.createdAt) }}</div>
                          <div class="text-xs text-muted-foreground">Created</div>
                        </div>
                      </TableCell>
                    </TableRow>
                  </template>
                  <template v-else>
                    <TableRow>
                      <TableCell :colspan="4" class="h-24 text-center text-muted-foreground">
                        No groups found.
                      </TableCell>
                    </TableRow>
                  </template>
                </TableBody>
              </Table>
            </div>
            <div v-if="loadError" class="mt-4 rounded-lg border border-destructive/40 bg-destructive/10 p-3 text-sm text-destructive">
              {{ loadError }}
              <Button variant="secondary" size="sm" class="ml-3" @click="loadGroups">
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
              <CardTitle>Group details</CardTitle>
              <CardDescription>Metadata and new template assignments.</CardDescription>
            </CardHeader>
            <CardContent>
              <div v-if="selectedGroup" class="space-y-6">
                <div class="space-y-2">
                  <div class="text-lg font-semibold">{{ selectedGroup.name }}</div>
                  <div class="text-xs text-muted-foreground">{{ selectedGroup.id }}</div>
                  <div class="text-sm text-muted-foreground">
                    {{ selectedGroup.description || "No description provided." }}
                  </div>
                </div>
                <div class="grid gap-2 text-sm">
                  <div class="flex items-center justify-between">
                    <span class="text-muted-foreground">Created</span>
                    <span class="font-medium">{{ formatDate(selectedGroup.createdAt) }}</span>
                  </div>
                  <div class="flex items-center justify-between">
                    <span class="text-muted-foreground">Updated</span>
                    <span class="font-medium">{{ formatDate(selectedGroup.updatedAt) }}</span>
                  </div>
                </div>

                <div class="space-y-4">
                  <div class="text-sm font-medium">Add template assignment</div>
                  <FieldGroup class="grid gap-5">
                    <Field>
                      <FieldLabel for="assignment-template-id">Template ID</FieldLabel>
                      <Input
                        id="assignment-template-id"
                        v-model="assignmentForm.templateId"
                        autocomplete="off"
                        placeholder="Template UUID"
                        :disabled="assignmentSubmitting || !selectedGroup"
                        list="group-template-options"
                        :aria-invalid="assignmentSubmitted && !!assignmentErrors.templateId?.length"
                      />
                      <FieldError v-if="assignmentSubmitted" :errors="assignmentErrors.templateId" />
                    </Field>
                    <Field>
                      <FieldLabel for="assignment-version-id">Template version ID</FieldLabel>
                      <Input
                        id="assignment-version-id"
                        v-model="assignmentForm.templateVersionId"
                        autocomplete="off"
                        placeholder="Optional version UUID"
                        :disabled="assignmentSubmitting || !selectedGroup"
                      />
                      <FieldDescription>Leave blank to use latest version.</FieldDescription>
                    </Field>
                    <Field>
                      <FieldLabel for="assignment-priority">Priority</FieldLabel>
                      <Input
                        id="assignment-priority"
                        v-model="assignmentForm.priority"
                        type="number"
                        min="0"
                        step="1"
                        placeholder="0"
                        :disabled="assignmentSubmitting || !selectedGroup"
                        :aria-invalid="assignmentSubmitted && !!assignmentErrors.priority?.length"
                      />
                      <FieldDescription>Defaults to 0 when omitted.</FieldDescription>
                      <FieldError v-if="assignmentSubmitted" :errors="assignmentErrors.priority" />
                    </Field>
                  </FieldGroup>
                  <div v-if="templatesError" class="text-xs text-muted-foreground">
                    {{ templatesError }}
                  </div>
                  <div v-if="assignmentError" class="rounded-lg border border-destructive/40 bg-destructive/10 p-3 text-sm text-destructive">
                    {{ assignmentError }}
                  </div>
                  <Button type="button" :disabled="assignmentSubmitting || !selectedGroup" @click="submitAssignment">
                    {{ assignmentSubmitting ? "Saving..." : "Add assignment" }}
                  </Button>
                </div>
              </div>
              <div v-else class="rounded-lg border border-dashed p-4 text-sm text-muted-foreground">
                Select a group to view its details and assignments.
              </div>
            </CardContent>
          </Card>

          <Card class="border bg-muted/30">
            <CardHeader class="space-y-1">
              <CardTitle>Template assignments</CardTitle>
              <CardDescription>Assignments ordered by priority.</CardDescription>
            </CardHeader>
            <CardContent class="space-y-4">
              <div v-if="!selectedGroup" class="rounded-lg border border-dashed p-4 text-sm text-muted-foreground">
                Select a group to see assignments.
              </div>
              <template v-else>
                <div v-if="assignmentsLoading" class="space-y-2">
                  <Skeleton v-for="row in 3" :key="row" class="h-10 w-full" />
                </div>
                <div v-else-if="assignmentRows.length" class="space-y-3">
                  <div
                    v-for="assignment in assignmentRows"
                    :key="assignment.id"
                    class="flex items-start justify-between gap-4 rounded-lg border border-dashed p-3"
                  >
                    <div class="space-y-1">
                      <div class="font-medium">{{ assignment.templateLabel }}</div>
                      <div class="text-xs text-muted-foreground">
                        Template ID {{ assignment.templateId }}
                      </div>
                      <div class="text-xs text-muted-foreground">
                        Version {{ assignment.templateVersionId || "Latest" }} · Priority {{ assignment.priority }}
                      </div>
                    </div>
                    <Button
                      variant="ghost"
                      size="sm"
                      :disabled="isDeletingAssignment(assignment.id)"
                      @click="removeAssignment(assignment.id)"
                    >
                      {{ isDeletingAssignment(assignment.id) ? "Removing..." : "Remove" }}
                    </Button>
                  </div>
                </div>
                <div v-else class="rounded-lg border border-dashed p-3 text-sm text-muted-foreground">
                  No template assignments yet.
                </div>
              </template>
            </CardContent>
          </Card>
        </div>
      </div>
    </section>

    <datalist id="group-template-options">
      <option
        v-for="template in templates"
        :key="template.id"
        :value="template.id"
        :label="template.name"
      />
    </datalist>
  </div>
</template>

<script setup lang="ts">
import { Clock, Layers, Link2, Plus, Users } from "lucide-vue-next"

type InstanceGroupDto = {
  id: string
  name: string
  description?: string | null
  createdAt: string
  updatedAt: string
}

type TemplateAssignmentDto = {
  id: string
  templateId: string
  templateVersionId?: string | null
  priority: number
}

type TemplateSummary = {
  id: string
  name: string
}

type AssignmentRow = {
  id: string
  templateId: string
  templateLabel: string
  templateVersionId: string | null
  priority: number
}

const brainApi = useBrainApi()

const groups = ref<InstanceGroupDto[]>([])
const templateAssignments = ref<Record<string, TemplateAssignmentDto[]>>({})
const templates = ref<TemplateSummary[]>([])
const loading = ref(true)
const loadError = ref<string | null>(null)
const loadWarning = ref<string | null>(null)
const createDialogOpen = ref(false)
const createSubmitting = ref(false)
const createSubmitted = ref(false)
const createError = ref<string | null>(null)
const assignmentSubmitting = ref(false)
const assignmentSubmitted = ref(false)
const assignmentError = ref<string | null>(null)
const assignmentsRefreshing = ref(false)
const templatesLoading = ref(false)
const templatesError = ref<string | null>(null)
const selectedGroupId = ref<string | null>(null)
const assignmentDeleteSubmitting = reactive<Record<string, boolean>>({})

const createForm = reactive({
  name: "",
  description: "",
})

const assignmentForm = reactive({
  templateId: "",
  templateVersionId: "",
  priority: "",
})

const inputClasses =
  "file:text-foreground placeholder:text-muted-foreground selection:bg-primary selection:text-primary-foreground dark:bg-input/30 border-input w-full min-w-0 rounded-md border bg-transparent px-3 py-2 text-base shadow-xs transition-[color,box-shadow] outline-none file:inline-flex file:h-7 file:border-0 file:bg-transparent file:text-sm file:font-medium disabled:pointer-events-none disabled:cursor-not-allowed disabled:opacity-50 md:text-sm focus-visible:border-ring focus-visible:ring-ring/50 focus-visible:ring-[3px] aria-invalid:ring-destructive/20 dark:aria-invalid:ring-destructive/40 aria-invalid:border-destructive"

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

const extractErrorMessage = (error: unknown, fallback: string) => {
  if (!error || typeof error !== "object") return fallback
  const record = error as { data?: { message?: string }; message?: string }
  return record.data?.message || record.message || fallback
}

const createErrors = computed(() => {
  const next: Record<string, string[]> = {}
  if (!createForm.name.trim()) next.name = ["Name is required."]
  return next
})

const assignmentErrors = computed(() => {
  const next: Record<string, string[]> = {}
  if (!assignmentForm.templateId.trim()) next.templateId = ["Template ID is required."]
  const priorityValue = assignmentForm.priority.trim()
  if (priorityValue) {
    const parsed = Number(priorityValue)
    if (!Number.isInteger(parsed) || parsed < 0) {
      next.priority = ["Priority must be 0 or greater."]
    }
  }
  return next
})

const selectedGroup = computed(() =>
  groups.value.find((group) => group.id === selectedGroupId.value) ?? null,
)

const groupRows = computed(() =>
  [...groups.value].sort((first, second) => Date.parse(second.updatedAt) - Date.parse(first.updatedAt)),
)

const templateNameById = computed(() =>
  new Map(templates.value.map((template) => [template.id, template.name])),
)

const assignmentRows = computed<AssignmentRow[]>(() => {
  if (!selectedGroupId.value) return []
  const assignments = templateAssignments.value[selectedGroupId.value] ?? []
  return [...assignments]
    .sort((first, second) => first.priority - second.priority)
    .map((assignment) => ({
      id: assignment.id,
      templateId: assignment.templateId,
      templateLabel: templateNameById.value.get(assignment.templateId) ?? assignment.templateId,
      templateVersionId: assignment.templateVersionId ?? null,
      priority: assignment.priority,
    }))
})

const totalGroupsLabel = computed(() => (loading.value ? "--" : String(groups.value.length)))
const groupsWithAssignmentsLabel = computed(() => {
  if (loading.value) return "--"
  return String(
    groups.value.filter(
      (group) => (templateAssignments.value[group.id] ?? []).length > 0,
    ).length,
  )
})

const totalAssignmentsLabel = computed(() => {
  if (loading.value) return "--"
  const total = groups.value.reduce(
    (sum, group) => sum + (templateAssignments.value[group.id]?.length ?? 0),
    0,
  )
  return String(total)
})

const latestUpdatedLabel = computed(() => {
  if (loading.value) return "--"
  if (!groups.value.length) return "—"
  const latest = groups.value.reduce((current, group) =>
    Date.parse(group.updatedAt) > Date.parse(current.updatedAt) ? group : current,
  )
  return formatDate(latest.updatedAt)
})

const assignmentsLoading = computed(() => loading.value || assignmentsRefreshing.value)

const assignmentCountFor = (groupId: string) =>
  templateAssignments.value[groupId]?.length ?? 0

const selectGroup = (groupId: string) => {
  selectedGroupId.value = groupId
}

const isDeletingAssignment = (assignmentId: string) =>
  Boolean(assignmentDeleteSubmitting[assignmentId])

const resetCreateForm = () => {
  createForm.name = ""
  createForm.description = ""
  createSubmitted.value = false
  createError.value = null
}

const resetAssignmentForm = () => {
  assignmentForm.templateId = ""
  assignmentForm.templateVersionId = ""
  assignmentForm.priority = ""
  assignmentSubmitted.value = false
  assignmentError.value = null
}

const loadTemplates = async () => {
  if (templatesLoading.value) return
  templatesLoading.value = true
  templatesError.value = null
  try {
    const result = await brainApi<TemplateSummary[]>("/api/templates")
    templates.value = Array.isArray(result) ? result : []
  } catch (error) {
    templatesError.value = "Template list unavailable."
    templates.value = []
  } finally {
    templatesLoading.value = false
  }
}

const loadGroups = async () => {
  loading.value = true
  loadError.value = null
  loadWarning.value = null
  try {
    const result = await brainApi<InstanceGroupDto[]>("/api/instance-groups")
    groups.value = Array.isArray(result) ? result : []

    if (!groups.value.length) {
      templateAssignments.value = {}
      return
    }

    const assignmentResults = await Promise.allSettled(
      groups.value.map((group) =>
        brainApi<TemplateAssignmentDto[]>(`/api/instance-groups/${group.id}/template-assignments`),
      ),
    )

    const nextAssignments: Record<string, TemplateAssignmentDto[]> = {}
    let assignmentsFailed = false

    assignmentResults.forEach((result, index) => {
      const groupId = groups.value[index].id
      if (result.status === "fulfilled") {
        nextAssignments[groupId] = Array.isArray(result.value) ? result.value : []
      } else {
        nextAssignments[groupId] = []
        assignmentsFailed = true
      }
    })

    templateAssignments.value = nextAssignments

    if (assignmentsFailed) {
      loadWarning.value = "Some group assignments could not be loaded."
    }
  } catch (error) {
    loadError.value = "Unable to load instance groups."
    groups.value = []
    templateAssignments.value = {}
  } finally {
    loading.value = false
  }
}

const refreshAssignments = async (groupId: string) => {
  assignmentsRefreshing.value = true
  try {
    const result = await brainApi<TemplateAssignmentDto[]>(
      `/api/instance-groups/${groupId}/template-assignments`,
    )
    templateAssignments.value = {
      ...templateAssignments.value,
      [groupId]: Array.isArray(result) ? result : [],
    }
  } catch (error) {
    assignmentError.value = extractErrorMessage(error, "Unable to refresh assignments.")
  } finally {
    assignmentsRefreshing.value = false
  }
}

const submitCreate = async () => {
  createSubmitted.value = true
  if (Object.keys(createErrors.value).length > 0) return
  createSubmitting.value = true
  createError.value = null
  try {
    const created = await brainApi<InstanceGroupDto>("/api/instance-groups", {
      method: "POST",
      body: {
        name: createForm.name.trim(),
        ...(createForm.description.trim()
          ? { description: createForm.description.trim() }
          : {}),
      },
    })
    createDialogOpen.value = false
    resetCreateForm()
    await loadGroups()
    if (created?.id) {
      selectedGroupId.value = created.id
    }
  } catch (error) {
    createError.value = extractErrorMessage(error, "Unable to create group.")
  } finally {
    createSubmitting.value = false
  }
}

const submitAssignment = async () => {
  assignmentSubmitted.value = true
  if (!selectedGroupId.value) return
  if (Object.keys(assignmentErrors.value).length > 0) return
  assignmentSubmitting.value = true
  assignmentError.value = null
  try {
    const payload: Record<string, unknown> = {
      templateId: assignmentForm.templateId.trim(),
    }
    const versionId = assignmentForm.templateVersionId.trim()
    if (versionId) {
      payload.templateVersionId = versionId
    }
    const priorityValue = assignmentForm.priority.trim()
    if (priorityValue) {
      payload.priority = Number(priorityValue)
    }
    await brainApi<TemplateAssignmentDto>(
      `/api/instance-groups/${selectedGroupId.value}/template-assignments`,
      {
        method: "POST",
        body: payload,
      },
    )
    resetAssignmentForm()
    await refreshAssignments(selectedGroupId.value)
  } catch (error) {
    assignmentError.value = extractErrorMessage(error, "Unable to add assignment.")
  } finally {
    assignmentSubmitting.value = false
  }
}

const removeAssignment = async (assignmentId: string) => {
  if (!selectedGroupId.value) return
  if (assignmentDeleteSubmitting[assignmentId]) return
  assignmentDeleteSubmitting[assignmentId] = true
  assignmentError.value = null
  try {
    await brainApi<void>(
      `/api/instance-groups/${selectedGroupId.value}/template-assignments/${assignmentId}`,
      { method: "DELETE" },
    )
    await refreshAssignments(selectedGroupId.value)
  } catch (error) {
    assignmentError.value = extractErrorMessage(error, "Unable to remove assignment.")
  } finally {
    assignmentDeleteSubmitting[assignmentId] = false
  }
}

watch(
  () => groups.value,
  (next) => {
    if (!next.length) {
      selectedGroupId.value = null
      return
    }
    if (!selectedGroupId.value || !next.some((group) => group.id === selectedGroupId.value)) {
      selectedGroupId.value = next[0].id
    }
  },
  { immediate: true },
)

watch(selectedGroupId, () => {
  resetAssignmentForm()
})

watch(createDialogOpen, (open) => {
  if (!open) resetCreateForm()
})

await Promise.all([loadGroups(), loadTemplates()])
</script>

<style scoped></style>
