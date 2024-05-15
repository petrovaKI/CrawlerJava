package entities;

public class Message {
    public Message(String link, String title, String id) {
        this.link = link;
        this.title = title;
        this.id = id;
    }
    private final String link;
    private final String title;
    private final String id;
    public String getLink() {return link;}
    public String getID() {return id;}
    public String getTitle(){return title;}
}

