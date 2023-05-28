package org.project.es.common.util;

import com.jfinal.aop.Inject;
import com.monitorjbl.xlsx.StreamingReader;
import org.apache.poi.ss.usermodel.*;
import org.project.es.policy.PolicyService;

import java.io.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Administrator
 * 参考：https://www.cnblogs.com/cksvsaaa/p/7280261.html
 */
public class ExcelUtil {
    @Inject
    PolicyService policyService;
    public static void testXlsx(String filePath,String sheetName) throws Exception {
        File file = new File(filePath);
        System.out.println("file------" + file.exists());
        //获取输入流
        InputStream stream = new FileInputStream(file);
        Workbook xssfWorkbook = StreamingReader.builder()
                //缓存到内存中的行数，默认是10
                .rowCacheSize(100)
                //读取资源时，缓存到内存的字节大小，默认是1024
                .bufferSize(4096)
                .open(stream);
        //根据SheetName获取Sheet
        Sheet sheet = xssfWorkbook.getSheet(sheetName);
        List<Row> listRow = new ArrayList<Row>();
        for(Row row : sheet){
            listRow.add(row);
        }
        //根据SheetNum获取Sheet
        sheet = xssfWorkbook.getSheetAt(0);
        int rowSize = listRow.size();
        for(int i=1; i<=372; i++){
            Row row = sheet.getRow(i);
            BigDecimal big15 = BigDecimal.valueOf(row.getCell(15).getNumericCellValue());
            if(big15.compareTo(new BigDecimal("0.00")) == 0){
                String province = row.getCell(0).getStringCellValue();
                String year = BigDecimal.valueOf(row.getCell(1).getNumericCellValue()).stripTrailingZeros().toPlainString();
                String flag1 = year + province;
                BigDecimal sumAmt = new BigDecimal("0.00");
                for (Row cells : listRow) {
                    String amt = cells.getCell(3).getStringCellValue();
                    String year2 = cells.getCell(5).getStringCellValue();
                    String province2 = cells.getCell(6).getStringCellValue();
                    String flag2 = year2.substring(0, 4) + province2;
                    if (flag2.contains(flag1)) {
                        sumAmt = sumAmt.add(new BigDecimal(amt));
                    }
                }
                if(sumAmt.compareTo(new BigDecimal("0.00")) > 0){
                    System.out.println(province + year + "---" + sumAmt);
                }
                //在Excel中写入数据
                row.getCell(15).setCellValue(Double.parseDouble(sumAmt.toPlainString()));
            }
        }
        //在输出流中写入数据: 在写入数据时不要打开该文件，不然会报错另外进程打开了该文件
        OutputStream output = new FileOutputStream(file);
        xssfWorkbook.write(output);
        output.flush();
        output.close();
        stream.close();
    }

    /**
     * 读取excel数据中的policyId列，并同时将每一个id对应存入数据库
     */
    public void setPolicyId(){
        String path = "D:\\Python\\PyCharm-2021.3.2\\Project\\policy_label\\data\\predict_data\\policyinfo.xlsx";
        File file = new File(path);
        System.out.println("file------" + file.exists());
        //获取输入流
        InputStream stream = null;
        try {
            stream = new FileInputStream(file);
            Workbook xssfWorkbook = StreamingReader.builder()
                    //缓存到内存中的行数，默认是10
                    .rowCacheSize(100)
                    //读取资源时，缓存到内存的字节大小，默认是1024
                    .bufferSize(4096)
                    .open(stream);
            Sheet sheet = xssfWorkbook.getSheetAt(0);
            //遍历所有的行
            for (Row row : sheet) {
                System.out.print("开始遍历第" + row.getRowNum() + "行数据：");
                if(row.getRowNum()!=0){
                    //将每一行的policyId列存入数据库(跳过第0行，其中存储字段名)
                    Cell cell=row.getCell(0);
                    long policyId= Long.parseLong(cell.getStringCellValue());
                    System.out.print(policyId);
                    System.out.println(policyService.setPolicyId(row.getRowNum(), policyId));
                    System.out.println("添加结果："+policyService.setPolicyId(row.getRowNum(), policyId));
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println("文件读取失败");
            e.printStackTrace();
        }
    }
}
