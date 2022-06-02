/* questa classe rappresenta gli upvote e i downvote che vengono dati ad un post
ogni voto ha un autore e un valore che deve essere +1 nel caso di valutazione 
e -1 nel caso di valutazione negativa*/

public class Vote {
    private final String user;// ->autore della votazione
    private final int vote;// -> valore del voto

    public Vote(String user, int vote) {
        this.user = user;
        this.vote = vote;
    }

    public String getUser() {
        return this.user;
    }

    public int getVote() {
        return this.vote;
    }

    // metodo che controlla se il valore del voto è corretto
    public boolean validVode(int vote) {
        if (vote == 1 || vote == -1) {
            return true;
        } else {
            return false;
        }
    }
}