package com.ety.natively.domain;

import lombok.Data;

@Data
public class ElasticVo<T> {
	T data;
	Integer from;

	public static <T> ElasticVo<T> of(T data, Integer from) {
		ElasticVo<T> elasticVo = new ElasticVo<>();
		elasticVo.setData(data);
		elasticVo.setFrom(from);
		return elasticVo;
	}
}
