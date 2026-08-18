<template>
  <div class="app-container fabric-page">
    <el-tabs
      v-model="activeRecordType"
      class="record-type-tabs"
      @tab-change="handleRecordTypeChange"
    >
      <el-tab-pane label="面料档案" name="fabric" />
      <el-tab-pane label="辅料档案" name="accessory" />
    </el-tabs>

    <AccessoryPanel v-if="showAccessory" />
    <div v-else>
    <el-alert
      title="档案与库存规则：面料档案可关联多个库存批次；一旦被库存关联便作为追溯依据永久保留，不能删除。"
      type="info"
      :closable="false"
      show-icon
      class="relation-tip"
    />
    <el-form
      ref="queryRef"
      :model="queryParams"
      :inline="true"
      v-show="showSearch"
      label-width="68px"
    >
      <el-form-item label="编号" prop="code">
        <el-input
          v-model="queryParams.code"
          placeholder="例如 A-260001-PO"
          clearable
          style="width: 190px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="品名" prop="productName">
        <el-input
          v-model="queryParams.productName"
          placeholder="请输入品名"
          clearable
          style="width: 200px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="供应商" prop="supplierId">
        <el-select
          v-model="queryParams.supplierId"
          placeholder="全部供应商"
          clearable
          filterable
          style="width: 220px"
        >
          <el-option
            v-for="item in supplierOptions"
            :key="item.id"
            :label="item.name"
            :value="item.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="分类" prop="categoryId">
        <el-cascader
          v-model="queryParams.categoryId"
          :options="categoryOptions"
          :props="categoryFilterProps"
          placeholder="全部分类"
          clearable
          filterable
          style="width: 210px"
        />
      </el-form-item>
      <el-form-item label="年份" prop="year">
        <el-select v-model="queryParams.year" style="width: 120px" @change="handleQuery">
          <el-option
            v-for="year in yearOptions"
            :key="year"
            :label="year"
            :value="year"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="Plus"
          @click="handleAdd"
          v-hasPermi="['fabric:fabric:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="editSelectionDisabled"
          @click="handleUpdate"
          v-hasPermi="['fabric:fabric:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['fabric:fabric:remove']"
        >删除</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table
      v-loading="loading"
      :data="fabricList"
      row-key="id"
      @selection-change="handleSelectionChange"
    >
      <el-table-column
        v-if="hasEditPermission || hasDeletePermission"
        type="selection"
        width="48"
        align="center"
      />
      <el-table-column label="编号" prop="code" width="160" fixed />
      <el-table-column label="图片" width="76" align="center">
        <template #default="{ row }">
          <el-image
            v-if="row.images?.length"
            class="fabric-thumbnail"
            :src="imageSrc(row.images[0]?.thumbnailUrl || row.images[0]?.imageUrl)"
            :preview-src-list="imagePreviewList(row)"
            fit="cover"
            preview-teleported
          />
          <span v-else class="no-image">暂无</span>
        </template>
      </el-table-column>
      <el-table-column label="日期" prop="entryDate" width="105" />
      <el-table-column label="品名" prop="productName" min-width="160" show-overflow-tooltip />
      <el-table-column label="克重" width="90" align="right">
        <template #default="{ row }">{{ formatNumber(row.weight) }} g/㎡</template>
      </el-table-column>
      <el-table-column label="门幅" width="90" align="right">
        <template #default="{ row }">{{ formatNumber(row.width) }} cm</template>
      </el-table-column>
      <el-table-column label="色号" prop="colorNo" width="80" show-overflow-tooltip />
      <el-table-column label="面料分类" prop="categoryName" min-width="140" show-overflow-tooltip />
      <el-table-column label="供应商" prop="supplierName" min-width="150" show-overflow-tooltip />
      <el-table-column label="库存关联" width="130" align="center">
        <template #default="{ row }">
          <el-button v-if="row.inventoryStockCount > 0" link type="success" @click="openInventory(row)">
            {{ row.inventoryStockCount }} 个库存批次
          </el-button>
          <span v-else class="no-stock">未关联</span>
        </template>
      </el-table-column>
      <el-table-column label="成分" prop="compositionSummary" min-width="220" show-overflow-tooltip />
      <el-table-column label="报价" min-width="130">
        <template #default="{ row }">{{ formatPrice(row.priceValue, row.priceUnit) }}</template>
      </el-table-column>
      <el-table-column label="折算米价" min-width="120">
        <template #default="{ row }">
          {{ row.meterPrice == null ? "不折算" : `${formatMoney(row.meterPrice)} 元/M` }}
        </template>
      </el-table-column>
      <el-table-column label="备注" prop="notes" min-width="200" show-overflow-tooltip>
        <template #default="{ row }">{{ row.notes || "—" }}</template>
      </el-table-column>
      <el-table-column label="录入员" prop="recorderName" min-width="120" show-overflow-tooltip>
        <template #default="{ row }">{{ row.recorderName || row.createBy || "—" }}</template>
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
            @click="handleUpdate(row)"
            v-hasPermi="['fabric:fabric:edit']"
          >修改</el-button>
          <el-button
            link
            type="danger"
            icon="Delete"
            @click="handleDelete(row)"
            v-hasPermi="['fabric:fabric:remove']"
          >删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total > 0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />

    <el-dialog
      :title="title"
      v-model="open"
      width="1120px"
      append-to-body
      destroy-on-close
      :close-on-click-modal="false"
    >
      <el-form ref="fabricRef" :model="form" :rules="rules" label-width="105px">
        <div class="form-section-title">基础信息</div>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="面料编号">
              <el-input :model-value="codePreview" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="8">
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
          <el-col :span="8">
            <el-form-item label="面料分类" prop="categoryId">
              <el-cascader
                v-model="form.categoryId"
                :options="categoryOptions"
                :props="categoryFormProps"
                filterable
                placeholder="请选择一级 / 二级分类"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="24">
            <el-form-item label="供应商" prop="supplierId">
              <div class="supplier-picker">
                <el-select
                  v-model="form.supplierId"
                  filterable
                  placeholder="输入名称搜索"
                  style="flex: 1"
                >
                  <el-option
                    v-for="item in supplierOptions"
                    :key="item.id"
                    :label="item.name"
                    :value="item.id"
                  />
                </el-select>
                <el-button type="primary" plain icon="Plus" @click="openSupplierDialog">
                  快速添加
                </el-button>
              </div>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="7">
            <el-form-item label="品名" prop="productName">
              <el-input v-model="form.productName" placeholder="请输入品名" maxlength="200" />
            </el-form-item>
          </el-col>
          <el-col :span="5">
            <el-form-item label="色号" prop="colorNo">
              <el-input v-model="form.colorNo" placeholder="选填" maxlength="100" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="克重" prop="weight">
              <el-input-number
                v-model="form.weight"
                :min="1"
                :precision="0"
                :step="1"
                controls-position="right"
                style="width: 100%"
              />
              <span class="field-unit">g/㎡</span>
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="包边门幅" prop="width">
              <el-input-number
                v-model="form.width"
                :min="1"
                :precision="0"
                :step="1"
                controls-position="right"
                style="width: 100%"
              />
              <span class="field-unit">cm</span>
            </el-form-item>
          </el-col>
        </el-row>

        <div class="form-section-title">成分与报价</div>
        <el-form-item label="成分" prop="compositions">
          <div class="composition-editor">
            <div
              v-for="(item, index) in form.compositions"
              :key="index"
              class="composition-row"
            >
              <el-select
                v-model="item.componentCode"
                filterable
                placeholder="选择成分"
                class="composition-select"
              >
                <el-option
                  v-for="option in componentOptions"
                  :key="option.code"
                  :label="`${option.nameCn}（${option.nameEn}）`"
                  :value="option.code"
                  :disabled="isComponentUsed(option.code, index)"
                />
              </el-select>
              <el-input-number
                v-model="item.percentage"
                :min="1"
                :max="100"
                :precision="0"
                :step="1"
                controls-position="right"
                class="percentage-input"
              />
              <span class="percent-sign">%</span>
              <el-button
                v-if="index > 0"
                link
                type="danger"
                icon="Delete"
                @click="removeComposition(index)"
              >删除</el-button>
            </div>
            <div class="composition-actions">
              <el-button
                type="primary"
                plain
                icon="Plus"
                :disabled="form.compositions?.length >= 4"
                @click="addComposition"
              >添加成分</el-button>
              <span :class="['composition-total', { invalid: compositionTotal !== 100 }]">
                合计：{{ formatNumber(compositionTotal) }}%
              </span>
              <el-tag type="info">编号后缀：{{ derivedCompositionCode }}</el-tag>
            </div>
          </div>
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="价格数值" prop="priceValue">
              <el-input-number
                v-model="form.priceValue"
                :min="0.01"
                :precision="2"
                :step="0.01"
                controls-position="right"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="计价单位" prop="priceUnit">
              <el-select v-model="form.priceUnit" placeholder="请选择" style="width: 100%">
                <el-option
                  v-for="item in priceUnitOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="折算米价">
              <el-input
                :model-value="
                  form.priceUnit === 'ROLL'
                    ? '元/卷暂不折算'
                    : (meterPricePreview == null ? '填写价格后自动计算' : `${meterPricePreview} 元/M`)
                "
                disabled
              />
            </el-form-item>
          </el-col>
        </el-row>

        <div class="form-section-title">图片与备注</div>
        <el-form-item label="备注" prop="notes">
          <el-input
            v-model="form.notes"
            type="textarea"
            :rows="3"
            maxlength="2000"
            show-word-limit
            placeholder="其他说明"
          />
        </el-form-item>

        <el-form-item label="面料图片" prop="imageUrls">
          <image-upload
            v-model="form.imageUrls"
            action="/fabric/fabric/upload"
            :limit="8"
            :file-size="10"
            :file-type="['png', 'jpg', 'jpeg']"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" :loading="submitting" @click="submitForm">确 定</el-button>
          <el-button :disabled="submitting" @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog
      v-model="supplierDialogOpen"
      title="快速添加供应商"
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
        <el-form-item label="供应商名称" prop="name">
          <el-input v-model="supplierForm.name" maxlength="150" placeholder="请输入供应商名称" />
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
            placeholder="请输入地址"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" :loading="supplierSaving" @click="submitSupplier">
            保存并选择
          </el-button>
          <el-button @click="supplierDialogOpen = false">取消</el-button>
        </div>
      </template>
    </el-dialog>
    </div>
  </div>
</template>

<script setup name="Fabric">
import { ElMessageBox } from "element-plus"
import useUserStore from "@/store/modules/user"
import AccessoryPanel from "./components/AccessoryPanel.vue"
import {
  addFabric,
  createSupplier,
  delFabric,
  getFabric,
  listCategories,
  listComponents,
  listFabric,
  listSuppliers,
  updateFabric
} from "@/api/fabric/fabric"

const { proxy } = getCurrentInstance()
const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const baseApi = import.meta.env.VITE_APP_BASE_API
const currentYear = new Date().getFullYear()
const yearOptions = Array.from(
  { length: Math.max(currentYear - 2024, 1) },
  (_, index) => 2025 + index
)

const priceUnitOptions = [
  { value: "M", label: "元/M（元/米）" },
  { value: "Y", label: "元/Y（元/码）" },
  { value: "KG", label: "元/KG（元/公斤）" },
  { value: "ROLL", label: "元/卷" }
]

const categoryFormProps = {
  value: "id",
  label: "name",
  children: "children",
  emitPath: false
}

const categoryFilterProps = {
  ...categoryFormProps,
  checkStrictly: true
}

const combinationCodes = {
  PO: "PO",
  CO: "CO",
  VI: "VI",
  SP: "SP",
  LI: "LI",
  PA: "PA",
  PU: "PU",
  OT: "OT",
  "CO+PO": "TC",
  "PO+VI": "TR",
  "CO+SP": "COSP",
  "PO+SP": "POSP",
  "PA+SP": "PASP",
  "CO+LI": "LICO",
  "LI+VI": "LIVI",
  "LI+PO": "LIPO",
  "PO+SP+VI": "TRSP"
}

const activeRecordType = ref(route.query.type === "accessory" ? "accessory" : "fabric")
const loading = ref(false)
const submitting = ref(false)
const showSearch = ref(true)
const fabricList = ref([])
const supplierOptions = ref([])
const categoryOptions = ref([])
const componentOptions = ref([])
const ids = ref([])
const selectedRows = ref([])
const multiple = ref(true)
const total = ref(0)
const open = ref(false)
const title = ref("")
const supplierDialogOpen = ref(false)
const supplierSaving = ref(false)
const supplierForm = reactive({
  name: "",
  phone: "",
  address: ""
})

const supplierRules = {
  name: [{ required: true, message: "供应商名称不能为空", trigger: "blur" }],
  phone: [{ required: true, message: "联系电话不能为空", trigger: "blur" }],
  address: [{ required: true, message: "地址不能为空", trigger: "blur" }]
}

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 20,
    code: undefined,
    productName: undefined,
    supplierId: undefined,
    categoryId: undefined,
    year: currentYear
  },
  form: {},
  rules: {
    categoryId: [{ required: true, message: "请选择面料分类", trigger: "change" }],
    supplierId: [{ required: true, message: "请选择供应商", trigger: "change" }],
    productName: [{ required: true, message: "品名不能为空", trigger: "blur" }],
    weight: [{ required: true, message: "克重不能为空", trigger: "change" }],
    width: [{ required: true, message: "包边门幅不能为空", trigger: "change" }],
    compositions: [{ validator: validateCompositions, trigger: "change" }],
    priceValue: [{ required: true, message: "价格数值不能为空", trigger: "change" }],
    priceUnit: [{ required: true, message: "请选择计价单位", trigger: "change" }],
    imageUrls: [{ required: true, message: "请至少上传一张面料图片", trigger: "change" }]
  }
})

const { queryParams, form, rules } = toRefs(data)

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
const editSelectionDisabled = computed(() =>
  selectedRows.value.length !== 1 || !canEditRow(selectedRows.value[0])
)
const showAccessory = computed(() => activeRecordType.value === "accessory")

function handleRecordTypeChange(type) {
  const isAccessory = type === "accessory"
  router.replace({
    path: isAccessory ? "/fabric/accessory" : "/fabric/list",
    query: isAccessory ? { type: "accessory" } : {}
  })
}

watch(
  () => route.query.type,
  type => {
    activeRecordType.value = type === "accessory" ? "accessory" : "fabric"
  }
)

const compositionTotal = computed(() =>
  (form.value.compositions || []).reduce(
    (sum, item) => sum + (Number(item.percentage) || 0),
    0
  )
)

const derivedCompositionCode = computed(() => deriveCompositionCode(form.value.compositions))

const codePreview = computed(() => {
  if (form.value.code) return form.value.code
  const year = form.value.entryDate
    ? Number(String(form.value.entryDate).slice(0, 4))
    : currentYear
  return `A-${String(year).slice(-2)}????-${derivedCompositionCode.value}`
})

const recorderDisplay = computed(() =>
  form.value.id
    ? (form.value.recorderName || form.value.createBy || currentAccountDisplay())
    : currentAccountDisplay()
)

const meterPricePreview = computed(() => {
  const price = Number(form.value.priceValue)
  if (!price || !form.value.priceUnit) return null
  let value
  if (form.value.priceUnit === "M") {
    value = price
  } else if (form.value.priceUnit === "Y") {
    value = price / 0.9144
  } else if (form.value.priceUnit === "KG") {
    const width = Number(form.value.width)
    const weight = Number(form.value.weight)
    if (!width || !weight) return null
    value = price * (width / 100) * (weight / 1000)
  } else {
    return null
  }
  return value.toFixed(2)
})

function getList() {
  loading.value = true
  listFabric(queryParams.value).then(response => {
    fabricList.value = response.rows
    total.value = response.total
  }).finally(() => {
    loading.value = false
  })
}

function loadDictionaries() {
  return Promise.all([listSuppliers(), listCategories(), listComponents()])
    .then(([supplierRes, categoryRes, componentRes]) => {
      supplierOptions.value = supplierRes.data || []
      categoryOptions.value = categoryRes.data || []
      componentOptions.value = componentRes.data || []
    })
}

function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

function resetQuery() {
  proxy.resetForm("queryRef")
  queryParams.value.year = currentYear
  handleQuery()
}

function handleSelectionChange(selection) {
  selectedRows.value = selection
  ids.value = selection.map(item => item.id)
  multiple.value = selection.length === 0
}

function reset() {
  form.value = {
    id: undefined,
    code: undefined,
    entryDate: localDateString(),
    categoryId: undefined,
    supplierId: undefined,
    productName: undefined,
    weight: undefined,
    width: undefined,
    colorNo: undefined,
    compositions: [{ componentCode: undefined, percentage: 100 }],
    priceValue: undefined,
    priceUnit: "M",
    notes: undefined,
    imageUrls: undefined,
    createBy: undefined
  }
  proxy.resetForm("fabricRef")
}

function handleAdd() {
  reset()
  title.value = "新增面料"
  open.value = true
}

function handleUpdate(row) {
  const target = row?.id ? row : selectedRows.value[0]
  if (!target || !canEditRow(target)) {
    proxy.$modal.msgWarning("录入员只能修改本人录入的面料")
    return
  }
  reset()
  const id = target.id
  getFabric(id).then(response => {
    form.value = response.data
    if (!form.value.compositions?.length) {
      form.value.compositions = [{ componentCode: undefined, percentage: 100 }]
    }
    title.value = `修改面料 ${form.value.code}`
    open.value = true
  })
}

function canEditRow(row) {
  if (!hasEditPermission.value || !row) return false
  return isSuperAdmin.value
    || isFabricManager.value
    || row.createBy === userStore.name
}

function submitForm() {
  if (submitting.value) return
  proxy.$refs.fabricRef.validate(valid => {
    if (!valid) return
    submitting.value = true
    const isEdit = Boolean(form.value.id)
    ElMessageBox.confirm(
      buildQuoteConfirmation(isEdit),
      isEdit ? "修改二次确认" : "新增二次确认",
      {
        confirmButtonText: "确认提交",
        cancelButtonText: "取消",
        type: "warning",
        showClose: false,
        closeOnClickModal: false,
        closeOnPressEscape: false
      }
    ).then(() => persistForm(isEdit)).catch(() => {
      submitting.value = false
    })
  })
}

function persistForm(isEdit) {
  const request = isEdit ? updateFabric(form.value) : addFabric(form.value)
  request.then(() => {
    proxy.$modal.msgSuccess(isEdit ? "修改成功" : "新增成功")
    open.value = false
    getList()
  }).finally(() => {
    submitting.value = false
  })
}

function buildQuoteConfirmation(isEdit) {
  const description = [
    form.value.productName,
    `${formatNumber(form.value.weight)}g`,
    form.value.colorNo
  ].filter(Boolean).join("/")
  return `${isEdit ? "即将修改" : "即将新增"}【${description}】，报价：${formatMoney(form.value.priceValue)} ${priceUnitLabel(form.value.priceUnit)}，录入人：${recorderDisplay.value}。请再次确认品名、规格、成分、供应商和报价均无误。`
}

function handleDelete(row) {
  const fabricIds = row.id || ids.value
  proxy.$modal.confirm("只能删除从未关联库存的面料档案；已关联档案会由系统拒绝删除。是否继续？").then(() => {
    return delFabric(fabricIds)
  }).then(() => {
    proxy.$modal.msgSuccess("删除成功")
    getList()
  }).catch(() => {})
}

function openInventory(row) {
  router.push({ path: "/inventory/stock", query: { fabricId: row.id } })
}

function cancel() {
  open.value = false
  reset()
}

function addComposition() {
  if ((form.value.compositions || []).length >= 4) return
  form.value.compositions.push({ componentCode: undefined, percentage: undefined })
}

function removeComposition(index) {
  if (index === 0) return
  form.value.compositions.splice(index, 1)
}

function isComponentUsed(code, currentIndex) {
  return (form.value.compositions || []).some(
    (item, index) => index !== currentIndex && item.componentCode === code
  )
}

function validateCompositions(rule, value, callback) {
  const rows = value || []
  if (!rows.length) {
    callback(new Error("成分不能为空"))
    return
  }
  if (rows.length > 4) {
    callback(new Error("成分最多填写4项"))
    return
  }
  if (rows.some(item => !item.componentCode || item.percentage == null)) {
    callback(new Error("请完整填写每一项成分及比例"))
    return
  }
  const codes = rows.map(item => item.componentCode)
  if (new Set(codes).size !== codes.length) {
    callback(new Error("同一种成分不能重复填写"))
    return
  }
  if (rows.some(item => Number(item.percentage) < 1 || Number(item.percentage) > 100)) {
    callback(new Error("成分比例必须在1%到100%之间"))
    return
  }
  if (Math.abs(compositionTotal.value - 100) > 0.0001) {
    callback(new Error("成分配比合计需等于100%"))
    return
  }
  callback()
}

function deriveCompositionCode(rows) {
  const codes = (rows || [])
    .map(item => item.componentCode)
    .filter(Boolean)
  if (!codes.length) return "OT"
  const key = [...new Set(codes)].sort().join("+")
  return combinationCodes[key] || "OT"
}

function openSupplierDialog() {
  supplierForm.name = ""
  supplierForm.phone = ""
  supplierForm.address = ""
  supplierDialogOpen.value = true
  nextTick(() => proxy.resetForm("supplierRef"))
}

function submitSupplier() {
  proxy.$refs.supplierRef.validate(valid => {
    if (!valid) return
    supplierSaving.value = true
    createSupplier(supplierForm).then(response => {
      const supplier = response.data
      const existingIndex = supplierOptions.value.findIndex(item => item.id === supplier.id)
      if (existingIndex < 0) {
        supplierOptions.value.push(supplier)
        supplierOptions.value.sort((a, b) => a.name.localeCompare(b.name, "zh-CN"))
        proxy.$modal.msgSuccess("供应商添加成功")
      } else {
        proxy.$modal.msgSuccess("供应商已存在，已自动选择")
      }
      form.value.supplierId = supplier.id
      supplierDialogOpen.value = false
    }).finally(() => {
      supplierSaving.value = false
    })
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

function formatPrice(value, unit) {
  if (value == null) return "-"
  return `${formatMoney(value)} ${priceUnitLabel(unit)}`
}

function priceUnitLabel(unit) {
  return priceUnitOptions.find(item => item.value === unit)?.label.split("（")[0] || unit || ""
}

function formatNumber(value) {
  if (value == null || value === "") return "-"
  return Number(value).toString()
}

function formatMoney(value) {
  if (value == null || value === "") return "-"
  return Number(value).toFixed(2)
}

function currentAccountDisplay() {
  const account = userStore.name || ""
  const nickname = userStore.nickName || ""
  return nickname && nickname !== account
    ? `${nickname}（${account}）`
    : (account || nickname)
}

function localDateString() {
  const date = new Date()
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, "0")
  const day = String(date.getDate()).padStart(2, "0")
  return `${year}-${month}-${day}`
}

loadDictionaries()
getList()
</script>

<style scoped>
.record-type-tabs {
  margin-bottom: 14px;
}

.relation-tip {
  margin-bottom: 16px;
}

.no-stock {
  color: #a8abb2;
  font-size: 12px;
}

.form-section-title {
  margin: 4px 0 16px;
  padding: 9px 13px;
  border-left: 4px solid #0f766e;
  border-radius: 4px;
  color: #0f5f5c;
  background: #f0fdfa;
  font-size: 14px;
  font-weight: 700;
}

.fabric-thumbnail {
  width: 50px;
  height: 50px;
  display: block;
  margin: 0 auto;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  overflow: hidden;
  cursor: zoom-in;
  background: #f5f7fa;
}

.no-image {
  color: #a8abb2;
  font-size: 12px;
}

.supplier-picker {
  display: flex;
  width: 100%;
  gap: 8px;
}

.field-unit {
  position: absolute;
  right: 42px;
  color: #909399;
  pointer-events: none;
}

.composition-editor {
  width: 100%;
  padding: 12px;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  background: #fafafa;
}

.composition-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.composition-select {
  width: 360px;
}

.percentage-input {
  width: 180px;
}

.percent-sign {
  margin-left: -4px;
  color: #606266;
}

.composition-actions {
  display: flex;
  align-items: center;
  gap: 14px;
}

.composition-total {
  color: #67c23a;
  font-weight: 600;
}

.composition-total.invalid {
  color: #f56c6c;
}

</style>
