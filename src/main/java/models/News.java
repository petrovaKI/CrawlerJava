package models;

public class News {
    private final String link;
    private final String title;
    private final String text;
    private final String date;
    private final String author;
    private final String id;

    public News(String link, String title, String text, String date, String author, String id) {
        this.link = link;
        this.title = title;
        this.text = text;
        this.date = date;
        this.author = author;
        this.id = id;
    }
    public String getLink() {return link;}
    public String getId(){return id;}

}
