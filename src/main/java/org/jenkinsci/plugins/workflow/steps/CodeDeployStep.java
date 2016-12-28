package org.jenkinsci.plugins.workflow.steps;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

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

import org.jenkinsci.plugins.puppetenterprise.models.PEResponse;
import org.jenkinsci.plugins.puppetenterprise.models.PuppetCodeManager;
import org.jenkinsci.plugins.workflow.PEException;

public final class CodeDeployStep extends PuppetEnterpriseStep implements Serializable {
  private ArrayList<String> environments = new ArrayList();
  private String credentialsId = "";

  @DataBoundSetter private void setEnvironment(String environment) {
    this.environments.add(environment);
  }

  @DataBoundSetter private void setEnvironments(ArrayList environments) {
    this.environments.addAll(environments);
  }

  //TODO: Move this back to the PuppetEnterpriseStep class when done refactoring
  @DataBoundSetter public void setCredentialsId(String credentialsId) {
    this.credentialsId = Util.fixEmpty(credentialsId);
  }

  public ArrayList<String> getEnvironments() {
    return this.environments;
  }

  //TODO: Move this back to the PuppetEnterpriseStep class when done refactoring
  private String getToken() {
    return lookupCredentials(this.credentialsId).getSecret().toString();
  }

  //TODO: Move this back to the PuppetEnterpriseStep class when done refactoring
  private static StringCredentials lookupCredentials(@Nonnull String credentialId) {
    return CredentialsMatchers.firstOrNull(
      CredentialsProvider.lookupCredentials(StringCredentials.class, Jenkins.getInstance(), ACL.SYSTEM, null),
      CredentialsMatchers.withId(credentialId)
    );
  }

  @DataBoundConstructor public CodeDeployStep() { }

  public static class CodeDeployStepExecution extends AbstractSynchronousStepExecution<Void> {

    @Inject private transient CodeDeployStep step;
    @StepContextParameter private transient Run<?, ?> run;
    @StepContextParameter private transient TaskListener listener;

    @Override protected Void run() throws Exception {
      PuppetCodeManager codemanager = new PuppetCodeManager();

      codemanager.setEnvironments(step.getEnvironments());
      codemanager.setToken(step.getToken());
      codemanager.setWait(true);
      codemanager.deploy();

      listener.getLogger().println(codemanager.formatReport());

      if (codemanager.hasErrors()) {
        Integer errorCount = codemanager.getErrors().size();
        listener.getLogger().println("\n");
        String message = errorCount.toString() + " Puppet environments failed to deploy.";
        throw new PEException(message, listener);
      }

      return null;
    }

    private static final long serialVersionUID = 1L;
  }

  @Extension public static final class DescriptorImpl extends AbstractStepDescriptorImpl {
    public DescriptorImpl() {
      super(CodeDeployStepExecution.class);
    }

    public ListBoxModel doFillCredentialsIdItems(@AncestorInPath Item context, @QueryParameter String source) {
      if (context == null || !context.hasPermission(Item.CONFIGURE)) {
        return new ListBoxModel();
      }
      return new StandardListBoxModel().withEmptySelection().withAll(
      CredentialsProvider.lookupCredentials(StringCredentials.class, context, ACL.SYSTEM, URIRequirementBuilder.fromUri(source).build()));
    }

    @Override public String getFunctionName() {
      return "puppetCode";
    }

    @Override public String getDisplayName() {
      return "Deploy Puppet Environment Code";
    }
  }
}
