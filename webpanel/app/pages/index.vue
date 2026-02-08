<template>
    <div class="flex flex-1 flex-col gap-10 p-4 pt-0">
      <div class="grid auto-rows-min gap-4 md:grid-cols-4">

        <AppStatsCard v-for="i in 4" :key="i" variant="blue"/>
      </div>
      <section class="text-foreground space-y-4">
        <div>
          <h2 class="text-2xl font-semibold">Penis</h2>
          <div class="text-muted-foreground">Penis 123</div>
        </div>
        <div class="bg-muted/50 min-h-screen flex-1 rounded-xl md:min-h-min">
          <AppInstancesTable :data="instances" :columns="columns"/>
        </div>
      </section>

    </div>
</template>

<script lang="ts" setup>
import {columns, type InstanceRow} from "~/components/app/instances/columns";
import {type InstanceDto, mockNodes} from "~/store/nodes";

const instances: Ref<InstanceRow[]> = ref([])
const mockInstances: InstanceDto[] = [
  {
    id: 'inst_01J0A1B2C3D4E5F6G7H8J9K0L',
    name: 'survival-01',
    displayName: 'Survival EU #01',
    state: 'RUNNING',
    requestedByUserId: 'user_01',
    nodeId: mockNodes[0].id,
    region: 'eu-central',
    tags: 'survival,public',
    devModeAllowed: false,
    portsJson: '{"game":25565,"query":25566}',
    variablesJson: '{"maxPlayers":80,"difficulty":"hard"}',
    createdAt: '2026-01-28T18:10:03.000Z',
    updatedAt: '2026-01-29T12:01:30.000Z',
    startedAt: '2026-01-28T18:12:12.000Z',
  },
  {
    id: 'inst_01J0A1B2C3D4E5F6G7H8J9K0M',
    name: 'hub-01',
    displayName: 'Lobby / Hub',
    state: 'RUNNING',
    requestedByUserId: 'user_01',
    nodeId: mockNodes[1].id,
    region: 'eu-central',
    tags: 'hub,critical',
    devModeAllowed: false,
    portsJson: '{"game":25570}',
    variablesJson: '{"motd":"Welcome"}',
    createdAt: '2026-01-20T12:00:00.000Z',
    updatedAt: '2026-01-29T12:01:10.000Z',
    startedAt: '2026-01-20T12:01:03.000Z',
  },
  {
    id: 'inst_01J0A1B2C3D4E5F6G7H8J9K0N',
    name: 'minigames-02',
    displayName: 'Minigames EU #02',
    state: 'STARTING',
    requestedByUserId: 'user_02',
    nodeId: mockNodes[1].id,
    region: 'eu-central',
    tags: 'minigames,autoscale',
    devModeAllowed: false,
    portsJson: '{"game":25610}',
    variablesJson: '{"maxPlayers":40}',
    createdAt: '2026-01-29T12:00:05.000Z',
    updatedAt: '2026-01-29T12:02:01.000Z',
  },
  {
    id: 'inst_01J0A1B2C3D4E5F6G7H8J9K0P',
    name: 'creative-01',
    displayName: 'Creative Build Server',
    state: 'STOPPED',
    requestedByUserId: 'user_03',
    nodeId: mockNodes[2].id,
    region: 'eu-north',
    tags: 'creative,private',
    devModeAllowed: true,
    portsJson: '{"game":25700}',
    variablesJson: '{"world":"plots"}',
    createdAt: '2026-01-10T09:14:22.000Z',
    updatedAt: '2026-01-28T22:31:02.000Z',
    startedAt: '2026-01-10T09:16:00.000Z',
    stoppedAt: '2026-01-28T22:30:11.000Z',
  },
  {
    id: 'inst_01J0A1B2C3D4E5F6G7H8J9K0Q',
    name: 'events-01',
    displayName: 'Event Server (One-Off)',
    state: 'FAILED',
    requestedByUserId: 'user_02',
    nodeId: mockNodes[4].id,
    region: 'eu-west',
    tags: 'events,temporary',
    devModeAllowed: false,
    portsJson: '{"game":25800}',
    variablesJson: '{"maxPlayers":120}',
    createdAt: '2026-01-29T10:44:10.000Z',
    updatedAt: '2026-01-29T10:50:44.000Z',
    failureReason: 'Node offline during startup (last heartbeat too old)',
  },
  {
    id: 'inst_01J0A1B2C3D4E5F6G7H8J9K0R',
    name: 'dev-test-01',
    displayName: 'Dev Test Instance',
    state: 'RUNNING',
    requestedByUserId: 'user_01',
    nodeId: mockNodes[3].id,
    region: 'local',
    tags: 'dev,test',
    devModeAllowed: true,
    portsJson: '{"game":25900}',
    variablesJson: '{"debug":true,"logLevel":"trace"}',
    createdAt: '2026-01-29T11:40:00.000Z',
    updatedAt: '2026-01-29T12:02:20.000Z',
    startedAt: '2026-01-29T11:41:12.000Z',
  },
  {
    id: 'inst_01J0A1B2C3D4E5F6G7H8J9K0S',
    name: 'unassigned-01',
    displayName: 'Waiting for Node',
    state: 'CREATED',
    requestedByUserId: 'user_04',
    nodeId: undefined,
    region: 'eu-central',
    tags: 'pending',
    devModeAllowed: false,
    portsJson: '{}',
    variablesJson: '{"note":"scheduler not assigned yet"}',
    createdAt: '2026-01-29T12:01:55.000Z',
    updatedAt: '2026-01-29T12:01:55.000Z',
  },
]
</script>

<style lang="scss" scoped>

</style>
