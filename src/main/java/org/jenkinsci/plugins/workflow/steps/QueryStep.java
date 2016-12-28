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
import org.jenkinsci.plugins.puppetenterprise.models.PQLQuery;
import org.jenkinsci.plugins.puppetenterprise.apimanagers.puppetdbv4.PuppetDBException;
import org.jenkinsci.plugins.workflow.PEException;

public final class QueryStep extends PuppetEnterpriseStep implements Serializable {
  private String query = "";
  private String credentialsId = null;

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
      PQLQuery query = new PQLQuery();
      ArrayList results = new ArrayList();

      query.setQuery(step.getQuery());

      try {
        query.setToken(step.getToken());
      } catch(java.lang.NullPointerException e) {
        String summary = "Could not find Jenkins credential with ID: " + step.getCredentialsId() + "\n";
        StringBuilder message = new StringBuilder();

        message.append(summary);
        message.append("Please ensure the credentials exist in Jenkins. Note, the credentials description is not its ID\n");

        listener.getLogger().println(message.toString());
        throw new PEException(summary);
      }

      try {
        query.run();
      } catch(PuppetDBException e) {
        StringBuilder message = new StringBuilder();
        message.append("PQL Query Error\n");
        message.append("Kind:    " + e.getKind() + "\n");
        message.append("Message: " + e.getMessage() + "\n");

        throw new PEException(message.toString(), listener);
      }

      results = query.getResults();
      Integer size = results.size();
      listener.getLogger().println(step.getQuery() + "\nQuery returned " + size.toString() + " results.");

      return results;
    }

    private static final long serialVersionUID = 1L;
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
