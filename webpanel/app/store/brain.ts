interface PingResponse {
  status: "UP" | "DOWN"
}

export const useBrainStore = defineStore('brain', () => {
  const brainApi = useBrainApi()

  const alive = ref(false)
  const lastHeartbeat = ref(0)

  async function checkAlive() {
    try {
      const response = await brainApi<PingResponse>('/actuator/health/ping')
      if (response.status === "UP") {
        alive.value = true
      }
    } catch (e) {
      alive.value = false
    }
    lastHeartbeat.value = Date.now()
  }

  return { alive, checkAlive, lastHeartbeat }
})