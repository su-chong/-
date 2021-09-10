<template>
  <el-tree :data="menus" :props="defaultProps" node-key="catId" ref="menuTree" @node-click="nodeclick">
  </el-tree>
</template>

<script>
export default {
  components: {},
  props: {},
  data() {
    return {
      menus: [],
      defaultProps: {
        children: "children",
        label: "name",
      },
    };
  },
  methods: {
    getMenus() {
      this.$http({
        url: this.$http.adornUrl("/product/category/list/tree"),
        method: "get",
      }).then(({ data }) => {
        console.log("已成功获取到categoryEntity: ", data.data);
        this.menus = data.data;
      });
    },
    nodeclick(data,node,component) {
      console.log("子组件category的节点被点击",data,node,component);
      // 向父组件发送事件
      this.$emit('tree-node-click', data,node,component);
    }
  },
  created() {
    this.getMenus();
  },
};
</script>
<style lang='scss' scoped>
</style>