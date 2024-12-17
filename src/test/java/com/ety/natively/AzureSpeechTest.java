package com.ety.natively;

import com.ety.natively.properties.AzureProperties;
import com.microsoft.cognitiveservices.speech.*;
import com.microsoft.cognitiveservices.speech.audio.AudioConfig;
import com.microsoft.cognitiveservices.speech.audio.AudioOutputStream;
import com.microsoft.cognitiveservices.speech.audio.PullAudioOutputStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;

@SpringBootTest
public class AzureSpeechTest {

	@Autowired
	private AzureProperties properties;

	@Test
	public void testGenerate() throws InterruptedException, ExecutionException, IOException {
		SpeechConfig speechConfig = SpeechConfig.fromSubscription(properties.getKey(), properties.getRegion());

		speechConfig.setSpeechSynthesisVoiceName(/*"en-US-AndrewMultilingualNeural"*/ "en-US-AlloyTurboMultilingualNeural" /*"en-US-AvaMultilingualNeural"*/);
		speechConfig.setSpeechSynthesisOutputFormat(SpeechSynthesisOutputFormat.Ogg16Khz16BitMonoOpus);

		SpeechSynthesizer speechSynthesizer = new SpeechSynthesizer(speechConfig);

		System.out.println("started");

		String text = """
				弁護側は「前方や左右を確認していたが、発見するのは困難だった」などと無罪を主張しましたが、1審の福島地方裁判所は「湖に浮かんでいる人がいることは予測可能で、前方や左右の見張りを十分に行っていれば、被害者を発見して衝突を回避できた」として、禁錮2年を言い渡しました。
				""";

		SpeechSynthesisResult speechSynthesisResult = speechSynthesizer.SpeakTextAsync(text).get();

		FileOutputStream fos = new FileOutputStream("output.wav");
		fos.write(speechSynthesisResult.getAudioData());
		fos.close();

		if (speechSynthesisResult.getReason() == ResultReason.SynthesizingAudioCompleted) {
			System.out.println("Speech synthesized to speaker for text [" + text + "]");
		}
		else if (speechSynthesisResult.getReason() == ResultReason.Canceled) {
			SpeechSynthesisCancellationDetails cancellation = SpeechSynthesisCancellationDetails.fromResult(speechSynthesisResult);
			System.out.println("CANCELED: Reason=" + cancellation.getReason());

			if (cancellation.getReason() == CancellationReason.Error) {
				System.out.println("CANCELED: ErrorCode=" + cancellation.getErrorCode());
				System.out.println("CANCELED: ErrorDetails=" + cancellation.getErrorDetails());
				System.out.println("CANCELED: Did you set the speech resource key and region values?");
			}
		}

		System.exit(0);
	}
}
