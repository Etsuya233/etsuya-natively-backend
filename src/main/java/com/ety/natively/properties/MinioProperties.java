package com.ety.natively.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "natively.minio")
@Data
public class MinioProperties {
	private String endpoint;
	private String accessKey;
	private String secretKey;
	private String publicPrefix;
}
