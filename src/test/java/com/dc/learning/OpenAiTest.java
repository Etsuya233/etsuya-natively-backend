package com.dc.learning;

import com.dc.learning.utils.openai.ApiV3Provider;
import com.dc.learning.utils.openai.TextAi;
import com.dc.learning.utils.openai.TextRequest;
import com.dc.learning.utils.openai.TextResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
public class OpenAiTest {

	@Autowired
	private ApiV3Provider apiV3Provider;

	@Test
	public void test(){
		TextRequest request = TextRequest.builder()
				.model("gpt-4o-mini")
				.messages(List.of(
						TextRequest.Message.builder().role("user").content("hey!").build()
				))
				.build();
		TextResponse response = TextAi.ai(apiV3Provider, request);
		System.out.println(response);
	}
}
