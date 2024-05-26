package org.parser;

import models.News;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Objects;

public class Parser{
    private static final Logger logger = LogManager.getLogger(Parser.class);

    static News startParsing(String urlString, String id) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();
            int responseCode = connection.getResponseCode();
            String status = getResponseStatus(responseCode);
            if (status.equals("OK")) {
                InputStream inputStream = connection.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder content = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line);
                }
                reader.close();
                inputStream.close();

                connection.disconnect();
                return getNews(content, urlString, id);
            }else {
                logger.error(status);
            }
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
        return null;
    }

    private static News getNews(StringBuilder content, String url, String id){
        Document doc = Jsoup.parse(content.toString());
        Element newsDoc = doc.select("div.vmroot.article.marticle.marticle_first").first();
        if (newsDoc == null) {
            logger.error("Warning: URL not processed properly: " + url);
            return null;
        }

        String author;
        Element authorTitle = newsDoc.selectFirst("div.author-title");
        if (authorTitle == null) {
            authorTitle = newsDoc.selectFirst("a.article-rubric");
            Objects.requireNonNull(authorTitle);
            author = authorTitle.text();
        }else {
            authorTitle = authorTitle.selectFirst("a");
            Objects.requireNonNull(authorTitle);
            author = authorTitle.attr("title");
        }

        String title = Objects.requireNonNull(newsDoc.select("h1").first()).text();
        String text = newsDoc.attr("data-descr");
        String date = newsDoc.attr("data-doc-published");

        return new News(url, title, text, date, author, id);
    }
    private static String getResponseStatus(int responseCode) {
        return switch (responseCode) {
            case 200 -> "OK";
            case 400 -> "Bad request";
            case 403 -> "Forbidden";
            case 404 -> "Not Found";
            case 410 -> "Gone";
            case 500 -> "Internal Server Error";
            case 520 -> "Unknown error";
            case 522 -> "Connection Timed Out";
            case 505 -> "HTTP Version Not Supported ";
            default -> "Error! \n" +
                    "Server response code: " + responseCode;
        };
    }
}