package com.dc.learning.utils.openai;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("dc.ai.apiv3")
public class ApiV3Provider extends AiProvider {

}
