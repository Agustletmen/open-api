package io.agust.payment.controller;


import cn.hutool.json.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.diagnosis.DiagnosisUtils;
import com.alipay.api.domain.AlipayDataDataserviceBillDownloadurlQueryModel;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.*;
import com.alipay.api.response.*;
import io.agust.payment.config.AlipayProperties;
import io.agust.common.pojo.dto.alipay.AlipayDownloadDTO;
import io.agust.common.pojo.dto.alipay.AlipayPayDTO;
import io.agust.common.pojo.dto.alipay.AlipayRefundDTO;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/alipay")
@RequiredArgsConstructor
public class AlipayController {
    //Alipay SDK提供的Client，负责调用支付宝的API
    private AlipayClient alipayClient;

    private static final String GATEWAY_URL = "https://openapi-sandbox.dl.alipaydev.com/gateway.do";
    private static final String FORMAT = "JSON";
    private static final String CHARSET = "UTF-8";
    private static final String SIGN_TYPE = "RSA2";//签名方式


    private final AlipayProperties alipayProperties;

    @PostConstruct // 只在类初始化的时候构建一次 AlipayClient 实例
    private void initAlipayClient() {
        if (alipayClient == null) {
            alipayClient = new DefaultAlipayClient(
                    GATEWAY_URL,
                    alipayProperties.getAppId(),
                    alipayProperties.getMerchantPrivateKey(),
                    FORMAT,
                    CHARSET,
                    alipayProperties.getAlipayPublicKey(),
                    SIGN_TYPE
            );
        }
    }


    //http://localhost:8888/alipay/pay?subject=xxx&outTraceNo=xxx&totalAmount=xxx
    //http://localhost:8888/alipay/pay?subject=测试支付接口&outTraceNo=123456789&totalAmount=0.01
    @Operation(summary = "alipay.trade.page.pay(统一收单下单并支付页面接口)")
    @GetMapping("/pay")
    public void pay(AlipayPayDTO aliPay, HttpServletResponse httpResponse) throws Exception {
        AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();

        //异步接收地址，仅支持http/https，公网可访问 http://r4jv9r.natappfree.cc/alipay/notify
        request.setNotifyUrl(alipayProperties.getNotifyUrl());

        //同步跳转地址，仅支持http/https  http://127.0.0.1:8888/user/userinfo.html
        //request.setReturnUrl(alipayProperties.getReturnUrl());

        /******必传参数******/
        JSONObject bizContent = new JSONObject();
        // FIXME 前端传入的时间戳可能并不唯一（并发问题），后续可修改为后端生成outTraceNo
        bizContent.set("out_trade_no", aliPay.getOutTraceNo()); //商户订单号（我们自己生成的订单编号，前端使用时间戳传入），商家自定义，保持唯一性
        bizContent.set("total_amount", aliPay.getTotalAmount()); //支付金额（订单的总金额），最小值0.01元
        bizContent.set("subject", aliPay.getSubject()); // 订单标题（支付的名称），不可使用特殊符号
        bizContent.set("product_code", "FAST_INSTANT_TRADE_PAY");  // 电脑网站支付场景固定传值FAST_INSTANT_TRADE_PAY


        /******可选参数******/
        //bizContent.put("time_expire", "2022-08-01 22:00:00");

        //// 商品明细信息，按需传入
        //JSONArray goodsDetail = new JSONArray();
        //JSONObject goods1 = new JSONObject();
        //goods1.put("goods_id", "goodsNo1");
        //goods1.put("goods_name", "子商品1");
        //goods1.put("quantity", 1);
        //goods1.put("price", 0.01);
        //goodsDetail.add(goods1);
        //bizContent.put("goods_detail", goodsDetail);

        //// 扩展信息，按需传入
        //JSONObject extendParams = new JSONObject();
        //extendParams.put("sys_service_provider_id", "2088511833207846");
        //bizContent.put("extend_params", extendParams);

        request.setBizContent(bizContent.toString());

        // 执行请求，拿到响应的结果，返回给浏览器
        String form = "";
        try {
            AlipayTradePagePayResponse alipayTradePagePayResponse = alipayClient.pageExecute(request);
            form = alipayTradePagePayResponse.getBody(); // 调用SDK生成表单
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        httpResponse.setContentType("text/html;charset=" + CHARSET);
        httpResponse.getWriter().write(form); // 直接将完整的表单html输出到页面
        httpResponse.getWriter().flush();
        httpResponse.getWriter().close();
    }

    @Operation(summary = "回调接口，提供给支付宝使用的")
    @PostMapping("/notify")  // 注意这里必须是POST接口
    public String payNotify(HttpServletRequest request) throws Exception {
        if (request.getParameter("trade_status").equals("TRADE_SUCCESS")) {
            System.out.println("=========支付宝异步回调========");

            Map<String, String> params = new HashMap<>();
            Map<String, String[]> requestParams = request.getParameterMap();
            for (String name : requestParams.keySet()) {
                params.put(name, request.getParameter(name));
                // System.out.println(name + " = " + request.getParameter(name));
            }

            String outTradeNo = params.get("out_trade_no"); //商户订单号
            String gmtPayment = params.get("gmt_payment");  //买家付款时间
            String alipayTradeNo = params.get("trade_no");  //支付宝交易凭证号


            // 验证签名
            String sign = params.get("sign");
            String content = AlipaySignature.getSignCheckContentV1(params);
            boolean checkSignature = AlipaySignature.rsa256CheckContent(content, sign, alipayProperties.getAlipayPublicKey(), "UTF-8");

            // 支付宝验签
            if (checkSignature) {
                // 验签通过
                System.out.println("交易名称: " + params.get("subject"));
                System.out.println("交易状态: " + params.get("trade_status"));
                System.out.println("支付宝交易凭证号: " + params.get("trade_no"));
                System.out.println("商户订单号: " + params.get("out_trade_no"));
                System.out.println("交易金额: " + params.get("total_amount"));
                System.out.println("买家在支付宝唯一id: " + params.get("buyer_id"));
                System.out.println("买家付款时间: " + params.get("gmt_payment"));
                System.out.println("买家付款金额: " + params.get("buyer_pay_amount"));
            }
        }
        return "success";
    }


    @Operation(summary = "alipay.trade.refund(统一收单交易退款接口)")
    @GetMapping("/refund")
    //http://localhost:8888/alipay/refund?traceNo=202402172201497170502402959&outRequestNo=HZ01RF001&refundAmount=11
    public void refund(AlipayRefundDTO alipayRefundDto, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws AlipayApiException {
        AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
        JSONObject bizContent = new JSONObject();
        bizContent.put("trade_no", alipayRefundDto.getTraceNo()); //bizContent.put("trade_no", "2021081722001419121412730660");
        bizContent.put("refund_amount", alipayRefundDto.getRefundAmount()); //bizContent.put("refund_amount", 0.01);
        bizContent.put("out_request_no", alipayRefundDto.getOutRequestNo());//bizContent.put("out_request_no", "HZ01RF001");

        // 返回参数选项，按需传入
        //JSONArray queryOptions = new JSONArray();
        //queryOptions.add("refund_detail_item_list");
        //bizContent.put("query_options", queryOptions);

        request.setBizContent(bizContent.toString());
        AlipayTradeRefundResponse response = alipayClient.execute(request);
        if (response.isSuccess()) {
            System.out.println("调用成功");
        } else {
            System.out.println("调用失败");
        }
    }


    // 可使用该接口查询自已通过alipay.trade.refund提交的退款请求是否执行成功。
    @Operation(summary = "alipay.trade.fastpay.refund.query(统一收单交易退款查询)")
    @GetMapping("/refund/confirm")
    public void refundConfirm() throws AlipayApiException {
        AlipayTradeFastpayRefundQueryRequest request = new AlipayTradeFastpayRefundQueryRequest();
        JSONObject bizContent = new JSONObject();
        bizContent.put("trade_no", "2021081722001419121412730660");
        bizContent.put("out_request_no", "HZ01RF001");

        //// 返回参数选项，按需传入
        //JSONArray queryOptions = new JSONArray();
        //queryOptions.add("refund_detail_item_list");
        //bizContent.put("query_options", queryOptions);

        request.setBizContent(bizContent.toString());
        AlipayTradeFastpayRefundQueryResponse response = alipayClient.execute(request);
        if (response.isSuccess()) {
            System.out.println("调用成功");
        } else {
            System.out.println("调用失败");
        }
    }


    // 交易关闭 用于交易创建后，用户在一定时间内未进行支付，可调用该接口直接将未付款的交易进行关闭。
    //http://localhost:8888/alipay/close/216c749ba1584131b18cd29976138d66.00
    @Operation(summary = "alipay.trade.close(统一收单交易关闭接口)")
    @GetMapping("/close/{tradeNo}")
    public void close(@PathVariable("tradeNo") String trade_no) throws AlipayApiException {
        AlipayTradeCloseRequest request = new AlipayTradeCloseRequest();
        JSONObject bizContent = new JSONObject();
        //bizContent.put("trade_no", "2013112611001004680073956707");
        bizContent.put("out_trade_no", trade_no);
        bizContent.put("trade_no", null);
        request.setBizContent(bizContent.toString());
        AlipayTradeCloseResponse response = alipayClient.execute(request);
        if (response.isSuccess()) {
            System.out.println("调用成功");
        } else {
            System.out.println("调用失败");
        }
    }


    // 目前支付宝沙箱暂不支持下载对账文件，需要到正式环境才可使用
    @Operation(summary = "alipay.data.dataservice.bill.downloadurl.query(查询对账单下载地址)")
    @GetMapping("/bill_download") //http://localhost:8888/alipay/bill_download?billType=trade&billDate=2024-02-17
    public void billDownload(AlipayDownloadDTO alipayDownloadDto) throws AlipayApiException {
        AlipayDataDataserviceBillDownloadurlQueryRequest request = new AlipayDataDataserviceBillDownloadurlQueryRequest();
        AlipayDataDataserviceBillDownloadurlQueryModel model = new AlipayDataDataserviceBillDownloadurlQueryModel();
        //model.setSmid("2088123412341234");
        //model.setBillType("trade");
        //model.setBillDate("2016-04-05");

        //model.setSmid("2088123412341234"); //二级商户smid，这个参数只在bill_type是trade_zft_merchant时才能使用
        model.setBillType(alipayDownloadDto.getBillType());
        model.setBillDate(alipayDownloadDto.getBillDate());
        request.setBizModel(model);
        AlipayDataDataserviceBillDownloadurlQueryResponse response = alipayClient.execute(request);
        //System.out.println(response.getBody());
        System.out.println(response.getBillDownloadUrl());
        if (response.isSuccess()) {
            System.out.println("调用成功");
        } else {
            System.out.println("调用失败");
            // sdk版本是"4.38.0.ALL"及以上,可以参考下面的示例获取诊断链接
            String diagnosisUrl = DiagnosisUtils.getDiagnosisUrl(response);
            System.out.println(diagnosisUrl);
        }
    }


    @Operation(summary = "alipay.trade.query(统一收单交易查询)")
    @GetMapping("/query")
    public void query() throws AlipayApiException {
        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        request.setBizContent("{" +
                "  \"out_trade_no\":\"20150320010101001\"," +
                "  \"trade_no\":\"2014112611001004680 073956707\"," +
                "  \"query_options\":[" +
                "    \"trade_settle_info\"" +
                "  ]" +
                "}");
        AlipayTradeQueryResponse response = alipayClient.execute(request);
        if (response.isSuccess()) {
            System.out.println("调用成功");
        } else {
            System.out.println("调用失败");
        }
    }
}
