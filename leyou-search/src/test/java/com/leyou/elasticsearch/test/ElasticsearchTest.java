package com.leyou.elasticsearch.test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.leyou.common.pojo.PageResult;
import com.leyou.item.bo.SpuBo;
import com.leyou.search.client.GoodsClient;
import com.leyou.search.pojo.Goods;
import com.leyou.search.repository.GoodsRepository;
import com.leyou.search.service.SearchService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;
import java.util.stream.Collectors;

@SpringBootTest
@RunWith(SpringRunner.class)
public class ElasticsearchTest {

    @Autowired
    private ElasticsearchTemplate template;

    @Autowired
    private GoodsRepository goodsRepository;

    @Autowired
    private GoodsClient goodsClient;

    @Autowired
    private SearchService searchService;
    
    @Test
    public void test(){
        this.template.createIndex(Goods.class);
        this.template.putMapping(Goods.class);

        Integer page = 1;
        Integer rows = 100;

        do{
        //分页查询spu，获取分页结果集
        PageResult<SpuBo> result = this.goodsClient.querySpuByPage(null, null, page, rows);
//        获取当前页的数据
        List<SpuBo> spuBos = result.getItems();
        //处理List<SpuBo> ==> List<Goods>
        List<Goods> goodsList = spuBos.stream().map(spuBo -> {
            try {
                return this.searchService.buildGoods(spuBo);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
            return null;
        }).collect(Collectors.toList());

        //执行新增数据的方法
        this.goodsRepository.saveAll(goodsList);
        rows = spuBos.size();
        page++;
        }while (rows == 100);

    }
}
