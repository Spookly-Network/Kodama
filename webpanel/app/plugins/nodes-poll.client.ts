import {useNodesStore} from "~/store/nodes";
import {useAuthStore} from "~/store/auth";
import {useBrainStore} from "~/store/brain";

export default defineNuxtPlugin(() => {
    const auth = useAuthStore()
    const nodes = useNodesStore()
    const brain = useBrainStore()

    let timer: number | undefined

    watch(
        () => auth.isAuthenticated,
        (isAuthenticated) => {
            if (!isAuthenticated) {
                if (timer) window.clearInterval(timer)
                timer = undefined
                return
            }

            nodes.refresh()
          brain.checkAlive()

            if (!timer) {
                timer = window.setInterval(() => {
                  brain.checkAlive()
                  nodes.ensureFresh(0) // force refresh on interval
                }, 15_000)
            }
        },
        { immediate: true }
    )
})
