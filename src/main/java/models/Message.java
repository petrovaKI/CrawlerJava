package models;

public class Message {
    private final String link;
    private final String title;
    private final String id;
    public Message(String link, String title, String id) {
        this.link = link;
        this.title = title;
        this.id = id;
    }
    public String getLink() {return link;}
    public String getID() {return id;}
}

