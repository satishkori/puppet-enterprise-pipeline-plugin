package org.jenkinsci.plugins.workflow.steps;

import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.ClassRule;
import org.junit.runners.model.Statement;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.domains.Domain;

import jenkins.model.Jenkins;
import hudson.model.Result;
import hudson.model.FreeStyleBuild;
import hudson.util.Secret;
import hudson.ExtensionList;
import hudson.security.ACL;
import org.jvnet.hudson.test.BuildWatcher;
import org.jvnet.hudson.test.RestartableJenkinsRule;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.common.ConsoleNotifier;

import org.jenkinsci.plugins.puppetenterprise.models.HieraConfig;
import org.jenkinsci.plugins.puppetenterprise.TestUtils;

public class HieraStepTest extends Assert {

  @ClassRule
  public static BuildWatcher buildWatcher = new BuildWatcher();

  @Rule
  public RestartableJenkinsRule story = new RestartableJenkinsRule();

  @Test
  public void setKeyValuePairInExistingScope() throws Exception {

    story.addStep(new Statement() {
      @Override
      public void evaluate() throws Throwable {

        //Create a job where the Hiera key/value pair is set for existing scope
        WorkflowJob separateCredsJob = story.j.jenkins.createProject(WorkflowJob.class, "Set Hiera Key/Value pair in existing scope");
        separateCredsJob.setDefinition(new CpsFlowDefinition(
          "node { \n" +
          "  puppet.hiera scope: 'existing', key: 'testkey', value: 'testvalue'\n" +
          "}", true));
        story.j.assertBuildStatusSuccess(separateCredsJob.scheduleBuild2(0));

        //Verify key/pair exists through API call.
      }
    });
  }

  @Test
  public void setKeyValuePairInNewScope() throws Exception {

    story.addStep(new Statement() {
      @Override
      public void evaluate() throws Throwable {

        //Create a job where Hiera key/value pair is set for new scope
        WorkflowJob separateCredsJob = story.j.jenkins.createProject(WorkflowJob.class, "Set Hiera Key/Value pair in new scope");
        separateCredsJob.setDefinition(new CpsFlowDefinition(
          "node { \n" +
          "  puppet.hiera scope: 'new', key: 'testkey', value: 'testvalue'\n" +
          "}", true));
        story.j.assertBuildStatusSuccess(separateCredsJob.scheduleBuild2(0));

        //Verify key/pair exists through API call.
      }
    });
  }

  @Test
  public void setNewValueForExistingKey() throws Exception {

    story.addStep(new Statement() {
      @Override
      public void evaluate() throws Throwable {

        //Create a job where Hiera key/value pair is set for new scope
        WorkflowJob separateCredsJob = story.j.jenkins.createProject(WorkflowJob.class, "Set new Hiera value for existing key");
        separateCredsJob.setDefinition(new CpsFlowDefinition(
          "node { \n" +
          "  puppet.hiera scope: 'existing', key: 'testkey', value: 'newvalue'\n" +
          "}", true));
        story.j.assertBuildStatusSuccess(separateCredsJob.scheduleBuild2(0));

        //Verify key's value is correct through API call.
      }
    });
  }

}
