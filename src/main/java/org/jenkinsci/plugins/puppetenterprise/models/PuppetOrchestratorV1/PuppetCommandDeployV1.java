public class PuppetCommandDeployV1 {
  private String uri = "/orchestrator/v1/command/deploy";
  private PuppetCommandDeployRequest  request  = null;
  private PuppetCommandDeployResponse response = null;

  public PuppetCommandDeployV1() {
    this.request = new PuppetCommandDeployRequest();
    this.response = new PuppetCommandDeployResponse();
  }

  public setScope(String application, ArrayList nodes, String query) {
    this.request.setScope(application, nodes, query);
  }

  public setEnvironment(String environment) {
    this.request.environment = environment;
  }

  public setConcurrency(Integer concurrency) {
    this.request.concurrency = concurrency;
  }

  public setEnforceEnvironment(Boolean enforcement) {
    this.request.enforce_environment = enforcement;
  }

  public setDebug(Boolean debug) {
    this.request.debug = debug;
  }

  public setTrace(Boolean trace) {
    this.request.trace = trace;
  }

  public setNoop(Boolean noop) {
    this.request.noop = noop;
  }

  public setEvalTrace(Boolean evalTrace) {
    this.request.evaltrace = evalTrace;
  }

  public setTarget(String target) {
    this.request.target = target;
  }

  private isSuccessful(peResponse) {
    if ({400,404}.contains(peResponse.getResponseCode())) {
      return false;
    }

    return true;
  }

  public void execute(PERequest peRequest) {
    LinkedTreeMap body = Gson.toJson(request);
    PEResponse peResponse = peRequest.request(this.uri, body);

    if (isSuccessful(peResponse)) {
      PuppetCommandDeployResponse response = Gson.fromJson(peResponse.getResponseBody(), PuppetCommandDeployResponse.class);
    } else {
      PuppetCommandDeployError error = Gson.fromJson(peResponse.getResponseBody(), PuppetCommandDeployError.class);
      throw PuppetOrchestratorException(error.kind, error.msg, error.details);
    }

  }

  public String getID() {
    response.getID();
  }

  public String getName() {
    response.getName();
  }

  class PuppetCommandDeployRequest {
    public String environment = null;
    public Boolean noop = null;
    public PuppetCommandDeployRequestScope scope = new PuppetCommandDeployRequestScope();
    public Integer concurrency = null;
    public Boolean enforce_environment = null;
    public Boolean debug = null;
    public Boolean trace = null;
    public Boolean evaltrace = null;
    public String target = null;

    class PuppetCommandDeployRequestScope {
      public String application = null;
      public ArrayList nodes = null;
      public String query = null;
      public Boolean whole_environment = null;

      public Boolean isEmpty() {
        return (this.application == null && this.nodes == null && this.query == null);
      }
    }

    public setScope(String application, ArrayList nodes, String query) {
      this.scope.application = application;
      this.scope.nodes = nodes;
      this.scope.query = query;
    }
  }

  class PuppetCommandDeployResponse {
    class PuppetCommandDeployResponseJob {
      public String id = "";
      public String name = "";
    }

    private PuppetCommandDeployResponseJob job = new PuppetCommandDeployResponseJob();

    public String getID() {
      return job.id;
    }

    public String getName() {
      return job.name;
    }
  }

  class PuppetCommandDeployError {
    public String kind;
    public String msg;
    private LinkedTreeMap<String,Object> details;
  }
}
