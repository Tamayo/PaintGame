package models;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "words", schema = "paintgame@cassandra_pu")
public class Word{

  @Id
  @Column(name="word")
  private String word;

  public Word(){

  }

  public Word(String word){
    this.word = word;
  }

  public void setWord(String word){
    this.word = word;
  }

  public String getWord(){
    return this.word;
  }

}