package org.jenkinsci.plugins.puppetenterprise.api;

import java.net.*;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.model.Statement;
import org.junit.rules.ExpectedException;
import static org.junit.Assert.*;
import org.jvnet.hudson.test.RestartableJenkinsRule;
import org.jvnet.hudson.test.JenkinsRule;
import org.jenkinsci.plugins.puppetenterprise.models.HieraConfig;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.internal.LinkedTreeMap;
import org.apache.http.*;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.*;
import org.apache.http.client.entity.*;
import org.apache.http.entity.*;
import org.apache.http.client.methods.*;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.commons.io.IOUtils;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.BasicHttpContext;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlButton;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;
import com.gargoylesoftware.htmlunit.html.HtmlSubmitInput;
import com.gargoylesoftware.htmlunit.html.HtmlPasswordInput;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.DefaultCredentialsProvider;
import org.acegisecurity.Authentication;
import jenkins.model.Jenkins;
import hudson.model.User;
import hudson.security.LegacyAuthorizationStrategy;
import hudson.security.HudsonPrivateSecurityRealm;
import hudson.security.FullControlOnceLoggedInAuthorizationStrategy;
import hudson.security.GlobalMatrixAuthorizationStrategy;

public class HieraDataStoreTest extends Assert {

  class HTTPUnauthorized extends Exception {}

  @Rule
  public final ExpectedException exception = ExpectedException.none();

  @Rule
  public RestartableJenkinsRule story = new RestartableJenkinsRule();

  private LinkedTreeMap lookup(String scope, String key, String user, String password) throws HTTPUnauthorized {
    LinkedTreeMap responseHash = new LinkedTreeMap();

    try {
      String url = story.j.jenkins.getRootUrl() + "/hiera/lookup?scope=" + scope + "&key=" + key;
      URL urlObj = new URL(url);
      HttpGet httpGet = new HttpGet(url);
      HttpClient httpClient = new DefaultHttpClient();
      HttpContext localContext = new BasicHttpContext();

      if (user != null && password != null) {
        byte[] credentials = Base64.encodeBase64((user + ":" + password).getBytes());
        httpGet.setHeader("Authorization", "Basic " + new String(credentials));
      }

      HttpResponse response = httpClient.execute(httpGet, localContext);
      String json = IOUtils.toString(response.getEntity().getContent());

      if (response.getStatusLine().getStatusCode() == 403) {
        throw new HTTPUnauthorized();
      }

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
  public void unauthorizedReadOnlyRequestsCannotDeleteKeys() throws Exception {
    story.addStep(new Statement() {
      @Override
      public void evaluate() throws Throwable {
        HtmlPage hieraDataStorePage = null;

        try {
          story.j.jenkins.setSecurityRealm(story.j.createDummySecurityRealm());
          story.j.jenkins.setAuthorizationStrategy(new LegacyAuthorizationStrategy());
        } catch(java.lang.NullPointerException e) { e.printStackTrace(); }

        HieraConfig.setKeyValue("testscope", "testkey", "Hiera Data Store Test", "testvalue");

        try {
          WebClient client = new WebClient();
          client.setCssEnabled(false);
          client.setThrowExceptionOnScriptError(false);
          client.setJavaScriptEnabled(false);
          hieraDataStorePage = (HtmlPage) client.getPage(story.j.jenkins.getRootUrl() + "/hiera/");
        } catch(com.gargoylesoftware.htmlunit.ScriptException e) { }
          catch(java.lang.NullPointerException e) { e.printStackTrace(); }

        HtmlElement removeScopeLink = (HtmlElement) hieraDataStorePage.getElementById("testscope-remove-button");
        assertNull(removeScopeLink);

        HtmlElement removeKeyLink = (HtmlElement) hieraDataStorePage.getElementById("testscope-testkey-remove-button");
        assertNull(removeKeyLink);
      }
    });
  }

  @Test
  public void authorizedRequestsCanDeleteKeys() throws Exception {
    story.addStep(new Statement() {
      @Override
      public void evaluate() throws Throwable {
        HtmlPage hieraDataStorePage = null;
        WebClient client = new WebClient();
        HudsonPrivateSecurityRealm realm = new HudsonPrivateSecurityRealm(false);

        realm.createAccount("casey", "password");
        story.j.jenkins.setSecurityRealm(realm);
        story.j.jenkins.setAuthorizationStrategy(new FullControlOnceLoggedInAuthorizationStrategy());

        byte[] credentials = Base64.encodeBase64(("casey:password").getBytes());

        client.setCssEnabled(false);
        client.setThrowExceptionOnScriptError(false);
        client.setJavaScriptEnabled(false);
        client.addRequestHeader("Authorization", "Basic " + new String(credentials));

        hieraDataStorePage = (HtmlPage) client.getPage(story.j.jenkins.getRootUrl() + "/hiera/");

        HieraConfig.setKeyValue("testscope", "testkey", "Hiera Data Store Test", "testvalue");

        HtmlElement removeScopeLink = (HtmlElement) hieraDataStorePage.getElementById("testscope-remove-button");
        assertNotNull(removeScopeLink);

        HtmlElement removeKeyLink = (HtmlElement) hieraDataStorePage.getElementById("testscope-testkey-remove-button");
        assertNotNull(removeKeyLink);
      }
    });
  }

  @Test
  public void lookupAuthenticatedRequestsWithLOOKUPPermissionSucceeds() throws Exception {

    story.addStep(new Statement() {
      @Override
      public void evaluate() throws Throwable {
        LinkedTreeMap response = new LinkedTreeMap();
        GlobalMatrixAuthorizationStrategy authorizationStrategy = new GlobalMatrixAuthorizationStrategy();
        HudsonPrivateSecurityRealm realm = new HudsonPrivateSecurityRealm(false);

        realm.createAccount("casey", "password");

        story.j.jenkins.setSecurityRealm(realm);
        story.j.jenkins.setAuthorizationStrategy(authorizationStrategy);
        authorizationStrategy.add(HieraDataStore.LOOKUP, "casey");
        authorizationStrategy.add(Jenkins.READ, "casey");
        HieraConfig.setKeyValue("testscope", "testkey", "Lookup Authenticated Request With Hiera/Lookup Permission Succeeds", "testvalue");

        response = lookup("testscope", "testkey", "casey", "password");
        assertEquals( (String) response.get("testkey"), "testvalue");
      }
    });
  }

  @Test
  public void lookupAuthenticatedRequestsWithoutLOOKUPPermissionFails() throws Exception {

    story.addStep(new Statement() {
      @Override
      public void evaluate() throws Throwable {
        LinkedTreeMap response = new LinkedTreeMap();
        GlobalMatrixAuthorizationStrategy authorizationStrategy = new GlobalMatrixAuthorizationStrategy();
        HudsonPrivateSecurityRealm realm = new HudsonPrivateSecurityRealm(false);

        realm.createAccount("don", "password");

        story.j.jenkins.setSecurityRealm(realm);
        story.j.jenkins.setAuthorizationStrategy(authorizationStrategy);
        authorizationStrategy.add(Jenkins.READ, "don");
        HieraConfig.setKeyValue("testscope", "testkey", "Lookup Authenticated Request Without Hiera/Lookup Permission Fails", "testvalue");

        exception.expect(HTTPUnauthorized.class);
        response = lookup("testscope", "testkey", "don", "password");
      }
    });
  }

  @Test
  public void lookupUnauthenticatedRequestsFails() throws Exception {

    story.addStep(new Statement() {
      @Override
      public void evaluate() throws Throwable {
        LinkedTreeMap response = new LinkedTreeMap();
        GlobalMatrixAuthorizationStrategy authorizationStrategy = new GlobalMatrixAuthorizationStrategy();
        HudsonPrivateSecurityRealm realm = new HudsonPrivateSecurityRealm(false);

        story.j.jenkins.setSecurityRealm(realm);
        story.j.jenkins.setAuthorizationStrategy(authorizationStrategy);
        HieraConfig.setKeyValue("testscope", "testkey", "Lookup Unauthenticated Request Fails", "testvalue");

        exception.expect(HTTPUnauthorized.class);
        response = lookup("testscope", "testkey", null, null);
      }
    });
  }
}
