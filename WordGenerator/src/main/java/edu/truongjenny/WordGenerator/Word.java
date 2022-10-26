package edu.truongjenny.WordGenerator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class Word {
	private String word, pronunciation;
	private String family;
	private String synonyms;
	private boolean isFound = false;

	ArrayList<EngViMeaning> en_vi_meanings = new ArrayList<>();

	private static final String cardPad = "<div class=\"card-word\">";
	private static final String cardPadEnd = "</div>\r\n";

	private static final String wPad = "<div class=\"word\">";
	private static final String wPadEnd = "</div>";
	private static final String photicsPad = "<div class=\"ipa\">Phonetics: ";
	private static final String photicsPadEnd = "</div>";

	private static final String meaningsPad = "<ul class=\"list-meanings\">";
	private static final String meaningsPadEnd = "</ul>";

	private static final String enPad = "<div class=\"meaning-en\">";
	private static final String enPadEnd = "</div>";
	private static final String enviPad = "<div class=\"meaning-vi\">";
	private static final String enviPadEnd = "</div>";
	private static final String enExamplePad = "<li class=\"example\">";
	private static final String enExamplePadEnd = "</li>";

	private static final String answerPad = "<span class=\"answer\">";
	private static final String answerPadEnd = "</span>";

	private static final String fPad = "<div class=\"family\">";
	private static final String fPadEnd = "</div>";
	private static final String sPad = "<div class=\"synonyms\">";
	private static final String sPadEnd = "</div>";

	Word() {
		this.pronunciation = "";
	}

	class EngViMeaning {
		private String word;
		private String vi, en;
		private ArrayList<String> en_examples;

		// ArrayList<String> vi_examples;
		private EngViMeaning(String word) {
			this.word = word;
			this.vi = "";
			this.en = "";
			this.en_examples = new ArrayList<String>();
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder("<li class=\"meaning\">");
			if (!en.isEmpty()) {
				sb.append(enPad).append(en).append(enPadEnd);
			}
			if (!vi.isEmpty()) {
				sb.append(enviPad).append(vi).append(enviPadEnd);
			}
			if (!en_examples.isEmpty()) {
				String word = this.word;
				String answer = answerPad + this.word + answerPadEnd;
				for (String example : this.en_examples) {
					sb.append(enExamplePad).append(example.replaceAll(word, answer)).append(enExamplePadEnd);
				}
			}
			sb.append("</li>");
			return sb.toString();
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(cardPad).append(wPad).append(word).append(wPadEnd).append(photicsPad).append(pronunciation)
				.append(photicsPadEnd);
		if (family != null) {
			sb.append(fPad).append(family).append(fPadEnd);
		}
		if (!en_vi_meanings.isEmpty()) {
			sb.append(meaningsPad);
			for (EngViMeaning envi : en_vi_meanings) {
				sb.append(envi.toString());
			}
			sb.append(meaningsPadEnd);
		}
		if (synonyms != null) {
			sb.append(sPad).append(synonyms).append(sPadEnd);
		}
		sb.append(cardPadEnd);
		return sb.toString();
	}

	// Cac phuong thuc xay dung
	// Ham phu cho ham getWordFromMochi(String word)
	private static JSONObject wordGetJSON(String word) throws JSONException, IOException {
		StringBuilder urlSB = new StringBuilder();
		urlSB.append("https://mochien3.1-api.mochidemy.com/v3.1/words/dictionary-english?key=")
				.append(word.trim().replaceAll(" +", "+")).append("&user_token=notLogin");
		URL url = new URL(urlSB.toString());

		HttpURLConnection http = (HttpURLConnection) url.openConnection();
		http.setRequestProperty("authority", "mochien3.1-api.mochidemy.com");
		http.setRequestProperty("accept", "application/json, text/plain, */*");
		http.setRequestProperty("privatekey", "M0ch1M0ch1_En_$ecret_k3y");

		String inputLine;
		StringBuilder temp = new StringBuilder();
		BufferedReader in = new BufferedReader(new InputStreamReader(http.getInputStream()));
		while ((inputLine = in.readLine()) != null)
			temp.append(inputLine);
		in.close();
		http.disconnect();
		return new JSONObject(temp.toString());
	}

	private static Word jsonGetWordDetails(JSONObject json, Word word_trans) {
		if (!json.getJSONObject("data").getJSONArray("vi").isEmpty()) {// Neu tu do co trong Mochi
			JSONArray data_vi = json.getJSONObject("data").getJSONArray("vi");
			JSONObject temp = ((JSONObject) data_vi.get(0));
			// word_trans.word = temp.get("content").toString(); // Lay tu
			word_trans.pronunciation = "us "
					+ temp.get("phonetic_uk").toString().concat("  uk " + temp.get("phonetic_us").toString()); // Lay //
			word_trans.isFound = true;																						// am
			for (Object data_vi_details : data_vi) { // data_vi la mot mang. Cac phan tu mang chua details cung la mang
														// chua detail la cai minh can
				JSONArray details = ((JSONObject) data_vi_details).getJSONArray("detail");
				for (Object detail : details) {
					EngViMeaning en_vi_mean = word_trans.new EngViMeaning(word_trans.word);
					en_vi_mean.vi += "(" + ((JSONObject) detail).get("position").toString() + ") "; // Tu loai
					en_vi_mean.vi += ((JSONObject) detail).get("trans").toString(); // Nghia
					en_vi_mean.en_examples.add((((JSONObject) detail).get("en_sentence").toString()));
					// en_vi_mean.vi_examples.add(((JSONObject)detail).get("vi_sentence").toString());
					word_trans.en_vi_meanings.add(en_vi_mean);
				}
			}
		}
		return word_trans;
	}

	// Lay tu MochiMochi
	static synchronized void getWordFromMochi(String word, Word word_trans) {
		if (word != null) {
			try {
				JSONObject json = wordGetJSON(word);
				word_trans.word = word.trim();
				jsonGetWordDetails(json, word_trans);
			} catch (Exception e) {
				System.out.println("Loi o ham getWordFromMochi!");
			}
		}
	}

	// Lay tu EnVi Cambridge
	private void getEnViFromCamDic(String word) {
		if (word != null) {
			this.word = word.trim();
			final String url = "https://dictionary.cambridge.org/dictionary/english-vietnamese/"
					.concat(this.word.replaceAll(" +", "-"));
			try {
				final Document document = Jsoup.connect(url).userAgent("Mozilla").timeout(2000).get();
				Element entry;
				if ((entry = document.selectFirst(".entry-body")) != null) {
					if (entry.select(".pron-info").first() != null) {
						this.pronunciation = entry.select(".pron-info").first().text();
					}
					if (entry.select(".def-block").first() != null) {
						this.isFound = true;
						for (Element div : entry.select(".def-block")) {
							EngViMeaning enviMean = new EngViMeaning(this.word);
							enviMean.en = div.select(".ddef_h > .db.ddef_d.def").text(); // Nghia tieng Anh
							enviMean.vi = div.select(".ddef_b-t.ddef_b.def-body>.dtrans.trans").text();// Nghia tieng
																										// Viet

							for (String each : div.select(".examp.dexamp").eachText()) {
								enviMean.en_examples.add(each);// Cac vi du nghia do
							}
							this.en_vi_meanings.add(enviMean);
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	// Lay tu Eng Cambridge
//	private void getEnFromCamDic(String word) {
//		if (word != null) {
//			this.word = word.trim();
//			final String url = "https://dictionary.cambridge.org/dictionary/english/"
//					.concat(this.word.replaceAll(" +", "-"));
//			try {
//				final Document document = Jsoup.connect(url).userAgent("Mozilla")
//						  .timeout(5000).get();
//				if (document.selectFirst(".entry-body") != null) {
//					this.isFound = true;
//					if (document.select(".pr.entry-body__el > .pos-header.dpos-h > .dpron-i.uk > .dpron.pron")
//							.first() != null) {
//						this.pronunciation = "uk"
//								+ document.select(".pr.entry-body__el > .pos-header.dpos-h > .dpron-i.uk > .dpron.pron")
//										.first().text();
//						this.pronunciation = this.pronunciation.concat("  " + "us"
//								+ document.select(".pr.entry-body__el > .dpos-h.pos-header > .dpron-i.us > .dpron.pron")
//										.first().text());
//					}
//					for (Element div : document
//							.select("div.pr.dsense > .dsense_b.sense-body > .ddef_block.def-block")) {
//						EngViMeaning en_meaning = new EngViMeaning();
//						en_meaning.en = div.select(".ddef_h > .db.ddef_d.def").text(); // Lay nghia TA
//						for (String each : div.select(".ddef_b.def-body > div.dexamp.examp > .deg.eg").eachText()) {// Lay															// du
//							en_meaning.en_examples.add(each);
//						}
//						this.en_vi_meanings.add(en_meaning);
//					}
//				}
//			} catch (Exception e) {
//				e.printStackTrace();
//			}
//		}
//	}

	// Lay Word Family
	private String getOwnWordFamily(String word) {
		final String url = "https://www.ldoceonline.com/dictionary/".concat(word.trim().replaceAll(" +", "-"));
		try {
			final Document document = Jsoup.connect(url).userAgent("Mozilla").timeout(5000).get();
			StringBuilder result = new StringBuilder();
			if (document.selectFirst(".wordfams") != null) {
				Elements wfamily = document.select(".dictionary > .wordfams > span,.dictionary > .wordfams > a");
				for (Element a : wfamily) {
					String w = a.text();
					if (w.equals("Word family")) {
						result.append(w);
					} else if (w.matches("\\((.+?)\\)")) {
						result.append(" ").append(w).append(" ");
					} else {
						result.append(w).append(", ");
					}
				}
			}
			return result.toString().replaceAll("\u2022(.)[(]", "\n   (");
		} catch (Exception e) {
			System.out.println("getOwnWordFamily from class Word");
		}
		return "";
	}

	// Lay Synonyms
//	private String getOwnSynonyms(String word) {
//		try {
//			// URL url = new URL("https://tuna.thesaurus.com/pageData/".concat());
//			Document doc = Jsoup.connect("https://www.collinsdictionary.com/dictionary/english-thesaurus/"
//					+ word.trim().replaceAll(" +", "-")).userAgent("Mozilla").get();
//			Element temp = doc.selectFirst(".thesbase");
//			if (temp == null) {
//				return "";
//			}
//			StringBuilder result = new StringBuilder();
//			Elements blocks = temp.getElementsByClass("moreSyn");
//			for (Element block : blocks) {
//				result.append("Def: ").append(block.selectFirst(".synonymBlock").text()); // Definition
//				if (block.selectFirst(".cit") != null) {
//					result.append("\nExample: ").append(block.selectFirst(".cit").text());// hoac type-example
//				}
//				Elements syns = block.select(".type-syn");
//				for (Element syn : syns) {
//					temp = syn.selectFirst(".orth");
//					if (temp != null) {
//						result.append(" \u2022 ").append(temp.text());
//					}
//
//					temp = syn.selectFirst(".quote");
//					if (temp != null) {
//						result.append("\n  ~ ").append(temp.text());
//					}
//					result.append("\n\n");
//				}
//				if (block.selectFirst(".blockAnt") != null) {
//					result.append("Antonyms: ").append(block.selectFirst(".blockAnt").text());
//				}
//				result.append("\n\n");
//			}
//			return result.toString();
//		} catch (MalformedURLException e) {
//			System.out.println("Url khong hop le o getOwnSynonyms!");
//		} catch (IOException e) {
//			// e.printStackTrace();
//			System.out.println("Khong the lay Document o getOwnSynonyms!");
//		}
//		return "";
//	}

	public void getDataForWord(String word) {
		System.gc();
		Word word_trans = this;
		if (!word.isEmpty()) {
			ExecutorService executor = Executors.newCachedThreadPool();

			Runnable getfamsThr = new Runnable() {
				@Override
				public void run() {
					family = getOwnWordFamily(word);
				}
			};
//				Runnable getsynThr = new Runnable() {
//					@Override
//					public void run() {
//						synonyms = getOwnSynonyms(word);
//					}
//				};
			Runnable getEnViRun = new Runnable() {
				public void run() {
					System.out.println(word);
					word_trans.getEnViFromCamDic(word);
					if (word_trans.isFound == false) {
						getWordFromMochi(word, word_trans);
					}
					if (word_trans.isFound == false) {
						// word_trans.getEnFromCamDic(word);
					}

				};
			};
			executor.execute(getEnViRun);
			executor.execute(getfamsThr);
//				executor.execute(getsynThr);
			executor.shutdown();
			while (true) {
				if (executor.isTerminated()) {
					break;
				}
				;
				try {
					executor.awaitTermination(1000L, TimeUnit.MILLISECONDS);
					//System.out.println("42");
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	boolean isFound() {
		return this.isFound;
	}

	String getWord() {
		return this.word;
	}
}
