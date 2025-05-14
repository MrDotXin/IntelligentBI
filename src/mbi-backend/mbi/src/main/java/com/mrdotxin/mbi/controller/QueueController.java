package com.mrdotxin.mbi.controller;

import cn.hutool.json.JSON;
import cn.hutool.json.JSONUtil;
import com.mrdotxin.mbi.exception.BusinessException;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

@RestController
@RequestMapping("/queue")
public class QueueController {


    @Resource
    private ThreadPoolExecutor threadPoolExecutor;


    @GetMapping("/get")
    public String get() {
        Map<String, Object> map = new HashMap<>();
        int size = threadPoolExecutor.getQueue().size();
        map.put("队列长度", size);

        long tasks = threadPoolExecutor.getTaskCount();
        map.put("任务数", tasks);

        long completedTasks = threadPoolExecutor.getCompletedTaskCount();
        map.put("已完成任务数", completedTasks);

        long activeCount = threadPoolExecutor.getActiveCount();
        map.put("活跃线程数", activeCount);

        return JSONUtil.toJsonStr(map);
    }


    @PostMapping("/add")
    public void add(@RequestParam("taskId") String tasks) {
        try {
            CompletableFuture.runAsync(() -> {
                // 模拟处理任务
                try {
                    System.out.println("处理任务" + tasks + " " + Thread.currentThread().getName());
                    Thread.sleep(600000);
                    System.out.println(Thread.currentThread().getName() + " 处理任务" + tasks + " 完成");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }, threadPoolExecutor);
        } catch (Exception e) {
            throw new BusinessException(500, "请求失败, 请重试");
        }
    }
}
