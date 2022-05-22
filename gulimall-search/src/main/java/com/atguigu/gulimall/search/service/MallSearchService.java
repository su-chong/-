package com.atguigu.gulimall.search.service;

import com.atguigu.gulimall.search.vo.SearchParam;
import com.atguigu.gulimall.search.vo.SearchResult;

public interface MallSearchService {

    /**
     *
     * @param searchParam 用于search的参数
     * @return 检索结果
     */
    SearchResult search(SearchParam searchParam);
}
