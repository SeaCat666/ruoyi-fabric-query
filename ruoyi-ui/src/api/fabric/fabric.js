import request from '@/utils/request'

export function listFabric(query) {
  return request({
    url: '/fabric/fabric/list',
    method: 'get',
    params: query
  })
}

export function getFabricDashboard() {
  return request({
    url: '/fabric/fabric/dashboard',
    method: 'get'
  })
}

export function getFabric(id) {
  return request({
    url: `/fabric/fabric/${id}`,
    method: 'get'
  })
}

export function addFabric(data) {
  return request({
    url: '/fabric/fabric',
    method: 'post',
    data
  })
}

export function updateFabric(data) {
  return request({
    url: '/fabric/fabric',
    method: 'put',
    data
  })
}

export function delFabric(ids) {
  return request({
    url: `/fabric/fabric/${ids}`,
    method: 'delete'
  })
}

export function listSuppliers() {
  return request({
    url: '/fabric/fabric/suppliers',
    method: 'get'
  })
}

export function createSupplier(data) {
  return request({
    url: '/fabric/fabric/suppliers',
    method: 'post',
    data
  })
}

export function updateSupplier(data) {
  return request({
    url: '/fabric/fabric/suppliers',
    method: 'put',
    data
  })
}

export function deleteSupplier(id) {
  return request({
    url: `/fabric/fabric/suppliers/${id}`,
    method: 'delete'
  })
}

export function listCategories() {
  return request({
    url: '/fabric/fabric/categories',
    method: 'get'
  })
}

export function createCategory(data) {
  return request({
    url: '/fabric/fabric/categories',
    method: 'post',
    data
  })
}

export function updateCategory(data) {
  return request({
    url: '/fabric/fabric/categories',
    method: 'put',
    data
  })
}

export function deleteCategory(id) {
  return request({
    url: `/fabric/fabric/categories/${id}`,
    method: 'delete'
  })
}

export function listComponents() {
  return request({
    url: '/fabric/fabric/components',
    method: 'get'
  })
}
