package com.mealsubsidy.service.excel;

import com.mealsubsidy.dto.MonthlyOrderRow;
import com.mealsubsidy.dto.MonthlyReportVO;
import com.mealsubsidy.dto.MonthlySummaryVO;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

public final class MonthlyReportExcelWriter {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private MonthlyReportExcelWriter() {
    }

    public static void write(MonthlyReportVO report, OutputStream out) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);

            Sheet summarySheet = workbook.createSheet("月度汇总");
            MonthlySummaryVO s = report.getSummary();
            String[][] summaryRows = {
                    {"账期", nvl(s.getAccountMonth())},
                    {"部门ID", String.valueOf(s.getDeptId())},
                    {"实扣人数", str(s.getUserCount())},
                    {"实扣份数", str(s.getApprovedCount())},
                    {"实扣金额", money(s.getApprovedAmount())},
                    {"退餐份数", str(s.getRefundCount())},
                    {"退餐金额", money(s.getRefundAmount())}
            };
            for (int i = 0; i < summaryRows.length; i++) {
                Row row = summarySheet.createRow(i);
                Cell k = row.createCell(0);
                k.setCellValue(summaryRows[i][0]);
                k.setCellStyle(headerStyle);
                row.createCell(1).setCellValue(summaryRows[i][1]);
            }
            summarySheet.autoSizeColumn(0);
            summarySheet.autoSizeColumn(1);

            Sheet detailSheet = workbook.createSheet("订单明细");
            String[] headers = {"订单号", "员工", "用餐日期", "餐次", "菜品名称(快照)", "菜品单价(快照)", "订单金额", "状态"};
            Row header = detailSheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }
            int r = 1;
            if (report.getDetails() != null) {
                for (MonthlyOrderRow item : report.getDetails()) {
                    Row row = detailSheet.createRow(r++);
                    row.createCell(0).setCellValue(nvl(item.getOrderNo()));
                    row.createCell(1).setCellValue(nvl(item.getRealName()));
                    row.createCell(2).setCellValue(item.getOrderDate() == null ? "" : DATE.format(item.getOrderDate()));
                    row.createCell(3).setCellValue(nvl(item.getMealType()));
                    row.createCell(4).setCellValue(nvl(item.getDishName()));
                    row.createCell(5).setCellValue(money(item.getDishPrice()));
                    row.createCell(6).setCellValue(money(item.getOrderAmount()));
                    row.createCell(7).setCellValue(nvl(item.getOrderStatus()));
                }
            }
            for (int i = 0; i < headers.length; i++) {
                detailSheet.autoSizeColumn(i);
            }
            workbook.write(out);
        }
    }

    private static String nvl(String v) {
        return v == null ? "" : v;
    }

    private static String str(Long v) {
        return v == null ? "0" : String.valueOf(v);
    }

    private static String money(BigDecimal v) {
        return v == null ? "0.00" : v.toPlainString();
    }
}
