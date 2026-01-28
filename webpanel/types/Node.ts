export interface Node {
    id: string,
    name: string,
    region: string,
    status: NodeStatus
    devMode: boolean
    capacitySlots: number
    usedSlots: number
    lastHeartbeatAt: Date
    nodeVersion: string
    tags: string
    baseUrl: string
}

export enum NodeStatus {
    ONLINE,
    OFFLINE,
    UNKNOWN
}