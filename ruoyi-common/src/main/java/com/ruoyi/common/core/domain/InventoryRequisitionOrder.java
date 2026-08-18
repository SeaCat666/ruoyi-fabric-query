package com.ruoyi.common.core.domain;

import java.time.LocalDate;
import java.util.List;

/** 领用单。 */
public class InventoryRequisitionOrder extends BaseEntity
{
    private static final long serialVersionUID = 1L;
    private Long id;
    private String orderNo;
    private LocalDate orderDate;
    private String applicantName;
    private String developmentStyleNo;
    private String status;
    private List<InventoryRequisitionDetail> details;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public LocalDate getOrderDate() { return orderDate; }
    public void setOrderDate(LocalDate orderDate) { this.orderDate = orderDate; }
    public String getApplicantName() { return applicantName; }
    public void setApplicantName(String applicantName) { this.applicantName = applicantName; }
    public String getDevelopmentStyleNo() { return developmentStyleNo; }
    public void setDevelopmentStyleNo(String developmentStyleNo) { this.developmentStyleNo = developmentStyleNo; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public List<InventoryRequisitionDetail> getDetails() { return details; }
    public void setDetails(List<InventoryRequisitionDetail> details) { this.details = details; }
}
