package com.atguigu.gulimall.thirdparty;

import com.aliyun.oss.OSS;
import com.atguigu.gulimall.thirdparty.component.SmsComponent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;

@SpringBootTest
class GulimallThirdPartyApplicationTests {

    @Test
    void contextLoads() {


    }

    @Autowired
    OSS ossClient;

    @Autowired
    SmsComponent smsComponent;


    @Test
    public void testSendCode() {
        smsComponent.sendSmsCode("18657168436", "887799");
    }

    @Test
    public void testUpload() throws FileNotFoundException {
//        // Endpoint以杭州为例，其它Region请按实际情况填写。
//        String endpoint = "oss-cn-beijing.aliyuncs.com";
//        // 云账号AccessKey有所有API访问权限，建议遵循阿里云安全最佳实践，创建并使用RAM子账号进行API访问或日常运维，请登录 https://ram.console.aliyun.com 创建。
//        String accessKeyId = "LTAI4FwvfjSycd1APnuG9bjj";
//        String accessKeySecret = "O6xaxyiWfSIitcOkSuK27ju4hXT5Hl";
//
//        // 创建OSSClient实例。
//        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);

        // 上传文件流。
//        InputStream inputStream = new FileInputStream("/Users/suzhenghua/Downloads/testUpload.txt");

//        ossClient.putObject("gulimall-20210904", "hahaha.jpg", inputStream);

        String content = "/Users/suzhenghua/Downloads/testUpload.txt";
        ossClient.putObject("gulimall-20210904", "abc", new ByteArrayInputStream(content.getBytes()));

        // 关闭OSSClient。
        ossClient.shutdown();

        System.out.println("上传完成...");
    }
}
