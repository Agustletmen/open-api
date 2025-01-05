package io.agust.payment.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.agust.common.pojo.po.GoodPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author 15272
 */
@Mapper
public interface GoodsMapper extends BaseMapper<GoodPO> {
}
