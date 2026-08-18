<template>
  <div class="help-page">
    <section class="help-hero">
      <div>
        <span class="eyebrow">QUICK GUIDE</span>
        <h1>面料查询系统使用帮助</h1>
        <p>面向业务、跟单、采购、生产等岗位的简明操作说明。</p>
      </div>
      <div class="hero-role">
        <span>当前身份</span>
        <strong>{{ roleLabel }}</strong>
      </div>
    </section>

    <section class="quick-grid">
      <article class="quick-card">
        <span class="step-number">01</span>
        <h2>查找面料</h2>
        <p>进入“档案管理 → 面料档案”或“辅料档案”，再按年份及其他条件筛选。</p>
      </article>
      <article class="quick-card">
        <span class="step-number">02</span>
        <h2>查看实物图</h2>
        <p>面料和辅料都支持多图；点击列表缩略图即可放大并左右切换。</p>
      </article>
      <article class="quick-card">
        <span class="step-number">03</span>
        <h2>录入与维护</h2>
        <p>有录入权限的账号可新增面料或辅料；两类编号均由系统自动生成。</p>
      </article>
      <article class="quick-card">
        <span class="step-number">04</span>
        <h2>库存业务</h2>
        <p>档案确认后建立库存批次，再通过入库、锁定、发料和退回形成完整流水。</p>
      </article>
    </section>

    <div class="content-grid">
      <el-card class="guide-card" shadow="never">
        <template #header>
          <div class="card-heading">
            <el-icon><Tickets /></el-icon>
            <span>编号与录入规则</span>
          </div>
        </template>
        <div class="code-example">
          <strong>A-260300-TRSP</strong>
          <span>类型 - 年份 - 年度流水 - 成分代码</span>
        </div>
        <div class="code-example accessory-code-example">
          <strong>B-260001</strong>
          <span>辅料类型 - 年份 - 年度流水</span>
        </div>
        <ul class="rule-list">
          <li>四位流水号每年从 0001 开始连续递增，不按天重置。</li>
          <li>同一天新增多条数据时，系统会并发锁定流水号，避免重复。</li>
          <li>入库日期以服务器当天日期为准，新增时无需手工填写。</li>
          <li>成分代码根据多行成分自动推导，配比合计必须为 100%。</li>
          <li>“元/卷”保留原始卷价，不自动折算为米价。</li>
        </ul>
      </el-card>

      <el-card class="guide-card" shadow="never">
        <template #header>
          <div class="card-heading">
            <el-icon><Key /></el-icon>
            <span>角色权限</span>
          </div>
        </template>
        <div class="role-list">
          <div>
            <strong>系统管理员</strong>
            <span>维护账号、权限、部门、岗位及全部面料、辅料数据。</span>
          </div>
          <div>
            <strong>面料主管</strong>
            <span>维护全部面料、辅料、分类、两类供应商和实物图片。</span>
          </div>
          <div>
            <strong>面料录入员</strong>
            <span>查询全部面料，新增并修改本人录入的数据。</span>
          </div>
          <div>
            <strong>面料查询员</strong>
            <span>查询面料详情和浏览实物图片，不可修改数据。</span>
          </div>
        </div>
      </el-card>
    </div>

    <el-card class="guide-card relation-card" shadow="never">
      <template #header>
        <div class="card-heading">
          <el-icon><Connection /></el-icon>
          <span>档案、库存、单据与流水的关系</span>
        </div>
      </template>
      <div class="relation-flow">
        <div><strong>面料/辅料档案</strong><span>说明物料是什么</span></div>
        <b>一对多 →</b>
        <div><strong>库存批次</strong><span>记录颜色、款号和余额</span></div>
        <b>一对多 →</b>
        <div><strong>入库单/领用单</strong><span>驱动库存变化</span></div>
        <b>自动生成 →</b>
        <div><strong>库存流水</strong><span>永久保存操作历史</span></div>
      </div>
      <ul class="rule-list relation-rules">
        <li>一条面料或辅料档案可以关联多个库存批次；一个库存批次只能属于一种物料类型，并最多关联一条对应档案。</li>
        <li>旧 Excel 库存无法可靠对应现有档案时显示“历史库存未归档”，允许后续补充关联；一旦关联并产生历史记录便不能改绑。</li>
        <li>已被有效库存关联的面料或辅料档案不能删除，因为单据和流水需要永久引用该档案。</li>
        <li>库存余额不能直接随意改写：正常变化通过入库、冲销、锁定、发料、取消和退回完成；主管调整也会生成流水。</li>
        <li>只有主/辅助余额和锁定量全部为零，并且从未产生单据或流水的误建库存行，才允许主管删除。</li>
        <li>草稿单据可以修改或删除；已过账、已锁定、已发料单据只能按规定冲销、取消或退回，不能删除历史。</li>
      </ul>
    </el-card>

    <el-card class="guide-card operation-card" shadow="never">
      <template #header>
        <div class="card-heading">
          <el-icon><InfoFilled /></el-icon>
          <span>库存操作名词与使用规则</span>
        </div>
      </template>
      <p class="operation-intro">
        凡涉及库存数量的操作都不是删除记录，而是通过一条新的库存流水记录实际变化。原单据和原流水始终保留，方便以后核对“谁在什么时候做了什么”。
      </p>
      <div class="status-flow">
        <div><strong>入库单</strong><span>草稿</span><b>过账 →</b><span>已过账</span><b>冲销 →</b><span>已冲销</span></div>
        <div><strong>领用单</strong><span>草稿</span><b>锁定 →</b><span>已锁定</span><b>发料 →</b><span>已发料</span><b>退回 →</b><span>已退回</span></div>
      </div>
      <div class="term-grid">
        <article>
          <strong>过账</strong>
          <p>确认入库真实发生。系统把入库数量加到现有库存，并生成“入库”流水；过账后原单不能再修改或删除。</p>
        </article>
        <article>
          <strong>冲销</strong>
          <p>纠正一张已经过账但不应生效的入库单。系统用反向流水扣回该单增加的数量，原入库单仍保留并标记为“已冲销”。如果库存已经被领用或锁定、无法足额扣回，系统会拒绝冲销。</p>
        </article>
        <article>
          <strong>锁定</strong>
          <p>为领用单预留库存。实物库存总量暂时不减少，但可用数量会减少，避免同一批库存被其他领用单重复占用。</p>
        </article>
        <article>
          <strong>取消</strong>
          <p>终止尚未发料的领用单。草稿取消不影响库存；已锁定后取消会释放预留数量，库存总量仍不改变。</p>
        </article>
        <article>
          <strong>发料</strong>
          <p>确认物料已经从仓库交付。系统同时扣减库存总量和此前的锁定量，并生成“领用发料”流水。</p>
        </article>
        <article>
          <strong>退回（回退）</strong>
          <p>已经发出的物料重新退回仓库。系统把原领用数量加回库存并生成“领用退回”流水；它不是返回上一页面，也不会抹掉原发料记录。</p>
        </article>
        <article>
          <strong>库存调整</strong>
          <p>仅用于盘点差异、期初纠错等无法用正常单据表达的情况。可增加或减少库存，但必须填写原因，并永久生成调整流水。</p>
        </article>
        <article>
          <strong>删除</strong>
          <p>只清理从未发生业务的错误草稿或误建库存行。已经产生过账、锁定、发料、冲销、取消、退回或调整流水的数据不能删除。</p>
        </article>
      </div>
      <el-alert
        title="简单判断：业务没有发生可修改或删除草稿；业务已经发生要用冲销、取消或退回留下反向记录，不要删除历史。"
        type="warning"
        :closable="false"
        show-icon
      />
    </el-card>

    <el-card class="guide-card organization-card" shadow="never">
      <template #header>
        <div class="card-heading">
          <el-icon><OfficeBuilding /></el-icon>
          <span>组织架构</span>
        </div>
      </template>
      <div class="department-list">
        <span v-for="department in departments" :key="department">{{ department }}</span>
      </div>
      <p class="muted-tip">账号只能归属一个部门，但可以关联多个岗位；部门和岗位由系统管理员维护。</p>
    </el-card>

    <el-card class="guide-card faq-card" shadow="never">
      <template #header>
        <div class="card-heading">
          <el-icon><QuestionFilled /></el-icon>
          <span>常见问题</span>
        </div>
      </template>
      <el-collapse>
        <el-collapse-item title="找不到供应商怎么办？" name="supplier">
          面料与辅料供应商独立维护。有新增权限的账号可在对应表单中快速新建，名称、电话和地址均为必填。
        </el-collapse-item>
        <el-collapse-item title="为什么我不能修改某条面料？" name="permission">
          面料录入员只能修改本人录入的数据；如需修改他人数据，请联系面料主管或系统管理员。
        </el-collapse-item>
        <el-collapse-item title="图片为什么显示失败？" name="image">
          请先确认原图仍位于系统上传目录，并尽量使用 JPG 或 PNG 图片。上传失败时重新选择图片即可。
        </el-collapse-item>
        <el-collapse-item title="编号能否手工修改？" name="code">
          不能。面料编号按年度流水和成分生成，辅料编号按 B-两位年份-四位流水生成。
        </el-collapse-item>
        <el-collapse-item title="为什么关联库存后不能删除档案？" name="archive-delete">
          档案是库存、入库单、领用单和流水的追溯依据。删除会让历史业务失去来源，所以系统会拒绝删除已关联档案。
        </el-collapse-item>
        <el-collapse-item title="为什么有些库存显示“历史库存未归档”？" name="unlinked-stock">
          旧 Excel 编号与系统档案编号无法可靠匹配，系统不会猜测关联。可在确认实物后修改库存资料并补充一次正确档案关联。
        </el-collapse-item>
        <el-collapse-item title="库存台账什么时候可以删除？" name="stock-delete">
          仅限误建且余额、锁定量全部为零，同时没有任何入库、领用或流水记录的库存行；其他情况请使用业务单据或库存调整。
        </el-collapse-item>
        <el-collapse-item title="冲销、取消和退回有什么区别？" name="reverse-operation">
          冲销针对已过账入库，把错误入库数量反向扣回；取消针对尚未发料的领用，已锁定时会释放预留量；退回针对已经发料的领用，把物料数量重新加回库存。三者都会保留原单据，不能代替正常的草稿删除。
        </el-collapse-item>
      </el-collapse>
    </el-card>

    <section class="action-bar">
      <div>
        <strong>现在开始</strong>
        <span>根据当前账号权限进入常用功能。</span>
      </div>
      <div class="action-buttons">
        <el-button v-if="canViewFabric" type="primary" @click="router.push('/fabric/list')">
          打开面料档案
        </el-button>
        <el-button v-if="canManageMaster" @click="router.push('/fabric/master')">
          维护基础资料
        </el-button>
        <el-button v-if="canViewInventory" @click="router.push('/inventory/stock')">
          打开库存台账
        </el-button>
        <el-button v-if="isAdmin" @click="router.push('/system/dept')">
          部门设置
        </el-button>
      </div>
    </section>
  </div>
</template>

<script setup name="Help">
import useUserStore from "@/store/modules/user"

const router = useRouter()
const userStore = useUserStore()
const departments = ["办公室", "开发部", "生产管理部", "业务部", "跟单部", "采购部", "裁床部", "版房"]

const isAdmin = computed(() => userStore.roles.includes("admin"))
const canViewFabric = computed(() =>
  userStore.permissions.includes("*:*:*")
  || userStore.permissions.includes("fabric:fabric:list")
)
const canManageMaster = computed(() =>
  userStore.permissions.includes("*:*:*")
  || userStore.permissions.includes("fabric:master:list")
)
const canViewInventory = computed(() =>
  userStore.permissions.includes("*:*:*")
  || userStore.permissions.includes("inventory:stock:list")
)
const roleLabel = computed(() => {
  if (userStore.roles.includes("admin")) return "系统管理员"
  if (userStore.roles.includes("fabric_manager")) return "面料主管"
  if (userStore.roles.includes("fabric_entry")) return "面料录入员"
  if (userStore.roles.includes("fabric_view")) return "面料查询员"
  return "系统用户"
})
</script>

<style scoped lang="scss">
.help-page {
  min-height: calc(100vh - 84px);
  padding: 22px;
  background: #f4f6f9;
}

.help-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 32px 38px;
  color: #fff;
  border-radius: 20px;
  background:
    radial-gradient(circle at 84% 18%, rgba(94, 234, 212, 0.22), transparent 26%),
    linear-gradient(125deg, #0f172a, #164e63 72%, #0f766e);

  h1 {
    margin: 6px 0 8px;
    font-size: 32px;
  }

  p {
    margin: 0;
    color: #cbd5e1;
  }
}

.eyebrow {
  color: #5eead4;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.12em;
}

.hero-role {
  min-width: 150px;
  padding: 14px 18px;
  text-align: right;
  border: 1px solid rgba(153, 246, 228, 0.28);
  border-radius: 14px;
  background: rgba(15, 118, 110, 0.2);

  span,
  strong {
    display: block;
  }

  span {
    color: #99f6e4;
    font-size: 12px;
  }

  strong {
    margin-top: 5px;
  }
}

.quick-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  margin: 18px 0;
}

.quick-card {
  position: relative;
  padding: 22px 22px 20px 72px;
  border: 1px solid #e7ebf0;
  border-radius: 15px;
  background: #fff;

  h2 {
    margin: 0 0 7px;
    color: #0f172a;
    font-size: 16px;
  }

  p {
    margin: 0;
    color: #64748b;
    font-size: 13px;
    line-height: 1.65;
  }
}

.step-number {
  position: absolute;
  top: 22px;
  left: 20px;
  color: #0f766e;
  font-size: 22px;
  font-weight: 750;
}

.content-grid {
  display: grid;
  grid-template-columns: 1.1fr 0.9fr;
  gap: 18px;
  margin-bottom: 18px;
}

.guide-card {
  border: 1px solid #e7ebf0;
  border-radius: 15px;
}

.card-heading {
  display: flex;
  gap: 8px;
  align-items: center;
  color: #0f172a;
  font-size: 16px;
  font-weight: 650;

  .el-icon {
    color: #0f766e;
  }
}

.code-example {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 15px;
  margin-bottom: 14px;
  border-radius: 11px;
  background: #ecfeff;

  strong {
    color: #155e75;
    font-family: Consolas, monospace;
    font-size: 18px;
  }

  span {
    color: #64748b;
    font-size: 12px;
  }
}

.rule-list {
  padding-left: 20px;
  margin: 0;
  color: #475569;
  line-height: 1.9;
}

.role-list {
  display: grid;
  gap: 10px;

  div {
    padding: 12px 14px;
    border-radius: 10px;
    background: #f8fafc;
  }

  strong,
  span {
    display: block;
  }

  strong {
    color: #1e293b;
    font-size: 14px;
  }

  span {
    margin-top: 4px;
    color: #64748b;
    font-size: 12px;
    line-height: 1.55;
  }
}

.organization-card,
.faq-card {
  margin-bottom: 18px;
}

.relation-card,
.operation-card {
  margin-bottom: 18px;
}

.relation-flow {
  display: grid;
  grid-template-columns: 1fr auto 1fr auto 1fr auto 1fr;
  gap: 12px;
  align-items: center;

  div {
    padding: 16px;
    text-align: center;
    border: 1px solid #ccfbf1;
    border-radius: 12px;
    background: #f0fdfa;
  }

  strong,
  span {
    display: block;
  }

  strong {
    color: #115e59;
  }

  span {
    margin-top: 5px;
    color: #64748b;
    font-size: 12px;
  }

  b {
    color: #0f766e;
    font-size: 12px;
    white-space: nowrap;
  }
}

.relation-rules {
  margin-top: 18px;
}

.operation-intro {
  padding: 13px 15px;
  margin: 0 0 16px;
  color: #475569;
  line-height: 1.7;
  border-left: 4px solid #0f766e;
  border-radius: 0 10px 10px 0;
  background: #f0fdfa;
}

.status-flow {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 16px;

  div {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    align-items: center;
    padding: 13px 15px;
    border: 1px solid #e2e8f0;
    border-radius: 11px;
    background: #f8fafc;
  }

  strong {
    margin-right: 4px;
    color: #0f172a;
  }

  span {
    padding: 4px 9px;
    color: #155e75;
    border-radius: 999px;
    background: #cffafe;
    font-size: 12px;
  }

  b {
    color: #94a3b8;
    font-size: 12px;
  }
}

.term-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 16px;

  article {
    padding: 15px 16px;
    border: 1px solid #e7ebf0;
    border-radius: 11px;
    background: #fff;
  }

  strong {
    color: #0f766e;
  }

  p {
    margin: 7px 0 0;
    color: #64748b;
    font-size: 13px;
    line-height: 1.7;
  }
}

.department-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;

  span {
    padding: 8px 13px;
    color: #155e75;
    border: 1px solid #bae6fd;
    border-radius: 999px;
    background: #f0f9ff;
  }
}

.muted-tip {
  margin: 13px 0 0;
  color: #94a3b8;
  font-size: 12px;
}

.action-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px;
  border: 1px solid #dbe5eb;
  border-radius: 15px;
  background: #fff;

  strong,
  span {
    display: block;
  }

  strong {
    color: #0f172a;
  }

  span {
    margin-top: 4px;
    color: #94a3b8;
    font-size: 12px;
  }
}

@media (max-width: 900px) {
  .quick-grid,
  .content-grid,
  .status-flow,
  .term-grid {
    grid-template-columns: 1fr;
  }

  .relation-flow {
    grid-template-columns: 1fr;

    b {
      text-align: center;
      transform: rotate(90deg);
    }
  }

  .help-hero,
  .action-bar {
    gap: 18px;
    align-items: flex-start;
    flex-direction: column;
  }

  .hero-role {
    text-align: left;
  }
}
</style>
