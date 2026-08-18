import request from "@/utils/request"

export function listAccessories(query) {
  return request({ url: "/fabric/accessory/list", method: "get", params: query })
}

export function getAccessory(id) {
  return request({ url: `/fabric/accessory/${id}`, method: "get" })
}

export function addAccessory(data) {
  return request({ url: "/fabric/accessory", method: "post", data })
}

export function updateAccessory(data) {
  return request({ url: "/fabric/accessory", method: "put", data })
}

export function deleteAccessories(ids) {
  return request({ url: `/fabric/accessory/${ids}`, method: "delete" })
}

export function listAccessorySuppliers() {
  return request({ url: "/fabric/accessory/suppliers", method: "get" })
}

export function createAccessorySupplier(data) {
  return request({ url: "/fabric/accessory/suppliers", method: "post", data })
}

export function updateAccessorySupplier(data) {
  return request({ url: "/fabric/accessory/suppliers", method: "put", data })
}

export function deleteAccessorySupplier(id) {
  return request({ url: `/fabric/accessory/suppliers/${id}`, method: "delete" })
}
