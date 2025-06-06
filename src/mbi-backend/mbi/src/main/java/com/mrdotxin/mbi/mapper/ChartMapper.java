package com.mrdotxin.mbi.mapper;

import com.mrdotxin.mbi.model.entity.Chart;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
* @author MrXin
* @description 针对表【chart(图表信息表)】的数据库操作Mapper
* @createDate 2025-04-18 18:49:07
* @Entity com.mrdotxin.mbi.model.entity.Chart
*/
public interface ChartMapper extends BaseMapper<Chart> {

    // List<Map<String, Object>> queryChartData(@Param("querySql") String querySql);
}




