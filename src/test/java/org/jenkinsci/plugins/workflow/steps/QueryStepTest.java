package org.jenkinsci.plugins.workflow.steps;

import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.ClassRule;
import org.junit.AfterClass;
import org.junit.BeforeClass;
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
import org.jenkinsci.plugins.puppetenterprise.TestUtils;

public class QueryStepTest extends Assert {

  @ClassRule
  public static WireMockRule mockPuppetDBService = new WireMockRule(options()
    .dynamicPort()
    .httpsPort(8081)
    .keystorePath(TestUtils.getKeystorePath())
    .keystorePassword(TestUtils.getKeystorePassword()));

  @ClassRule
  public static BuildWatcher buildWatcher = new BuildWatcher();

  @Rule
  public RestartableJenkinsRule story = new RestartableJenkinsRule();

  @Before
  public void setup() {
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

  private String getNodeQueryResponseString() {
    return TestUtils.getAPIResponseBody("2016.4", "/pdb/query/v4", "node_results.json");
  }

  private String getBadQueryString() {
    return TestUtils.getAPIResponseBody("2016.4", "/pdb/query/v4", "bad_query.json");
  }

  @Test
  public void queryPuppetDBNodesSuccessful() throws Exception {

    mockPuppetDBService.stubFor(post(urlEqualTo("/pdb/query/v4"))
        .withHeader("content-type", equalTo("application/json"))
        .withHeader("X-Authentication", equalTo("super_secret_token_string"))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(getNodeQueryResponseString())));

    story.addStep(new Statement() {
      @Override
      public void evaluate() throws Throwable {

        //Create a job where the credentials are defined separately
        WorkflowJob job = story.j.jenkins.createProject(WorkflowJob.class, "Successful Query of All Nodes");
        job.setDefinition(new CpsFlowDefinition(
          "node { \n" +
          "  puppet.credentials 'pe-test-token'\n" +
          "  results = puppet.query 'nodes {}'\n" +
          "  println 'Root object is of type: ' + results.getClass()\n" +
          "  println 'First object latest_report_corrective_change is of type: ' + results[0]['latest_report_corrective_change'].getClass()\n" +
          "  println 'First object facts_timestamp is of type: ' + results[0]['facts_timestamp'].getClass()\n" +
          "  println 'First certname is: ' + results[0].certname\n" +
          "}", true));
        WorkflowRun result = job.scheduleBuild2(0).get();
        story.j.assertBuildStatusSuccess(result);
        story.j.assertLogContains("nodes {}", result);
        story.j.assertLogContains("Query returned 10 results.", result);
        story.j.assertLogContains("Root object is of type: class java.util.ArrayList", result);
        story.j.assertLogContains("First object latest_report_corrective_change is of type: class java.lang.Boolean", result);
        //TODO: The timestamp should be a Date object in a future release with breaking changes
        story.j.assertLogContains("First object facts_timestamp is of type: class java.lang.String", result);
        story.j.assertLogContains("First certname is: gitlab.inf.puppet.vm", result);

        verify(postRequestedFor(urlMatching("/pdb/query/v4"))
            .withRequestBody(equalToJson("{\"query\": \"nodes {}\"}"))
            .withHeader("Content-Type", matching("application/json"))
            .withHeader("X-Authentication", matching("super_secret_token_string")));
      }
    });
  }

  @Test
  public void queryPuppetDBNEmptyResults() throws Exception {

    mockPuppetDBService.stubFor(post(urlEqualTo("/pdb/query/v4"))
        .withHeader("content-type", equalTo("application/json"))
        .withHeader("X-Authentication", equalTo("super_secret_token_string"))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody("[]")));

    story.addStep(new Statement() {
      @Override
      public void evaluate() throws Throwable {

        //Create a job where the credentials are defined separately
        WorkflowJob job = story.j.jenkins.createProject(WorkflowJob.class, "Successful Query of All Nodes With No Results");
        job.setDefinition(new CpsFlowDefinition(
          "node { \n" +
          "  puppet.credentials 'pe-test-token'\n" +
          "  results = puppet.query 'nodes {}'\n" +
          "  assert results instanceof ArrayList \n" +
          "}", true));
        WorkflowRun result = job.scheduleBuild2(0).get();
        story.j.assertBuildStatusSuccess(result);
        story.j.assertLogContains("nodes {}", result);
        story.j.assertLogContains("Query returned 0 results.", result);

        verify(postRequestedFor(urlMatching("/pdb/query/v4"))
            .withRequestBody(equalToJson("{\"query\": \"nodes {}\"}"))
            .withHeader("Content-Type", matching("application/json"))
            .withHeader("X-Authentication", matching("super_secret_token_string")));
      }
    });
  }

  @Test
  public void malformedQueryFails() throws Exception {

    mockPuppetDBService.stubFor(post(urlEqualTo("/pdb/query/v4"))
        .withHeader("content-type", equalTo("application/json"))
        .withHeader("X-Authentication", equalTo("super_secret_token_string"))
        .willReturn(aResponse()
            .withStatus(400)
            .withHeader("Content-Type", "text/plain")
            .withBody(getBadQueryString())));

    story.addStep(new Statement() {
      @Override
      public void evaluate() throws Throwable {

        //Create a job where the credentials are defined separately
        WorkflowJob job = story.j.jenkins.createProject(WorkflowJob.class, "Failed Job With Malformed PQL Query");
        job.setDefinition(new CpsFlowDefinition(
          "node { \n" +
          "  puppet.credentials 'pe-test-token'\n" +
          "  puppet.query 'nodes badquery {}'\n" +
          "}", true));
        WorkflowRun result = job.scheduleBuild2(0).get();
        story.j.assertBuildStatus(Result.FAILURE, result);
        story.j.assertLogContains("PQL Query Error", result);
        story.j.assertLogContains("Kind:    puppetlabs.puppdb/malformed-query", result);
        story.j.assertLogContains("Message: PQL parse error at line 1, column 7:", result);
      }
    });
  }

  @Test
  public void queryFailsOnExpiredToken() throws Exception {

    mockPuppetDBService.stubFor(post(urlEqualTo("/pdb/query/v4"))
        .withHeader("content-type", equalTo("application/json"))
        .willReturn(aResponse()
            .withStatus(401)
            .withHeader("Content-Type", "application/json")
            .withBody(TestUtils.getAPIResponseBody("2016.4", "pdb/query/v4", "expired_token.json"))));

    story.addStep(new Statement() {
      @Override
      public void evaluate() throws Throwable {

        WorkflowJob job = story.j.jenkins.createProject(WorkflowJob.class, "Query Fails on Expired Token");
        job.setDefinition(new CpsFlowDefinition(
          "node { \n" +
          "  puppet.query 'production', credentials: 'pe-test-token'\n" +
          "}", true));
        WorkflowRun result = job.scheduleBuild2(0).get();
        story.j.assertBuildStatus(Result.FAILURE, result);
        story.j.assertLogContains("Kind:    puppetlabs.rbac/token-expired", result);
        story.j.assertLogContains("Message: The provided token has expired.", result);
      }
    });
  }

  @Test
  public void queryFailsOnMissingToken() throws Exception {
    story.addStep(new Statement() {
      @Override
      public void evaluate() throws Throwable {

        WorkflowJob job = story.j.jenkins.createProject(WorkflowJob.class, "Query Fails on Missing Token");
        job.setDefinition(new CpsFlowDefinition(
          "node { \n" +
          "  puppet.query 'production', credentials: 'doesnotexist'\n" +
          "}", true));
        WorkflowRun result = job.scheduleBuild2(0).get();
        story.j.assertBuildStatus(Result.FAILURE, result);
        story.j.assertLogContains("Could not find Jenkins credential with ID: doesnotexist", result);
        story.j.assertLogContains("Please ensure the credentials exist in Jenkins. Note, the credentials description is not its ID", result);
      }
    });
  }
}
