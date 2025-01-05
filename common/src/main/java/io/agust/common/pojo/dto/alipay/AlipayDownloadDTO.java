package io.agust.common.pojo.dto.alipay;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class AlipayDownloadDTO {

    @Schema(description = "账单类型")
    private String billType;


    @Schema(description = "账单时间 yyyy-MM-dd")
    private String billDate;
}
