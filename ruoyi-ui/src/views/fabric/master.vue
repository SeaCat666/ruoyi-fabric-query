<template>
  <div class="app-container master-page">
    <section class="master-hero">
      <div>
        <div class="eyebrow">MASTER DATA</div>
        <h2>面料与辅料基础资料</h2>
        <p>分别维护面料、辅料供应商与面料分类，各档案的下拉选项会即时同步。</p>
      </div>
      <div class="hero-stats">
        <div>
          <strong>{{ categoryCount }}</strong>
          <span>面料分类</span>
        </div>
        <div>
          <strong>{{ suppliers.length }}</strong>
          <span>面料供应商</span>
        </div>
        <div>
          <strong>{{ accessorySuppliers.length }}</strong>
          <span>辅料供应商</span>
        </div>
      </div>
    </section>

    <el-row :gutter="18">
      <el-col :xs="24" :lg="13">
        <el-card shadow="never" class="master-card">
          <template #header>
            <div class="card-header">
              <div>
                <strong>{{ supplierTypeLabel }}供应商档案</strong>
                <span>面料与辅料供应商完全独立</span>
              </div>
              <el-button type="primary" icon="Plus" @click="openSupplierDialog()">
                新增{{ supplierTypeLabel }}供应商
              </el-button>
            </div>
          </template>

          <el-tabs v-model="supplierType" class="supplier-tabs">
            <el-tab-pane label="面料供应商" name="fabric" />
            <el-tab-pane label="辅料供应商" name="accessory" />
          </el-tabs>
          <el-input
            v-model="supplierKeyword"
            clearable
            prefix-icon="Search"
            :placeholder="`搜索${supplierTypeLabel}供应商名称、电话或地址`"
            class="table-search"
          />
          <el-table v-loading="loading" :data="filteredSuppliers" height="560">
            <el-table-column label="供应商名称" prop="name" min-width="145" show-overflow-tooltip />
            <el-table-column label="电话" prop="phone" width="130" show-overflow-tooltip>
              <template #default="{ row }">{{ row.phone || "—" }}</template>
            </el-table-column>
            <el-table-column label="地址" prop="address" min-width="170" show-overflow-tooltip>
              <template #default="{ row }">{{ row.address || "—" }}</template>
            </el-table-column>
            <el-table-column label="操作" width="112" fixed="right" align="center">
              <template #default="{ row }">
                <el-button link type="primary" icon="Edit" @click="openSupplierDialog(row)" />
                <el-button link type="danger" icon="Delete" @click="removeSupplier(row)" />
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="11">
        <el-card shadow="never" class="master-card category-card">
          <template #header>
            <div class="card-header">
              <div>
                <strong>面料分类</strong>
                <span>一级大类与二级分类</span>
              </div>
              <el-button type="primary" plain icon="Plus" @click="openCategoryDialog()">
                新增分类
              </el-button>
            </div>
          </template>

          <el-alert
            title="已有面料使用的分类不能删除；存在下级分类的一级分类也不能删除。"
            type="info"
            :closable="false"
            show-icon
            class="category-tip"
          />
          <el-table
            v-loading="loading"
            :data="categories"
            row-key="id"
            default-expand-all
            :tree-props="{ children: 'children' }"
            height="550"
          >
            <el-table-column label="分类名称" prop="name" min-width="180" />
            <el-table-column label="级别" width="82" align="center">
              <template #default="{ row }">
                <el-tag :type="row.level === 1 ? 'success' : 'info'" effect="plain">
                  {{ row.level === 1 ? "一级" : "二级" }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="排序" prop="sortOrder" width="72" align="center" />
            <el-table-column label="操作" width="112" fixed="right" align="center">
              <template #default="{ row }">
                <el-button link type="primary" icon="Edit" @click="openCategoryDialog(row)" />
                <el-button link type="danger" icon="Delete" @click="removeCategory(row)" />
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog
      v-model="supplierDialogOpen"
      :title="supplierForm.id
        ? `修改${supplierDialogTypeLabel}供应商`
        : `新增${supplierDialogTypeLabel}供应商`"
      width="520px"
      append-to-body
      :close-on-click-modal="false"
    >
      <el-form
        ref="supplierRef"
        :model="supplierForm"
        :rules="supplierRules"
        label-width="90px"
      >
        <el-form-item label="名称" prop="name">
          <el-input v-model="supplierForm.name" maxlength="150" :placeholder="`请输入${supplierDialogTypeLabel}供应商名称`" />
        </el-form-item>
        <el-form-item label="电话" prop="phone">
          <el-input v-model="supplierForm.phone" maxlength="100" placeholder="请输入联系电话" />
        </el-form-item>
        <el-form-item label="地址" prop="address">
          <el-input
            v-model="supplierForm.address"
            type="textarea"
            :rows="3"
            maxlength="500"
            placeholder="请输入供应商地址"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" :loading="saving" @click="saveSupplier">保存</el-button>
        <el-button @click="supplierDialogOpen = false">取消</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="categoryDialogOpen"
      :title="categoryForm.id ? '修改面料分类' : '新增面料分类'"
      width="480px"
      append-to-body
      :close-on-click-modal="false"
    >
      <el-form
        ref="categoryRef"
        :model="categoryForm"
        :rules="categoryRules"
        label-width="90px"
      >
        <el-form-item label="上级分类">
          <el-select
            v-model="categoryForm.parentId"
            clearable
            placeholder="不选择表示一级分类"
            style="width: 100%"
          >
            <el-option
              v-for="item in availableParents"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="分类名称" prop="name">
          <el-input v-model="categoryForm.name" maxlength="100" placeholder="请输入分类名称" />
        </el-form-item>
        <el-form-item label="显示排序" prop="sortOrder">
          <el-input-number
            v-model="categoryForm.sortOrder"
            :min="0"
            :max="999"
            controls-position="right"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" :loading="saving" @click="saveCategory">保存</el-button>
        <el-button @click="categoryDialogOpen = false">取消</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="FabricMaster">
import {
  createCategory,
  createSupplier,
  deleteCategory,
  deleteSupplier,
  listCategories,
  listSuppliers,
  updateCategory,
  updateSupplier
} from "@/api/fabric/fabric"
import {
  createAccessorySupplier,
  deleteAccessorySupplier,
  listAccessorySuppliers,
  updateAccessorySupplier
} from "@/api/fabric/accessory"

const { proxy } = getCurrentInstance()

const loading = ref(false)
const saving = ref(false)
const suppliers = ref([])
const accessorySuppliers = ref([])
const categories = ref([])
const supplierType = ref("fabric")
const supplierDialogType = ref("fabric")
const supplierKeyword = ref("")
const supplierDialogOpen = ref(false)
const categoryDialogOpen = ref(false)

const supplierForm = reactive({
  id: undefined,
  name: "",
  phone: "",
  address: ""
})

const categoryForm = reactive({
  id: undefined,
  parentId: undefined,
  name: "",
  sortOrder: 0
})

const supplierRules = {
  name: [{ required: true, message: "供应商名称不能为空", trigger: "blur" }],
  phone: [{ required: true, message: "联系电话不能为空", trigger: "blur" }],
  address: [{ required: true, message: "供应商地址不能为空", trigger: "blur" }]
}

const categoryRules = {
  name: [{ required: true, message: "分类名称不能为空", trigger: "blur" }],
  sortOrder: [{ required: true, message: "排序值不能为空", trigger: "change" }]
}

const supplierTypeLabel = computed(() =>
  supplierType.value === "accessory" ? "辅料" : "面料"
)

const supplierDialogTypeLabel = computed(() =>
  supplierDialogType.value === "accessory" ? "辅料" : "面料"
)

const currentSuppliers = computed(() =>
  supplierType.value === "accessory" ? accessorySuppliers.value : suppliers.value
)

const filteredSuppliers = computed(() => {
  const keyword = supplierKeyword.value.trim().toLowerCase()
  if (!keyword) return currentSuppliers.value
  return currentSuppliers.value.filter(item =>
    [item.name, item.phone, item.address]
      .filter(Boolean)
      .some(value => String(value).toLowerCase().includes(keyword))
  )
})

const categoryCount = computed(() =>
  categories.value.reduce((sum, item) => sum + 1 + (item.children?.length || 0), 0)
)

const availableParents = computed(() =>
  categories.value.filter(item => item.id !== categoryForm.id)
)

function loadData() {
  loading.value = true
  Promise.all([listSuppliers(), listAccessorySuppliers(), listCategories()])
    .then(([supplierRes, accessorySupplierRes, categoryRes]) => {
      suppliers.value = supplierRes.data || []
      accessorySuppliers.value = accessorySupplierRes.data || []
      categories.value = categoryRes.data || []
    })
    .finally(() => {
      loading.value = false
    })
}

function openSupplierDialog(row) {
  supplierDialogType.value = supplierType.value
  Object.assign(supplierForm, {
    id: row?.id,
    name: row?.name || "",
    phone: row?.phone || "",
    address: row?.address || ""
  })
  supplierDialogOpen.value = true
  nextTick(() => proxy.$refs.supplierRef?.clearValidate())
}

function saveSupplier() {
  proxy.$refs.supplierRef.validate(valid => {
    if (!valid) return
    saving.value = true
    const isAccessory = supplierDialogType.value === "accessory"
    const request = supplierForm.id
      ? (isAccessory
          ? updateAccessorySupplier(supplierForm)
          : updateSupplier(supplierForm))
      : (isAccessory
          ? createAccessorySupplier(supplierForm)
          : createSupplier(supplierForm))
    request.then(() => {
      proxy.$modal.msgSuccess(
        `${supplierDialogTypeLabel.value}供应商${supplierForm.id ? "修改" : "新增"}成功`
      )
      supplierDialogOpen.value = false
      loadData()
    }).finally(() => {
      saving.value = false
    })
  })
}

function removeSupplier(row) {
  const label = supplierTypeLabel.value
  const request = supplierType.value === "accessory"
    ? deleteAccessorySupplier
    : deleteSupplier
  proxy.$modal.confirm(`是否删除${label}供应商“${row.name}”？`).then(() =>
    request(row.id)
  ).then(() => {
    proxy.$modal.msgSuccess(`${label}供应商删除成功`)
    loadData()
  }).catch(() => {})
}

function openCategoryDialog(row) {
  Object.assign(categoryForm, {
    id: row?.id,
    parentId: row?.parentId,
    name: row?.name || "",
    sortOrder: row?.sortOrder ?? 0
  })
  categoryDialogOpen.value = true
  nextTick(() => proxy.$refs.categoryRef?.clearValidate())
}

function saveCategory() {
  proxy.$refs.categoryRef.validate(valid => {
    if (!valid) return
    saving.value = true
    const request = categoryForm.id
      ? updateCategory(categoryForm)
      : createCategory(categoryForm)
    request.then(() => {
      proxy.$modal.msgSuccess(categoryForm.id ? "分类修改成功" : "分类新增成功")
      categoryDialogOpen.value = false
      loadData()
    }).finally(() => {
      saving.value = false
    })
  })
}

function removeCategory(row) {
  proxy.$modal.confirm(`是否删除分类“${row.name}”？`).then(() =>
    deleteCategory(row.id)
  ).then(() => {
    proxy.$modal.msgSuccess("分类删除成功")
    loadData()
  }).catch(() => {})
}

loadData()
</script>

<style scoped lang="scss">
.master-page {
  min-height: calc(100vh - 84px);
  background: #f4f7fb;
}

.master-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18px;
  padding: 25px 30px;
  border-radius: 16px;
  color: #fff;
  background:
    radial-gradient(circle at 88% 20%, rgba(61, 212, 183, 0.3), transparent 28%),
    linear-gradient(125deg, #11283b, #086d6b);
  box-shadow: 0 12px 28px rgba(17, 40, 59, 0.16);

  .eyebrow {
    margin-bottom: 6px;
    color: #69e0cb;
    font-size: 12px;
    font-weight: 700;
    letter-spacing: 1.8px;
  }

  h2 {
    margin: 0 0 7px;
    font-size: 26px;
  }

  p {
    margin: 0;
    color: rgba(255, 255, 255, 0.75);
  }
}

.hero-stats {
  display: flex;
  gap: 12px;

  div {
    min-width: 106px;
    padding: 13px 18px;
    border: 1px solid rgba(255, 255, 255, 0.18);
    border-radius: 12px;
    background: rgba(255, 255, 255, 0.08);
    text-align: center;
  }

  strong,
  span {
    display: block;
  }

  strong {
    font-size: 23px;
  }

  span {
    margin-top: 3px;
    color: rgba(255, 255, 255, 0.7);
    font-size: 12px;
  }
}

.master-card {
  border: 0;
  border-radius: 14px;

  :deep(.el-card__header) {
    padding: 18px 20px;
  }
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;

  strong,
  span {
    display: block;
  }

  strong {
    color: #172332;
    font-size: 17px;
  }

  span {
    margin-top: 4px;
    color: #8b97a5;
    font-size: 12px;
  }
}

.table-search {
  margin-bottom: 14px;
}

.supplier-tabs {
  margin-top: -8px;
}

.category-tip {
  margin-bottom: 14px;
}

@media (max-width: 992px) {
  .master-hero {
    align-items: flex-start;
    flex-direction: column;
    gap: 18px;
  }

  .category-card {
    margin-top: 18px;
  }
}

@media (max-width: 560px) {
  .master-hero {
    padding: 22px;
  }

  .hero-stats {
    width: 100%;

    div {
      flex: 1;
      min-width: 0;
    }
  }
}
</style>
