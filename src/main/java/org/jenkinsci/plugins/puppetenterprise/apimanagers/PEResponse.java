package org.jenkinsci.plugins.puppetenterprise.apimanagers;

import java.io.*;
import java.util.*;

public class PEResponse {
  private String json = null;
  private Integer code = null;
  private Object body = null;

  public PEResponse(Object body, Integer code, String json) {
    this.json = json;
    this.body = body;
    this.code = code;
  }

  public PEResponse(Object body, Integer code) {
    this.body = body;
    this.code = code;
  }

  public String getJSON() {
    return this.json;
  }

  public Integer getResponseCode() {
    return this.code;
  }

  public Object getResponseBody() {
    return this.body;
  }
}
