import java.io.*;
import org.jenkensci.plugins.puppetenterprise.models.PuppetOrchestratorV1.PuppetNodeV1.*;

public class PuppetJob {
  private String state = null;
  private String token = null;
  private ArrayList<PuppetNodeItemV1> nodes = new ArrayList();
  private Integer nodeCount = null;
  private PERequest peRequest = null;
  private LinkedTreeMap scope = new LinkedTreeMap();
  private String target = null;
  private String environment = null;
  private Integer concurrency = null;
  private Boolean enforceEnvironment = null;
  private Boolean debug = false;
  private Boolean trace = false;
  private Boolean noop = false;
  private Boolean evalTrace = false;

  public PuppetJob() {
    this.peRequest = new PERequest();
  }

  public setScope(String application, ArrayList nodes, String query) {
    this.scope.put("application", application);
    this.nodes.put("nodes", nodes);
    this.query.put("query", query);
  }

  public setTarget(String target) {
    this.target = target;
  }

  public setToken(String token) {
    this.token = token;
    peRequest.setToken(this.token);
  }

  public setEnvironment(String environment) {
    this.environment = environment;
  }

  public setConcurrency(Integer concurrency) {
    this.concurrency = concurrency;
  }

  public setEnforceEnvironment(Boolean enforcement) {
    this.enforce_environment = enforcement;
  }

  public setDebug(Boolean debug) {
    this.debug = debug;
  }

  public setTrace(Boolean trace) {
    this.trace = trace;
  }

  public setNoop(Boolean noop) {
    this.noop = noop;
  }

  public setEvalTrace(Boolean evalTrace) {
    this.evalTrace = evalTrace;
  }

  public setTarget(String target) {
    this.target = target;
  }

  public PuppetJobResult run(PERequest request) throws PuppetOrchestratorException {
    start();

    do {
      try {
        Thread.sleep(500);
      } catch(InterruptedException ex) {
        Thread.currentThread().interrupt();
      }

      update();
    } while isRunning();

  }

  public void start() throws PuppetOrchestratorException {
    PuppetCommandDeployV1 deployCommand = new PuppetCommandDeployV1();

    if (this.scope.isEmpty() && this.target != null) {
      deployCommand.setTarget(this.target);
    } else {
      deployCommand.setScope(this.scope);
    }

    deployCommand.setConcurrency(this.concurrency);
    deployCommand.setEnvironment(this.environment);
    deployCommand.setEnforceEnvironment(this.enforceEnvironment);
    deployCommand.setDebug(this.debug);
    deployCommand.setTrace(this.trace);
    deployCommand.setNoop(this.noop);
    deployCommand.setEvalTrace(this.evalTrace);
    deployCommand.execute(peDeployRequest);
  }

  public void stop() {

  }

  public Boolean failed() {
    return (this.stae == "failed");
  }

  public Boolean stopped() {
    return (this.state == "stopped");
  }

  public Boolean isRunning() {
    return (this.state == "running" || this.state == "new" || this.state == "ready");
  }

  public void update() {
    PuppetJobsIDV1 job = new PuppetJobsIDV1();
    job.name = this.name;
    job.execute(peRequest);

    this.state = job.getState();
    this.nodes = job.getNodes();
    this.enviroenmtn = job.getEnvironment();
    this.nodeCount = job.getNodeCount();
  }

  public String formatReport() {
    StringBuilder formattedReport = new StringBuilder();

    formattedReport.append("Puppet Job Name: " + this.name + "\n");
    formattedReport.append("State: " + this.state + "\n");
    formattedReport.append("Environment: " + this.environment + "\n");
    formattedReport.append("Nodes: " + this.node_count + "\n\n");

    for (PuppetNodeItemV1 node : this.nodes) {
      formattedReport.append(node.getName()) + "\n");

      if (node.getMetrics() != null) {
        PuppetNodeMetricsV1 metrics = node.getMetrics();

        formattedReport.append("  Resource Events: ");
        formattedReport.append(metrics.getFailed().toString() + " failed   ");
        formattedReport.append(metrics.getChanged().toString() + " changed   ");

        //PE versions prior to 2016.4 do not include corrective changes
        if (metrics.get("corrective_change") != null) {
          formattedReport.append(metrics.getCorrectiveChanged().toString() + " corrective   ");
        }

        formattedReport.append(metrics.getSkipped().toString() + " skipped    ");
        formattedReport.append("\n");

        formattedReport.append("  Report URL: " + node.getReportURL().toString() + "\n");
        formattedReport.append("\n");

      } else {
        //There's always a message, but it's only useful if the run was not able to take place,
        //  which we'll know if there are no metrics.
        if (node.getMessage() != null) {
          formattedReport.append(node.getMessage() + "\n");
          formattedReport.append("\n");
        }
      }
    }

    return formattedReport.toString();
  }
}
