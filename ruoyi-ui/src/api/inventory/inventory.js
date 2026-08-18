import request from "@/utils/request"

export const listStocks = query => request({ url: "/inventory/stock/list", method: "get", params: query })
export const getStock = id => request({ url: `/inventory/stock/${id}`, method: "get" })
export const addStock = data => request({ url: "/inventory/stock", method: "post", data })
export const updateStock = data => request({ url: "/inventory/stock", method: "put", data })
export const adjustStock = data => request({ url: "/inventory/stock/adjust", method: "put", data })
export const deleteStock = id => request({ url: `/inventory/stock/${id}`, method: "delete" })

export const listInbound = query => request({ url: "/inventory/inbound/list", method: "get", params: query })
export const getInbound = id => request({ url: `/inventory/inbound/${id}`, method: "get" })
export const addInbound = data => request({ url: "/inventory/inbound", method: "post", data })
export const updateInbound = data => request({ url: "/inventory/inbound", method: "put", data })
export const deleteInbound = id => request({ url: `/inventory/inbound/${id}`, method: "delete" })
export const postInbound = id => request({ url: `/inventory/inbound/${id}/post`, method: "put" })
export const cancelInbound = id => request({ url: `/inventory/inbound/${id}/cancel`, method: "put" })

export const listRequisition = query => request({ url: "/inventory/requisition/list", method: "get", params: query })
export const getRequisition = id => request({ url: `/inventory/requisition/${id}`, method: "get" })
export const addRequisition = data => request({ url: "/inventory/requisition", method: "post", data })
export const updateRequisition = data => request({ url: "/inventory/requisition", method: "put", data })
export const deleteRequisition = id => request({ url: `/inventory/requisition/${id}`, method: "delete" })
export const lockRequisition = id => request({ url: `/inventory/requisition/${id}/lock`, method: "put" })
export const issueRequisition = id => request({ url: `/inventory/requisition/${id}/issue`, method: "put" })
export const cancelRequisition = id => request({ url: `/inventory/requisition/${id}/cancel`, method: "put" })
export const returnRequisition = id => request({ url: `/inventory/requisition/${id}/return`, method: "put" })

export const listMovements = query => request({ url: "/inventory/movement/list", method: "get", params: query })
