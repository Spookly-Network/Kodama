import { h } from 'vue'
import type {Instance} from "~~/types/Instance";
import type { ColumnDef } from '@tanstack/vue-table'

export const columns: ColumnDef<Instance>[] = [
    {
        accessorKey: 'amount',
        header: () => h('div', { class: 'text-right' }, 'Amount'),
        cell: ({ row }) => {
            const amount = Number.parseFloat(row.getValue('amount'))
            const formatted = new Intl.NumberFormat('en-US', {
                style: 'currency',
                currency: 'USD',
            }).format(amount)
            return h('div', { class: 'text-right font-medium' }, formatted)
        },
    }
]