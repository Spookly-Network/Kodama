import { h } from 'vue'
import type { ColumnDef } from '@tanstack/vue-table'
import { default as AppStatusBadge } from "~/components/app/AppStatusBadge.vue";
import { ArrowUpDown, ChevronDown } from 'lucide-vue-next'
import {Button} from "#components";
import DropdownAction from '@/components/app/nodes/AppNodesTableDropDown.vue'

const statusStyles = {
    'ONLINE': 'bg-green-500/10 text-green-500',
    'OFFLINE': 'bg-red-500/10 text-red-500',
    'UNKNOWN': 'bg-gray-500/10 text-gray-500'
}

export const columns: ColumnDef<NodeDto>[] = [
    {
        accessorKey: 'id',
        header: 'ID',
        cell: ({ row }) => h('div', row.getValue('id')),
    },
    {
        accessorKey: 'name',
        header: 'Name',
        cell: ({ row }) => h('div', row.getValue('name')),
    },
    {
        accessorKey: 'status',
        header: ({ column }) => {
            return h(Button, {
                variant: 'ghost',
                onClick: () => column.toggleSorting(column.getIsSorted() === 'asc'),
            }, () => ['Status', h(ArrowUpDown, { class: 'ml-2 h-4 w-4' })])
        },
        cell: ({ row }) => {
            return h(AppStatusBadge, { variant: row.getValue('status') }, { default: () => row.getValue('status') })
        }
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
        accessorKey: 'nodeVersion',
        header: 'Version',
        cell: ({ row }) => h('div', row.getValue('nodeVersion')),
        enableGrouping: true,
        enableSorting: true,
        enableHiding: true
    },
    {
        id: 'actions',
        enableHiding: false,
        cell: ({ row }) => {
            const payment = row.original

            return h('div', { class: 'relative' }, h(DropdownAction, {
                payment,
            }))
        },
    },
]