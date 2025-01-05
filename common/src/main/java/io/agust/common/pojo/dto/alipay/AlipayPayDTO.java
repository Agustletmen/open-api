package io.agust.common.pojo.dto.alipay;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class AlipayPayDTO {

    @Schema(description = "商户订单号（我们自己生成的订单编号），商家自定义，保持唯一性")
    private String outTraceNo;

    @Schema(description = "订单标题（支付的名称），不可使用特殊符号")
    private String subject;

    @Schema(description = "支付金额（订单的总金额），最小值0.01元")
    private double totalAmount;

    //@Schema(description = "支付宝订单号")
    //private String alipayTraceNo;

    /**
     * 充值的用户ID
     */
    private long userId;
}
