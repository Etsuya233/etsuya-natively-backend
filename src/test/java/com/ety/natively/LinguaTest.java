package com.ety.natively;

import com.github.pemistahl.lingua.api.Language;
import com.github.pemistahl.lingua.api.LanguageDetector;
import com.github.pemistahl.lingua.api.LanguageDetectorBuilder;

import java.util.SortedMap;

public class LinguaTest {
	public static void main(String[] args) {
		LanguageDetector detector = LanguageDetectorBuilder
				.fromLanguages(Language.ENGLISH, Language.CHINESE, Language.FRENCH,
						Language.JAPANESE, Language.KOREAN)
				.build();
		SortedMap<Language, Double> map = detector.computeLanguageConfidenceValues("By default, Lingua returns the most likely language for a given input text. However, there are certain words that are spelled the same in more than one language. ");
		System.out.println(map);
	}
}
