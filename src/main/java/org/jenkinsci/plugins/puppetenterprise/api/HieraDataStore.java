package org.jenkinsci.plugins.puppetenterprise.api;

import java.io.*;
import java.util.*;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import hudson.model.RootAction;
import hudson.Extension;
import org.json.*;
import javax.servlet.ServletException;
import org.kohsuke.stapler.bind.JavaScriptMethod;

import org.jenkinsci.plugins.puppetenterprise.models.HieraConfig;

@Extension
public class HieraDataStore implements RootAction {
  private static final String ICON_PATH = "/plugin/puppet-enterprise-pipeline/images/cfg_logo.png";

  public HieraDataStore() {
    HieraConfig.loadGlobalConfig();
  }

  public String[] getScopes() {
    Set<String> scopeSet = HieraConfig.getScopes();
    return scopeSet.toArray(new String[scopeSet.size()]);
  }

  public String[] getKeys(String scope) {
    //For some reason the Jenkins repeatable jelly tag
    // has a null value for the first element in any of its
    // lists, despite no methods in this class returning a
    // list with a null index. The only way I can find to
    // work around it is to detect a null request and
    // send back a null value.
    if (scope == null) {
      return null;
    }

    Set<String> keySet = HieraConfig.getKeys(scope);
    return keySet.toArray(new String[keySet.size()]);
  }

  public String getKeyValue(String scope, String key) {
    //For some reason the Jenkins repeatable jelly tag
    // has a null value for the first element in any of its
    // lists, despite no methods in this class returning a
    // list with a null index. The only way I can find to
    // work around it is to detect a null request and
    // send back a null value.
    if (scope == null || key == null) {
      return null;
    }

    Object value = HieraConfig.getKeyValue(scope, key);
    return value.toString();
  }

  public String getKeySource(String scope, String key) {
    //For some reason the Jenkins repeatable jelly tag
    // has a null value for the first element in any of its
    // lists, despite no methods in this class returning a
    // list with a null index. The only way I can find to
    // work around it is to detect a null request and
    // send back a null value.
    if (scope == null || key == null) {
      return null;
    }

    String source = HieraConfig.getKeySource(scope, key);
    return source;
  }

  @JavaScriptMethod
  public void deleteScope(String scope) {
    HieraConfig.deleteScope(scope);
  }

  @JavaScriptMethod
  public void deleteKey(String key, String scope) {
    HieraConfig.deleteKey(key, scope);
  }

  @Override
  public String getUrlName() {
    return "hiera";
  }

  @Override
  public String getDisplayName() {
    return "Hiera Data Lookup";
  }

  @Override
  public String getIconFileName() {
    return ICON_PATH;
  }

  public void doLookup(StaplerRequest req, StaplerResponse rsp) throws IOException {
    net.sf.json.JSONObject form = null;
    Map parameters = null;

    parameters = req.getParameterMap();

    String returnValue = "";
    String scopeArr[] = (String[]) parameters.get("scope");
    String scope = scopeArr[0];
    String keyArr[] = (String[]) parameters.get("key");
    String key = keyArr[0];

    Object value = HieraConfig.getKeyValue(scope, key);

    if (value == null) {
      rsp.setStatus(404);
      return;
    }

    rsp.setContentType("application/json;charset=UTF-8");
    rsp.getOutputStream().print(serializeResult(key, value));
  }

  private String serializeResult(String key, Object result) {
    HashMap hash = new HashMap();

    if (result instanceof String) {
      String valueString = (String) result;
      hash.put(key, valueString);
    } else if (result instanceof ArrayList) {
      ArrayList valueArray = (ArrayList) result;
      hash.put(key, valueArray);
    } else if (result instanceof HashMap) {
      LinkedHashMap valueHash = (LinkedHashMap) result;
      hash.put(key, valueHash);
    }

    return new JSONObject(hash).toString();
  }
}
