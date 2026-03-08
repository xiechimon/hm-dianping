package com.xmon;

import com.xmon.service.impl.ShopServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@SpringBootTest
class HmDianpingApplicationTests {

    @Resource
    private ShopServiceImpl shopService;

    @Test
    void saveShop() throws InterruptedException {
        shopService.saveData2Redis(1L, 10L);
    }

}
