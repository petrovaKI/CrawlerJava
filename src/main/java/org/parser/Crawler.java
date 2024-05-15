package org.parser;

import entities.Message;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.*;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.util.*;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Crawler {
    public static  List<Message> startCrawling(String startUrl) throws InterruptedException {

        System.setProperty("webdriver.chrome.driver", "chromedriver.exe");

        ChromeOptions chromeOptions = new ChromeOptions();
        chromeOptions.addArguments("--remote-allow-origins=*");
        chromeOptions.addArguments("--headless");
        WebDriver driver = new ChromeDriver(chromeOptions);
        driver.get(startUrl + "/tag/87413-moda");

        while (isSeeMoreButtonPresent(driver)) {
            clickSeeMoreButton(driver);
        }

        List<Message> messages = getLinks(startUrl, driver);
        driver.quit();

        return messages;
    }

    private static List<Message> getLinks(String startUrl, WebDriver driver) {
        List<Message> messages = new ArrayList<>();
        Document doc = Jsoup.parse(driver.getPageSource());
        Elements linkElements = doc.select("div.listing.news-listing a[href]");
        for (Element linkElement : linkElements) {
            String href = linkElement.attr("href");
            String title = linkElement.text();
            String hash = calculateHash(href);

            Message message = new Message(startUrl+href, title, hash);
            messages.add(message);
        }
        return messages;
    }


    private static void clickSeeMoreButton(WebDriver driver) throws InterruptedException {
        try {
            WebElement seeMoreButton = driver.findElement(By.cssSelector("button.btn-link.btn-more.red"));
            Thread.sleep(1000);
            seeMoreButton.click();
        }catch (ElementNotInteractableException e){
            Thread.sleep(2000);
        } catch (Exception e) {
            System.out.println(e);
        }
    }
    private static boolean isSeeMoreButtonPresent(WebDriver driver) {
        try {
            driver.findElement(By.cssSelector("button.btn-link.btn-more.red.loading.remove"));
            return false;
        } catch (NoSuchElementException e) {
            try {
                driver.findElement(By.cssSelector("button.btn-link.btn-more.red"));
                return true;
            } catch (NoSuchElementException ex) {
                return false;
            }
        }
    }

    public static String calculateHash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hashBytes = md.digest(input.getBytes());
            StringBuilder hash = new StringBuilder();
            for (byte b : hashBytes) {
                hash.append(String.format("%02x", b));
            }
            return hash.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }


}