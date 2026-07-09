package com.platform.gateway.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class MetricSaveRequest {

    @NotBlank
    @JsonProperty("metric_name")
    private String metricName;

    @NotBlank
    @JsonProperty("raw_value")
    private String rawValue;

    @JsonProperty("unit")
    private String unit;

    @NotNull
    @JsonProperty("source_doc_id")
    private Long sourceDocId;

    @NotNull
    @JsonProperty("page_number")
    private Integer pageNumber;

    @NotNull
    @JsonProperty("x_min")
    private Double xMin;

    @NotNull
    @JsonProperty("y_min")
    private Double yMin;

    @NotNull
    @JsonProperty("x_max")
    private Double xMax;

    @NotNull
    @JsonProperty("y_max")
    private Double yMax;

    public MetricSaveRequest() {}

    public String getMetricName() { return metricName; }
    public void setMetricName(String metricName) { this.metricName = metricName; }
    public String getRawValue() { return rawValue; }
    public void setRawValue(String rawValue) { this.rawValue = rawValue; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public Long getSourceDocId() { return sourceDocId; }
    public void setSourceDocId(Long sourceDocId) { this.sourceDocId = sourceDocId; }
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
}
