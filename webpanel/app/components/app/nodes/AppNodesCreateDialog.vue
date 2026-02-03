<template>
  <Dialog>
    <DialogTrigger>
      <Button>
        <Plus/>
        Create Node
      </Button>
    </DialogTrigger>

    <DialogContent class="sm:max-w-4xl">
      <DialogHeader>
        <DialogTitle>Edit profile</DialogTitle>
        <DialogDescription>
          Make changes to your profile here. Click save when you're
          done.
        </DialogDescription>
      </DialogHeader>
      <AppNodesCreateForm />
      <DialogFooter>
        <DialogClose as-child>
          <Button variant="outline">
            Cancel
          </Button>
        </DialogClose>
        <Button type="submit">
          Save changes
        </Button>
      </DialogFooter>
    </DialogContent>
  </Dialog>
</template>

<script setup lang="ts">

import {Plus} from "lucide-vue-next";
</script>

<style scoped>

</style>