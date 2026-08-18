<template>
  <section class="accessory-panel">
    <el-alert
      title="辅料档案和供应商独立存储；档案可关联多个库存批次，一旦被库存关联便永久保留，不能删除。"
      type="info"
      :closable="false"
      show-icon
      class="accessory-tip"
    />

    <el-form ref="queryRef" :model="query" inline label-width="88px">
      <el-form-item label="辅料编号" prop="code">
        <el-input v-model="query.code" clearable placeholder="例如 B-260001" style="width: 170px" />
      </el-form-item>
      <el-form-item label="年份" prop="year">
        <el-select v-model="query.year" style="width: 120px" @change="search">
          <el-option v-for="year in yearOptions" :key="year" :label="year" :value="year" />
        </el-select>
      </el-form-item>
      <el-form-item label="辅料供应商" prop="supplierId">
        <el-select v-model="query.supplierId" clearable filterable placeholder="全部" style="width: 210px">
          <el-option v-for="item in suppliers" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="常规使用" prop="regularUse">
        <el-select v-model="query.regularUse" clearable placeholder="全部" style="width: 120px">
          <el-option label="是" value="1" />
          <el-option label="否" value="0" />
          <el-option label="未设置" value="blank" />
        </el-select>
      </el-form-item>
      <el-form-item label="是否合规" prop="compliant">
        <el-select v-model="query.compliant" clearable placeholder="全部" style="width: 120px">
          <el-option label="是" value="1" />
          <el-option label="否" value="0" />
          <el-option label="未设置" value="blank" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="search">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="openAdd"
          v-hasPermi="['fabric:fabric:add']">新增辅料</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="!ids.length" @click="remove()"
          v-hasPermi="['fabric:fabric:remove']">批量删除</el-button>
      </el-col>
    </el-row>

    <el-table v-loading="loading" :data="rows" row-key="id" @selection-change="selectRows">
      <el-table-column
        v-if="hasEditPermission || hasDeletePermission"
        type="selection"
        width="48"
        align="center"
      />
      <el-table-column label="图片" width="76" align="center" fixed>
        <template #default="{ row }">
          <el-image
            v-if="row.images?.length"
            class="accessory-thumbnail"
            :src="imageSrc(row.images[0].thumbnailUrl || row.images[0].imageUrl)"
            :preview-src-list="imagePreviewList(row)"
            :initial-index="0"
            preview-teleported
            fit="cover"
          />
          <span v-else class="no-image">暂无</span>
        </template>
      </el-table-column>
      <el-table-column label="辅料编号" prop="code" width="125" fixed />
      <el-table-column label="录入日期" prop="entryDate" width="110" />
      <el-table-column label="录入员" prop="recorderName" min-width="130" show-overflow-tooltip />
      <el-table-column label="辅料供应商" prop="supplierName" min-width="155" show-overflow-tooltip />
      <el-table-column label="库存关联" width="130" align="center">
        <template #default="{ row }">
          <el-button v-if="row.inventoryStockCount > 0" link type="success" @click="openInventory(row)">
            {{ row.inventoryStockCount }} 个库存批次
          </el-button>
          <span v-else class="no-image">未关联</span>
        </template>
      </el-table-column>
      <el-table-column label="电话" prop="supplierPhone" width="125" show-overflow-tooltip>
        <template #default="{ row }">{{ row.supplierPhone }}</template>
      </el-table-column>
      <el-table-column label="地址" prop="supplierAddress" min-width="190" show-overflow-tooltip>
        <template #default="{ row }">{{ row.supplierAddress }}</template>
      </el-table-column>
      <el-table-column label="尺寸规格" prop="sizeSpec" min-width="140" show-overflow-tooltip />
      <el-table-column label="大货价" prop="bulkPrice" min-width="130" show-overflow-tooltip />
      <el-table-column label="常规使用" width="100" align="center">
        <template #default="{ row }"><status-tag :value="row.regularUse" /></template>
      </el-table-column>
      <el-table-column label="是否合规" width="100" align="center">
        <template #default="{ row }"><status-tag :value="row.compliant" /></template>
      </el-table-column>
      <el-table-column label="备注" prop="notes" min-width="210" show-overflow-tooltip>
        <template #default="{ row }">{{ row.notes || "—" }}</template>
      </el-table-column>
      <el-table-column
        v-if="hasEditPermission || hasDeletePermission"
        label="操作"
        width="150"
        align="center"
        fixed="right"
      >
        <template #default="{ row }">
          <el-button
            v-if="canEditRow(row)"
            link
            type="primary"
            icon="Edit"
            @click="openEdit(row)"
            v-hasPermi="['fabric:fabric:edit']"
          >修改</el-button>
          <el-button
            link
            type="danger"
            icon="Delete"
            @click="remove(row)"
            v-hasPermi="['fabric:fabric:remove']"
          >删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total > 0" :total="total" v-model:page="query.pageNum"
      v-model:limit="query.pageSize" @pagination="load" />

    <el-dialog
      v-model="dialogOpen"
      :title="form.id ? `修改辅料 ${form.code}` : '新增辅料'"
      width="1120px"
      append-to-body
      destroy-on-close
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="105px">
        <div class="form-section-title">基础信息</div>
        <el-row :gutter="20">
          <el-col :span="6">
            <el-form-item label="辅料编号">
              <el-input
                :model-value="form.id ? form.code : accessoryCodePreview"
                disabled
              />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="日期" prop="entryDate">
              <el-date-picker
                v-model="form.entryDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="系统自动生成"
                disabled
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="辅料供应商" prop="supplierId">
              <div class="supplier-picker">
                <el-select
                  v-model="form.supplierId"
                  filterable
                  placeholder="输入名称搜索"
                  style="flex: 1"
                >
                  <el-option
                    v-for="item in suppliers"
                    :key="item.id"
                    :label="item.name"
                    :value="item.id"
                  />
                </el-select>
                <el-button type="primary" plain icon="Plus" @click="supplierOpen = true">
                  快速添加
                </el-button>
              </div>
            </el-form-item>
          </el-col>
        </el-row>

        <div class="form-section-title">规格与业务状态</div>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="尺寸规格" prop="sizeSpec">
              <el-input
                v-model="form.sizeSpec"
                maxlength="200"
                show-word-limit
                placeholder="例如：2.5 cm、32L、20 mm"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="大货价" prop="bulkPrice">
              <el-input
                v-model="form.bulkPrice"
                maxlength="200"
                show-word-limit
                placeholder="保留原始单位，例如 1.30 元/M、35 元/包"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="常规使用" prop="regularUse">
              <el-select
                v-model="form.regularUse"
                clearable
                placeholder="未设置"
                style="width: 100%"
              >
                <el-option label="是，常规使用" value="1" />
                <el-option label="否，非常规使用" value="0" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="是否合规" prop="compliant">
              <el-select
                v-model="form.compliant"
                clearable
                placeholder="未设置"
                style="width: 100%"
              >
                <el-option label="是，已确认合规" value="1" />
                <el-option label="否，不合规" value="0" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <div class="form-section-title">图片与备注</div>
        <el-form-item label="备注" prop="notes">
          <el-input
            v-model="form.notes"
            type="textarea"
            :rows="4"
            maxlength="1000"
            show-word-limit
            placeholder="记录颜色、用途、采购要求或其他说明"
          />
        </el-form-item>
        <el-form-item label="辅料图片" prop="imageUrls">
          <div class="image-field">
            <image-upload
              v-model="form.imageUrls"
              action="/fabric/accessory/upload"
              :limit="8"
              :file-size="10"
              :file-type="['png', 'jpg', 'jpeg']"
            />
            <span class="field-hint">最多 8 张，列表中点击首图可浏览全部原图。</span>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" :loading="saving" @click="submit">确 定</el-button>
          <el-button :disabled="saving" @click="dialogOpen = false">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog
      v-model="supplierOpen"
      title="快速添加辅料供应商"
      width="520px"
      append-to-body
      :close-on-click-modal="false"
    >
      <el-form ref="supplierRef" :model="supplierForm" :rules="supplierRules" label-width="105px">
        <el-form-item label="供应商名称" prop="name">
          <el-input
            v-model="supplierForm.name"
            maxlength="150"
            placeholder="请输入辅料供应商名称"
          />
        </el-form-item>
        <el-form-item label="联系电话" prop="phone">
          <el-input
            v-model="supplierForm.phone"
            maxlength="100"
            placeholder="请输入联系电话"
          />
        </el-form-item>
        <el-form-item label="供应商地址" prop="address">
          <el-input
            v-model="supplierForm.address"
            type="textarea"
            :rows="3"
            maxlength="500"
            show-word-limit
            placeholder="请输入详细地址"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" :loading="supplierSaving" @click="saveSupplier">
            保存并选择
          </el-button>
          <el-button :disabled="supplierSaving" @click="supplierOpen = false">取消</el-button>
        </div>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { defineComponent, h } from "vue"
import useUserStore from "@/store/modules/user"
import {
  addAccessory, createAccessorySupplier, deleteAccessories, getAccessory,
  listAccessories, listAccessorySuppliers, updateAccessory
} from "@/api/fabric/accessory"

const { proxy } = getCurrentInstance()
const router = useRouter()
const userStore = useUserStore()
const baseApi = import.meta.env.VITE_APP_BASE_API
const currentYear = new Date().getFullYear()
const yearOptions = Array.from(
  { length: Math.max(currentYear - 2024, 1) },
  (_, index) => 2025 + index
)
const loading = ref(false), saving = ref(false), supplierSaving = ref(false)
const rows = ref([]), suppliers = ref([]), total = ref(0), ids = ref([])
const dialogOpen = ref(false), supplierOpen = ref(false)
const query = reactive({ pageNum: 1, pageSize: 20, code: undefined, year: currentYear, supplierId: undefined, regularUse: undefined, compliant: undefined })
const form = reactive({})
const supplierForm = reactive({ name: "", phone: "", address: "" })
const rules = {
  entryDate: [{ required: true, message: "请选择日期", trigger: "change" }],
  supplierId: [{ required: true, message: "请选择辅料供应商", trigger: "change" }],
  imageUrls: [{ required: true, message: "请至少上传一张辅料图片", trigger: "change" }]
}
const accessoryCodePreview = computed(() => {
  const year = form.entryDate ? new Date(form.entryDate).getFullYear() : currentYear
  return `B-${String(year).slice(-2)}????`
})
const selectedSupplier = computed(() =>
  suppliers.value.find(item => item.id === form.supplierId)
)
const supplierRules = {
  name: [{ required: true, message: "名称不能为空", trigger: "blur" }],
  phone: [{ required: true, message: "电话不能为空", trigger: "blur" }],
  address: [{ required: true, message: "地址不能为空", trigger: "blur" }]
}
const isSuperAdmin = computed(() => userStore.roles.includes("admin"))
const isFabricManager = computed(() => userStore.roles.includes("fabric_manager"))
const hasEditPermission = computed(() =>
  userStore.permissions.includes("*:*:*")
  || userStore.permissions.includes("fabric:fabric:edit")
)
const hasDeletePermission = computed(() =>
  userStore.permissions.includes("*:*:*")
  || userStore.permissions.includes("fabric:fabric:remove")
)
function canEditRow(row) {
  if (!hasEditPermission.value || !row) return false
  return isSuperAdmin.value
    || isFabricManager.value
    || row.createBy === userStore.name
}

const StatusTag = defineComponent({
  props: { value: String },
  setup(props) {
    return () => h("span", { class: ["status-pill", props.value === "1" ? "is-yes" : props.value === "0" ? "is-no" : "is-empty"] },
      props.value === "1" ? "是" : props.value === "0" ? "否" : "未设置")
  }
})

function load() {
  loading.value = true
  Promise.all([listAccessories(query), listAccessorySuppliers()]).then(([data, supplierData]) => {
    rows.value = data.rows || []; total.value = data.total || 0; suppliers.value = supplierData.data || []
  }).finally(() => loading.value = false)
}
function search() { query.pageNum = 1; load() }
function resetQuery() { proxy.resetForm("queryRef"); query.year = currentYear; search() }
function selectRows(selection) { ids.value = selection.map(item => item.id) }
function localDateString() {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`
}
function resetForm() {
  Object.assign(form, {
    id: undefined,
    code: undefined,
    entryDate: localDateString(),
    supplierId: undefined,
    sizeSpec: "",
    bulkPrice: "",
    regularUse: undefined,
    compliant: undefined,
    notes: "",
    imageUrls: undefined,
    images: []
  })
}
function openAdd() { resetForm(); dialogOpen.value = true; nextTick(() => proxy.$refs.formRef?.clearValidate()) }
function openEdit(row) { getAccessory(row.id).then(res => { resetForm(); Object.assign(form, res.data); dialogOpen.value = true }) }
function submit() {
  if (saving.value) return
  proxy.$refs.formRef.validate(valid => {
    if (!valid) return
    saving.value = true
    proxy.$modal.confirm(`请再次确认辅料供应商、尺寸、大货价及两个状态均填写正确；日期、业务年份和编号由系统自动生成。`).then(() => {
      return form.id ? updateAccessory(form) : addAccessory(form)
    }).then(() => { proxy.$modal.msgSuccess(form.id ? "修改成功" : "新增成功"); dialogOpen.value = false; load() })
      .finally(() => { saving.value = false })
  })
}
function remove(row) {
  const target = row?.id || ids.value
  proxy.$modal.confirm("只能删除从未关联库存的辅料档案；已关联档案会由系统拒绝删除。是否继续？").then(() => deleteAccessories(target))
    .then(() => { proxy.$modal.msgSuccess("删除成功"); load() }).catch(() => {})
}
function openInventory(row) {
  router.push({ path: "/inventory/stock", query: { accessoryId: row.id } })
}
function saveSupplier() {
  proxy.$refs.supplierRef.validate(valid => {
    if (!valid) return
    supplierSaving.value = true
    createAccessorySupplier(supplierForm).then(res => {
      form.supplierId = res.data.id; supplierOpen.value = false
      return listAccessorySuppliers()
    }).then(res => suppliers.value = res.data || []).finally(() => supplierSaving.value = false)
  })
}
function imageSrc(url) {
  if (!url) return ""
  return /^(https?:)?\/\//i.test(url) ? url : `${baseApi}${url}`
}
function imagePreviewList(row) {
  return (row.images || [])
    .map(image => image.imageUrl || image.thumbnailUrl)
    .filter(Boolean)
    .map(imageSrc)
}
watch(supplierOpen, open => { if (open) Object.assign(supplierForm, { name: "", phone: "", address: "" }) })
load()
</script>

<style scoped>
.accessory-tip {
  margin-bottom: 16px;
}

.form-section-title {
  margin: 4px 0 16px;
  padding: 9px 13px;
  color: #0f5f5c;
  font-size: 14px;
  font-weight: 700;
  border-left: 4px solid #0f766e;
  border-radius: 4px;
  background: #f0fdfa;
}

.supplier-picker {
  display: flex;
  width: 100%;
  gap: 8px;
}

.status-tip {
  margin: 0 0 18px;
}

.image-field {
  display: flex;
  flex-direction: column;
  width: 100%;
  gap: 8px;
}

.field-hint {
  color: #909399;
  font-size: 12px;
}

.accessory-thumbnail {
  display: block;
  width: 50px;
  height: 50px;
  margin: 0 auto;
  overflow: hidden;
  cursor: zoom-in;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: #f5f7fa;
}

.no-image {
  color: #a8abb2;
  font-size: 12px;
}

:deep(.el-table__header th) {
  color: #475569;
  font-weight: 600;
  background: #f8fafc;
}

:deep(.status-pill) {
  display: inline-block;
  min-width: 54px;
  padding: 3px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
}

:deep(.status-pill.is-yes) {
  color: #067647;
  background: #dcfae6;
}

:deep(.status-pill.is-no) {
  color: #b42318;
  background: #fee4e2;
}

:deep(.status-pill.is-empty) {
  color: #667085;
  background: #f2f4f7;
}

@media (max-width: 900px) {
  .supplier-picker {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
