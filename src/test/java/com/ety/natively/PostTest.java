package com.ety.natively;

import cn.hutool.json.JSONUtil;
import com.ety.natively.service.IPostService;
import com.ety.natively.utils.BaseContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.CSVWriterBuilder;
import com.opencsv.ICSVWriter;
import org.junit.jupiter.api.Test;
import org.mockito.internal.util.reflection.FieldReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.*;
import java.util.ArrayList;

@SpringBootTest
public class PostTest {

	@Autowired
	private IPostService postService;
	@Autowired
	private ObjectMapper objectMapper;

	@Test
	public void generateVerificationCode(){
		BaseContext.setUserId(1852303734730272770L);

		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 2; i++) {
			String code = postService.getCreatePostVerificationCode();
			sb.append(code).append(",");
		}
		System.out.println(sb);
	}

	@Test
	public void generateQuestionCsv() throws Exception {
		File file = new File("C:\\Users\\Etsuya\\Softwares\\apache-jmeter-5.6.3\\bin\\natively\\question.csv");
		File fileOutput = new File("C:\\Users\\Etsuya\\Softwares\\apache-jmeter-5.6.3\\bin\\natively\\questionWIthCode.csv");

		BaseContext.setUserId(1852303734730272770L);

		ArrayList<String> codes = new ArrayList<>();
		for (int i = 0; i < 100; i++) {
			String code = postService.getCreatePostVerificationCode();
			codes.add(code);
		}

		FileReader fr = new FileReader(file);
		BufferedReader br = new BufferedReader(fr);
		FileWriter fw = new FileWriter(fileOutput);

		String abandoned = br.readLine();

		ArrayList<String[]> list = new ArrayList<>();
		for (int i = 0; i < 100; i++) {
			String[] data = new String[4];
			String line = br.readLine();
			String lang = line.substring(0, 2);
			int titleEndIndex = line.indexOf(",", 3);
			String title = line.substring(3, titleEndIndex);
			String content = line.substring(titleEndIndex + 1);

			String code = codes.get(i);
			data[0] = lang;
			data[1] = title;
			data[2] = content;
			data[3] = code;
			list.add(data);
		}

		ICSVWriter writer = new CSVWriterBuilder(fw)
				.withSeparator(',')
				.build();
		writer.writeAll(list);
	}

}
