import {type NodeDto, NodeStatus} from "#shared/types/Node";
import {type LoginRequest, type LoginResponse, useAuthStore} from "~/store/auth";
import type {NodeCreatePayload} from "~/components/app/nodes/AppNodesCreateForm.vue";

export type InstanceState = 'CREATED' | 'STARTING' | 'RUNNING' | 'STOPPING' | 'STOPPED' | 'FAILED'

export interface InstanceDto {
    id: string
    name: string
    displayName: string
    state: InstanceState
    requestedByUserId: string
    nodeId?: string
    region: string
    tags: string
    devModeAllowed: boolean
    portsJson: string
    variablesJson: string
    createdAt: string
    updatedAt: string
    startedAt?: string
    stoppedAt?: string
    failureReason?: string
}

export interface NodeUpdatePayload {
    region: string
    capacitySlots: number
    nodeVersion: string
    devMode: boolean
    tags?: string | null
    baseUrl?: string | null
}

export const mockNodes: NodeDto[] = [
    {
        id: 'node_01HZXQ2W7QK5B3K9R2K4C3W2P1',
        name: 'fra-1',
        region: 'eu-central',
        status: NodeStatus.ONLINE,
        devMode: false,
        capacitySlots: 32,
        usedSlots: 18,
        lastHeartbeatAt: '2026-01-29T12:02:11.000Z',
        nodeVersion: '1.4.2',
        tags: 'prod,ssd,hetzner',
        baseUrl: 'https://node-fra-1.kodama.internal',
    },
    {
        id: 'node_01HZXQ38J0H9K7Q5Z1F0H2J8D7',
        name: 'fra-2',
        region: 'eu-central',
        status: NodeStatus.OFFLINE,
        devMode: false,
        capacitySlots: 32,
        usedSlots: 31,
        lastHeartbeatAt: '2026-01-29T12:01:44.000Z',
        nodeVersion: '1.4.1',
        tags: 'prod,ssd',
        baseUrl: 'https://node-fra-2.kodama.internal',
    },
    {
        id: 'node_01HZXQ4Z9T8D3M0X2N6A7B1C5V',
        name: 'hel-1',
        region: 'eu-north',
        status: NodeStatus.ONLINE,
        devMode: false,
        capacitySlots: 16,
        usedSlots: 4,
        lastHeartbeatAt: '2026-01-29T11:58:03.000Z',
        nodeVersion: '1.4.2',
        tags: 'prod,maintenance',
        baseUrl: 'https://node-hel-1.kodama.internal',
    },
    {
        id: 'node_01HZXQ5W6P7D9S3K2L1J0H8G6F',
        name: 'dev-local',
        region: 'local',
        status: NodeStatus.ONLINE,
        devMode: true,
        capacitySlots: 6,
        usedSlots: 2,
        lastHeartbeatAt: '2026-01-29T12:02:28.000Z',
        nodeVersion: '1.4.2-dev',
        tags: 'dev,local',
        baseUrl: 'http://127.0.0.1:8081',
    },
    {
        id: 'node_01HZXQ6A1B2C3D4E5F6G7H8J9K',
        name: 'ams-1',
        region: 'eu-west',
        status: NodeStatus.UNKNOWN,
        devMode: false,
        capacitySlots: 24,
        usedSlots: 0,
        lastHeartbeatAt: '2026-01-29T11:12:09.000Z',
        nodeVersion: '1.3.9',
        tags: 'prod,old',
        baseUrl: 'https://node-ams-1.kodama.internal',
    },
]

export const useNodesStore = defineStore('nodes', () => {
    const byId = reactive<Record<string, NodeDto>>({})
    const lastLoadedAt = ref(0)
    const loading = ref(false)
    const brainApi = useBrainApi()

    function upsertMany(nodes: NodeDto[]) {
        for (const n of nodes) byId[n.id] = n
    }

    async function refresh() {
        if (loading.value) return
        loading.value = true
        try {
            const nodes = await brainApi<NodeDto[]>('/api/nodes')
            upsertMany(nodes)
            lastLoadedAt.value = Date.now()
        } finally {
            loading.value = false
        }
    }

    async function ensureFresh(maxAgeMs = 15_000) {
        if (Date.now() - lastLoadedAt.value > maxAgeMs) {
            await refresh()
        }
    }

    function get(nodeId?: string) {
        return nodeId ? byId[nodeId] : undefined
    }

    async function create(node: NodeCreatePayload) {
        const api = useBrainApi()
        const auth = useAuthStore();

        const request = await api<CreateNodeResponse>('/api/nodes/register', {
            method: 'POST',
            headers: [['Authorization', auth.authHeader || ""]],
            body: { ...node } satisfies NodeCreatePayload,
        })
    }

    async function update(nodeId: string, payload: NodeUpdatePayload) {
        const api = useBrainApi()
        const updated = await api<NodeDto>(`/api/nodes/${nodeId}`, {
            method: 'PUT',
            body: { ...payload } satisfies NodeUpdatePayload,
        })
        byId[updated.id] = updated
        return updated
    }

    return { byId, refresh, create, update, ensureFresh, get, lastLoadedAt, upsertMany, loading }
})

interface CreateNodeResponse {
    nodeId: string
}
