package com.ety.natively.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "natively.elastic")
@Data
public class ElasticProperties {
	private String serverUrl;
}
