package org.jenkinsci.plugins.puppetenterprise.models;

import java.io.*;
import java.util.*;
import org.jenkinsci.plugins.puppetenterprise.apimanagers.puppetdbv4.PuppetDBQueryV4;
import org.jenkinsci.plugins.puppetenterprise.apimanagers.puppetdbv4.PuppetDBException;
import org.jenkinsci.plugins.puppetenterprise.apimanagers.PERequest;
import com.google.gson.internal.LinkedTreeMap;

public class PQLQuery {
  private ArrayList results = new ArrayList();
  private String query = null;
  private String token = null;

  public void setToken(String token) {
    this.token = token;
  }

  public void setQuery(String query) {
    this.query = query;
  }

  public ArrayList getResults() {
    return this.results;
  }

  public void run() throws PuppetDBException, Exception {
    PuppetDBQueryV4 query = new PuppetDBQueryV4();
    query.setQuery(this.query);
    query.setToken(this.token);
    query.execute();

    this.results = query.getResults();
  }
}
