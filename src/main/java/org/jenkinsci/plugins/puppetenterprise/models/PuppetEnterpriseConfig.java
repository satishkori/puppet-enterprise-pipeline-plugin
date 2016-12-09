package org.jenkinsci.plugins.puppetenterprise.models;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import hudson.security.ACL;
import hudson.XmlFile;
import hudson.model.Saveable;
import jenkins.model.Jenkins;
import java.io.Serializable;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.net.Socket;
import java.util.HashMap;

import org.apache.http.*;
import org.apache.http.util.ExceptionUtils;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.*;
import org.apache.http.client.entity.*;
import org.apache.http.entity.*;
import org.apache.http.client.methods.*;
import org.apache.http.impl.client.*;
import org.apache.http.conn.ssl.*;
import org.apache.commons.io.IOUtils;

public final class PuppetEnterpriseConfig implements Serializable {
  private static String puppetMasterUrl = null;
  private static String puppetMasterCACertificate = "";

  private PuppetEnterpriseConfig() {
    loadGlobalConfig();
  }

  public static void validatePuppetMasterUrl(String url) throws IOException, java.net.UnknownHostException,
    java.security.NoSuchAlgorithmException, java.security.KeyStoreException, java.security.KeyManagementException, org.apache.http.conn.HttpHostConnectException  {
    //this makes a connection to the master, so if the connection fails, the given address is invalid
    retrievePuppetMasterCACertificate(url);
  }

  public static void setPuppetMasterUrl(String url) throws IOException, java.net.UnknownHostException,
    java.security.NoSuchAlgorithmException, java.security.KeyStoreException, java.security.KeyManagementException, org.apache.http.conn.HttpHostConnectException  {
    puppetMasterUrl = url;

    if(puppetMasterCACertificate == null || puppetMasterCACertificate.isEmpty()) {
      puppetMasterCACertificate = retrievePuppetMasterCACertificate();
    }

    save();
  }

  public static void setPuppetMasterCACertificate(String cert) {
    puppetMasterCACertificate = cert;

    try {
      save();
    } catch(IOException e) {e.printStackTrace();}
  }

  public static String getPuppetMasterCACertificate() {
    return puppetMasterCACertificate;
  }

  private static String retrievePuppetMasterCACertificate() throws java.net.UnknownHostException, IOException,
    java.security.NoSuchAlgorithmException, java.security.KeyStoreException, java.security.KeyManagementException, org.apache.http.conn.HttpHostConnectException {

    return retrievePuppetMasterCACertificate(puppetMasterUrl);
  }

  private static String retrievePuppetMasterCACertificate(String address) throws java.net.UnknownHostException, IOException,
    java.security.NoSuchAlgorithmException, java.security.KeyStoreException, java.security.KeyManagementException, org.apache.http.conn.HttpHostConnectException {
    String returnString = "";

    SSLContextBuilder builder = new SSLContextBuilder();

    builder.loadTrustMaterial(null, new TrustSelfSignedStrategy());

    SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(builder.build());
    CloseableHttpClient httpclient = HttpClients.custom().setSSLSocketFactory(sslsf).build();

    HttpGet httpGet = new HttpGet("https://" + address + ":8140/puppet-ca/v1/certificate/ca");
    returnString = IOUtils.toString(httpclient.execute(httpGet).getEntity().getContent());

    return returnString;
  }

  public static void loadGlobalConfig() {
    try {
      HashMap config = new HashMap();

      XmlFile xml = getConfigFile();
      if (xml.exists()) {
        config = (HashMap) xml.unmarshal(config);

        puppetMasterUrl = (String) config.get("puppetMasterUrl");
        puppetMasterCACertificate = (String) config.get("puppetMasterCACertificate");
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @SuppressFBWarnings(
    value = "SBSC_USE_STRINGBUFFER_CONCATENATION",
    justification = "performance irrelevant compared to I/O, String much more convenient"
  )
  public static String getPuppetMasterUrl() {
    InputStreamReader is = null;
    BufferedReader br = null;
    Process puppetCmd = null;

    if (!puppetMasterUrl.equals("")) {
      return puppetMasterUrl;
    } else {
      String puppetConfigPath = "/etc/puppetlabs/puppet/puppet.conf";
      File puppetFileHandler = new File(puppetConfigPath);
      try {
        if (puppetFileHandler.exists()) {
          String cmd = "/opt/puppetlabs/bin/puppet config print server --config /etc/puppetlabs/puppet/puppet.conf";

          puppetCmd = Runtime.getRuntime().exec(cmd);
          puppetCmd.waitFor();
          is = new InputStreamReader(puppetCmd.getInputStream(), "UTF-8");
          br = new BufferedReader(is);

          String line = "";

          StringBuffer buf = new StringBuffer();
          while ((line = br.readLine()) != null) {
            buf.append(line);
          }

          is.close();
          br.close();
          puppetCmd.destroy();

          puppetMasterUrl = buf.toString();
        } else {
          puppetMasterUrl = "https://puppet";
        }

        setPuppetMasterUrl(puppetMasterUrl);

      //If we fail here, it's the same as not having a config
      } catch(java.io.IOException e) {
      } catch(java.security.NoSuchAlgorithmException e) {
      } catch(java.security.KeyStoreException e) {
      } catch(java.security.KeyManagementException e) {
      } catch(InterruptedException e) {
      } finally {
        if (is != null) {
          try {
            is.close();
          } catch(IOException e) {
            e.printStackTrace();
          }
        }

        if (br != null) {
          try {
            br.close();
          } catch(IOException e) {
            e.printStackTrace();
          }
        }

        if (puppetCmd != null) {
          puppetCmd.destroy();
        }
      }
    }

    return PuppetEnterpriseConfig.puppetMasterUrl;
  }

  public static void save() throws IOException {
    HashMap config = new HashMap();
    config.put("puppetMasterUrl", puppetMasterUrl);
    config.put("puppetMasterCACertificate", puppetMasterCACertificate);

    getConfigFile().write(config);
  }

  @SuppressFBWarnings(
    value = "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE",
    justification = "The values are asserted to not be null, but findbugs doesn't know that."
  )
  public static XmlFile getConfigFile() {
    File pe_config_file = new File(Jenkins.getInstance().getRootDir(), "puppet_enterprise.xml");
    assert pe_config_file != null;

    XmlFile pe_config_xml = new XmlFile(pe_config_file);
    assert pe_config_xml != null;

    return pe_config_xml;
  }
}
