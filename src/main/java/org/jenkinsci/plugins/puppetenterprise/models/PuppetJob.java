package org.jenkinsci.plugins.puppetenterprise.models;

import java.io.*;
import java.util.*;
import org.jenkinsci.plugins.puppetenterprise.models.puppetorchestratorv1.PuppetOrchestratorException;
import org.jenkinsci.plugins.puppetenterprise.models.puppetorchestratorv1.PuppetCommandDeployV1;
import org.jenkinsci.plugins.puppetenterprise.models.puppetorchestratorv1.PuppetJobsIDV1;
import org.jenkinsci.plugins.puppetenterprise.models.puppetorchestratorv1.puppetnodev1.*;
import org.jenkinsci.plugins.puppetenterprise.models.PERequest;
import com.google.gson.internal.LinkedTreeMap;

public class PuppetJob {
  private String state = null;
  private String name = null;
  private String token = null;
  private ArrayList<PuppetNodeItemV1> nodes = null;
  private Integer nodeCount = null;
  private LinkedTreeMap scope = new LinkedTreeMap();
  private String target = null;
  private String environment = null;
  private Integer concurrency = null;
  private Boolean enforceEnvironment = null;
  private Boolean debug = false;
  private Boolean trace = false;
  private Boolean noop = false;
  private Boolean evalTrace = false;

  public PuppetJob() { }

  public void setScope(String application, ArrayList nodes, String query) {
    this.scope.put("application", application);
    this.scope.put("nodes", nodes);
    this.scope.put("query", query);
  }

  public void setTarget(String target) {
    this.target = target;
  }

  public void setToken(String token) {
    this.token = token;
  }

  public void setEnvironment(String environment) {
    this.environment = environment;
  }

  public void setConcurrency(Integer concurrency) {
    this.concurrency = concurrency;
  }

  public void setEnforceEnvironment(Boolean enforcement) {
    this.enforceEnvironment = enforcement;
  }

  public void setDebug(Boolean debug) {
    this.debug = debug;
  }

  public void setTrace(Boolean trace) {
    this.trace = trace;
  }

  public void setNoop(Boolean noop) {
    this.noop = noop;
  }

  public void setEvalTrace(Boolean evalTrace) {
    this.evalTrace = evalTrace;
  }

  public void run() throws PuppetOrchestratorException, Exception {
    start();

    do {
      try {
        Thread.sleep(500);
      } catch(InterruptedException ex) {
        Thread.currentThread().interrupt();
      }

      update();
    } while(isRunning());

  }

  public void start() throws PuppetOrchestratorException, Exception {
    PuppetCommandDeployV1 deployCommand = new PuppetCommandDeployV1();

    if (this.scope.isEmpty() && this.target != null) {
      deployCommand.setTarget(this.target);
    } else {
      deployCommand.setScope((String) this.scope.get("application"),
        (ArrayList<PuppetNodeItemV1>) this.scope.get("nodes"),
        (String) this.scope.get("query"));
    }

    deployCommand.setConcurrency(this.concurrency);
    deployCommand.setEnvironment(this.environment);
    deployCommand.setToken(this.token);
    deployCommand.setEnforceEnvironment(this.enforceEnvironment);
    deployCommand.setDebug(this.debug);
    deployCommand.setTrace(this.trace);
    deployCommand.setNoop(this.noop);
    deployCommand.setEvalTrace(this.evalTrace);
    deployCommand.execute();

    this.name = deployCommand.getName();
  }

  public void stop() {

  }

  public Boolean failed() {
    return (this.state == "failed");
  }

  public Boolean stopped() {
    return (this.state == "stopped");
  }

  public Boolean isRunning() {
    return (this.state == "running" || this.state == "new" || this.state == "ready");
  }

  public void update() throws PuppetOrchestratorException, Exception {
    PuppetJobsIDV1 job = new PuppetJobsIDV1(this.name);
    job.setToken(this.token);

    job.execute();

    this.state = job.getState();
    this.nodes = job.getNodes();
    this.environment = job.getEnvironment();
    this.nodeCount = job.getNodeCount();
  }

  public String formatReport() {
    StringBuilder formattedReport = new StringBuilder();

    formattedReport.append("Puppet Job Name: " + this.name + "\n");
    formattedReport.append("State: " + this.state + "\n");
    formattedReport.append("Environment: " + this.environment + "\n");
    formattedReport.append("Nodes: " + this.nodeCount + "\n\n");

    for (PuppetNodeItemV1 node : this.nodes) {
      formattedReport.append(node.getName() + "\n");

      if (node.getMetrics() != null) {
        PuppetNodeMetricsV1 metrics = node.getMetrics();

        formattedReport.append("  Resource Events: ");
        formattedReport.append(metrics.getFailed().toString() + " failed   ");
        formattedReport.append(metrics.getChanged().toString() + " changed   ");

        //PE versions prior to 2016.4 do not include corrective changes
        if (metrics.getCorrectiveChanged() != null) {
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
