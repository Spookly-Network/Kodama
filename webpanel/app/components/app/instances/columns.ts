import { h } from 'vue'
import type { ColumnDef } from '@tanstack/vue-table'
import { Badge } from '#components'
import type { InstanceState } from "#shared/types/Instance";

export type InstanceRow = {
    id: string
    name: string
    displayName: string
    state: InstanceState
    stateLabel: string
    nodeId: string | null
    nodeLabel: string
    regionLabel: string
    tags: string[]
    devModeAllowed: boolean | null
    templateLayerCount: number
    groupLayerCount: number
    updatedAtLabel: string
    createdAtLabel: string
    startedAtLabel: string | null
    stoppedAtLabel: string | null
    failureReason: string | null
}

const statusStyles: Record<InstanceState, string> = {
    REQUESTED: 'bg-slate-500/10 text-slate-300 border-slate-500/20',
    PREPARING: 'bg-sky-500/10 text-sky-300 border-sky-500/20',
    PREPARED: 'bg-cyan-500/10 text-cyan-300 border-cyan-500/20',
    STARTING: 'bg-blue-500/10 text-blue-300 border-blue-500/20',
    RUNNING: 'bg-emerald-500/10 text-emerald-300 border-emerald-500/20',
    STOPPING: 'bg-amber-500/10 text-amber-300 border-amber-500/20',
    STOPPED: 'bg-zinc-500/10 text-zinc-300 border-zinc-500/20',
    DESTROYED: 'bg-neutral-500/10 text-neutral-300 border-neutral-500/20',
    FAILED: 'bg-red-500/10 text-red-300 border-red-500/20',
}

const devModeStyles = 'bg-amber-500/10 text-amber-300 border-amber-500/20'

export const columns: ColumnDef<InstanceRow>[] = [
    {
        accessorKey: 'displayName',
        header: 'Instance',
        cell: ({ row }) => {
            const instance = row.original
            const tagBadges = instance.tags.slice(0, 3).map((tag) =>
                h(Badge, { variant: 'outline', class: 'text-xs' }, { default: () => tag })
            )
            if (instance.tags.length > 3) {
                tagBadges.push(
                    h(Badge, { variant: 'outline', class: 'text-xs' }, { default: () => `+${instance.tags.length - 3}` })
                )
            }
            if (instance.devModeAllowed) {
                tagBadges.unshift(
                    h(Badge, { class: `text-xs ${devModeStyles}` }, { default: () => 'Dev OK' })
                )
            }
            return h('div', { class: 'space-y-2' }, [
                h('div', { class: 'font-medium' }, instance.displayName || instance.name),
                h('div', { class: 'text-xs text-muted-foreground' }, instance.name),
                tagBadges.length ? h('div', { class: 'flex flex-wrap gap-1' }, tagBadges) : null,
            ])
        },
    },
    {
        accessorKey: 'state',
        header: 'State',
        cell: ({ row }) => {
            const instance = row.original
            const metaRows = [
                instance.failureReason
                    ? h('div', { class: 'text-xs text-destructive' }, instance.failureReason)
                    : null,
                h('div', { class: 'text-xs text-muted-foreground' }, `Updated ${instance.updatedAtLabel}`),
            ].filter(Boolean)
            return h('div', { class: 'space-y-2' }, [
                h(Badge, { class: `border ${statusStyles[instance.state]}` }, { default: () => instance.stateLabel }),
                ...metaRows,
            ])
        },
    },
    {
        accessorKey: 'nodeLabel',
        header: 'Node',
        cell: ({ row }) => {
            const instance = row.original
            return h('div', { class: 'space-y-1', title: instance.nodeId ?? undefined }, [
                h('div', { class: 'font-medium' }, instance.nodeLabel),
                instance.nodeId
                    ? h('div', { class: 'text-xs text-muted-foreground truncate max-w-[160px]' }, instance.nodeId)
                    : h('div', { class: 'text-xs text-muted-foreground' }, 'Awaiting assignment'),
            ])
        },
    },
    {
        accessorKey: 'regionLabel',
        header: 'Region',
        cell: ({ row }) => h('div', row.getValue('regionLabel')),
    },
    {
        accessorKey: 'templateLayerCount',
        header: 'Layers',
        cell: ({ row }) => {
            const instance = row.original
            const instanceLayers = instance.templateLayerCount - instance.groupLayerCount
            const breakdown = instance.templateLayerCount
                ? `${instanceLayers} instance / ${instance.groupLayerCount} group`
                : 'No layers'
            return h('div', { class: 'space-y-1' }, [
                h('div', { class: 'font-medium' }, String(instance.templateLayerCount)),
                h('div', { class: 'text-xs text-muted-foreground' }, breakdown),
            ])
        },
    },
    {
        accessorKey: 'updatedAtLabel',
        header: 'Updated',
        cell: ({ row }) => {
            const instance = row.original
            const timelineLabel = instance.stoppedAtLabel
                ? `Stopped ${instance.stoppedAtLabel}`
                : instance.startedAtLabel
                    ? `Started ${instance.startedAtLabel}`
                    : `Created ${instance.createdAtLabel}`
            return h('div', { class: 'space-y-1' }, [
                h('div', { class: 'font-medium' }, instance.updatedAtLabel),
                h('div', { class: 'text-xs text-muted-foreground' }, timelineLabel),
            ])
        },
    },
]
