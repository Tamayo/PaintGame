package models;
import java.io.IOException;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Lob;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;







import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Entity
@Table(name = "images", schema = "PaintGameXX@cassandra_pu")
public class SavedImage{

  @Id
  @Column(name = "imageId")
  @GeneratedValue(strategy = GenerationType.TABLE)
  private String imageId;

  @Lob
  @Column(name="image")
  private byte[] image;

  @Column(name="word")
  private String word;

  public SavedImage(){

  }

  public SavedImage(String id, String word, JsonNode image){
    this.imageId = id;
    ObjectMapper mapper = new ObjectMapper();
    try {
		this.image = mapper.writeValueAsBytes(image);
	} catch (JsonGenerationException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} catch (JsonMappingException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
    System.out.println(this.image.length);
    this.word = word;
  }

  public JsonNode getImage(){
	  ObjectMapper mapper = new ObjectMapper();
    try {
		return mapper.readTree(image);
	} catch (JsonProcessingException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
    return null;
  }

  public String getWord(){
    return word;
  }

}