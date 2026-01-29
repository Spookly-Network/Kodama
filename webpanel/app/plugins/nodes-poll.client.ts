import {useNodesStore} from "~/store/nodes";
import {useAuthStore} from "~/store/auth";

export default defineNuxtPlugin(() => {
    const auth = useAuthStore()
    const nodes = useNodesStore()

    let timer: number | undefined

    watch(
        () => auth.accessToken,
        (t) => {
            if (!t) {
                if (timer) window.clearInterval(timer)
                timer = undefined
                return
            }

            nodes.refresh()

            if (!timer) {
                timer = window.setInterval(() => {
                    nodes.ensureFresh(0) // force refresh on interval
                }, 15_000)
            }
        },
        { immediate: true }
    )
})