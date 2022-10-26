package edu.truongjenny.WordGenerator;

import java.awt.Desktop;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public class MainSceneController {

	@FXML
	private Button cancel;

	@FXML
	private Button generate;

	@FXML
	private TextField savedFile;

	@FXML
	private TextField separator;

	@FXML
	private TextArea ta_words;
	
	
	
	private String getInput() {
		String input = ta_words.getSelectedText();
		if(input != null && !input.isBlank()) {
			return input;
		}
		return ta_words.getText();
		
	}
	
	
	private static final String directory = Paths.get("").toAbsolutePath().toString() + "/Note";
	boolean validAutoAdd() {
		if (getInput().isBlank()) {
			return false;
		}
		return true;
	}

	ArrayList<String> convertStrToList(String str, String separator) {
		if(separator.isEmpty()) {
			separator = "\n";
		}
		return new ArrayList<String>(
				Arrays.stream(str.split(separator)).map(s -> s.trim())
				.filter(s ->  !s.isBlank())
				.collect(Collectors.toList()));
	}

	public class WordDataBuilder implements Callable<Word> {
		private String word;

		public WordDataBuilder(String word) {
			this.word = word;
		}
		
		@Override
		public Word call() throws Exception {
			Word word_trans = new Word();
			word_trans.getDataForWord(this.word);
			return word_trans;
		}
	}

	private static void copyFile(File sourceFile, File destFile) throws IOException {
		if (!sourceFile.exists()) {
			System.out.println("Thieu mat " + sourceFile.getAbsolutePath());
		}
		FileInputStream source = null;
		FileOutputStream destination = null;
		try {
			source = new FileInputStream(sourceFile);
			destination = new FileOutputStream(destFile);
			destination.getChannel().transferFrom(source.getChannel(), 0, source.getChannel().size());
		} finally {
			if (source != null) {
				source.close();
			}
			if (destination != null) {
				destination.close();
			}
		}
	}
	
	private File createIfNotExist(String d, String fileName) {
		if(fileName.isBlank()) {
			fileName = getDefaultFileName();
		}
		File directory = new File(d);
		if (!directory.exists()) {
			directory.mkdirs();
		}
		if (!directory.isDirectory()) {
			return null;
		}
		
		File f = new File(directory.getAbsoluteFile() + "/" + fileName + ".html");	
		if (f.exists()) {
			return f;
		}
		try {
			f.createNewFile();
			copyFile(new File(d+"/Model/htmlModel.html"), f);
			return f;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	private void writeToFile(String d, String fileName, ArrayList<Word> words) throws IOException {
		File f = createIfNotExist(d, fileName);
		if(f!=null) {
			Document doc = Jsoup.parse(f, "UTF-8");
			doc.title(fileName);
			if(doc.select("div.word-cards").first()==null) {
				doc.select("body").first().appendChild(new Element("div").addClass("word-cards"));
			}
			Element wordCards = doc.select(".word-cards").first();
			for (Word word : words) {
				wordCards.appendChild(new Element("div").html(word.toString()));
			}
			BufferedWriter bw =
		            new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f.getAbsoluteFile()), "UTF-8"));
			bw.write(doc.html());
			bw.close();
		} else {
			System.out.println("Khong the tao file!");
			return;
		}
	}

	@FXML
	public void autoAddToExcel() {
		if (!validAutoAdd()) {
			return;
		}
		ArrayList<String> listWords = convertStrToList(getInput(), separator.getText());
		if (!listWords.isEmpty()) {
			try {
				ArrayList<Word> wordsFound = new ArrayList<>();
				StringBuilder wordsNotFound = new StringBuilder();
				int bufferSize = 50;
				int index = 0;
				while (index < listWords.size()) {
					ExecutorService executorService = Executors.newFixedThreadPool(4, new ThreadFactory() {
			            public Thread newThread(Runnable r) {
			                Thread t = Executors.defaultThreadFactory().newThread(r);
			                t.setDaemon(true);
			                return t;
			            }
			        });
					List<Future<Word>> listFuture = new ArrayList<Future<Word>>();
					int end;
					if((end = index + bufferSize) <= listWords.size()) {
					}else {
						end = listWords.size();
					}
					for (int i = index; i < end; i++) {
						listFuture.add(executorService.submit(new WordDataBuilder(listWords.get(i))));
					} // For loop 50 phan tu
					for (Future<Word> future : listFuture) {
						Word w = future.get();
						if(w.isFound()) {
							wordsFound.add(w);
						}else {
							wordsNotFound.append(w.getWord() + ", ");
						}
					}
					writeToFile(directory, savedFile.getText(), wordsFound);
					wordsFound.clear();
					index = index + bufferSize;
					if (index >= listWords.size()) {
						break;
					}
					System.gc();
				}
				ta_words.appendText("\n\nNhững từ không tìm thấy: " + wordsNotFound.toString());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	@FXML
	public void openFile() {
		if(savedFile.getText().isBlank()) {
			savedFile.setText(getDefaultFileName());
		}
		File f = createIfNotExist(directory, savedFile.getText());
		try {
			Desktop.getDesktop().browse(f.toURI());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private String getDefaultFileName() {
		 return java.time.LocalDate.now().toString().replaceAll("-", "_");
	}
}
