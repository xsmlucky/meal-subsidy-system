package com.mealsubsidy.dto;

import lombok.Data;

import java.util.List;

@Data
public class MonthlyReportVO {

    private MonthlySummaryVO summary;
    private List<MonthlyOrderRow> details;
}
