<script setup lang="ts">
import TableContainer from "~/components/TableContainer.vue";
import {PageOptions, Pagination} from "~/types";

const pageOpts =ref<PageOptions>({
  currentPage: 1,
  pageSize: 10,
  total: 0
})

const handleAdd = () => {

}
</script>

<template>
  <TableContainer
      title="role.title"
      showAddButton
      add-button-text="role.create"
      @add="handleAdd"
      v-model:page-size="pageOpts.pageSize"
      v-model:current-page="pageOpts.currentPage"
  >

  </TableContainer>
</template>

<style scoped>

</style>