interface PingResponse {
  status: boolean
}

export const useBrainStore = defineStore('brain', () => {
  const brainApi = useBrainApi()

  const alive = ref(false)
  const lastHeartbeat = ref(0)

  async function checkAlive() {
    try {
      const response = await brainApi<PingResponse>('/actuator/health/ping')
      if (response.status == "UP") {
        alive.value = true
        lastHeartbeat.value = Date.now()
      }
    } catch (e) {
      alive.value = false
    }
  }
  return { alive, checkAlive, lastHeartbeat }
})