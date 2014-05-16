package controllers;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.Query;
import java.util.ArrayList;
import java.util.Random;


import models.Word;
import play.Play;
import play.mvc.Controller;
import play.mvc.Result;

public class WordController extends Controller{
  static EntityManagerFactory emf;

  public static Result persist(String word){
    EntityManager em = getEmf().createEntityManager();
    Word wordObj = new Word(word);
    em.persist(wordObj);
    em.close();
    return ok(wordObj.getWord() + " added");
  }

  public static Result findAll(){
    EntityManager em = getEmf().createEntityManager();
    Query q = em.createQuery("Select w from Word w");
    ArrayList<Word> results = new ArrayList<>(q.getResultList());
    String ret = "";
    for(Word w : results){
      ret += w.getWord() + ", ";
    }
    return ok(ret + " : and random " + findRandom());
  }

  public static String findRandom(){
    EntityManager em = getEmf().createEntityManager();
    Query q = em.createQuery("Select w from Word w");
    ArrayList<Word> results = new ArrayList<>(q.getResultList());
    int size = results.size();
    Random rand = new Random();
    int index = Math.abs(rand.nextInt() % (size));
    return results.get(index).getWord();
  }

  private static EntityManagerFactory getEmf(){
    if (emf == null){
      emf = Persistence.createEntityManagerFactory("cassandra_pu");
    }
    return emf;
  }
}