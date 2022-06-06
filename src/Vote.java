/* questa classe rappresenta gli upvote e i downvote che vengono dati ad un post
ogni voto ha un autore e un valore che deve essere +1 nel caso di valutazione 
e -1 nel caso di valutazione negativa*/

public class Vote {
    private final User user;// ->autore della votazione
    private final int vote;// -> valore del voto

    public Vote(User user, int vote) {
        this.user = user;
        this.vote = vote;
    }

    // metodo getter del nome del votante
    public User getUser() {
        return this.user;
    }

    // metodo getter del valore del voto
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