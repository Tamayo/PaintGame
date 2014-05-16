package models;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;

@Entity
@Table(name = "words", schema = "PaintGame@cassandra_pu")
public class Word{

  @Id
  @Column(name = "wordId")
  @GeneratedValue(strategy = GenerationType.TABLE)
  private String wordId;

  @Column(name="word")
  private String word;

  public Word(){

  }

  public Word(String word){
    this.wordId = word;
    this.word = word;
  }

  public void setWord(String word){
    this.wordId = word;
    this.word = word;
  }

  public String getWord(){
    return this.word;
  }

}