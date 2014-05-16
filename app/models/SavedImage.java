package models;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Lob;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import com.fasterxml.jackson.databind.JsonNode;

@Entity
@Table(name = "images", schema = "PaintGame@cassandra_pu")
public class SavedImage{

  @Id
  @Column(name = "imageId")
  @GeneratedValue(strategy = GenerationType.TABLE)
  private String imageId;

  @Lob
  @Column(name="image")
  private JsonNode image;

  @Column(name="word")
  private String word;

  public SavedImage(){

  }

  public SavedImage(String id, String word, JsonNode image){
    this.imageId = id;
    this.image = image;
    this.word = word;
  }

  public JsonNode getImage(){
    return image;
  }

  public String getWord(){
    return word;
  }

}