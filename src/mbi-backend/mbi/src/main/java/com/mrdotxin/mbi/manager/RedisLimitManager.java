package com.mrdotxin.mbi.manager;

import com.mrdotxin.mbi.common.ErrorCode;
import com.mrdotxin.mbi.exception.BusinessException;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class RedisLimitManager {
    @Resource
    private RedissonClient redissonClient;

    public void doRateLimit(String key) {
        RRateLimiter rateLimiter = redissonClient.getRateLimiter(key);
        rateLimiter.trySetRate(
                RateType.OVERALL,
                2,
                5,
                RateIntervalUnit.SECONDS
        );

        boolean permission = rateLimiter.tryAcquire(1);
        if (!permission) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUEST);
        }
    }
}
