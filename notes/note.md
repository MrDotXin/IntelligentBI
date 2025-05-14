# 智能BI

[toc]

---

## 功能性需求

-   [x]  智能分析： 用户输入目标和原始数据, 自动生成图表和分析结论
-   [ ] 图表生成异步化(消息队列)
-   [x] 图表管理
-   [ ] 对接AI
-   [ ] 安全

##  技术栈

前端

1.  React
2.  Umi + Ant Design pro
3.  可视化开发库(Echarts + highCharts + AntV)
4.  umi openapi 代码生成

后端

1.  Spring Boot 
2.  MySQL 
3.  MyBatisPlus
4.  RabbitMQ
5.  AI (Open AI / 现成的AI接口)
6.  Excel的上传和数据的解析(Easy Excel)
7.  Swagger + Knife4j 项目接口文档
8.  Hutool 

---

## 智能分析业务开发

-   业务流程
    -   分析目标
    -   上传原始数据
        -   更加精细地控制图表
-   后端校验
    -   检验用户输入的合法性
    -   成本控制
-   把处理后的数据输入给AI模型(调用AI 接口), AI提供图表信息，结论文本
-   返回给前端





