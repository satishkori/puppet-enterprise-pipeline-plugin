import java.io.*;
import java.util.*;

import com.google.gson.reflect.TypeToken;
import com.google.gson.internal.LinkedTreeMap;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import org.joda,time.DateTime;

public class PuppetJobsIDV1 {
  private String uri = "/orchestrator/v1/jobs/%s";
  PuppetJobsIDResponse response = null;
  private String name = "";
  private String state = "";

  public PuppetJobsIDV1(String name) {
    this.name = name;
    this.response = new PuppetJobsIDResponse();
  }

  public String getState(){
    return this.state;
  }

  public ArrayList<PuppetNodeItemV1> getNodes(PERequest peRequest, URL url) {
    PEResponse peResponse = peRequest.request(url.toURI());

    if (isSuccessful(peResponse)) {
      return Gson.fromJson(peResponse.getResponseBody(), ArrayList<PuppetNodeItemV1>.class);
    }
  }

  public getNodeCount() {
    return this.nodeCount;
  }

  public void execute(PERequest peRequest) throws PuppetOrchestratorException {
    String fullURI = Sring.format(uri, this.name);
    PEResponse peResponse = peRequest.request(fullURI);

    if (isSuccessful(peResponse)) {
      PuppetJobsIDResponse response = Gson.fromJson(peReponse.getResponseBody(), PuppetJobsIDResponse.class);
      this.state = response.getLastStatus().state;
      this.nodes = getNodes(peRequest, response.getNodesURL());
      this.nodeCount = response.node_count;
    } else {
      PuppetJobsIDError error = Gson.fromJson(peResponse.getResponseBody(), PuppetJobsIDError.class);
      throw PuppetOrchestratorException(error.kind, error.msg, error.details);
    }
  }

  public isSuccessul(PEResponse response) {
    if ({400,404}.contains(response.getResponseCode()) {
      return false;
    }

    return true;
  }

  class PuppetJobsIDResponse {
    public ArrayList items = new ArrayList();
    public URL id = new URL();
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
      status.get(status.size() - 1);
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
