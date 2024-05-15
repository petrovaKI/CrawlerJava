package entities;

public class News {
    private final String link;
    private final String title;
    private final String text;
    private final String date;
    private final String author;

    public News(String link, String title, String text, String date, String author) {
        this.link = link;
        this.title = title;
        this.text = text;
        this.date = date;
        this.author = author;
    }
    public String getLink() {return link;}
    public String getTitle() { return title;}
    public String getText() {
        return text;
    }
    public String getDate(){return date;}
    public String getAuthor(){return author;}

}
