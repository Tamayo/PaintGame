package controllers;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.Query;
import java.util.ArrayList;
import java.util.Random;
import com.fasterxml.jackson.databind.JsonNode;

import models.SavedImage;
import play.Play;
import play.mvc.Controller;
import play.mvc.Result;

public class SavedImageController extends Controller{
  static EntityManagerFactory emf;

  public static void persist(JsonNode jn, String word){
    EntityManager em = getEmf().createEntityManager();
    String id = String.valueOf(System.currentTimeMillis());
    SavedImage SavedImageObj = new SavedImage(id,word,jn);
    em.persist(SavedImageObj);
    em.close();
  }

  public static ArrayList<SavedImage> findImagesByWord(String word){
    EntityManager em = getEmf().createEntityManager();
    Query q = em.createQuery("Select w from SavedImage w where w.word = " + word);
    ArrayList<SavedImage> results = new ArrayList<>(q.getResultList());
    return results;
  }

  private static EntityManagerFactory getEmf(){
    if (emf == null){
      emf = Persistence.createEntityManagerFactory("cassandra_pu");
    }
    return emf;
  }
}