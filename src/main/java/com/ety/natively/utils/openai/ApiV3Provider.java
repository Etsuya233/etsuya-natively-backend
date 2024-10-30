package com.ety.natively.utils.openai;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("dc.ai.apiv3")
public class ApiV3Provider extends AiProvider {

}
