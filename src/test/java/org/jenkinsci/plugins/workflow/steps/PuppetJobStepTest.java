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

import org.jenkinsci.plugins.puppetenterprise.models.PuppetEnterpriseConfig;

public class PuppetJobStepTest extends Assert {

  @ClassRule
  public static WireMockRule wireMockRule = new WireMockRule(options()
    .httpsPort(8143)
    .keystorePath("src/test/java/resources/server.keystore")
    .keystorePassword("password"));

  @ClassRule
  public static BuildWatcher buildWatcher = new BuildWatcher();

  @Rule
  public RestartableJenkinsRule story = new RestartableJenkinsRule();

  @Before
  public void setup() {
    story.addStep(new Statement() {
      @Override
      public void evaluate() throws Throwable {
        //The value must be a valid certificate string.
        //This is better than stubbing out every method
        PuppetEnterpriseConfig.setPuppetMasterCACertificate(
        "-----BEGIN CERTIFICATE-----\n" +
        "MIIGgDCCBGigAwIBAgIJANiyjv+rJuUAMA0GCSqGSIb3DQEBCwUAMIGGMQswCQYD\n" +
        "VQQGEwJVUzELMAkGA1UECBMCT1IxETAPBgNVBAcTCFBvcnRsYW5kMQ8wDQYDVQQK\n" +
        "EwZQdXBwZXQxFDASBgNVBAsTC0VuZ2luZWVyaW5nMRAwDgYDVQQDEwdKZW5raW5z\n" +
        "MR4wHAYJKoZIhvcNAQkBFg9jYXJsQHB1cHBldC5jb20wHhcNMTYxMTI5MTYzNTU1\n" +
        "WhcNMzYxMTI0MTYzNTU1WjCBhjELMAkGA1UEBhMCVVMxCzAJBgNVBAgTAk9SMREw\n" +
        "DwYDVQQHEwhQb3J0bGFuZDEPMA0GA1UEChMGUHVwcGV0MRQwEgYDVQQLEwtFbmdp\n" +
        "bmVlcmluZzEQMA4GA1UEAxMHSmVua2luczEeMBwGCSqGSIb3DQEJARYPY2FybEBw\n" +
        "dXBwZXQuY29tMIICIjANBgkqhkiG9w0BAQEFAAOCAg8AMIICCgKCAgEAzuABe71S\n" +
        "Rb9x1lgo8otNavDfAxSJ5PijYpEYbNq6hdQBOwDjMEnP0WJBiv7aBSoNosMNgA8k\n" +
        "viLDZRLzrFBfO7e0d5uGDn41dOOUDlMB5NevZ2mZQEiN92CiGtEUx+WkPfERAmu/\n" +
        "cNWfPdX+28Pc9RVWZFw1E0xgDtQKYy9XjgP/i+i9N9w+40oPlklR4+uthqUO+YqZ\n" +
        "08Ag8eV2THeBOgO8nwnOX6OZK4CG9nNKQViM1Mz9Y8wD2lO8BGIMdqt/TfWdYaql\n" +
        "ak0XuuJwOV1cOc7iHIHyDjrifLFLZmWfQit3LocKpdARdhY0Mpcy5IG7ZvIjPD5J\n" +
        "cvE7a+gWxnjss6pSGwmA0chRXV87zd9pMyIqW2PT+hHt9fx0a9wAEATb22uMQq+X\n" +
        "2QgwrybTyK66298qDb6bTqVRbzW4I7batw1HDU6rrtIIzCCH/SQc0KlU8w0fr3In\n" +
        "u9zOOT0kztnqmACHgUSqEQXWMJff1seb1RE/eEklJVqKwqkdD7cUv0lDKFnxwHsS\n" +
        "aRh39k08b7SS/1448TDkrVfiJUNb7t6GFoS+K6XKADLWqaWzr760Rm89ISOAinyy\n" +
        "dc5oEnDtvuyP0Z0zaTXhNMgqjlChy77LxStIRGl0AOmZZ32716ufXs3dFTT2XtWd\n" +
        "zbvb2klbE1v2NWDUvd7byGdzjRjXb2FgNh0CAwEAAaOB7jCB6zAdBgNVHQ4EFgQU\n" +
        "s2GknyTtcEW5NOjMbo7KwiDHpHIwgbsGA1UdIwSBszCBsIAUs2GknyTtcEW5NOjM\n" +
        "bo7KwiDHpHKhgYykgYkwgYYxCzAJBgNVBAYTAlVTMQswCQYDVQQIEwJPUjERMA8G\n" +
        "A1UEBxMIUG9ydGxhbmQxDzANBgNVBAoTBlB1cHBldDEUMBIGA1UECxMLRW5naW5l\n" +
        "ZXJpbmcxEDAOBgNVBAMTB0plbmtpbnMxHjAcBgkqhkiG9w0BCQEWD2NhcmxAcHVw\n" +
        "cGV0LmNvbYIJANiyjv+rJuUAMAwGA1UdEwQFMAMBAf8wDQYJKoZIhvcNAQELBQAD\n" +
        "ggIBAMejVMdYN9mYkbcYQ8FaGVN1bphOwP0bEIEwzdxI9CbR2X4erHsXrTkpV+g4\n" +
        "XPCAOSOaPUj83T4yN8kHxLlXKKISnfXy1IZfAuT3kgu3UR6mNIOy1HoW7BaeXJvn\n" +
        "WK7hxm1pAIT+m+ihSmQrIsXlaL3WHLmwP1dOUx5rhS2OUXwafZBo5VvbLwx0CxwE\n" +
        "HN6rzXkEyBMWEWgtuPGX4z3yn+vPotJ/g/VVdw1APJr0AYpWiYQT5nd3La0iknoL\n" +
        "5lA2jIM9nvWk3Wpqbm5/Gnhbqf7/epBPHQpKNlmtPhYUqC7irium8fj10rqzvzYC\n" +
        "gJt4kMTIw9S2ZOngFmIhvBHFkLfI6xTsTpqn9TUbTB+jrjV+f5bpGRNyRkjNNF8m\n" +
        "bGh3cjo/UVrGOk5yROQZqG3ZhosVq4R6UNUEt3riPhuPEeaNM1+gIzFgT5FOElfW\n" +
        "rVqyZXek7HtHXzwhveK+gqxONMRA0nFmd/M15z9QNtvbnkw0NEBE6ftCQezmqJPz\n" +
        "ikURdc8WRl1/sh1Jbrsww3sBEGl6EF69+LedRKnJkUvaGr1Er3hR5IU5hwANB8Wy\n" +
        "RhDx8BH6dMKN2UxYA1SyHGpnQ8EQEkIaFmfvKSmY9LI1yGgol/jZYNlbnB9GEB6Z\n" +
        "hvvqzMJim/m4rdqogJHzdM0HmF1Qct+VgsscBh/E/ClfCyHz\n" +
        "-----END CERTIFICATE-----");

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

  private void stubJobDeploySuccessful() {
    stubFor(post(urlEqualTo("/orchestrator/v1/command/deploy"))
        .withHeader("content-type", equalTo("application/json"))
        .withHeader("X-Authentication", equalTo("super_secret_token_string"))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody("{\"job\" : {\"id\" : \"https://localhost:8143/orchestrator/v1/jobs/711\",\"name\": \"711\" }}")));

    stubFor(get(urlEqualTo("/orchestrator/v1/jobs/711"))
        .withHeader("X-Authentication", equalTo("super_secret_token_string"))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(
              " {\n" +
              "   \"report\" : {\n" +
              "     \"id\" : \"https://localhost:8143/orchestrator/v1/jobs/714/report\"\n" +
              "   },\n" +
              "   \"name\" : \"714\",\n" +
              "   \"events\" : {\n" +
              "     \"id\" : \"https://localhost:8143/orchestrator/v1/jobs/714/events\"\n" +
              "   },\n" +
              "   \"state\" : \"finished\",\n" +
              "   \"nodes\" : {\n" +
              "     \"id\" : \"https://localhost:8143/orchestrator/v1/jobs/714/nodes\"\n" +
              "   },\n" +
              "   \"status\" : [ {\n" +
              "     \"state\" : \"new\",\n" +
              "     \"enter_time\" : \"2016-11-30T05:35:16Z\",\n" +
              "     \"exit_time\" : \"2016-11-30T05:35:17Z\"\n" +
              "   }, {\n" +
              "     \"state\" : \"ready\",\n" +
              "     \"enter_time\" : \"2016-11-30T05:35:17Z\",\n" +
              "     \"exit_time\" : \"2016-11-30T05:35:17Z\"\n" +
              "   }, {\n" +
              "     \"state\" : \"running\",\n" +
              "     \"enter_time\" : \"2016-11-30T05:35:17Z\",\n" +
              "     \"exit_time\" : \"2016-11-30T05:35:17Z\"\n" +
              "   }, {\n" +
              "     \"state\" : \"finished\",\n" +
              "     \"enter_time\" : \"2016-11-30T05:35:17Z\",\n" +
              "     \"exit_time\" : null\n" +
              "   } ],\n" +
              "   \"id\" : \"https://localhost:8143/orchestrator/v1/jobs/714\",\n" +
              "   \"environment\" : {\n" +
              "     \"name\" : \"production\"\n" +
              "   },\n" +
              "   \"options\" : {\n" +
              "     \"concurrency\" : null,\n" +
              "     \"noop\" : false,\n" +
              "     \"trace\" : false,\n" +
              "     \"debug\" : false,\n" +
              "     \"scope\" : {\n" +
              "       \"whole_environment\" : true\n" +
              "     },\n" +
              "     \"enforce_environment\" : true,\n" +
              "     \"environment\" : \"production\",\n" +
              "     \"evaltrace\" : false,\n" +
              "     \"target\" : \"whole environment\"\n" +
              "   },\n" +
              "   \"timestamp\" : \"2016-11-30T05:35:17Z\",\n" +
              "   \"owner\" : {\n" +
              "     \"email\" : \"casey@test.com\",\n" +
              "     \"is_revoked\" : false,\n" +
              "     \"last_login\" : \"2016-11-30T04:55:03.625Z\",\n" +
              "     \"is_remote\" : true,\n" +
              "     \"login\" : \"casey\",\n" +
              "     \"inherited_role_ids\" : [ 3, 2 ],\n" +
              "     \"group_ids\" : [ \"76fa0087-980f-49c4-915a-b20de7405b31\", \"38da1d42-a50c-489c-a6d0-3d5525662778\" ],\n" +
              "     \"is_superuser\" : false,\n" +
              "     \"id\" : \"885c5177-1ea5-416f-bf09-2858bef5c46b\",\n" +
              "     \"role_ids\" : [ 3, 1 ],\n" +
              "     \"display_name\" : \"Casey London\",\n" +
              "    \"is_group\" : false\n" +
              "  },\n" +
              "  \"node_count\" : 11\n" +
              "}")));
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

    stubFor(post(urlEqualTo("/orchestrator/v1/command/deploy"))
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

    stubFor(post(urlEqualTo("/orchestrator/v1/command/deploy"))
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

    // stubJobDeploySuccessful();

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
        // story.j.assertBuildStatus(Result.FAILURE, result);

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
        // story.j.assertBuildStatus(Result.FAILURE, result);

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
        // story.j.assertBuildStatus(Result.FAILURE, result);

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
        // story.j.assertBuildStatus(Result.FAILURE, result);

        verify(postRequestedFor(urlMatching("/orchestrator/v1/command/deploy"))
            .withRequestBody(equalToJson("{\"environment\": \"production\", \"scope\": {\"query\": \"inventory[certname] {environment = \\\"production\\\"}\"}, \"noop\": false}"))
            .withHeader("X-Authentication", matching("super_secret_token_string"))
            .withHeader("Content-Type", matching("application/json")));
      }
    });
  }
}
