<template>
  <div>
    <el-switch
      v-model="draggable"
      active-text="开启拖拽"
      inactive-text="关闭拖拽"
    >
    </el-switch>
    <el-button v-if="draggable" @click="batchSave">批量保存</el-button>
    <el-button @click="batchDelete">批量删除</el-button>
    <el-tree
      :data="menus"
      :props="defaultProps"
      :expand-on-click-node="false"
      show-checkbox
      node-key="catId"
      :default-expanded-keys="expandedKey"
      :draggable="draggable"
      :allow-drop="allowDrop"
      @node-drop="handleDrop"
      ref="menuTree"
    >
      <span class="custom-tree-node" slot-scope="{ node, data }">
        <span>{{ node.label }}</span>
        <span>
          <el-button
            v-if="node.level <= 2"
            type="text"
            size="mini"
            @click="() => append(data)"
          >
            Append
          </el-button>
          <el-button type="text" size="mini" @click="() => edit(data)">
            edit
          </el-button>
          <el-button
            v-if="node.childNodes.length == 0"
            type="text"
            size="mini"
            @click="() => remove(node, data)"
          >
            Delete
          </el-button>
        </span>
      </span>
    </el-tree>
    <el-dialog
      :title="title"
      :visible.sync="dialogVisible"
      :close-on-click-modal="false"
    >
      <el-form :model="category">
        <el-form-item label="分类名称">
          <el-input v-model="category.name" autocomplete="off"></el-input>
        </el-form-item>
        <el-form-item label="图标">
          <el-input v-model="category.icon" autocomplete="off"></el-input>
        </el-form-item>
        <el-form-item label="计量单位">
          <el-input
            v-model="category.productUnit"
            autocomplete="off"
          ></el-input>
        </el-form-item>
      </el-form>
      <span slot="footer" class="dialog-footer">
        <el-button @click="dialogVisible = false">Cancel</el-button>
        <el-button type="primary" @click="submitData()">Confirm</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
export default {
  //import 引入的组件需要注入到对象中才能使用
  components: {},
  props: {},
  data() {
    return {
      pCid: [],
      draggable: false,
      updateNodes: [],
      maxDepth: 0,
      maxLevel: 0,
      title: "",
      dialogType: "",
      category: {
        name: "",
        parentCid: 0,
        showStatus: 1,
        sort: 0,
        catId: null,
        icon: "",
        productUnit: "",
      },
      dialogVisible: false,
      menus: [],
      expandedKey: [],
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

    append(data) {
      this.title = "增加分类";
      this.dialogType = "add";
      console.log("要添加的数据", data);
      this.dialogVisible = true;
      this.category.catLevel = data.catLevel * 1 + 1;
      this.category.parentCid = data.catId;
      this.category.catId = null;
      this.category.icon = "";
      this.category.sort = 0;
      this.category.showStatus = 1;
      this.category.productUnit = "";
      this.category.name = "";
    },

    edit(data) {
      this.title = "修改分类";
      this.dialogType = "edit";
      this.dialogVisible = true;
      console.log("要修改的数据", data);
      // 发送request获取当前node的最新数据
      this.$http({
        url: this.$http.adornUrl(`/product/category/info/${data.catId}`),
        method: "get",
      }).then(({ data }) => {
        console.log("要回显的数据", data);
        this.category.name = data.data.name;
        this.category.catId = data.data.catId;
        this.category.productUnit = data.data.productUnit;
        this.category.icon = data.data.icon;
      });
    },

    submitData() {
      if (this.dialogType == "add") {
        this.addCategory();
      }
      if (this.dialogType == "edit") {
        this.editCategory();
      }
    },

    editCategory() {
      var { catId, name, icon, productUnit } = this.category;
      this.$http({
        url: this.$http.adornUrl("/product/category/update"),
        method: "post",
        data: this.$http.adornParams({ catId, name, icon, productUnit }, false),
      }).then(({ data }) => {
        this.$message({
          message: "菜单修改成功",
          type: "success",
        });
        // 关闭dialog
        this.dialogVisible = false;
        // 刷新数据
        this.getMenus();
        // 展开已增加node的那个母node
        this.expandedKey = [this.category.parentCid];
      });
    },

    addCategory() {
      this.$http({
        url: this.$http.adornUrl("/product/category/save"),
        method: "post",
        data: this.$http.adornParams(this.category, false),
      }).then(({ data }) => {
        this.$message({
          message: "保存成功",
          type: "success",
        });
        console.log("已添加菜单：", this.category);
        // 关闭dialog
        this.dialogVisible = false;
        // 刷新数据
        this.getMenus();
        // 展开已增加node的那个母node
        this.expandedKey = [this.category.parentCid];
      });
    },

    remove(node, data) {
      var ids = [data.catId];
      this.$confirm(`是否删除【${data.name}】菜单？`, "Warning", {
        confirmButtonText: "确认",
        cancelButtonText: "取消",
        type: "warning",
      })
        .then(() => {
          this.$http({
            url: this.$http.adornUrl("/product/category/delete"),
            method: "post",
            data: this.$http.adornParams(ids, false),
          }).then(({ data }) => {
            this.$message({
              message: "菜单删除成功",
              type: "success",
            });
            // 刷新数据
            this.getMenus();
            // 设置被删除node的母node
            this.expandedKey = [node.parent.data.catId];
          });
          console.log("remove", node, data);
        })
        .catch(() => {});
    },

    // 判断是否满足drag的条件
    allowDrop(draggingNode, dropNode, type) {
      console.log("allowDrop:", draggingNode, dropNode, type);

      var depth = this.countDepth(draggingNode);
      console.log("draggingNode depth: ", depth);

      if (type == "inner") {
        return depth + dropNode.level <= 3;
      } else {
        return depth + dropNode.level - 1 <= 3;
      }
    },

    // 返回node的深度
    countDepth(node) {
      var max = 0;
      if (node == null) return max;

      for (let i = 0; i < node.childNodes.length; i++) {
        var depth = this.countDepth(node.childNodes[i]);
        max = Math.max(max, depth);
      }
      return max + 1;
    },

    // allowDrop(draggingNode, dropNode, type) {
    //   console.log("allowDrop:", draggingNode, dropNode, type);
    //   this.countNodeLevel(draggingNode.data);
    //   let deep = this.maxLevel - draggingNode.data.catLevel + 1;
    //   console.log("深度：", deep);

    //   if (type == "inner") {
    //     return deep + dropNode.level <= 3;
    //   } else {
    //     return deep + dropNode.parent.level <= 3;
    //   }
    // },

    // countNodeLevel(node) {
    //   // 找到所有子节点，求出最大深度
    //   if (node.children != null && node.children.length > 0) {
    //     for (let i = 0; i < node.children.length; i++) {
    //       if (node.children[i].catLevel > this.maxLevel) {
    //         this.maxLevel = node.children[i].catLevel;
    //       }
    //       this.countNodeLevel(node.children[i]);
    //     }
    //   }
    // },

    // 把已发生变化的CategoryEntity的数据告诉database。那些数据发生了变化？
    // 1. “当前项”的parentCid变了
    // 2. "当前项"的sort变了；视droptype，"目标项"的sort或"目标项的子项"的sort也会变
    // 3. "当前项及其子项"的catLevel变了
    handleDrop(draggingNode, dropNode, dropType, ev) {
      console.log("handleDrop: ", draggingNode, dropNode, dropType);

      // 1. 把所有"sort属性发生变动的node"找出来，放到siblings里；
      //    把"当前项"的"新parentCid"找出来，放到pCid里
      //    把"当前项"的"新catLevel"找出来，放到catLevel里
      let siblings = null;
      let pCid = 0;
      let catLevel = null;
      if (dropType == "before" || dropType == "after") {
        pCid =
          dropNode.parent.data.catId == undefined
            ? 0
            : dropNode.parent.data.catId;
        siblings = dropNode.parent.childNodes;
        catLevel = dropNode.level;
      } else {
        pCid = dropNode.data.catId;
        siblings = dropNode.childNodes;
        catLevel = dropNode.level + 1;
      }
      this.pCid.push(pCid);

      // 2. 把"新parentCid"、"新catLevel"和"新sort属性值"以Json格式封装起来，放到updateNodes里
      //    database改draggingNode需要知道4个属性: catId, parentId, catLevel, sort
      //    database改被拖至位置所在层级的node需要2个属性: catId, sort
      //    database改draggingNode的子节点需要2个属性: catId, catLevel
      for (let i = 0; i < siblings.length; i++) {
        // 对draggingNode，这么做：
        if (siblings[i].data.catId == draggingNode.data.catId) {
          this.updateNodes.push({
            catId: siblings[i].data.catId,
            sort: i,
            parentCid: pCid,
            catLevel: catLevel,
          });
          // 对draggingNode的子节点，这么做：
          this.updateChildNodeLevel(siblings[i]);
        }
        // 对draggingNode被拖至位置的所在层级的node，这么做：
        else {
          this.updateNodes.push({ catId: siblings[i].data.catId, sort: i });
        }
      }
    },

    updateChildNodeLevel(node) {
      if (node != null) {
        for (let i = 0; i < node.childNodes.length; i++) {
          var cNode = node.childNodes[i];
          this.updateNodes.push({
            catId: cNode.data.catId,
            catLevel: cNode.level,
          });
          this.updateChildNodeLevel(cNode);
        }
      }
    },

    batchSave() {
      this.$http({
        url: this.$http.adornUrl("/product/category/update/sort"),
        method: "post",
        data: this.$http.adornParams(this.updateNodes, false),
      }).then(({ data }) => {
        this.$message({
          message: "菜单顺序等修改成功",
          type: "success",
        });
        // 刷新出新的菜单
        this.getMenus();
        // 设置默认展开的菜单
        this.expandedKey = this.pCid;
        // 清空updateNodes
        this.updateNodes = [];
      });

      // console.log("updateNodes: ", this.updateNodes);
    },

    batchDelete() {
      let catIds = [];
      let checkedNodes = this.$refs.menuTree.getCheckedNodes();
      for(let i = 0; i < checkedNodes.length; i++) {
        catIds.push(checkedNodes[i].catId);
      }
      this.$confirm(`是否批量删除【${catIds}】菜单?`, "提示", {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning"
      })
        .then(() => {
          this.$http({
            url: this.$http.adornUrl("/product/category/delete"),
            method: "post",
            data: this.$http.adornData(catIds, false)
          }).then(({ data }) => {
            this.$message({
              message: "菜单批量删除成功",
              type: "success"
            });
            this.getMenus();
          });
        })
        .catch(() => {});
    }
  },

  created() {
    this.getMenus();
  },
};
</script>
<style lang='scss' scoped>
//@import url(); 引入公共 css 类
</style>