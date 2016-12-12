package org.jenkinsci.plugins.workflow.steps;

import java.util.*;
import com.google.inject.Inject;
import hudson.Extension;
import hudson.Util;
import hudson.util.ListBoxModel;
import hudson.security.ACL;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.apache.commons.lang.StringUtils;
import hudson.model.Run;
import hudson.model.Item;
import hudson.model.TaskListener;
import java.net.*;
import jenkins.model.Jenkins;
import javax.annotation.Nonnull;

import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.jenkinsci.plugins.plaincredentials.*;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.AncestorInPath;
import java.io.Serializable;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.jenkinsci.plugins.puppetenterprise.PuppetEnterpriseManagement;
import org.jenkinsci.plugins.puppetenterprise.models.PEResponse;
import org.jenkinsci.plugins.workflow.PEException;

public final class PuppetJobStep extends PuppetEnterpriseStep implements Serializable {

  private static final Logger logger = Logger.getLogger(PuppetJobStep.class.getName());

  private String target = "";
  private ArrayList nodes = new ArrayList();
  private String application = "";
  private String query = "";
  private Integer concurrency = null;
  private Boolean noop = false;
  private String environment = null;

  @DataBoundSetter private void setTarget(String target) {
    this.target = Util.fixEmpty(target);
  }

  @DataBoundSetter private void setConcurrency(Integer concurrency) {
    this.concurrency = concurrency;
  }

  @DataBoundSetter private void setNoop(Boolean noop) {
    this.noop = noop;
  }

  @DataBoundSetter private void setEnvironment(String environment) {
    this.environment = environment;
  }

  @DataBoundSetter private void setQuery(String query) {
    this.query = query;
  }

  @DataBoundSetter private void setNodes(ArrayList nodes) {
    this.nodes = nodes;
  }

  @DataBoundSetter private void setApplication(String application) {
    this.application = application;
  }

  public String getQuery() {
    return this.query;
  }

  public ArrayList getNodes() {
    return this.nodes;
  }

  public String getApplication() {
    return this.application;
  }

  public String getTarget() {
    return this.target;
  }

  public Integer getConcurrency() {
    return this.concurrency;
  }

  public String getEnvironment() {
    return this.environment;
  }

  public Boolean getNoop() {
    return this.noop;
  }

  @DataBoundConstructor public PuppetJobStep() { }

  private static String parseJobId(String idUrl) {
    String[] jobUrlElements = idUrl.split("/");
    return jobUrlElements[jobUrlElements.length - 1];
  }

  public static class PuppetJobStepExecution extends AbstractSynchronousStepExecution<Void> {

    @Inject private transient PuppetJobStep step;
    @StepContextParameter private transient Run<?, ?> run;
    @StepContextParameter private transient TaskListener listener;

    @Override protected Void run() throws Exception {
      HashMap scope = new HashMap();
      HashMap body = new HashMap();

      // Target is still supported to support older versions of PE.
      // 2016.4 installs of PE should use the scope parameter when
      // creating orchestrator jobs.
      if (step.getTarget() != "" && step.getTarget() != null) {
        body.put("target", step.getTarget());
      } else {
        if (step.getQuery() != "") {
          scope.put("query", step.getQuery());
        }

        if (!step.getNodes().isEmpty()) {
          scope.put("nodes", step.getNodes());
        }

        if (step.getApplication() != "") {
          scope.put("application", step.getApplication());
        }

        if (!scope.isEmpty()) {
          body.put("scope", scope);
        }
      }

      if (step.getConcurrency() != null) {
        body.put("concurrency", step.getConcurrency());
      }

      body.put("noop", step.getNoop());
      body.put("environment", step.getEnvironment());

      PEResponse result = step.request("/orchestrator/v1/command/deploy", 8143, "POST", body);
      HashMap responseHash = (HashMap) result.getResponseBody();

      if (!step.isSuccessful(result)) {
        String error = null;

        if (responseHash.get("error") != null) {
          if (responseHash.get("error") instanceof HashMap) {
            HashMap errorHash = (HashMap) responseHash.get("error");
            error = errorHash.toString();
          } else if (responseHash.get("error") instanceof String) {
            String errorString = (String) responseHash.get("error");
            error = errorString;
          } else if (responseHash.get("error") instanceof ArrayList) {
            ArrayList errorArray = (ArrayList) responseHash.get("error");
            error = errorArray.toString();
          }
        }

        if (responseHash.get("msg") != null) {
          error = (String) responseHash.get("msg");
        }

        if (result.getResponseCode() == 404 && error == null) {
          error = "Environment " + step.getEnvironment() + " not found";
        }

        throw new PEException(error, result.getResponseCode());
      }

      HashMap job = new HashMap();
      String jobID = "";
      String jobStatus = "";
      HashMap jobStatusResponseHash = new HashMap();

      try {
        job = (HashMap) responseHash.get("job");
        jobID = (String) job.get("id");
        jobStatus = "";


        listener.getLogger().println("Successfully created Puppet job " + parseJobId(jobID));
        logger.log(Level.INFO, "Successfully created Puppet job " + parseJobId(jobID));
      } catch(NullPointerException e){
        throw new PEException(responseHash.toString(), 200);
      }

      do {
        String peRequestPath = "/orchestrator/v1/" + jobID;
        Integer peRequestPort = 8143;

        // The orchestrator API in 2015.2 and 2016.1 returned
        // a relative path for the job ID while 2016.2 returns
        // a full URL. This code checks which was returned so
        // we can support older PE installs.  This should eventually
        // be deprecated.
        try {
          URI uri = new URI (jobID);
          peRequestPath = uri.getPath();
          peRequestPort = uri.getPort();
        } catch(URISyntaxException e) { //do nothing
        }

        PEResponse jobStatusResponse = step.request(peRequestPath, peRequestPort, "GET", null);
        jobStatusResponseHash = (HashMap) jobStatusResponse.getResponseBody();

        if (!step.isSuccessful(jobStatusResponse)) {
          throw new PEException(jobStatusResponseHash.toString(), jobStatusResponse.getResponseCode());
        }

        ArrayList statuses = (ArrayList) jobStatusResponseHash.get("status");
        HashMap latestStatus = (HashMap) statuses.get(statuses.size() - 1);
        String currentState = (String) latestStatus.get("state");
        jobStatus = currentState;

        // Sleep for .5 seconds
        try {
          Thread.sleep(500);
        } catch(InterruptedException ex) {
          Thread.currentThread().interrupt();
        }
      } while (!jobStatus.equals("finished") && !jobStatus.equals("stopped") && !jobStatus.equals("failed"));

      PEResponse nodes_response = step.request("/orchestrator/v1/jobs/" + parseJobId(jobID) + "/nodes", 8143, "GET", null);
      jobStatusResponseHash.put("nodes", (HashMap) nodes_response.getResponseBody());
      jobStatusResponseHash.put("status", jobStatus);

      if (jobStatus.equals("failed") || jobStatus.equals("stopped")) {
        String message = "Puppet job " + parseJobId(jobID) + " " + jobStatus + "\n---------\n" + step.formatReport(jobStatusResponseHash);
        listener.getLogger().println(message);
        throw new PEException(message, listener);
      }

      String message = "Successfully ran Puppet job " + parseJobId(jobID) + "\n---------\n" + step.formatReport(jobStatusResponseHash);
      listener.getLogger().println(message);

      return null;
    }

    private static final long serialVersionUID = 1L;
  }

  public String formatReport(HashMap report) {
    StringBuilder formattedReport = new StringBuilder();

    formattedReport.append("Puppet Job Name: " + ((String) report.get("name")) + "\n");
    formattedReport.append("Status: " + ((String) report.get("status")) + "\n");
    formattedReport.append("Environment: " + ((String)((HashMap) report.get("environment")).get("name")) + "\n");
    formattedReport.append("Nodes: " + (String) report.get("node_count") + "\n\n");

    ArrayList<HashMap> nodes = (ArrayList) ((HashMap) report.get("nodes")).get("items");
    for (HashMap node : nodes ) {
      formattedReport.append((String) node.get("name") + "\n");

      HashMap node_details = (HashMap) node.get("details");
      HashMap metrics = (HashMap) node_details.get("metrics");
      String failed = (String) metrics.get("failed");
      String changed = (String) metrics.get("changed");
      String skipped = (String) metrics.get("skipped");
      String corrective = null;

      if (metrics.get("corrective_change") != null) {
        corrective = (String) metrics.get("corrective_change");
      }

      formattedReport.append("  Resource Events: ");
      formattedReport.append(failed + " failed   ");
      formattedReport.append(changed + " changed   ");

      //PE versions prior to 2016.4 do not include corrective changes
      if (corrective != null) {
        formattedReport.append(corrective + " corrective   ");
      }

      formattedReport.append(skipped + " skipped    ");
      formattedReport.append("\n");

      formattedReport.append("  Report URL: " + (String) node_details.get("report-url") + "\n");
      formattedReport.append("\n");
    }

    return formattedReport.toString();
  }

  public Boolean isSuccessful(PEResponse response) {
    Integer responseCode = response.getResponseCode();
    Object responseBody = response.getResponseBody();

    if (responseCode < 200 || responseCode >= 300) {
      return false;
    }

    if (responseBody instanceof HashMap) {
      HashMap responseHash = (HashMap) responseBody;

      if (responseHash.get("error") != null) {
        return false;
      }
    }

    return true;
  }

  @Extension public static final class DescriptorImpl extends AbstractStepDescriptorImpl {
    public DescriptorImpl() {
      super(PuppetJobStepExecution.class);
    }

    public ListBoxModel doFillCredentialsIdItems(@AncestorInPath Item context, @QueryParameter String source) {
      if (context == null || !context.hasPermission(Item.CONFIGURE)) {
        return new ListBoxModel();
      }
      return new StandardListBoxModel().withEmptySelection().withAll(
      CredentialsProvider.lookupCredentials(StringCredentials.class, context, ACL.SYSTEM, URIRequirementBuilder.fromUri(source).build()));
    }

    @Override public String getFunctionName() {
      return "puppetJob";
    }

    @Override public String getDisplayName() {
      return "Create Puppet Orchestrator Job";
    }
  }
}
