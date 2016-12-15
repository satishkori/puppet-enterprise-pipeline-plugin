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

import java.net.*;
import jenkins.model.Jenkins;
import hudson.model.Result;
import hudson.model.FreeStyleBuild;
import hudson.util.Secret;
import hudson.ExtensionList;
import hudson.security.ACL;
import org.jvnet.hudson.test.BuildWatcher;
import org.jvnet.hudson.test.RestartableJenkinsRule;

import java.io.File;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.internal.LinkedTreeMap;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.common.ConsoleNotifier;

import org.jenkinsci.plugins.puppetenterprise.models.HieraConfig;
import org.jenkinsci.plugins.puppetenterprise.TestUtils;


import org.apache.http.*;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.*;
import org.apache.http.client.entity.*;
import org.apache.http.entity.*;
import org.apache.http.client.methods.*;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.commons.io.IOUtils;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.BasicHttpContext;

public class HieraStepTest extends Assert {

  @ClassRule
  public static BuildWatcher buildWatcher = new BuildWatcher();

  @Rule
  public RestartableJenkinsRule story = new RestartableJenkinsRule();

  private LinkedTreeMap lookup(String scope, String key) {
    LinkedTreeMap responseHash = new LinkedTreeMap();

    try {
      String url = story.j.jenkins.getRootUrl() + "/hiera/lookup?scope=" + scope + "&key=" + key;
      URL urlObj = new URL(url);
      HttpGet httpGet = new HttpGet(url);
      HttpClient httpClient = new DefaultHttpClient();
      HttpContext localContext = new BasicHttpContext();
      HttpResponse response = httpClient.execute(httpGet, localContext);

      String json = IOUtils.toString(response.getEntity().getContent());
      if (response.getStatusLine().getStatusCode() != 404 && !json.isEmpty()) {
        Object responseBody = new Gson().fromJson(json, Object.class);
        responseHash = (LinkedTreeMap) responseBody;
      }
    } catch(java.net.MalformedURLException e) {
      fail(e.getMessage());
    } catch(java.io.IOException e) {
      fail(e.getMessage());
    }

    return responseHash;
  }

  @Test
  public void setKeyValuePairInExistingScope() throws Exception {

    story.addStep(new Statement() {
      @Override
      public void evaluate() throws Throwable {
        LinkedTreeMap response = new LinkedTreeMap();

        HieraConfig.deleteKey("testkey", "existing");
        response = lookup("existing", "testkey");
        assertNull( (String) response.get("testkey"));

        //Create a job where the Hiera key/value pair is set for existing scope
        WorkflowJob separateCredsJob = story.j.jenkins.createProject(WorkflowJob.class, "Set Hiera Key/Value pair in existing scope");
        separateCredsJob.setDefinition(new CpsFlowDefinition(
          "node { \n" +
          "  puppet.hiera scope: 'existing', key: 'testkey', value: 'newvalue'\n" +
          "}", true));
        story.j.assertBuildStatusSuccess(separateCredsJob.scheduleBuild2(0));

        //Verify key/pair exists through API call.
        response = lookup("existing", "testkey");
        assertEquals( (String) response.get("testkey"), "newvalue");

      }
    });
  }

  @Test
  public void setKeyValuePairInNewScope() throws Exception {

    story.addStep(new Statement() {
      @Override
      public void evaluate() throws Throwable {
        LinkedTreeMap response = new LinkedTreeMap();

        HieraConfig.deleteScope("new");
        response = lookup("new", "testvalue");
        assertNull((String) response.get("testkey"));

        //Create a job where Hiera key/value pair is set for new scope
        WorkflowJob separateCredsJob = story.j.jenkins.createProject(WorkflowJob.class, "Set Hiera Key/Value pair in new scope");
        separateCredsJob.setDefinition(new CpsFlowDefinition(
          "node { \n" +
          "  puppet.hiera scope: 'new', key: 'testkey', value: 'testvalue'\n" +
          "}", true));
        story.j.assertBuildStatusSuccess(separateCredsJob.scheduleBuild2(0));

        //Verify key/pair exists through API call.
        response = lookup("new", "testkey");
        assertEquals( (String) response.get("testkey"), "testvalue");
      }
    });
  }

  @Test
  public void setNewValueForExistingKey() throws Exception {

    story.addStep(new Statement() {
      @Override
      public void evaluate() throws Throwable {
        LinkedTreeMap response = new LinkedTreeMap();

        HieraConfig.setKeyValue("existing", "testkey", "Set Hiera Key/Value pair in existing scope", "oldvalue");
        response = lookup("existing", "testkey");
        assertEquals( (String) response.get("testkey"), "oldvalue");

        //Create a job where Hiera key/value pair is set for new scope
        WorkflowJob separateCredsJob = story.j.jenkins.createProject(WorkflowJob.class, "Set new Hiera value for existing key");
        separateCredsJob.setDefinition(new CpsFlowDefinition(
          "node { \n" +
          "  puppet.hiera scope: 'existing', key: 'testkey', value: 'newvalue'\n" +
          "}", true));
        story.j.assertBuildStatusSuccess(separateCredsJob.scheduleBuild2(0));

        //Verify key's value is correct through API call.
        response = lookup("existing", "testkey");
        assertEquals( (String) response.get("testkey"), "newvalue");
      }
    });
  }
}
