package org.jenkinsci.plugins.puppetenterprise.models.puppetorchestratorv1;

import java.io.*;
import java.util.*;
import java.net.*;
import com.google.gson.reflect.TypeToken;
import com.google.gson.internal.LinkedTreeMap;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.joda.time.DateTime;
import org.jenkinsci.plugins.puppetenterprise.models.PEResponse;
import org.jenkinsci.plugins.puppetenterprise.models.PuppetOrchestratorV1;
import org.jenkinsci.plugins.puppetenterprise.models.puppetorchestratorv1.puppetnodev1.*;
import org.jenkinsci.plugins.puppetenterprise.models.puppetorchestratorv1.PuppetOrchestratorException;

public class PuppetJobsIDV1 extends PuppetOrchestratorV1 {
  private String endpoint = "/jobs/%s";
  private PuppetJobsIDResponse response = null;
  private String name = "";
  private String state = "";
  private ArrayList<PuppetNodeItemV1> nodes = new ArrayList();
  private Integer nodeCount = null;
  private String environment = "";

  public PuppetJobsIDV1() {
    this.response = new PuppetJobsIDResponse();
  }

  public PuppetJobsIDV1(String name) {
    this.name = name;
    this.response = new PuppetJobsIDResponse();
  }

  public String getState(){
    return this.state;
  }

  public String getEnvironment(){
    return this.environment;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getName() {
    return this.name;
  }

  public ArrayList<PuppetNodeItemV1> getNodes() {
    return this.nodes;
  }

  private ArrayList<PuppetNodeItemV1> getNodes(URL url) throws URISyntaxException, Exception {
    URI uri = url.toURI();
    PEResponse peResponse = send(url.toURI());
    ArrayList<PuppetNodeItemV1> nodes = null;
    Gson gson = new Gson();

    // if (isSuccessful(peResponse)) {
    //   nodes = gson.fromJson(peResponse.getJSON(), (ArrayList<PuppetNodeItemV1>).class);
    // } else {
    //   PuppetJobsIDError error = gson.fromJson(peResponse.getJSON(), PuppetJobsIDError.class);
    //   throw new PuppetOrchestratorException(error.kind, error.msg, error.details);
    // }

    return nodes;
  }

  public Integer getNodeCount() {
    return this.nodeCount;
  }

  public void execute() throws PuppetOrchestratorException, Exception {
    URI fullURI = getURI(String.format(this.endpoint, this.name));
    PEResponse peResponse = send(fullURI);
    Gson gson = new Gson();

    if (isSuccessful(peResponse)) {
      PuppetJobsIDResponse response = gson.fromJson(peResponse.getJSON(), PuppetJobsIDResponse.class);
      this.state = response.getLastStatus().state;

      try {
        this.nodes = getNodes(response.getNodesURL());
      } catch(URISyntaxException e) {
        throw new Exception("Puppet Enterprise Orchestrator API Error: Returned job " + this.name + " data contained invalid URL for node URL. Value was " + response.getNodesURL());
      }

      this.nodeCount = response.node_count;
    } else {
      PuppetJobsIDError error = gson.fromJson(peResponse.getJSON(), PuppetJobsIDError.class);
      throw new PuppetOrchestratorException(error.kind, error.msg, error.details);
    }
  }

  public Boolean isSuccessful(PEResponse response) {
    if (response.getResponseCode() == 400 || response.getResponseCode() == 404) {
      return false;
    }

    return true;
  }

  class PuppetJobsIDResponse {
    public ArrayList items = new ArrayList();
    public URL id = null;
    public String name = "";
    public PuppetJobsIDResponseOptions options = new PuppetJobsIDResponseOptions();
    public Integer node_count = null;
    public LinkedTreeMap<String,String> owner = new LinkedTreeMap();
    public DateTime timestamp = new DateTime();
    public ArrayList<PuppetJobsIDResponseStatus> status = new ArrayList();
    private LinkedTreeMap<String,String> environment = new LinkedTreeMap();
    private LinkedTreeMap<String,URL> nodes = new LinkedTreeMap();
    private LinkedTreeMap<String,URL> report = new LinkedTreeMap();

    class PuppetJobsIDResponseOptions {
      public Integer concurrency = null;
      public Boolean noop = null;
      public Boolean trace = null;
      public Boolean debug = null;
      public LinkedTreeMap<String, Object> scope = new LinkedTreeMap();
      public Boolean enforce_environment = null;
      public String environment = "";
      public Boolean evaltrace = null;
      public String target = null;
    }

    class PuppetJobsIDResponseStatus {
      public String state = "";
      public DateTime enter_time = new DateTime();
      public DateTime exit_time = new DateTime();
    }

    public PuppetJobsIDResponseStatus getLastStatus() {
      return status.get(status.size() - 1);
    }

    public URL getNodesURL() {
      return nodes.get("id");
    }

    public URL getReportURL() {
      return report.get("id");
    }
  }

  class PuppetJobsIDError {
    public String kind;
    public String msg;
    public LinkedTreeMap<String,Object> details;
  }

}
