package com.atguigu.gulimall.order.config;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.atguigu.gulimall.order.vo.PayVo;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "alipay")
@Component
@Data
public class AlipayTemplate {

    // 应用ID,您的APPID，收款账号既是您的APPID对应支付宝账号
    public static String app_id = "2021000119666922";

    // 商户私钥，您的PKCS8格式RSA2私钥
    public static String merchant_private_key = "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCAYewRIjZMg6/Xx96H2fekSXAAH7L7p39n4G79WJwHkJEIvjHCoiyk24/9eIfaBzkemQQOBzKlIsl8t4hBi9Ot0cBdTZLiArfX+Msu5O8mOj6LnT5kzu/u5+/Ex7as9tG/dPzA7aAR0m/IyDgxYrvVIaL41MtG/1Hv7aOJ2ttTzIuj72/uguH8OLbYo091H9Kfdm6dF9ZyW/91X3PCeVpiCWdWlnz62G/BJDcGm3l5D9V0vl5mj83rhJby3f0Y5N/mRa3X2Gqz1Bx9NcS631ZjbOnrPEa9BtIruiouOOoyzi4ZSqaq0ROh+3u7XswmKQ9G8hkOHLr/+uF36QVEVNuRAgMBAAECggEALiUEONRXOAFNPv30GlhaDo7m1uSN7K4zuR6ORjmHO6DWwJG8SP0JlufkRXMBQXi/KyRMtM4ZggmS6mT9cLHVj/CIeADd/qaDdflS7rQS0Wa1/HC0cH+A9hofsFcobdr5YRunjaDbnAxLaZuttvKKGP1SGqZBum8jAF4mHfKwStoBEttN8J0pLsN/6RsnYRvNiDlz01ig50gtdSojAGFUd0IDDdLsGgS+otLeuUhO66xJLOQu5hb+lWLkUTD57SgC6NKW06YISD1mIMlBAjM/OrMED5hdEKbkjDUeqX0orYhSMnGnARGBG8TTGgkm2X7LU8yC897RK/RnmgsG6i/3kQKBgQDhnT8LIP7NaYx4LWeYtt1R2YT3druOZRlhQudus7xdAdIp214jufXH2wgUxPzDAmeB0HZ1H0yZAxNuWvhEBNKP8UGHMc5pSGuAo9G6uqL8L62MO8mRoLZJqpS1G7gN83HBfiUKtRPo5qoXFegzIiaDLfUbf+R/iEF7CHB1vbQMxwKBgQCRrE8Y503PJpjIxO9JaT1YQhBJJiNgF5g0PnigZSedV6AF+IZx5CcdiScdeQ5M33l0tdQQSl8x0UGKGP3nAQKeXpc3fbBTbKao0r8XTxbSSoB6IWFxPdTCMzp3Te4UQJE+ilNICBP/ggCX0u5ZsXv+9oSAs/oDnvYXHjH/O5wM5wKBgFYPbGfrWyax12JUN4s6XewF5EGdKegkSm0eXzeMGhMESc2rnxpTqhxlQYzio2Qju6X6BeWhXc+UK8pRqxgPGt7LcWqHpivk8xTr6GmpHdjCuxmeCMKjIkWxau42t5uivEbJvol6DzEtI76QnbU7lqM9WZz2yQuYxxmOW4+eIFsrAoGBAIVmJJzqnH95meopJfO1Ev8yqFVEPprtoG0fVDmVyHw21Q0CtKOxSfyrL4224NiWwVOWexdb8+bb+Mes8T5M14gNmS6ww/rxgYESVPnUbFKw9QxiayhXdfFNFlpQnBghtKBv7aoF88JE26Qs/iZW+PTX7SkrJ2WPeU5cUW6ouNO7AoGADJcpSRgYD301PAx4xqoRNQdBW5lqupyAlmpetEvl8hfJ2J1mp+qpuCqlW4aUVWkGO15GalVOg29c/5ZFQb+epXgWoTnRE0Wb8d0/WTGypM2uzrd9DCkc0zk2HmltfsZB8cwPLGcJD5b2Y/mqCdr64p3doGGaS6C/qw1JC+XuRyY=";

    // 支付宝公钥,查看地址：https://openhome.alipay.com/platform/keyManage.htm 对应APPID下的支付宝公钥。
    public static String alipay_public_key = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAnXJwk/96ZNbgzmddp95rAPvEqySI65VsDLw0ry9Phh5eSeWcvSxMtVuW1z4B/lLyU8waJqFnoMGoEfYcBhSzXMsLUEAIbZc5RPOKKa4+hy8Kl/0BYNJnFUw0lASMvpUrSzaqQ+ILi9CQFbpoKL85jOP2mkvWRV+D6eNNJLW5/irz0Qyz68tFR6VX6rlmm1LZo5XDzZw+sKsKCGC6Z/vALIEFopGB1G9JQ+mJgLQVD/Mj0npddWd65hlsvdT4MU2RXIpa/SODYu5WlftxI0Ov2J4XPPCYdn4/gXMveJGCJaK+Vdb0MOM8aA+Swlu5lR2RipYxK5k5CA4IYHqo0UiW7wIDAQAB";

    // 服务器异步通知页面路径  需http://格式的完整路径，不能加?id=123这类自定义参数，必须外网可以正常访问
    public static String notify_url = "http://8fcw0b8oc4.51xd.pub/alipay_trade_page_pay_JAVA_UTF_8_Web_exploded/notify_url.jsp";

    // 页面跳转同步通知页面路径 需http://格式的完整路径，不能加?id=123这类自定义参数，必须外网可以正常访问
    public static String return_url = "http://8fcw0b8oc4.51xd.pub/alipay_trade_page_pay_JAVA_UTF_8_Web_exploded/return_url.jsp";

    // 签名方式
    public static String sign_type = "RSA2";

    // 字符编码格式
    public static String charset = "utf-8";

    // 支付宝网关
    public static String gatewayUrl = "https://openapi.alipaydev.com/gateway.do";

    // 支付宝网关
    public static String log_path = "C:\\";

    public String pay(PayVo vo) throws AlipayApiException {
        //获得初始化的AlipayClient
        AlipayClient alipayClient = new DefaultAlipayClient(gatewayUrl, app_id, merchant_private_key, "json", charset, alipay_public_key, sign_type);

        //设置请求参数
        AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();
        alipayRequest.setReturnUrl(return_url);
        alipayRequest.setNotifyUrl(notify_url);

        //商户订单号，商户网站订单系统中唯一订单号，必填
        String out_trade_no = vo.getOut_trade_no();
        //付款金额，必填
        String total_amount = vo.getTotal_amount();
        //订单名称，必填
        String subject = vo.getSubject();
        //商品描述，可空
        String body = vo.getBody();

        alipayRequest.setBizContent("{\"out_trade_no\":\"" + out_trade_no + "\","
                + "\"total_amount\":\"" + total_amount + "\","
                + "\"subject\":\"" + subject + "\","
                + "\"body\":\"" + body + "\","
                + "\"product_code\":\"FAST_INSTANT_TRADE_PAY\"}");

        //若想给BizContent增加其他可选请求参数，以增加自定义超时时间参数timeout_express来举例说明
        //alipayRequest.setBizContent("{\"out_trade_no\":\""+ out_trade_no +"\","
        //		+ "\"total_amount\":\""+ total_amount +"\","
        //		+ "\"subject\":\""+ subject +"\","
        //		+ "\"body\":\""+ body +"\","
        //		+ "\"timeout_express\":\"10m\","
        //		+ "\"product_code\":\"FAST_INSTANT_TRADE_PAY\"}");
        //请求参数可查阅【电脑网站支付的API文档-alipay.trade.page.pay-请求参数】章节

        //请求
        String result = alipayClient.pageExecute(alipayRequest).getBody();

        //输出
        System.out.println("支付宝的响应" + result);

        return result;
    }

}
