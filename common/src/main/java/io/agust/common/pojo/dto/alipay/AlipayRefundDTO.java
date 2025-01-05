package io.agust.common.pojo.dto.alipay;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class AlipayRefundDTO {

    @Schema(description = "支付宝交易号")  // 2021081722001419121412730660
    private String traceNo;

    /**
     * 请求退款接口时，传入的退款请求号，如果在退款请求时未传入，则该值为创建交易时的商户订单号。
     */
    @Schema(description = "退款请求号")
    private String outRequestNo;

    @Schema(description = "退款金额")
    private double refundAmount;
}
