<template>
  <div class="fabric-dashboard" v-loading="loading">
    <section class="welcome-panel">
      <div class="welcome-copy">
        <div class="eyebrow">FABRIC LIBRARY · {{ today }}</div>
        <h1>{{ greeting }}，{{ userStore.nickName || userStore.name }}</h1>
        <p>
          欢迎回到面料查询系统。这里汇总面料与辅料档案、图片覆盖和供应资源，
          帮你快速掌握当前资料库状态。
        </p>
        <div class="welcome-actions">
          <el-button v-if="canViewFabric" type="primary" size="large" @click="goFabric">
            进入面料档案
            <el-icon class="el-icon--right"><ArrowRight /></el-icon>
          </el-button>
          <el-button v-if="canViewFabric" size="large" plain @click="goAccessory">
            进入辅料档案
          </el-button>
          <el-button v-if="isAdmin" size="large" plain @click="goUsers">
            账号权限管理
          </el-button>
          <el-button v-if="canManageMaster" size="large" plain @click="goMaster">
            基础资料维护
          </el-button>
          <el-button size="large" plain @click="goHelp">
            使用帮助
          </el-button>
        </div>
      </div>
      <div class="welcome-side">
        <div class="role-badge">{{ roleLabel }}</div>
        <div class="year-mark">
          <span>{{ dashboard.currentYear }}</span>
          <small>当前业务年度</small>
        </div>
        <div class="decor decor--one" />
        <div class="decor decor--two" />
      </div>
    </section>

    <section class="summary-grid">
      <article class="summary-card">
        <div class="summary-top">
          <span class="summary-icon summary-icon--navy"><Files /></span>
          <span class="summary-trend">全部档案</span>
        </div>
        <strong>{{ numberFormat(dashboard.summary.totalRecords) }}</strong>
        <h3>档案总数</h3>
        <p>面料 {{ dashboard.summary.totalFabrics }} · 辅料 {{ dashboard.summary.totalAccessories }}</p>
      </article>
      <article class="summary-card">
        <div class="summary-top">
          <span class="summary-icon summary-icon--teal"><Calendar /></span>
          <span class="summary-trend">{{ dashboard.currentYear }} 年</span>
        </div>
        <strong>{{ numberFormat(currentYearRecords) }}</strong>
        <h3>本年度档案</h3>
        <p>面料 {{ dashboard.summary.currentYearFabrics }} · 辅料 {{ dashboard.summary.currentYearAccessories }}</p>
      </article>
      <article class="summary-card">
        <div class="summary-top">
          <span class="summary-icon summary-icon--amber"><OfficeBuilding /></span>
          <span class="summary-trend">合作资源</span>
        </div>
        <strong>{{ numberFormat(totalSuppliers) }}</strong>
        <h3>供应商档案</h3>
        <p>面料 {{ dashboard.summary.fabricSupplierCount }} · 辅料 {{ dashboard.summary.accessorySupplierCount }}</p>
      </article>
      <article class="summary-card">
        <div class="summary-top">
          <span class="summary-icon summary-icon--violet"><Picture /></span>
          <span class="summary-trend">实物图片</span>
        </div>
        <strong>{{ numberFormat(totalImages) }}</strong>
        <h3>实物图片</h3>
        <p>面料 {{ dashboard.summary.imageCount }} · 辅料 {{ dashboard.summary.accessoryImageCount }}</p>
      </article>
    </section>

    <section class="insight-grid">
      <el-card class="insight-card category-card" shadow="never">
        <template #header>
          <div class="section-heading">
            <span>面料分类占比</span>
            <el-tag type="info" effect="plain">共 {{ dashboard.summary.totalFabrics }} 款</el-tag>
          </div>
        </template>
        <div v-if="dashboard.categoryStats.length" class="pie-wrap">
          <div ref="pieRef" class="pie-chart" />
          <div class="pie-legend">
            <div
              v-for="(item, index) in dashboard.categoryStats"
              :key="item.name"
              class="legend-item"
            >
              <i :style="{ background: progressColors[index % progressColors.length] }" />
              <span class="legend-name">{{ item.name || '未分类' }}</span>
              <strong>{{ item.value }}</strong>
            </div>
          </div>
        </div>
        <el-empty v-else description="暂无分类数据" :image-size="80" />
      </el-card>

      <el-card class="insight-card years-card" shadow="never">
        <template #header>
          <div class="section-heading">
            <span>年度与计价分布</span>
          </div>
        </template>
        <div class="year-list">
          <div
            v-for="item in combinedYearStats"
            :key="item.name"
            class="year-item"
          >
            <strong class="year-count">{{ item.value }}<small> 款</small></strong>
            <span class="year-title">{{ item.name }}</span>
            <small class="year-detail">面料 {{ item.fabricValue }} · 辅料 {{ item.accessoryValue }}</small>
          </div>
        </div>
        <div class="unit-block">
          <div class="unit-title">计价单位</div>
          <div class="unit-list">
            <span v-for="item in dashboard.priceUnitStats" :key="item.name">
              {{ unitLabel(item.name) }} <strong>{{ item.value }}</strong>
            </span>
          </div>
        </div>
      </el-card>
    </section>

    <el-card class="recent-card" shadow="never">
      <template #header>
        <div class="section-heading">
          <div>
            <span>最近录入</span>
            <small>分别查看最新面料与辅料档案</small>
          </div>
          <el-button v-if="canViewFabric" text type="primary" @click="goRecentArchive">
            查看全部
            <el-icon class="el-icon--right"><ArrowRight /></el-icon>
          </el-button>
        </div>
      </template>
      <el-tabs v-model="recentType" class="recent-tabs">
        <el-tab-pane label="最近面料" name="fabric" />
        <el-tab-pane label="最近辅料" name="accessory" />
      </el-tabs>
      <el-table v-if="recentType === 'fabric'" :data="dashboard.recentFabrics" class="recent-table">
        <el-table-column label="实物图" width="76">
          <template #default="{ row }">
            <el-image
              v-if="row.images?.length"
              class="recent-image"
              :src="imageSrc(row.images[0].thumbnailUrl || row.images[0].imageUrl)"
              :preview-src-list="row.images.map(item => imageSrc(item.imageUrl))"
              fit="cover"
              preview-teleported
            />
            <span v-else class="image-placeholder"><Picture /></span>
          </template>
        </el-table-column>
        <el-table-column label="编号" prop="code" min-width="145" />
        <el-table-column label="品名" prop="productName" min-width="130" show-overflow-tooltip />
        <el-table-column label="分类" prop="categoryName" min-width="150" show-overflow-tooltip />
        <el-table-column label="供应商" prop="supplierName" min-width="130" show-overflow-tooltip />
        <el-table-column label="成分" prop="compositionSummary" min-width="190" show-overflow-tooltip />
        <el-table-column label="录入日期" prop="entryDate" width="110" />
        <el-table-column label="录入员" prop="createBy" width="110" />
      </el-table>
      <el-table v-else :data="dashboard.recentAccessories" class="recent-table">
        <el-table-column label="实物图" width="76">
          <template #default="{ row }">
            <el-image
              v-if="row.images?.length"
              class="recent-image"
              :src="imageSrc(row.images[0].thumbnailUrl || row.images[0].imageUrl)"
              :preview-src-list="row.images.map(item => imageSrc(item.imageUrl || item.thumbnailUrl))"
              fit="cover"
              preview-teleported
            />
            <span v-else class="image-placeholder"><Picture /></span>
          </template>
        </el-table-column>
        <el-table-column label="辅料编号" prop="code" min-width="125" />
        <el-table-column label="年份" prop="year" width="85" />
        <el-table-column label="辅料供应商" prop="supplierName" min-width="150" show-overflow-tooltip />
        <el-table-column label="电话" prop="supplierPhone" width="130" />
        <el-table-column label="尺寸" prop="sizeSpec" min-width="130" show-overflow-tooltip />
        <el-table-column label="大货价" prop="bulkPrice" min-width="120" show-overflow-tooltip />
        <el-table-column label="常规使用" width="100" align="center">
          <template #default="{ row }">{{ statusLabel(row.regularUse) }}</template>
        </el-table-column>
        <el-table-column label="是否合规" width="100" align="center">
          <template #default="{ row }">{{ statusLabel(row.compliant) }}</template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup name="Index">
import * as echarts from "echarts"
import useUserStore from "@/store/modules/user"
import { getFabricDashboard } from "@/api/fabric/fabric"

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const recentType = ref("fabric")
const baseApi = import.meta.env.VITE_APP_BASE_API
const pieRef = ref(null)
const progressColors = ["#0f766e", "#2563eb", "#d97706", "#7c3aed"]

const dashboard = reactive({
  currentYear: new Date().getFullYear(),
  summary: {
    totalFabrics: 0,
    totalAccessories: 0,
    totalRecords: 0,
    currentYearFabrics: 0,
    currentYearAccessories: 0,
    supplierCount: 0,
    fabricSupplierCount: 0,
    accessorySupplierCount: 0,
    imageCount: 0,
    accessoryImageCount: 0,
    imageCoverage: 0,
    accessoryImageCoverage: 0
  },
  yearStats: [],
  accessoryYearStats: [],
  categoryStats: [],
  priceUnitStats: [],
  recentFabrics: [],
  recentAccessories: []
})

const currentYearRecords = computed(() =>
  Number(dashboard.summary.currentYearFabrics || 0)
  + Number(dashboard.summary.currentYearAccessories || 0)
)

const totalSuppliers = computed(() =>
  Number(dashboard.summary.fabricSupplierCount || 0)
  + Number(dashboard.summary.accessorySupplierCount || 0)
)

const totalImages = computed(() =>
  Number(dashboard.summary.imageCount || 0)
  + Number(dashboard.summary.accessoryImageCount || 0)
)

const combinedYearStats = computed(() => {
  const years = new Map()
  dashboard.yearStats.forEach(item => {
    years.set(String(item.name), {
      name: String(item.name),
      fabricValue: Number(item.value || 0),
      accessoryValue: 0
    })
  })
  dashboard.accessoryYearStats.forEach(item => {
    const key = String(item.name)
    const current = years.get(key) || {
      name: key,
      fabricValue: 0,
      accessoryValue: 0
    }
    current.accessoryValue = Number(item.value || 0)
    years.set(key, current)
  })
  return Array.from(years.values())
    .map(item => ({ ...item, value: item.fabricValue + item.accessoryValue }))
    .sort((left, right) => Number(left.name) - Number(right.name))
})

const isAdmin = computed(() => userStore.roles.includes("admin"))
const canViewFabric = computed(() =>
  userStore.permissions.includes("*:*:*")
  || userStore.permissions.includes("fabric:fabric:list")
)
const canManageMaster = computed(() =>
  userStore.permissions.includes("*:*:*")
  || userStore.permissions.includes("fabric:master:list")
)

const today = computed(() =>
  new Intl.DateTimeFormat("zh-CN", {
    year: "numeric",
    month: "long",
    day: "numeric",
    weekday: "long"
  }).format(new Date())
)

const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 6) return "夜深了"
  if (hour < 11) return "早上好"
  if (hour < 14) return "中午好"
  if (hour < 18) return "下午好"
  return "晚上好"
})

const roleLabel = computed(() => {
  if (userStore.roles.includes("admin")) return "系统管理员"
  if (userStore.roles.includes("fabric_manager")) return "面料主管"
  if (userStore.roles.includes("fabric_entry")) return "面料录入员"
  if (userStore.roles.includes("fabric_view")) return "面料查询员"
  return "系统用户"
})

const roleTip = computed(() => {
  if (userStore.roles.includes("fabric_entry")) {
    return "你可以查看全部面料，并新增或修改本人录入的数据。"
  }
  if (userStore.roles.includes("fabric_view")) {
    return "当前账号为只读权限，可查询档案和浏览全部实物图。"
  }
  if (userStore.roles.includes("fabric_manager")) {
    return "你可以维护全部面料档案、供应商资料和实物图片。"
  }
  return "系统管理员可以配置账号权限，并维护全部面料档案。"
})

function loadDashboard() {
  loading.value = true
  getFabricDashboard().then(response => {
    const data = response.data || {}
    dashboard.currentYear = data.currentYear || dashboard.currentYear
    Object.assign(dashboard.summary, data.summary || {})
    dashboard.yearStats = data.yearStats || []
    dashboard.accessoryYearStats = data.accessoryYearStats || []
    dashboard.categoryStats = data.categoryStats || []
    dashboard.priceUnitStats = data.priceUnitStats || []
    dashboard.recentFabrics = data.recentFabrics || []
    dashboard.recentAccessories = data.recentAccessories || []
  }).finally(() => {
    loading.value = false
    nextTick(renderPie)
  })
}

function categoryPercent(value) {
  const total = dashboard.categoryStats.reduce((sum, item) => sum + Number(item.value || 0), 0)
  if (!total) return 0
  return Math.round((Number(value || 0) / total) * 100)
}

let pieInstance = null
function renderPie() {
  if (!pieRef.value || !dashboard.categoryStats.length) return
  if (pieInstance) pieInstance.dispose()
  pieInstance = echarts.init(pieRef.value)
  pieInstance.setOption({
    tooltip: {
      trigger: "item",
      backgroundColor: "#fff",
      borderColor: "#e2e8f0",
      textStyle: { color: "#1e293b", fontSize: 13 },
      formatter: function (params) {
        return `<b>${params.name}</b><br/>${params.value} 款 · ${params.percent}%`
      }
    },
    series: [{
      type: "pie",
      radius: ["55%", "80%"],
      center: ["50%", "50%"],
      avoidLabelOverlap: false,
      itemStyle: { borderRadius: 4, borderColor: "#fff", borderWidth: 2 },
      label: { show: false },
      labelLine: { show: false },
      emphasis: {
        label: { show: false },
        scaleSize: 8
      },
      data: dashboard.categoryStats.map((item, i) => ({
        name: item.name || "未分类",
        value: Number(item.value || 0),
        itemStyle: { color: progressColors[i % progressColors.length] }
      }))
    }]
  })
}

function numberFormat(value) {
  return new Intl.NumberFormat("zh-CN").format(Number(value || 0))
}

function unitLabel(unit) {
  return {
    M: "元/米",
    Y: "元/码",
    KG: "元/公斤",
    ROLL: "元/卷"
  }[unit] || unit
}

function imageSrc(url) {
  if (!url) return ""
  return /^https?:\/\//i.test(url) ? url : baseApi + url
}

function goFabric() {
  router.push("/fabric/list")
}

function goAccessory() {
  router.push({ path: "/fabric/accessory", query: { type: "accessory" } })
}

function goRecentArchive() {
  if (recentType.value === "accessory") {
    goAccessory()
    return
  }
  goFabric()
}

function statusLabel(value) {
  return value === "1" ? "是" : value === "0" ? "否" : "未设置"
}

function goUsers() {
  router.push("/system/user")
}

function goMaster() {
  router.push("/fabric/master")
}

function goHelp() {
  router.push("/help")
}

onMounted(loadDashboard)
</script>

<style scoped lang="scss">
.fabric-dashboard {
  min-height: calc(100vh - 84px);
  padding: 22px;
  background: #f4f6f9;
}

.welcome-panel {
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 1.6fr) minmax(260px, 0.7fr);
  min-height: 250px;
  margin-bottom: 18px;
  overflow: hidden;
  color: #fff;
  border-radius: 22px;
  background:
    radial-gradient(circle at 78% 20%, rgba(45, 212, 191, 0.22), transparent 28%),
    linear-gradient(125deg, #0f172a 0%, #123b48 54%, #0f766e 120%);
  box-shadow: 0 18px 45px rgba(15, 23, 42, 0.16);
}

.welcome-copy {
  z-index: 1;
  padding: 38px 42px;

  h1 {
    margin: 8px 0 12px;
    font-size: clamp(29px, 3vw, 42px);
    line-height: 1.2;
  }

  p {
    max-width: 670px;
    margin: 0;
    color: #cbd5e1;
    font-size: 15px;
    line-height: 1.8;
  }
}

.eyebrow {
  color: #5eead4;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.1em;
}

.welcome-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 24px;
}

.recent-tabs {
  margin-top: -8px;
}

.welcome-side {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  justify-content: space-between;
  padding: 32px 36px;
}

.role-badge {
  z-index: 1;
  padding: 7px 14px;
  color: #ccfbf1;
  font-size: 13px;
  border: 1px solid rgba(153, 246, 228, 0.35);
  border-radius: 999px;
  background: rgba(15, 118, 110, 0.24);
  backdrop-filter: blur(8px);
}

.year-mark {
  z-index: 1;
  text-align: right;

  span,
  small {
    display: block;
  }

  span {
    color: rgba(255, 255, 255, 0.94);
    font-size: 58px;
    font-weight: 750;
    letter-spacing: -0.05em;
  }

  small {
    color: #99f6e4;
    letter-spacing: 0.08em;
  }
}

.decor {
  position: absolute;
  border: 1px solid rgba(153, 246, 228, 0.18);
  border-radius: 50%;
}

.decor--one {
  right: -48px;
  bottom: -80px;
  width: 260px;
  height: 260px;
}

.decor--two {
  right: 76px;
  bottom: -72px;
  width: 180px;
  height: 180px;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 18px;
}

.summary-card {
  padding: 20px;
  background: #fff;
  border: 1px solid #e7ebf0;
  border-radius: 16px;
  box-shadow: 0 8px 22px rgba(15, 23, 42, 0.035);

  > strong {
    display: block;
    margin-top: 18px;
    color: #0f172a;
    font-size: 34px;
    line-height: 1;
  }

  h3 {
    margin: 8px 0 5px;
    color: #1e293b;
    font-size: 15px;
  }

  p {
    margin: 0;
    color: #94a3b8;
    font-size: 12px;
  }
}

.summary-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.summary-icon {
  display: grid;
  width: 42px;
  height: 42px;
  place-items: center;
  border-radius: 12px;

  svg {
    width: 21px;
  }
}

.summary-icon--navy { color: #2563eb; background: #dbeafe; }
.summary-icon--teal { color: #0f766e; background: #ccfbf1; }
.summary-icon--amber { color: #d97706; background: #fef3c7; }
.summary-icon--violet { color: #7c3aed; background: #ede9fe; }

.summary-trend {
  padding: 4px 9px;
  color: #64748b;
  font-size: 11px;
  border-radius: 999px;
  background: #f1f5f9;
}

.insight-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-bottom: 16px;
}

.insight-card,
.recent-card {
  border: 1px solid #e7ebf0;
  border-radius: 16px;
}

.section-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;

  span {
    color: #0f172a;
    font-size: 15px;
    font-weight: 650;
  }
}

.pie-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 14px;
  padding: 4px 0 8px;
}

.pie-chart {
  width: 180px;
  height: 180px;
}

.pie-legend {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 8px 20px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #475569;

  i {
    width: 9px;
    height: 9px;
    border-radius: 2px;
    flex: 0 0 auto;
  }

  strong {
    color: #0f172a;
    margin-left: 2px;
  }
}

.year-list {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.year-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 14px 16px;
  border: 1px solid #e7edf3;
  border-radius: 12px;
  background: #f8fafc;
}

.year-count {
  color: #0f766e;
  font-size: 24px;
  line-height: 1;

  small {
    font-size: 12px;
    color: #64748b;
    font-weight: 400;
  }
}

.year-title {
  color: #0f172a;
  font-size: 14px;
  font-weight: 600;
}

.year-detail {
  color: #94a3b8;
  font-size: 11px;
}

.unit-block {
  margin-top: 14px;
  padding-top: 14px;
  border-top: 1px solid #eef2f6;
}

.unit-title {
  margin-bottom: 8px;
  color: #94a3b8;
  font-size: 12px;
}

.unit-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;

  span {
    padding: 5px 9px;
    color: #475569;
    font-size: 12px;
    border-radius: 7px;
    background: #f1f5f9;
  }

  strong {
    margin-left: 4px;
    color: #0f172a;
  }
}

.recent-image,
.image-placeholder {
  width: 44px;
  height: 44px;
  border-radius: 9px;
}

.recent-image {
  display: block;
}

.image-placeholder {
  display: grid;
  color: #94a3b8;
  place-items: center;
  background: #eef2f6;

  svg {
    width: 19px;
  }
}

:deep(.recent-table .el-table__header th) {
  color: #64748b;
  font-weight: 600;
  background: #f8fafc;
}

@media (max-width: 1200px) {
  .summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .insight-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .fabric-dashboard {
    padding: 14px;
  }

  .welcome-panel {
    grid-template-columns: 1fr;
  }

  .welcome-copy {
    padding: 28px 24px 14px;
  }

  .welcome-side {
    min-height: 120px;
    padding: 12px 24px 24px;
    align-items: flex-start;
  }

  .year-mark {
    text-align: left;

    span {
      font-size: 40px;
    }
  }

  .summary-grid,
  .year-list {
    grid-template-columns: 1fr;
  }
}
</style>
