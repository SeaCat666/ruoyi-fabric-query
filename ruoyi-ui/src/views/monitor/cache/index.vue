<template>
  <div class="app-container cache-page" v-loading="loading">
    <section class="cache-hero">
      <div>
        <div class="hero-eyebrow">SYSTEM CACHE</div>
        <h2>缓存运行状态</h2>
        <p>
          当前使用
          <strong>{{ cache.storageLabel || "缓存服务" }}</strong>
          ，{{ storageDescription }}
        </p>
      </div>
      <el-tag :type="cache.storageType === 'redis' ? 'success' : 'warning'" effect="dark" round>
        {{ cache.storageType === "redis" ? "Redis 已连接" : "开发模式" }}
      </el-tag>
    </section>

    <section class="metric-grid">
      <article class="metric-card">
        <div class="metric-icon metric-icon--blue"><Collection /></div>
        <div>
          <span>存储方式</span>
          <strong>{{ cache.storageLabel || "—" }}</strong>
          <small>{{ cache.storageType === "redis" ? cache.info?.redis_version : "无需安装 Redis" }}</small>
        </div>
      </article>
      <article class="metric-card">
        <div class="metric-icon metric-icon--green"><Key /></div>
        <div>
          <span>有效 Key</span>
          <strong>{{ cache.dbSize ?? 0 }}</strong>
          <small>已自动排除过期缓存</small>
        </div>
      </article>
      <article class="metric-card">
        <div class="metric-icon metric-icon--orange"><Cpu /></div>
        <div>
          <span>内存使用</span>
          <strong>{{ cache.info?.used_memory_human || "—" }}</strong>
          <small>上限 {{ cache.info?.maxmemory_human || "—" }}</small>
        </div>
      </article>
      <article class="metric-card">
        <div class="metric-icon metric-icon--purple"><Timer /></div>
        <div>
          <span>运行时间</span>
          <strong>{{ cache.info?.uptime_in_days ?? 0 }} 天</strong>
          <small>{{ cache.info?.connected_clients ?? 0 }} 个活动客户端</small>
        </div>
      </article>
    </section>

    <el-row :gutter="18">
      <el-col :xs="24" :lg="14">
        <el-card class="chart-card" shadow="never">
          <template #header>
            <div class="card-heading">
              <div>
                <strong>{{ distributionTitle }}</strong>
                <span>{{ distributionSubtitle }}</span>
              </div>
              <el-button text icon="Refresh" @click="getList">刷新</el-button>
            </div>
          </template>
          <div ref="distributionChart" class="chart" />
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="10">
        <el-card class="chart-card" shadow="never">
          <template #header>
            <div class="card-heading">
              <div>
                <strong>内存占用</strong>
                <span>当前缓存进程的内存使用比例</span>
              </div>
            </div>
          </template>
          <div ref="memoryChart" class="chart" />
        </el-card>
      </el-col>
    </el-row>

    <el-alert
      v-if="cache.storageType === 'local'"
      class="local-tip"
      title="当前为开发环境进程内缓存"
      type="info"
      :closable="false"
      show-icon
      description="验证码、登录令牌、系统参数和数据字典均可正常使用；应用重启后缓存会自动重建，不需要单独安装 Redis。"
    />
  </div>
</template>

<script setup name="Cache">
import * as echarts from "echarts"
import { getCache } from "@/api/monitor/cache"

const { proxy } = getCurrentInstance()
const loading = ref(false)
const cache = ref({
  storageType: "",
  storageLabel: "",
  info: {},
  dbSize: 0,
  commandStats: [],
  memoryUsedBytes: 0,
  memoryMaxBytes: 0
})
const distributionChart = ref(null)
const memoryChart = ref(null)
let distributionInstance
let memoryInstance

const storageDescription = computed(() =>
  cache.value.storageType === "redis"
    ? "缓存数据由独立 Redis 服务持久维护。"
    : "缓存随当前后端进程运行，适合免安装数据库和 Redis 的开发环境。"
)

const distributionTitle = computed(() =>
  cache.value.storageType === "redis" ? "命令统计" : "缓存内容分布"
)

const distributionSubtitle = computed(() =>
  cache.value.storageType === "redis"
    ? "Redis 命令调用占比"
    : "按登录、参数、字典等业务用途统计"
)

function getList() {
  loading.value = true
  getCache().then(response => {
    cache.value = response.data || cache.value
    nextTick(renderCharts)
  }).finally(() => {
    loading.value = false
  })
}

function renderCharts() {
  if (!distributionChart.value || !memoryChart.value) return
  distributionInstance?.dispose()
  memoryInstance?.dispose()

  distributionInstance = echarts.init(distributionChart.value, "macarons")
  distributionInstance.setOption({
    color: ["#0f766e", "#2563eb", "#f59e0b", "#7c3aed", "#dc2626", "#0891b2"],
    tooltip: { trigger: "item", formatter: "{b}<br/>{c} 个（{d}%）" },
    legend: { bottom: 8, icon: "circle" },
    series: [{
      name: distributionTitle.value,
      type: "pie",
      radius: ["42%", "68%"],
      center: ["50%", "44%"],
      padAngle: 3,
      itemStyle: { borderRadius: 8 },
      label: { formatter: "{b}\n{c}" },
      data: cache.value.commandStats?.length
        ? cache.value.commandStats
        : [{ name: "暂无缓存", value: 1, itemStyle: { color: "#d1d5db" } }]
    }]
  })

  const used = Number(cache.value.memoryUsedBytes || 0)
  const maximum = Math.max(Number(cache.value.memoryMaxBytes || 0), used, 1)
  const percent = Number(((used / maximum) * 100).toFixed(1))
  memoryInstance = echarts.init(memoryChart.value, "macarons")
  memoryInstance.setOption({
    series: [{
      type: "gauge",
      startAngle: 210,
      endAngle: -30,
      min: 0,
      max: 100,
      splitNumber: 5,
      progress: { show: true, width: 16, roundCap: true },
      axisLine: { lineStyle: { width: 16, color: [[1, "#e5e7eb"]] } },
      axisTick: { show: false },
      splitLine: { show: false },
      axisLabel: { distance: 22, color: "#64748b" },
      pointer: { show: false },
      anchor: { show: false },
      title: { offsetCenter: [0, "32%"], color: "#64748b" },
      detail: {
        valueAnimation: true,
        formatter: "{value}%",
        offsetCenter: [0, "-2%"],
        fontSize: 30,
        fontWeight: 700,
        color: "#0f766e"
      },
      data: [{ value: percent, name: cache.value.info?.used_memory_human || "0 MB" }]
    }]
  })
}

function resizeCharts() {
  distributionInstance?.resize()
  memoryInstance?.resize()
}

onMounted(() => {
  getList()
  window.addEventListener("resize", resizeCharts)
})

onBeforeUnmount(() => {
  window.removeEventListener("resize", resizeCharts)
  distributionInstance?.dispose()
  memoryInstance?.dispose()
})
</script>

<style scoped lang="scss">
.cache-page {
  min-height: calc(100vh - 84px);
  background: #f5f7fb;
}

.cache-hero {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  padding: 26px 30px;
  margin-bottom: 18px;
  color: #fff;
  border-radius: 18px;
  background:
    radial-gradient(circle at 85% 15%, rgba(45, 212, 191, 0.22), transparent 32%),
    linear-gradient(135deg, #0f172a 0%, #164e63 100%);

  h2 {
    margin: 4px 0 8px;
    font-size: 28px;
  }

  p {
    margin: 0;
    color: #cbd5e1;
  }
}

.hero-eyebrow {
  color: #5eead4;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.16em;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 18px;
}

.metric-card {
  display: flex;
  gap: 14px;
  align-items: center;
  min-height: 112px;
  padding: 20px;
  background: #fff;
  border: 1px solid #e8edf4;
  border-radius: 14px;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.04);

  span,
  small {
    display: block;
    color: #64748b;
  }

  strong {
    display: block;
    margin: 4px 0;
    color: #0f172a;
    font-size: 21px;
  }
}

.metric-icon {
  display: grid;
  flex: 0 0 46px;
  width: 46px;
  height: 46px;
  place-items: center;
  border-radius: 13px;

  svg {
    width: 22px;
  }
}

.metric-icon--blue { color: #2563eb; background: #dbeafe; }
.metric-icon--green { color: #059669; background: #d1fae5; }
.metric-icon--orange { color: #d97706; background: #fef3c7; }
.metric-icon--purple { color: #7c3aed; background: #ede9fe; }

.chart-card {
  margin-bottom: 18px;
  border: 1px solid #e8edf4;
  border-radius: 14px;
}

.card-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;

  strong,
  span {
    display: block;
  }

  strong {
    color: #0f172a;
    font-size: 16px;
  }

  span {
    margin-top: 4px;
    color: #94a3b8;
    font-size: 12px;
  }
}

.chart {
  height: 340px;
}

.local-tip {
  border-radius: 12px;
}

@media (max-width: 1100px) {
  .metric-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .cache-hero {
    padding: 22px;
  }

  .metric-grid {
    grid-template-columns: 1fr;
  }
}
</style>
