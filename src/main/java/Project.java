import java.util.List;

public class Project {
    private String id;
    private String owner;
    private String repo;
    private String sha;
    private String tag;


    public Project(String id, String owner, String repo, String tag, String sha) {
        this.id = id;
        this.owner = owner;
        this.repo = repo;
        this.tag = tag;
        this.sha = sha;
    }

    public String getId() {
        return id;
    }

    public String getOwner() {
        return owner;
    }

    public String getRepo() {
        return repo;
    }

    public String getSha() {
        return sha;
    }

    public String getTag() {
        return tag;
    }
}
