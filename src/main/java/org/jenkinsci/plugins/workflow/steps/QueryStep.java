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
import com.google.gson.internal.LinkedTreeMap;

import org.jenkinsci.plugins.puppetenterprise.PuppetEnterpriseManagement;
import org.jenkinsci.plugins.puppetenterprise.models.PEResponse;
import org.jenkinsci.plugins.workflow.PEException;

public final class QueryStep extends PuppetEnterpriseStep implements Serializable {

  private static final Logger logger = Logger.getLogger(PuppetJobStep.class.getName());

  private String query = "";

  @DataBoundSetter private void setQuery(String query) {
    this.query = query;
  }

  public String getQuery() {
    return this.query;
  }

  @DataBoundConstructor public QueryStep() { }

  public static class QueryStepExecution extends AbstractSynchronousStepExecution<ArrayList> {

    @Inject private transient QueryStep step;
    @StepContextParameter private transient Run<?, ?> run;
    @StepContextParameter private transient TaskListener listener;

    @Override protected ArrayList run() throws Exception {
      LinkedTreeMap body = new LinkedTreeMap();
      body.put("query", step.getQuery());

      PEResponse result = step.request("/pdb/query/v4", 8081, "POST", body);
      Object response = result.getResponseBody();

      if (!step.isSuccessful(result)) {
        String error = (String) response;

        logger.log(Level.SEVERE, error);
        throw new PEException(error, result.getResponseCode());
      }

      ArrayList responseArray = (ArrayList) response;

      return responseArray;
    }

    private static final long serialVersionUID = 1L;
  }

  public Boolean isSuccessful(PEResponse response) {
    Integer responseCode = response.getResponseCode();

    if (responseCode < 200 || responseCode >= 300) {
      return false;
    }

    return true;
  }

  @Extension public static final class DescriptorImpl extends AbstractStepDescriptorImpl {
    public DescriptorImpl() {
      super(QueryStepExecution.class);
    }

    public ListBoxModel doFillCredentialsIdItems(@AncestorInPath Item context, @QueryParameter String source) {
      if (context == null || !context.hasPermission(Item.CONFIGURE)) {
        return new ListBoxModel();
      }
      return new StandardListBoxModel().withEmptySelection().withAll(
      CredentialsProvider.lookupCredentials(StringCredentials.class, context, ACL.SYSTEM, URIRequirementBuilder.fromUri(source).build()));
    }

    @Override public String getFunctionName() {
      return "puppetQuery";
    }

    @Override public String getDisplayName() {
      return "Query PuppetDB with the Puppet Query Language (PQL)";
    }
  }
}
