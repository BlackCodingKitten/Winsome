

public class User {

    private String nickname;
    String[] tags;
    private String password;

    public User(String nickname, String pw, String[] tags) {
        this.nickname=nickname;
        this.tags=tags;
        this.password=pw;

    }

    public String getNickname(){
        return this.nickname;
    }

}
