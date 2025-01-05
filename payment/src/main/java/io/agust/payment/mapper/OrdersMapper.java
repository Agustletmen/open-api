package io.agust.payment.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.agust.common.pojo.po.OrderPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author 15272
 */
@Mapper
public interface OrdersMapper extends BaseMapper<OrderPO> {
}
