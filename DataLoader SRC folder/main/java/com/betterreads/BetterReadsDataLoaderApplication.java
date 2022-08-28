package com.betterreads;

import com.betterreads.author.Author;
import com.betterreads.author.AuthorRepository;
import com.betterreads.book.Book;
import com.betterreads.book.BookRepository;
import com.betterreads.connection.DataStaxAstraProperties;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.cassandra.CqlSessionBuilderCustomizer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SpringBootApplication
@EnableConfigurationProperties(DataStaxAstraProperties.class)
public class BetterReadsDataLoaderApplication {

	@Autowired
	private AuthorRepository authorRepository;

	@Autowired
	private BookRepository bookRepository;

	@Value("${datadump.location.author}")
	private String authorDumpLocation;

	@Value("${datadump.location.works}")
	private String worksDumpLocation;

	public static void main(String[] args) {
		SpringApplication.run(BetterReadsDataLoaderApplication.class, args);
	}


	@PostConstruct
	public void start() {
		//initAuthors();
		initWorks();
	}

	private void initWorks() {

		Path path = Paths.get(worksDumpLocation);
		DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS");

		try(Stream<String> lines = Files.lines(path)) {

			lines.forEach(line -> {

				//Read and Parse each line
				String jsonString = line.substring(line.indexOf("{"));

				try {
					JSONObject jsonObject = new JSONObject(jsonString);

					//Construct Book object
					Book book = new Book();
					book.setId(jsonObject.optString("key").replace("/works/",""));
					book.setName(jsonObject.optString("title"));

					JSONArray authorsJsonArr = jsonObject.optJSONArray("authors");
					if(authorsJsonArr != null) {

						List<String> authorIds = new ArrayList<>();

						for(int i=0; i<authorsJsonArr.length(); i++) {
							JSONObject authorObj = authorsJsonArr.optJSONObject(i);
							if(authorObj != null) {
								String id = authorObj.getJSONObject("author")
										.getString("key")
										.replace("/authors/", "");
								authorIds.add(id);
							}
						}

						book.setAuthorIds(authorIds);

						List<String> authorNames = authorIds.stream().
								map(id -> authorRepository.findById(id)).
								map(optAuthor -> {
									if (optAuthor.isPresent()) {
										return optAuthor.get().getName();
									} else return "Unknown Author";
								}).collect(Collectors.toList());

						book.setAuthorNames(authorNames);
					}

					JSONObject descObj = jsonObject.optJSONObject("description");
					if(descObj != null) {
						book.setDescription(descObj.optString("value"));
					}

					JSONArray coversArr = jsonObject.optJSONArray("covers");
					if(coversArr != null) {
						List<String> coverIds = new ArrayList<>();
						for(int i=0; i<coversArr.length(); i++) {
							coverIds.add(coversArr.getBigInteger(i).toString());
						}
						book.setCoverIds(coverIds);
					}

					JSONObject publishedJsonObj = jsonObject.optJSONObject("created");
					if(publishedJsonObj != null) {
						book.setPublishedDate(LocalDate.parse(publishedJsonObj.getString("value"), dateFormat));
					}


					System.out.println("Saving book : " + book.toString() + " ...");
					bookRepository.save(book);

				} catch(Exception e) {
					e.printStackTrace();
				}
			});

		} catch(IOException ioe) {
			ioe.printStackTrace();
		}
	}

	private void initAuthors() {
		Path path = Paths.get(authorDumpLocation);

		try(Stream<String> lines = Files.lines(path)) {

			lines.forEach(line -> {

				//Read and Parse each line
				String jsonString = line.substring(line.indexOf("{"));

				try {
					JSONObject jsonObject = new JSONObject(jsonString);

					//Construct Author object
					Author author = new Author();
					author.setName(jsonObject.optString("name"));
					author.setPersonalName(jsonObject.optString("personal_name"));
					author.setId(jsonObject.optString("key").replace("/authors/", ""));

					//save author
					System.out.println("Saving author: " + author.getName() + " ...");
					authorRepository.save(author);

				}catch(JSONException je) {
					je.printStackTrace();
				}
			});
		}
		catch(IOException ioe) {
			ioe.printStackTrace();
		}
	}

	/**
	 * This is necessary to have the Spring Boot app use the Astra secure bundle
	 * to connect to the database
	 */
	@Bean
	public CqlSessionBuilderCustomizer sessionBuilderCustomizer(DataStaxAstraProperties astraProperties) {
		Path bundle = astraProperties.getSecureConnectBundle().toPath();
		return builder -> builder.withCloudSecureConnectBundle(bundle);
	}

}
