package com.platform.gateway.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "document_records")
public class DocumentRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "deal_id", nullable = false)
    private String dealId;

    @Column(name = "filename", nullable = false)
    private String filename;

    @Column(name = "file_path")
    private String filePath;

    @Column(name = "page_count")
    private Integer pageCount;

    public DocumentRecord() {}

    public Long getId() { return id; }
    public String getDealId() { return dealId; }
    public void setDealId(String dealId) { this.dealId = dealId; }
    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public Integer getPageCount() { return pageCount; }
    public void setPageCount(Integer pageCount) { this.pageCount = pageCount; }
}
