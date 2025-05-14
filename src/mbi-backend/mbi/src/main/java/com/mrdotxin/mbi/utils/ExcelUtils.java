package com.mrdotxin.mbi.utils;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.support.ExcelTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.ResourceUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class ExcelUtils {

    public static String convertToCSV(MultipartFile multipartFile) {
        try {
            List<Map<Integer, String>> lists = EasyExcel.read(multipartFile.getInputStream())
                    .excelType(ExcelTypeEnum.XLSX)
                    .sheet()
                    .headRowNumber(0)
                    .doReadSync();
            if (CollUtil.isEmpty(lists)) {
                return "";
            }

             StringBuilder stringBuilder = new StringBuilder();
            LinkedHashMap<Integer, String> map = (LinkedHashMap<Integer, String>) lists.get(0);
            List<String> headerList = map.values().stream().filter(ObjectUtils::isNotEmpty).collect(Collectors.toList());

            stringBuilder.append(StringUtils.join(headerList, ','));
            stringBuilder.append("\n");
            for (int i = 1; i < lists.size(); i ++) {
                LinkedHashMap<Integer, String> mp = (LinkedHashMap<Integer, String>) lists.get(i);
                List<String> dataList = mp.values().stream().filter(ObjectUtils::isNotEmpty).collect(Collectors.toList());

                stringBuilder.append(StringUtils.join(dataList, ','));
                stringBuilder.append("\n");
            }

            return stringBuilder.toString();
        } catch (Exception e) {
            log.error("表格处理错误");
        }

        return "";
    }
}
