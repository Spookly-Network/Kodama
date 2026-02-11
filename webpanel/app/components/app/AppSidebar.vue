<script setup lang="ts">
import type { SidebarProps } from "@/components/ui/sidebar"
import {
  BookOpen,
  Bot,
  Command,
  Frame,
  LifeBuoy,
  Map,
  PieChart,
  Send,
  Settings2,
  SquareTerminal,
  BrainCircuit, Server, Package, SquareStack, BookDashed
} from "lucide-vue-next"
import {
  Sidebar,
  SidebarContent, SidebarFooter,
  SidebarHeader,
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem
} from "~/components/ui/sidebar";
import {useBrainStore} from "~/store/brain";
import {useAuthStore} from "~/store/auth";

const brainStore = useBrainStore()
const authStore = useAuthStore()
const props = withDefaults(defineProps<SidebarProps>(), {
  variant: "inset",
})

const formatter = new Intl.DateTimeFormat('de-DE', {
  year: 'numeric',
  month: '2-digit',
  day: '2-digit',
  hour: '2-digit',
  minute: '2-digit',
  second: '2-digit',
});

const lastHeartbeat = computed(() => formatter.format(new Date(brainStore.lastHeartbeat)))
const brainStatusClass = computed(() => brainStore.alive ? 'bg-green-500' : 'bg-red-500')

const data = {
  user: {
    name: "Nutzer",
    email: authStore.roles[0],
    avatar: "/avatars/shadcn.jpg",
  },
  navMain: [
    {
      title: "Nodes",
      url: "/nodes",
      icon: Server,
      isActive: true,
    },
    {
      title: "Templates",
      url: "/templates",
      icon: Package,
    },
    {
      title: "Blueprints",
      url: "#",
      icon: BookDashed,
    },
    {
      title: "Instances",
      url: "/instances",
      icon: SquareStack,
      items: [
        {
          title: "Groups",
          url: "/instances/groups",
        },
      ],
    },
  ],
  navSecondary: [
    {
      title: "Support",
      url: "#",
      icon: LifeBuoy,
    },
    {
      title: "Feedback",
      url: "#",
      icon: Send,
    },
  ],
  projects: [
    {
      name: "Design Engineering",
      url: "#",
      icon: Frame,
    },
    {
      name: "Sales & Marketing",
      url: "#",
      icon: PieChart,
    },
    {
      name: "Travel",
      url: "#",
      icon: Map,
    },
  ],
}
</script>

<template>
  <Sidebar v-bind="props">
    <SidebarHeader>
      <SidebarMenu>
        <SidebarMenuItem>
          <TooltipProvider>
            <Tooltip>
              <TooltipTrigger as-child>
                <SidebarMenuButton size="lg" as-child>
                  <div>
                    <div v-if="brainStore.alive" class="flex aspect-square size-8 items-center justify-center rounded-lg text-sidebar-primary-foreground bg-green-500">
                      <BrainCircuit class="size-4" />
                    </div>

                    <div v-else class="flex aspect-square size-8 items-center justify-center rounded-lg text-sidebar-primary-foreground bg-red-500">
                      <BrainCircuit class="size-4" />
                    </div>
                    <div class="grid flex-1 text-left text-sm leading-tight">
                      <span class="truncate font-medium">Kodama Webpanel</span>
                      <span class="truncate text-xs">
                    v0.0.1-SNAPSHOT.1
                  </span>
                    </div>
                  </div>
                </SidebarMenuButton>
              </TooltipTrigger>
              <TooltipContent>
                <p>Last heartbeat: {{ lastHeartbeat }}</p>
              </TooltipContent>
            </Tooltip>
          </TooltipProvider>


        </SidebarMenuItem>
      </SidebarMenu>
    </SidebarHeader>
    <SidebarContent>
      <NavMain :items="data.navMain" />
    </SidebarContent>
    <SidebarFooter>
      <NavUser :user="data.user" />
    </SidebarFooter>
  </Sidebar>
</template>
