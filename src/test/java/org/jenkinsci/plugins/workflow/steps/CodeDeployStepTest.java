package org.jenkinsci.plugins.workflow.steps;

import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.ClassRule;
import org.junit.runners.model.Statement;
import org.junit.experimental.theories.*;
import org.junit.runner.RunWith;

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

import org.jenkinsci.plugins.puppetenterprise.models.PuppetEnterpriseConfig;
import org.jenkinsci.plugins.puppetenterprise.TestUtils;

@RunWith(Theories.class)
public class CodeDeployStepTest extends Assert {

  public static @DataPoints String[] PEVersions = {"2016.2","2016.4"};

  @ClassRule
  public static WireMockRule mockCodeManagerService = new WireMockRule(options()
    .dynamicPort()
    .httpsPort(8170)
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

  @Theory
  public void codeDeploySeparateCredentialsCallSuccessful(final String peVersion) throws Exception {

    mockCodeManagerService.stubFor(post(urlEqualTo("/code-manager/v1/deploys"))
        .withHeader("content-type", equalTo("application/json"))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(TestUtils.getAPIResponseBody(peVersion, "/code-manager/v1/deploys", "code_deploy.json"))));

    story.addStep(new Statement() {
      @Override
      public void evaluate() throws Throwable {

        //Create a job where the credentials are defined separately
        WorkflowJob separateCredsJob = story.j.jenkins.createProject(WorkflowJob.class, "Code Deploy with Credentials Defined Separately");
        separateCredsJob.setDefinition(new CpsFlowDefinition(
          "node { \n" +
          "  puppet.credentials 'pe-test-token'\n" +
          "  puppet.codeDeploy 'production'\n" +
          "}", true));
        story.j.assertBuildStatusSuccess(separateCredsJob.scheduleBuild2(0));

        verify(postRequestedFor(urlMatching("/code-manager/v1/deploys"))
            .withRequestBody(equalToJson("{\"environments\": [\"production\"], \"wait\": true}"))
            .withHeader("Content-Type", matching("application/json"))
            .withHeader("X-Authentication", matching("super_secret_token_string")));
      }
    });
  }

  @Theory
  public void codeDeployCredentialsInMethodSuccessful(final String peVersion) throws Exception {

    mockCodeManagerService.stubFor(post(urlEqualTo("/code-manager/v1/deploys"))
        .withHeader("content-type", equalTo("application/json"))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(TestUtils.getAPIResponseBody(peVersion, "/code-manager/v1/deploys", "code_deploy.json"))));

    story.addStep(new Statement() {
      @Override
      public void evaluate() throws Throwable {
        //Create a job where the credentials are defined as part of the codeDeploy method call
        WorkflowJob job = story.j.jenkins.createProject(WorkflowJob.class, "Code Deploy with Credentials Defined With Method Call");
        job.setDefinition(new CpsFlowDefinition(
          "node { \n" +
          "  puppet.codeDeploy 'production', credentials: 'pe-test-token'\n" +
          "}", true));

        WorkflowRun result = job.scheduleBuild2(0).get();
        story.j.assertBuildStatusSuccess(result);
        story.j.assertLogContains("1 successful. 0 failed.", result);
        story.j.assertLogContains("production: complete", result);

        verify(postRequestedFor(urlMatching("/code-manager/v1/deploys"))
            .withRequestBody(equalToJson("{\"environments\": [\"production\"], \"wait\": true}"))
            .withHeader("X-Authentication", matching("super_secret_token_string"))
            .withHeader("Content-Type", matching("application/json")));
      }
    });
  }

  @Theory
  public void codeDeployFailsOnNoSuchEnvironment(final String peVersion) throws Exception {

    mockCodeManagerService.stubFor(post(urlEqualTo("/code-manager/v1/deploys"))
        .withHeader("content-type", equalTo("application/json"))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(TestUtils.getAPIResponseBody(peVersion, "/code-manager/v1/deploys", "code_deploy_failed_no_such_env.json"))));

    story.addStep(new Statement() {
      @Override
      public void evaluate() throws Throwable {

        WorkflowJob job = story.j.jenkins.createProject(WorkflowJob.class, "Code Deploy of Non-Existent Environment Fails");
        job.setDefinition(new CpsFlowDefinition(
          "node { \n" +
          "  puppet.codeDeploy 'nosuchenv', credentials: 'pe-test-token'\n" +
          "}", true));
        WorkflowRun result = job.scheduleBuild2(0).get();
        story.j.assertBuildStatus(Result.FAILURE, result);
        story.j.assertLogContains("nosuchenv: failed", result);
        story.j.assertLogContains("Kind:    puppetlabs.code-manager/deploy-failure", result);
      }
    });
  }

  @Theory
  public void codeDeployFailsOnExpiredToken(final String peVersion) throws Exception {

    mockCodeManagerService.stubFor(post(urlEqualTo("/code-manager/v1/deploys"))
        .withHeader("content-type", equalTo("application/json"))
        .willReturn(aResponse()
            .withStatus(401)
            .withHeader("Content-Type", "application/json")
            .withBody(TestUtils.getAPIResponseBody(peVersion, "/code-manager/v1/deploys", "expired_token.json"))));

    story.addStep(new Statement() {
      @Override
      public void evaluate() throws Throwable {

        WorkflowJob job = story.j.jenkins.createProject(WorkflowJob.class, "Code Deploy Fails on Expired Token");
        job.setDefinition(new CpsFlowDefinition(
          "node { \n" +
          "  puppet.codeDeploy 'production', credentials: 'pe-test-token'\n" +
          "}", true));
        WorkflowRun result = job.scheduleBuild2(0).get();
        story.j.assertBuildStatus(Result.FAILURE, result);
        story.j.assertLogContains("Kind:    puppetlabs.rbac/token-expired", result);
        story.j.assertLogContains("Message: The provided token has expired.", result);
      }
    });
  }

  @Test
  public void codeDeployFailsOnMissingToken() throws Exception {
    story.addStep(new Statement() {
      @Override
      public void evaluate() throws Throwable {

        WorkflowJob job = story.j.jenkins.createProject(WorkflowJob.class, "Code Deploy Fails on Missing Token");
        job.setDefinition(new CpsFlowDefinition(
          "node { \n" +
          "  puppet.codeDeploy 'production', credentials: 'doesnotexist'\n" +
          "}", true));
        WorkflowRun result = job.scheduleBuild2(0).get();
        story.j.assertBuildStatus(Result.FAILURE, result);
        story.j.assertLogContains("Could not find Jenkins credential with ID: doesnotexist", result);
        story.j.assertLogContains("Please ensure the credentials exist in Jenkins. Note, the credentials description is not its ID", result);
      }
    });
  }
}
