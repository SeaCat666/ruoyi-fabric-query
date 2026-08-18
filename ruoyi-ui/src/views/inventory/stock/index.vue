<template>
  <div class="app-container inventory-page">
    <section class="page-head">
      <div><span>INVENTORY</span><h2>面辅料库存台账</h2><p>库存按实物批次独立记录，面料与辅料共用流程但不混淆档案。</p></div>
      <el-button type="primary" icon="Plus" @click="openAdd" v-hasPermi="['inventory:stock:add']">新增库存行</el-button>
    </section>
    <el-form ref="queryRef" :model="query" inline label-width="74px">
      <el-form-item label="库存编号" prop="stockCode"><el-input v-model="query.stockCode" clearable placeholder="K-26..." /></el-form-item>
      <el-form-item label="物料类型" prop="materialType"><el-select v-model="query.materialType" clearable style="width:120px"><el-option label="面料" value="F"/><el-option label="辅料" value="A"/></el-select></el-form-item>
      <el-form-item label="物料编号" prop="materialCode"><el-input v-model="query.materialCode" clearable /></el-form-item>
      <el-form-item label="开发款号" prop="developmentStyleNo"><el-input v-model="query.developmentStyleNo" clearable /></el-form-item>
      <el-form-item><el-button type="primary" icon="Search" @click="search">查询</el-button><el-button icon="Refresh" @click="resetQuery">重置</el-button></el-form-item>
    </el-form>
    <el-alert title="规则：库存批次可关联一条面料或辅料档案；已有关联和历史流水时不能改绑。只有零余额、无单据、无流水的误建库存行才允许主管删除。" type="info" :closable="false" show-icon class="mb16" />
    <el-table v-loading="loading" :data="rows" row-key="id">
      <el-table-column label="图片" width="72" align="center">
        <template #default="{ row }"><el-image v-if="row.images?.length" class="thumb" :src="imageUrl(row.images[0].thumbnailUrl || row.images[0].imageUrl)" :preview-src-list="row.images.map(i => imageUrl(i.imageUrl || i.thumbnailUrl))" preview-teleported fit="cover"/><span v-else class="muted">暂无</span></template>
      </el-table-column>
      <el-table-column label="库存编号" prop="stockCode" width="142" fixed />
      <el-table-column label="类型" width="72" align="center"><template #default="{ row }"><el-tag :type="row.materialType === 'A' ? 'warning' : 'primary'">{{ row.materialType === 'A' ? '辅料' : '面料' }}</el-tag></template></el-table-column>
      <el-table-column label="关联档案" min-width="190">
        <template #default="{ row }">
          <div v-if="row.archiveCode" class="archive-cell"><el-tag type="success" size="small">已关联</el-tag><strong>{{ row.archiveCode }}</strong><span>{{ row.archiveName || row.archiveSupplierName || '' }}</span></div>
          <el-tag v-else type="info" size="small">历史库存未归档</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="物料编号" prop="materialCode" min-width="115"><template #default="{ row }">{{ row.materialCode || '未编号' }}</template></el-table-column>
      <el-table-column label="开发款号" prop="developmentStyleNo" min-width="120" show-overflow-tooltip />
      <el-table-column label="颜色" prop="colorNo" min-width="100" show-overflow-tooltip />
      <el-table-column label="货品类型" prop="goodsType" min-width="120" show-overflow-tooltip />
      <el-table-column label="现库存" width="112" align="right"><template #default="{ row }"><strong>{{ amount(row.onHandQty) }}</strong> {{ row.primaryUnit }}</template></el-table-column>
      <el-table-column label="已锁定" width="105" align="right"><template #default="{ row }">{{ amount(row.lockedQty) }} {{ row.primaryUnit }}</template></el-table-column>
      <el-table-column label="可用" width="105" align="right"><template #default="{ row }"><span class="available">{{ amount(Number(row.onHandQty)-Number(row.lockedQty)) }} {{ row.primaryUnit }}</span></template></el-table-column>
      <el-table-column label="辅助库存" width="115" align="right"><template #default="{ row }">{{ amount(row.onHandAuxQty) }} {{ row.auxiliaryUnit }}</template></el-table-column>
      <el-table-column label="备注" prop="remark" min-width="180" show-overflow-tooltip><template #default="{ row }">{{ row.remark || '—' }}</template></el-table-column>
      <el-table-column label="操作" width="190" fixed="right" align="center">
        <template #default="{ row }"><el-button link type="primary" icon="Edit" @click="openEdit(row)" v-hasPermi="['inventory:stock:edit']">修改</el-button><el-button link type="warning" icon="Sort" @click="openAdjust(row)" v-hasPermi="['inventory:stock:adjust']">调整</el-button><el-button link type="danger" icon="Delete" @click="removeStock(row)" v-hasPermi="['inventory:stock:remove']">删除</el-button></template>
      </el-table-column>
    </el-table>
    <pagination v-show="total>0" :total="total" v-model:page="query.pageNum" v-model:limit="query.pageSize" @pagination="load" />

    <el-dialog v-model="formOpen" :title="form.id ? '修改库存资料' : '新增库存行'" width="920px" class="stock-form-dialog" append-to-body align-center destroy-on-close :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="stock-form">
        <section class="form-section">
          <div class="section-heading">
            <span class="section-icon">01</span>
            <div><strong>物料与档案</strong><small>先确认库存属于面料还是辅料，建议关联对应档案</small></div>
          </div>
          <el-row :gutter="20">
            <el-col :xs="24" :sm="8">
              <el-form-item label="物料类型" prop="materialType">
                <el-radio-group v-model="form.materialType" @change="archiveChanged" class="material-type-group">
                  <el-radio-button value="F">面料</el-radio-button>
                  <el-radio-button value="A">辅料</el-radio-button>
                </el-radio-group>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="16">
              <el-form-item>
                <template #label>
                  <span>关联档案 <em class="recommended">建议关联</em></span>
                </template>
                <el-select v-model="archiveId" clearable filterable class="full-width" placeholder="输入编号、名称或供应商查找">
                  <el-option v-for="item in archiveOptions" :key="item.id" :label="archiveLabel(item)" :value="item.id">
                    <div class="archive-option"><strong>{{ item.code }}</strong><span>{{ archiveDescription(item) }}</span></div>
                  </el-option>
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
        </section>

        <section class="form-section">
          <div class="section-heading">
            <span class="section-icon">02</span>
            <div><strong>库存识别信息</strong><small>用于按实物标签、开发款号和颜色快速查找</small></div>
          </div>
          <el-row :gutter="20">
            <el-col :xs="24" :sm="8"><el-form-item label="物料编号"><el-input v-model="form.materialCode" clearable placeholder="实物或原表编号" /></el-form-item></el-col>
            <el-col :xs="24" :sm="8"><el-form-item label="开发款号"><el-input v-model="form.developmentStyleNo" clearable placeholder="关联开发款号" /></el-form-item></el-col>
            <el-col :xs="24" :sm="8"><el-form-item label="SKC"><el-input v-model="form.skc" clearable placeholder="款色编码" /></el-form-item></el-col>
            <el-col :xs="24" :sm="12"><el-form-item label="颜色"><el-input v-model="form.colorNo" clearable placeholder="颜色或色号" /></el-form-item></el-col>
            <el-col :xs="24" :sm="12"><el-form-item label="货品类型"><el-input v-model="form.goodsType" clearable placeholder="例如：大货、开发样" /></el-form-item></el-col>
          </el-row>
        </section>

        <section class="form-section compact-section">
          <div class="section-heading">
            <span class="section-icon">03</span>
            <div><strong>计量与备注</strong><small>主单位用于出入库，辅助单位可留空</small></div>
          </div>
          <el-row :gutter="20">
            <el-col :xs="24" :sm="12">
              <el-form-item label="主单位" prop="primaryUnit">
                <el-select v-model="form.primaryUnit" filterable allow-create default-first-option class="full-width" placeholder="请选择或输入单位">
                  <el-option v-for="unit in unitOptions" :key="unit" :label="unit" :value="unit" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12">
              <el-form-item label="辅助单位">
                <el-select v-model="form.auxiliaryUnit" clearable filterable allow-create default-first-option class="full-width" placeholder="可选，例如：米、千克">
                  <el-option v-for="unit in unitOptions" :key="unit" :label="unit" :value="unit" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="24"><el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="3" maxlength="1000" show-word-limit placeholder="填写批次特征、存放位置或其他需要说明的信息" /></el-form-item></el-col>
          </el-row>
        </section>

        <el-alert v-if="!form.id" title="新增后库存余额为 0" description="保存基础资料后，请通过入库单增加数量；只有盘点差异或期初纠错才使用库存调整。" type="warning" :closable="false" show-icon />
        <el-alert v-else title="库存资料修改规则" description="产生单据或流水后，物料类型和计量单位将锁定；档案只允许从未关联状态补充关联一次。" type="warning" :closable="false" show-icon />
      </el-form>
      <template #footer><div class="dialog-footer"><el-button @click="formOpen=false">取消</el-button><el-button type="primary" :loading="saving" @click="submit">{{ form.id ? '保存修改' : '创建库存行' }}</el-button></div></template>
    </el-dialog>

    <el-dialog v-model="adjustOpen" title="库存调整" width="560px" append-to-body :close-on-click-modal="false">
      <el-alert :title="`正数增加、负数减少；当前 ${amount(adjustBase.onHandQty)} ${adjustBase.primaryUnit} / ${amount(adjustBase.onHandAuxQty)} ${adjustBase.auxiliaryUnit || ''}`" type="warning" :closable="false" class="mb16"/>
      <el-form ref="adjustRef" :model="adjustForm" :rules="adjustRules" label-width="110px"><el-form-item label="主数量变动" prop="adjustQty"><el-input-number v-model="adjustForm.adjustQty" :precision="3" controls-position="right" style="width:100%" /></el-form-item><el-form-item label="辅助数量变动"><el-input-number v-model="adjustForm.adjustAuxQty" :precision="3" controls-position="right" style="width:100%" /></el-form-item><el-form-item label="调整原因" prop="remark"><el-input v-model="adjustForm.remark" type="textarea" :rows="3" /></el-form-item></el-form>
      <template #footer><el-button type="primary" @click="submitAdjust">确认调整</el-button><el-button @click="adjustOpen=false">取消</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { listStocks, getStock, addStock, updateStock, adjustStock, deleteStock } from "@/api/inventory/inventory"
import { listFabric } from "@/api/fabric/fabric"
import { listAccessories } from "@/api/fabric/accessory"
const { proxy } = getCurrentInstance()
const route = useRoute()
const baseApi = import.meta.env.VITE_APP_BASE_API
const loading=ref(false),saving=ref(false),formOpen=ref(false),adjustOpen=ref(false),rows=ref([]),total=ref(0)
const fabricOptions=ref([]),accessoryOptions=ref([]),archiveId=ref()
const unitOptions=["米","码","千克","克","卷","条","个","件","包","箱"]
const query=reactive({pageNum:1,pageSize:20,stockCode:undefined,materialType:undefined,materialCode:undefined,developmentStyleNo:undefined,fabricId:route.query.fabricId?Number(route.query.fabricId):undefined,accessoryId:route.query.accessoryId?Number(route.query.accessoryId):undefined})
const form=reactive({}),adjustForm=reactive({}),adjustBase=reactive({})
const rules={materialType:[{required:true,message:"请选择物料类型",trigger:"change"}],primaryUnit:[{required:true,message:"请输入主单位",trigger:"blur"}]}
const adjustRules={remark:[{required:true,message:"请填写调整原因",trigger:"blur"}]}
const archiveOptions=computed(()=>form.materialType==='A'?accessoryOptions.value:fabricOptions.value)
function load(){loading.value=true;listStocks(query).then(r=>{rows.value=r.rows||[];total.value=r.total||0}).finally(()=>loading.value=false)}
function loadArchives(){Promise.all([listFabric({pageNum:1,pageSize:500}),listAccessories({pageNum:1,pageSize:500})]).then(([f,a])=>{fabricOptions.value=f.rows||[];accessoryOptions.value=a.rows||[]})}
function search(){query.pageNum=1;load()} function resetQuery(){proxy.resetForm("queryRef");query.fabricId=undefined;query.accessoryId=undefined;search()}
function resetForm(){Object.assign(form,{id:undefined,materialType:'F',fabricId:undefined,accessoryId:undefined,materialCode:'',developmentStyleNo:'',skc:'',colorNo:'',goodsType:'',primaryUnit:'条',auxiliaryUnit:'米',remark:''});archiveId.value=undefined}
function openAdd(){resetForm();formOpen.value=true} function openEdit(row){getStock(row.id).then(r=>{resetForm();Object.assign(form,r.data);archiveId.value=form.materialType==='A'?form.accessoryId:form.fabricId;formOpen.value=true})}
function archiveChanged(){archiveId.value=undefined}
function archiveDescription(item){return [item.productName||item.sizeSpec,item.supplierName].filter(Boolean).join(' · ')||'暂无补充信息'}
function archiveLabel(item){return `${item.code} ${archiveDescription(item)}`}
function submit(){proxy.$refs.formRef.validate(valid=>{if(!valid)return;if(form.materialType==='A'){form.accessoryId=archiveId.value;form.fabricId=undefined}else{form.fabricId=archiveId.value;form.accessoryId=undefined} saving.value=true;(form.id?updateStock(form):addStock(form)).then(()=>{proxy.$modal.msgSuccess(form.id?'修改成功':'新增成功');formOpen.value=false;load()}).finally(()=>saving.value=false)})}
function openAdjust(row){Object.assign(adjustBase,row);Object.assign(adjustForm,{id:row.id,adjustQty:0,adjustAuxQty:0,remark:''});adjustOpen.value=true}
function submitAdjust(){proxy.$refs.adjustRef.validate(valid=>{if(!valid)return;proxy.$modal.confirm('库存调整会立即生成不可修改的库存流水，是否继续？').then(()=>adjustStock(adjustForm)).then(()=>{proxy.$modal.msgSuccess('库存调整成功');adjustOpen.value=false;load()}).catch(()=>{})})}
function removeStock(row){proxy.$modal.confirm(`只允许删除零余额且从未产生单据、流水的误建库存行。确认删除 ${row.stockCode}？`).then(()=>deleteStock(row.id)).then(()=>{proxy.$modal.msgSuccess('库存行已删除');load()}).catch(()=>{})}
function amount(v){const n=Number(v||0);return Number.isInteger(n)?String(n):n.toFixed(3).replace(/0+$/,'').replace(/\.$/,'')}
function imageUrl(url){return /^(https?:)?\/\//i.test(url||'')?url:`${baseApi}${url||''}`}
load();loadArchives()
</script>

<style scoped>
.page-head{display:flex;justify-content:space-between;align-items:center;margin-bottom:20px;padding:22px 26px;color:#fff;border-radius:12px;background:linear-gradient(120deg,#0f766e,#155e75)}.page-head span{font-size:11px;letter-spacing:2px;opacity:.75}.page-head h2{margin:4px 0;font-size:24px}.page-head p{margin:0;opacity:.8}.thumb{width:48px;height:48px;border-radius:6px}.muted{color:#a8abb2;font-size:12px}.available{color:#047857;font-weight:700}.archive-cell{display:grid;grid-template-columns:auto 1fr;gap:2px 7px;align-items:center}.archive-cell span{grid-column:2;color:#64748b;font-size:12px}.mb16{margin-bottom:16px}:deep(.el-table__header th){background:#f8fafc;color:#475569}

:global(.stock-form-dialog) {
  max-width: calc(100vw - 32px);
}

:global(.stock-form-dialog .el-dialog__header) {
  padding: 20px 24px 16px;
  margin-right: 0;
  border-bottom: 1px solid #edf0f3;
}

:global(.stock-form-dialog .el-dialog__body) {
  max-height: calc(100vh - 220px);
  padding: 18px 24px;
  overflow-y: auto;
}

:global(.stock-form-dialog .el-dialog__footer) {
  padding: 14px 24px 16px;
  border-top: 1px solid #edf0f3;
}

.form-section {
  padding: 15px 18px 3px;
  margin-bottom: 14px;
  border: 1px solid #e5eaf0;
  border-radius: 12px;
  background: #fbfcfd;
}

.compact-section {
  padding-bottom: 3px;
}

.section-heading {
  display: flex;
  gap: 10px;
  align-items: center;
  margin-bottom: 13px;

  strong,
  small {
    display: block;
  }

  strong {
    color: #1f2937;
    font-size: 14px;
  }

  small {
    margin-top: 2px;
    color: #909399;
    font-size: 12px;
  }
}

.section-icon {
  display: grid;
  width: 32px;
  height: 32px;
  flex: 0 0 32px;
  place-items: center;
  color: #0f766e;
  border-radius: 9px;
  background: #ccfbf1;
  font-size: 11px;
  font-weight: 700;
}

.stock-form :deep(.el-form-item) {
  margin-bottom: 15px;
}

.stock-form :deep(.el-form-item__label) {
  height: auto;
  padding-bottom: 6px;
  color: #4b5563;
  font-weight: 600;
  line-height: 20px;
}

.stock-form :deep(.el-input__wrapper),
.stock-form :deep(.el-select__wrapper) {
  min-height: 38px;
}

.material-type-group {
  display: flex;
  width: 100%;

  :deep(.el-radio-button) {
    flex: 1;
  }

  :deep(.el-radio-button__inner) {
    width: 100%;
  }
}

.full-width {
  width: 100%;
}

.recommended {
  padding: 2px 6px;
  margin-left: 5px;
  color: #0f766e;
  border-radius: 8px;
  background: #ccfbf1;
  font-size: 11px;
  font-style: normal;
  font-weight: 500;
}

.archive-option {
  display: flex;
  gap: 12px;
  align-items: center;
  justify-content: space-between;

  strong {
    color: #1f2937;
  }

  span {
    overflow: hidden;
    color: #909399;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.dialog-footer {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
}

@media (max-width: 767px) {
  :global(.stock-form-dialog .el-dialog__body) {
    padding: 14px 16px;
  }

  .form-section {
    padding-right: 14px;
    padding-left: 14px;
  }
}
</style>
