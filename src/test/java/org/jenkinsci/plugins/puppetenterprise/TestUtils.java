package org.jenkinsci.plugins.puppetenterprise;

import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileNotFoundException;
import org.apache.commons.lang.StringUtils;

public final class TestUtils {

  private static String RESOURCESBASEPATH = "src/test/java/resources/";
  private static String CACERTPATH = RESOURCESBASEPATH + "certs/ca.cert.pem";
  private static String APIRESPONSESBASEPATH = RESOURCESBASEPATH + "api-responses/";
  private static String KEYSTOREPATH = RESOURCESBASEPATH + "server.keystore";
  private static String KEYSTOREPASSWORD = "password";

  private TestUtils() { }

  public static String getFileContents(String file) {
    BufferedReader br = null;

    try {
      br = new BufferedReader(new FileReader(file));
    } catch(java.io.FileNotFoundException e) {
      e.printStackTrace();
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
      e.printStackTrace();
    } finally {
      try {
        br.close();
      } catch(java.io.IOException e){
        e.printStackTrace();
      }
    }

    return fileContents;
  }

  public static String getCACertificateString() {
    return getFileContents(CACERTPATH);
  }

  public static String getKeystorePath() {
    return KEYSTOREPATH;
  }

  public static String getKeystorePassword() {
    return KEYSTOREPASSWORD;
  }

  public static String getResourcesBasePath() {
    return RESOURCESBASEPATH;
  }

  public static String getAPIResonsesBasesPath() {
    return APIRESPONSESBASEPATH;
  }
}
