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

public class CodeDeployStepTest extends Assert {

  @ClassRule
  public static WireMockRule wireMockRule = new WireMockRule(options()
    .notifier(new ConsoleNotifier(true))
    .httpsPort(8170)
    .keystorePath("src/test/java/resources/server.keystore")
    .keystorePassword("password"));

  @ClassRule
  public static BuildWatcher buildWatcher = new BuildWatcher();

  @Rule
  public RestartableJenkinsRule story = new RestartableJenkinsRule();

  @Before
  public void setup() { }

  @Test
  public void codeDeploy() throws Exception {

    stubFor(post(urlEqualTo("/code-manager/v1/deploys"))
        .withHeader("content-type", equalTo("application/json"))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody("[{\"environment\":\"production\", \"id\":6, \"status\":\"complete\", \"file-sync\":{\"environment-commit\":\"0d1bf46d5613a819ebb76ded56ebdedd0a326be3\",\"code-commit\":\"4bbf215913a801f1c090f5a58382c3009e4e5905\"}, \"deploy-signature\":\"45ddf48253c2ee7537aae05c7e674879fd8bb616\"}]")));

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

        //Create a job where the credentials are defined separately
        WorkflowJob separateCredsJob = story.j.jenkins.createProject(WorkflowJob.class, "codeDeployWithCredentialsDefinedSeparately");
        separateCredsJob.setDefinition(new CpsFlowDefinition(
          "node { \n" +
          "  puppet.credentials 'pe-test-token'\n" +
          "  puppet.codeDeploy 'production'\n" +
          "}", true));
        story.j.assertBuildStatusSuccess(separateCredsJob.scheduleBuild2(0));

        verify(postRequestedFor(urlMatching("/code-manager/v1/deploys"))
            .withRequestBody(equalToJson("{\"environments\": [\"production\"], \"wait\": true}"))
            .withHeader("Content-Type", matching("application/json")));

        //Create a job where the credentials are defined as part of the codeDeploy method call
        WorkflowJob integratedCredsJob = story.j.jenkins.createProject(WorkflowJob.class, "Code Deploy with Credentials Defined With Method Call");
        integratedCredsJob.setDefinition(new CpsFlowDefinition(
          "node { \n" +
          "  puppet.codeDeploy 'production', credentials: 'pe-test-token'\n" +
          "}", true));
        story.j.assertBuildStatusSuccess(integratedCredsJob.scheduleBuild2(0));

        verify(postRequestedFor(urlMatching("/code-manager/v1/deploys"))
            .withRequestBody(equalToJson("{\"environments\": [\"production\"], \"wait\": true}"))
            .withHeader("Content-Type", matching("application/json")));
      }
    });
  }
}
