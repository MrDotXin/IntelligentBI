# backend

[toc]

---

## 数据库表设计



图表存储

```mysql
create table if not exists chart
(
    id           bigint auto_increment comment 'id' primary key,
   	goal		 text 	null comment'分析目标'
    chartData 	 text 	null comment'数据'
    chartType 	 VARCHAR(128) null comment'图表类型'
    genChart 	 text 	null comment'生成的图标数据'
    genResult	 text 	null comment'生成的分析结论'
    createTime   datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime   datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete     tinyint      default 0                 not null comment '是否删除',
    
) comment '用户' collate = utf8mb4_unicode_ci;
```

##  接入AI功能可能的问题

1.  用户重复提交多次
2.  上传超大文件
3.  AI生成速度慢



### 系统优化

#### 用户上传危险文件, 不符合格式的文件

对接三方API进行文件校验

#### 用户上传超大文件

-   用户传超大文件造成的负担
-   存储超大文件造成的增删改查的负担
    -   可以使用OSS
    -   可以按照用户上传的文件来分表本地存储, 即把每个表对应的字段都单独存储

##### 水平分表(优化)

1.  存储信息表，不把数据存储为字段，而是新建`chart_{图表id}`的数据表

    ```sql
    create table chart_18328732432423 (
    	column type null
    )
    ```

    现在就可以使用`DDL`动态查询这个表了

#### 限流, 分布式限流(优化)

-   限制调用次数
-   短时间内疯狂使用，导致服务器被占满



>   限流 
>
>   -   本地限流
>   -   分布式限流
>
>   限流算法
>
>   -   固定窗口
>   -   滑动窗口
>   -   漏桶
>   -   令牌桶

使用Redisson作为分布式限流工具，使得我们可以对每个用户的具体请求进行限流

```java
 // 针对单个用户的限流
 redisLimitManager.doRateLimit("uploadChartRequest " + loginUser.getId());
```

最后加到代码中其实也就这一行.

#### 服务异步化

>-   用户等待时间很长
>-   业务服务器处理能力有限，资源紧张
>-   调用三方服务处理能力有限

当用户要进行耗时很长的操作，不需要再界面等待，而是把任务保存在数据库中，

随后把用户执行的操作放进`任务队列`中让线程异步执行。

<img src="C:\Users\MrXin\AppData\Roaming\Typora\typora-user-images\image-20250429193457074.png" alt="image-20250429193457074" style="zoom:25%;" />

前端可以查询数据库得到数据

<img src="C:\Users\MrXin\AppData\Roaming\Typora\typora-user-images\image-20250429195029694.png" alt="image-20250429195029694" style="zoom:25%;" />

##### 开始使用线程池

>   线程池基本概念
>
>   >   *线程池*是一种多线程处理形式，处理过程中将任务添加到队列，然后在创建线程后自动启动这些任务。



如果我们是`Spring boot`项目, 我们可以使用`ThreadPoolExecutor`来创建线程池

```java
    public ThreadPoolExecutor(int corePoolSize, // 核心线程数
                              int maximumPoolSize, // 最大线程数
                              long keepAliveTime, // 空闲线程的存活时间
                              TimeUnit unit,	// 时间单位
                              BlockingQueue<Runnable> workQueue, // 工作队列
                              ThreadFactory threadFactory, // 生成线程的工厂
                              RejectedExecutionHandler handler // 拒绝策略
                             ) {
```

>   确定线程池参数
>
>   对于IO密集型: 主要是网络/硬盘/内存 读写, 经验值 2 * n
>
>   对于CPU(计算)密集型: 任务比较消耗资源, 经验值为CPU核心数 + 1

##### 业务流程二次改造(线程池 + 异步) (优化)

-   工作流程
    1.  给`chart`表新增`状态`、`任务执行信息`等字段
    2.  用户点击智能分析的提交按钮，先将任务存储到数据库，然后**提交任务**
    3.  任务: 先修改图表状态，随后执行，最后修改结果, 这个和`OJ判题系统`有异曲同工之妙

    4.  用户可以**查看**所有图表的信息和状态
    5.  用户可以修改生成失败的图表信息，并**重新生成**

-   可能的问题
    1.  用户提交任务，但是没有进队列, 使得前端一直等待
    2.  AI生成脏数据导致生成出
-   优化
    1.  guava Retrying 
    2.  异常处理AI生成的内容
    3.  使用`xxl-job`, `spring schedule` 等技术重新提交请求
    4.  增加超时时间
    5.  `反向压力`, 通过调用的服务状态选择当前系统策略, 比如根据AI服务的当前任务数来控制核心线程数
    6.  前端`轮询`
    7.  使用`websocket`、`server side event(SSE)`实时通知

>   反向压力
>
>   反向压力是一种动态调整系统负载的机制，主要用于解决服务调用方与处理方之间的能力不匹配问题。其核心思想是通过反馈机制控制请求流量，避免系统过载或资源浪费。**属于系统设计中的流量控制与资源管理机制**，尤其在**高并发、实时数据处理和分布式系统**中至关重要

##### 业务流程三次改造(分布式消息队列 ) (优化)

-   问题
    -   无法集中限制, 改成分布式项目
    -   任务由于是放进内存中，可能会丢失
    -   系统业务扩充导致耗时任务增加，系统会越来越复杂(出现资源抢占)

使用分布式消息队列可以解耦提交和生成的流程, 直接使用rabbitMQ改造即可

```java
    @RabbitListener(queues = "ChartGenQueue", ackMode = "MANUAL")
    public void receiveMessage(String message, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {

        try {
            long chartId = Long.parseLong(message);
            try {
                Chart chart = chartService.getById(chartId);
                ThrowUtils.throwIf(Objects.isNull(chart), new RuntimeException("请求的数据不存在"));
                chartService.changeGenState(chartId, ChartGenStateEnum.GENERATING.getValue());

                List<String> prompt = chartService.buildChartAiPrompt(chart);

                String outcome = aiChartService.doAIChart(prompt);
                BIChartResponse biChartResponse = chartService.buildResponseWithGenResult(outcome);

                chartService.updateGenResultAndState(chart.getId(), biChartResponse.getChartResult(), biChartResponse.getChartData());

                channel.basicAck(tag, false);
            } catch (Exception e) {
                channel.basicNack(tag, false, false);
                chartService.changeGenState(chartId, ChartGenStateEnum.ERROR.getValue());
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "无法生成 " + e.getMessage() + " 请重试!");
            }
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "无法生成 " + e.getMessage() + " 请重试!");
        }
    }
```

