package com.atguigu.gulimall.search;

import com.alibaba.fastjson.JSON;
import com.atguigu.gulimall.search.bean.Account;
import com.atguigu.gulimall.search.config.GulimallElasticSearchConfig;
import lombok.Data;
import lombok.ToString;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.metrics.Avg;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;

@SpringBootTest
class GulimallSearchApplicationTests {

	@Autowired
	RestHighLevelClient client;

	@Test
	void contextLoads() {
	}


	@Test
	void testElastic() {
		System.out.println(client);
	}

	@Test
	public void indexData() throws IOException {
		// 指定index
		IndexRequest indexRequest = new IndexRequest("users");
		// 指定id
		indexRequest.id("1");
		// 指定doc
		User user = new User();
		user.setUsername("xiaoyi");
		user.setAge(18);
		user.setGender("男");
		String jsonString = JSON.toJSONString(user);
		indexRequest.source(jsonString, XContentType.JSON);

		IndexResponse index = client.index(indexRequest, GulimallElasticSearchConfig.COMMON_OPTIONS);
		System.out.println(index);

	}

	@ToString
	@Data
	class User{
		private String username;
		private Integer age;
		private String gender;
	}

	@Test
	public void searchData() throws IOException {
		SearchRequest searchRequest = new SearchRequest();
		// 一.指定index
		searchRequest.indices("bank");

		// 二. 指定检索条件
		SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
		sourceBuilder.query(QueryBuilders.matchQuery("address","mill"));
		sourceBuilder.aggregation(AggregationBuilders.terms("ageAgg").field("age").size(10));
		sourceBuilder.aggregation(AggregationBuilders.avg("balanceAgg").field("balance"));

		searchRequest.source(sourceBuilder);

		// 执行search请求
		SearchResponse response = client.search(searchRequest, GulimallElasticSearchConfig.COMMON_OPTIONS);

		// 分析结果
		SearchHits hits = response.getHits();
		SearchHit[] searchHits = hits.getHits();
		for (SearchHit searchHit : searchHits) {
			String sourceAsString = searchHit.getSourceAsString();
			Account account = JSON.parseObject(sourceAsString, Account.class);
			System.out.println(account);
		}

		Aggregations aggregations = response.getAggregations();

		Terms agg = aggregations.get("ageAgg");
		for (Terms.Bucket bucket : agg.getBuckets()) {
			System.out.println("年龄: " + bucket.getKeyAsString() + "-->" + bucket.getDocCount() + "人");
		}

		Avg avg = aggregations.get("balanceAgg");
		System.out.println("平均薪资：" + avg.getValue());
	}


}
