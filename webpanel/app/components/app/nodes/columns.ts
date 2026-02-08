import { h } from 'vue'
import type { ColumnDef } from '@tanstack/vue-table'
import { Badge, Progress } from "#components"
import { default as AppStatusBadge } from "~/components/app/AppStatusBadge.vue"
import type { NodeStatus } from "#shared/types/Node"
import AppNodesTableActions from '@/components/app/nodes/AppNodesTableActions.vue'

export type NodeRow = {
    id: string
    name: string
    region: string
    status: NodeStatus
    devMode: boolean
    capacitySlots: number
    usedSlots: number
    lastHeartbeatAt: string
    nodeVersion: string
    tags: string[]
    baseUrl: string | null
    heartbeatLabel: string
    heartbeatAgoLabel: string
    usagePercent: number
}

export type NodeActionHandlers = {
    onEdit?: (node: NodeRow) => void
}

const devModeStyles = 'bg-amber-500/10 text-amber-300 border-amber-500/20'

const baseColumns: ColumnDef<NodeRow>[] = [
    {
        accessorKey: 'name',
        header: 'Node',
        cell: ({ row }) => {
            const node = row.original
            const tagBadges = node.tags.slice(0, 3).map((tag) =>
                h(Badge, { variant: 'outline', class: 'text-xs' }, { default: () => tag })
            )
            if (node.tags.length > 3) {
                tagBadges.push(
                    h(Badge, { variant: 'outline', class: 'text-xs' }, { default: () => `+${node.tags.length - 3}` })
                )
            }
            if (node.devMode) {
                tagBadges.unshift(
                    h(Badge, { class: `text-xs ${devModeStyles}` }, { default: () => 'Dev mode' })
                )
            }
            return h('div', { class: 'space-y-2' }, [
                h('div', { class: 'font-medium' }, node.name),
                node.baseUrl
                    ? h('div', { class: 'text-xs text-muted-foreground truncate max-w-[220px]' }, node.baseUrl)
                    : h('div', { class: 'text-xs text-muted-foreground' }, 'No base URL'),
                h('div', { class: 'text-xs text-muted-foreground font-mono truncate max-w-[220px]' }, node.id),
                tagBadges.length ? h('div', { class: 'flex flex-wrap gap-1' }, tagBadges) : null,
            ])
        },
    },
    {
        accessorKey: 'status',
        header: 'Status',
        cell: ({ row }) => {
            const node = row.original
            return h('div', { class: 'space-y-2' }, [
                h(AppStatusBadge, { variant: node.status }, { default: () => node.status }),
                h('div', { class: 'text-xs text-muted-foreground' }, `Heartbeat ${node.heartbeatAgoLabel}`),
                h('div', { class: 'text-xs text-muted-foreground' }, node.heartbeatLabel),
            ])
        },
    },
    {
        accessorKey: 'region',
        header: 'Region',
        cell: ({ row }) => h('div', row.getValue('region')),
        enableGrouping: true,
        enableSorting: true,
        enableHiding: true,
    },
    {
        accessorKey: 'capacitySlots',
        header: 'Slots',
        cell: ({ row }) => {
            const node = row.original
            return h('div', { class: 'space-y-2 min-w-[160px]' }, [
                h('div', { class: 'font-medium' }, `${node.usedSlots} / ${node.capacitySlots}`),
                h(Progress, { modelValue: node.usagePercent, class: 'h-2' }),
                h('div', { class: 'text-xs text-muted-foreground' }, `${node.usagePercent}% used`),
            ])
        },
    },
    {
        accessorKey: 'nodeVersion',
        header: 'Version',
        cell: ({ row }) => {
            const node = row.original
            return h('div', { class: 'space-y-1' }, [
                h('div', { class: 'font-medium' }, node.nodeVersion),
                h(
                    'div',
                    { class: 'text-xs text-muted-foreground' },
                    node.devMode ? 'Dev tooling enabled' : 'Production mode'
                ),
            ])
        },
        enableGrouping: true,
        enableSorting: true,
        enableHiding: true
    },
    {
        accessorKey: 'tags',
        header: 'Tags',
        cell: ({ row }) => {
            const node = row.original
            if (!node.tags.length) {
                return h('div', { class: 'text-xs text-muted-foreground' }, '—')
            }
            return h(
                'div',
                { class: 'flex flex-wrap gap-1' },
                node.tags.map((tag) =>
                    h(Badge, { variant: 'outline', class: 'text-xs' }, { default: () => tag })
                )
            )
        },
        enableGrouping: true,
        enableSorting: false,
        enableHiding: true,
    },
]

const buildActionsColumn = (actions: NodeActionHandlers): ColumnDef<NodeRow> => ({
    id: 'actions',
    enableHiding: false,
    cell: ({ row }) => {
        const node = row.original
        return h('div', { class: 'relative' }, h(AppNodesTableActions, {
            node,
            onEdit: () => actions.onEdit?.(node),
        }))
    },
})

export const buildColumns = (actions?: NodeActionHandlers): ColumnDef<NodeRow>[] => {
    if (!actions) {
        return baseColumns
    }
    return [...baseColumns, buildActionsColumn(actions)]
}

export const columns = baseColumns
