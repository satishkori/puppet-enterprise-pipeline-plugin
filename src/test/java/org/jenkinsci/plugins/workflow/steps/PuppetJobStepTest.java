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
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileNotFoundException;
import java.lang.StringBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.common.ConsoleNotifier;

import org.jenkinsci.plugins.puppetenterprise.models.PuppetEnterpriseConfig;

public class PuppetJobStepTest extends Assert {

  private static String CACERTPATH = "src/test/java/resources/certs/ca.cert.pem";
  private static String JOBDETAILSPATH = "src/test/java/resources/api-responses/job_details.json";
  private static String KEYSTOREPATH = "src/test/java/resources/server.keystore";
  private static String KEYSTOREPASSWORD = "password";

  @ClassRule
  public static WireMockRule mockOrchestratorService = new WireMockRule(options()
    .dynamicPort()
    .httpsPort(8143)
    .keystorePath(KEYSTOREPATH)
    .keystorePassword(KEYSTOREPASSWORD));

  @ClassRule
  public static WireMockRule mockPuppetService = new WireMockRule(options()
    .dynamicPort()
    .httpsPort(8140)
    .keystorePath(KEYSTOREPATH)
    .keystorePassword(KEYSTOREPASSWORD));

  @ClassRule
  public static BuildWatcher buildWatcher = new BuildWatcher();

  @Rule
  public RestartableJenkinsRule story = new RestartableJenkinsRule();

  @Before
  public void setup() {
    mockPuppetService.stubFor(get(urlEqualTo("/puppet-ca/v1/certificate/ca"))
        .willReturn(aResponse()
            .withStatus(200)
            .withBody(getCACertificateString())));

    story.addStep(new Statement() {
      @Override
      public void evaluate() throws Throwable {
        try {
          PuppetEnterpriseConfig.setPuppetMasterUrl("localhost");
        }
        catch(java.io.IOException e) {e.printStackTrace();}
        catch(java.security.NoSuchAlgorithmException e) {e.printStackTrace();}
        catch(java.security.KeyStoreException e) {e.printStackTrace();}
        catch(java.security.KeyManagementException e) {e.printStackTrace();}

        StringCredentialsImpl credential = new StringCredentialsImpl(CredentialsScope.GLOBAL, "pe-test-token", "PE test token", Secret.fromString("super_secret_token_string"));
        CredentialsStore store = CredentialsProvider.lookupStores(story.j.jenkins).iterator().next();
        store.addCredentials(Domain.global(), credential);
      }
    });
  }

  private String getFileContents(String file) {
    BufferedReader br = null;

    try {
      br = new BufferedReader(new FileReader(file));
    } catch(java.io.FileNotFoundException e) {
      fail("Cannot find file " + file + ". Did someone delete it from the source?");
    }

    String fileContents = "";

    try {
      StringBuilder sb = new StringBuilder();
      String line = br.readLine();

      while (line != null) {
        sb.append(line);
        sb.append(System.lineSeparator());
        line = br.readLine();
      }
      fileContents = sb.toString();
    } catch(java.io.IOException e) {
      fail(e.getMessage());
    } finally {
      try {
        br.close();
      } catch(java.io.IOException e){
        e.printStackTrace();
      }
    }

    return fileContents;
  }

  private String getCACertificateString() {
    return getFileContents(CACERTPATH);
  }

  private String getJobDetailsString() {
    return getFileContents(JOBDETAILSPATH);
  }

  private String getCommandDeployResponseString() {
    return getFileContents("src/test/java/resources/api-responses/job_deploy.json");
  }

  private void stubJobDeploySuccessful() {
    mockOrchestratorService.stubFor(post(urlEqualTo("/orchestrator/v1/command/deploy"))
        .withHeader("content-type", equalTo("application/json"))
        .withHeader("X-Authentication", equalTo("super_secret_token_string"))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(getCommandDeployResponseString())));

    mockOrchestratorService.stubFor(get(urlEqualTo("/orchestrator/v1/jobs/711"))
        .withHeader("X-Authentication", equalTo("super_secret_token_string"))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(getJobDetailsString())));
  }

  @Test
  public void puppetJobSeparateCredentialsCallSuccessful() throws Exception {

    stubJobDeploySuccessful();

    story.addStep(new Statement() {
      @Override
      public void evaluate() throws Throwable {

        //Create a job where the credentials are defined separately
        WorkflowJob job = story.j.jenkins.createProject(WorkflowJob.class, "Puppet Job with Credentials Defined Separately");
        job.setDefinition(new CpsFlowDefinition(
          "node { \n" +
          "  puppet.credentials 'pe-test-token'\n" +
          "  puppet.job 'production'\n" +
          "}", true));
        story.j.assertBuildStatusSuccess(job.scheduleBuild2(0));

        verify(postRequestedFor(urlMatching("/orchestrator/v1/command/deploy"))
            .withRequestBody(equalToJson("{\"environment\": \"production\", \"noop\": false}"))
            .withHeader("Content-Type", matching("application/json"))
            .withHeader("X-Authentication", matching("super_secret_token_string")));

        verify(getRequestedFor(urlMatching("/orchestrator/v1/jobs/711"))
            .withHeader("X-Authentication", matching("super_secret_token_string")));
      }
    });
  }

  @Test
  public void puppetJobCredentialsInMethodSuccessful() throws Exception {

    stubJobDeploySuccessful();

    story.addStep(new Statement() {
      @Override
      public void evaluate() throws Throwable {
        //Create a job where the credentials are defined as part of the job method call
        WorkflowJob job = story.j.jenkins.createProject(WorkflowJob.class, "Puppet Job with Credentials Defined With Method Call");
        job.setDefinition(new CpsFlowDefinition(
          "node { \n" +
          "  puppet.job 'production', credentials: 'pe-test-token'\n" +
          "}", true));
        story.j.assertBuildStatusSuccess(job.scheduleBuild2(0));

        verify(postRequestedFor(urlMatching("/orchestrator/v1/command/deploy"))
            .withRequestBody(equalToJson("{\"environment\": \"production\", \"noop\" : false}"))
            .withHeader("X-Authentication", matching("super_secret_token_string"))
            .withHeader("Content-Type", matching("application/json")));

        verify(getRequestedFor(urlMatching("/orchestrator/v1/jobs/711"))
            .withHeader("X-Authentication", matching("super_secret_token_string")));
      }
    });
  }

  @Test
  public void puppetJobFailsOnNoSuchEnvironment() throws Exception {

    mockOrchestratorService.stubFor(post(urlEqualTo("/orchestrator/v1/command/deploy"))
        .withHeader("content-type", equalTo("application/json"))
        .willReturn(aResponse()
            .withStatus(404)
            .withHeader("Content-Type", "application/json")
            .withBody("{\"kind\": \"puppetlabs.orchestrator/unknown-environment\",\"msg\": \"Unknown environment nosuchenv\",\"details\": {\"environment\": \"nosuchenv\" }}")));

    story.addStep(new Statement() {
      @Override
      public void evaluate() throws Throwable {

        WorkflowJob job = story.j.jenkins.createProject(WorkflowJob.class, "Puppet Job of Non-Existent Environment Fails");
        job.setDefinition(new CpsFlowDefinition(
          "node { \n" +
          "  puppet.job 'nosuchenv', credentials: 'pe-test-token'\n" +
          "}", true));
        WorkflowRun result = job.scheduleBuild2(0).get();
        story.j.assertBuildStatus(Result.FAILURE, result);
      }
    });
  }

  @Test
  public void puppetJobFailsOnExpiredToken() throws Exception {

    mockOrchestratorService.stubFor(post(urlEqualTo("/orchestrator/v1/command/deploy"))
        .withHeader("content-type", equalTo("application/json"))
        .willReturn(aResponse()
            .withStatus(401)
            .withHeader("Content-Type", "application/json")
            .withBody("{\"kind\":\"puppetlabs.rbac/token-expired\",\"msg\":\"The provided token has expired.\"}")));

    story.addStep(new Statement() {
      @Override
      public void evaluate() throws Throwable {

        WorkflowJob job = story.j.jenkins.createProject(WorkflowJob.class, "Puppet Job Fails on Expired Token");
        job.setDefinition(new CpsFlowDefinition(
          "node { \n" +
          "  puppet.job 'production', credentials: 'pe-test-token'\n" +
          "}", true));
        WorkflowRun result = job.scheduleBuild2(0).get();
        story.j.assertBuildStatus(Result.FAILURE, result);
      }
    });
  }

  @Test
  public void puppetJobParameters() throws Exception {

    story.addStep(new Statement() {
      @Override
      public void evaluate() throws Throwable {

        WorkflowJob job = story.j.jenkins.createProject(WorkflowJob.class, "Puppet Job Passes Application Parameter");
        job.setDefinition(new CpsFlowDefinition(
          "node { \n" +
          "  puppet.credentials 'pe-test-token'\n" +
          "  puppet.job 'production', application: 'MyApp'\n" +
          "}", true));
        WorkflowRun result = job.scheduleBuild2(0).get();

        verify(postRequestedFor(urlMatching("/orchestrator/v1/command/deploy"))
            .withRequestBody(equalToJson("{\"environment\": \"production\", \"scope\": {\"application\": \"MyApp\"}, \"noop\": false}"))
            .withHeader("X-Authentication", matching("super_secret_token_string"))
            .withHeader("Content-Type", matching("application/json")));

        verify(getRequestedFor(urlMatching("/orchestrator/v1/jobs/711"))
            .withHeader("X-Authentication", matching("super_secret_token_string")));
      }
    });

    story.addStep(new Statement() {
      @Override
      public void evaluate() throws Throwable {

        WorkflowJob job = story.j.jenkins.createProject(WorkflowJob.class, "Puppet Job Passes Target Parameter");
        job.setDefinition(new CpsFlowDefinition(
          "node { \n" +
          "  puppet.credentials 'pe-test-token'\n" +
          "  puppet.job 'production', target: 'MyApp'\n" +
          "}", true));
        WorkflowRun result = job.scheduleBuild2(0).get();

        verify(postRequestedFor(urlMatching("/orchestrator/v1/command/deploy"))
            .withRequestBody(equalToJson("{\"environment\": \"production\", \"target\": \"MyApp\", \"noop\": false}"))
            .withHeader("X-Authentication", matching("super_secret_token_string"))
            .withHeader("Content-Type", matching("application/json")));
      }
    });

    story.addStep(new Statement() {
      @Override
      public void evaluate() throws Throwable {

        WorkflowJob job = story.j.jenkins.createProject(WorkflowJob.class, "Puppet Job Passes Nodes Parameter");
        job.setDefinition(new CpsFlowDefinition(
          "node { \n" +
          "  puppet.credentials 'pe-test-token'\n" +
          "  puppet.job 'production', nodes: ['node1','node2']\n" +
          "}", true));
        WorkflowRun result = job.scheduleBuild2(0).get();

        verify(postRequestedFor(urlMatching("/orchestrator/v1/command/deploy"))
            .withRequestBody(equalToJson("{\"environment\": \"production\", \"scope\": {\"nodes\": [\"node1\",\n\"node2\"]}, \"noop\" : false}"))
            .withHeader("X-Authentication", matching("super_secret_token_string"))
            .withHeader("Content-Type", matching("application/json")));
      }
    });

    story.addStep(new Statement() {
      @Override
      public void evaluate() throws Throwable {

        WorkflowJob job = story.j.jenkins.createProject(WorkflowJob.class, "Puppet Job Passes Query Parameter");
        job.setDefinition(new CpsFlowDefinition(
          "node { \n" +
          "  puppet.credentials 'pe-test-token'\n" +
          "  puppet.job 'production', query: 'inventory[certname] {environment = \"production\"}'\n" +
          "}", true));
        WorkflowRun result = job.scheduleBuild2(0).get();

        verify(postRequestedFor(urlMatching("/orchestrator/v1/command/deploy"))
            .withRequestBody(equalToJson("{\"environment\": \"production\", \"scope\": {\"query\": \"inventory[certname] {environment = \\\"production\\\"}\"}, \"noop\": false}"))
            .withHeader("X-Authentication", matching("super_secret_token_string"))
            .withHeader("Content-Type", matching("application/json")));
      }
    });

    story.addStep(new Statement() {
      @Override
      public void evaluate() throws Throwable {

        WorkflowJob job = story.j.jenkins.createProject(WorkflowJob.class, "Puppet Job Passes Concurrency Parameter");
        job.setDefinition(new CpsFlowDefinition(
          "node { \n" +
          "  puppet.credentials 'pe-test-token'\n" +
          "  puppet.job 'production', concurrency: 40\n" +
          "}", true));
        WorkflowRun result = job.scheduleBuild2(0).get();

        verify(postRequestedFor(urlMatching("/orchestrator/v1/command/deploy"))
            .withRequestBody(equalToJson("{\"environment\": \"production\", \"concurrency\": 40, \"noop\": false}"))
            .withHeader("X-Authentication", matching("super_secret_token_string"))
            .withHeader("Content-Type", matching("application/json")));
      }
    });

    story.addStep(new Statement() {
      @Override
      public void evaluate() throws Throwable {

        WorkflowJob job = story.j.jenkins.createProject(WorkflowJob.class, "Puppet Job Passes Noop Parameter");
        job.setDefinition(new CpsFlowDefinition(
          "node { \n" +
          "  puppet.credentials 'pe-test-token'\n" +
          "  puppet.job 'production', noop: true\n" +
          "}", true));
        WorkflowRun result = job.scheduleBuild2(0).get();

        verify(postRequestedFor(urlMatching("/orchestrator/v1/command/deploy"))
            .withRequestBody(equalToJson("{\"environment\": \"production\", \"noop\": true}"))
            .withHeader("X-Authentication", matching("super_secret_token_string"))
            .withHeader("Content-Type", matching("application/json")));
      }
    });
  }
}
