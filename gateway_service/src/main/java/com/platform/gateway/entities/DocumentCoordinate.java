package com.platform.gateway.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "document_coordinates")
public class DocumentCoordinate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "metric_id", nullable = false)
    private Long metricId;

    @Column(name = "page_number", nullable = false)
    private Integer pageNumber;

    @Column(name = "x_min", nullable = false)
    private Double xMin;

    @Column(name = "y_min", nullable = false)
    private Double yMin;

    @Column(name = "x_max", nullable = false)
    private Double xMax;

    @Column(name = "y_max", nullable = false)
    private Double yMax;

    @Column(name = "row_index")
    private Integer rowIndex;

    @Column(name = "col_index")
    private Integer colIndex;

    public DocumentCoordinate() {}

    public Long getId() { return id; }
    public Long getMetricId() { return metricId; }
    public void setMetricId(Long metricId) { this.metricId = metricId; }
    public Integer getPageNumber() { return pageNumber; }
    public void setPageNumber(Integer pageNumber) { this.pageNumber = pageNumber; }
    public Double getXMin() { return xMin; }
    public void setXMin(Double xMin) { this.xMin = xMin; }
    public Double getYMin() { return yMin; }
    public void setYMin(Double yMin) { this.yMin = yMin; }
    public Double getXMax() { return xMax; }
    public void setXMax(Double xMax) { this.xMax = xMax; }
    public Double getYMax() { return yMax; }
    public void setYMax(Double yMax) { this.yMax = yMax; }
    public Integer getRowIndex() { return rowIndex; }
    public void setRowIndex(Integer rowIndex) { this.rowIndex = rowIndex; }
    public Integer getColIndex() { return colIndex; }
    public void setColIndex(Integer colIndex) { this.colIndex = colIndex; }
}
