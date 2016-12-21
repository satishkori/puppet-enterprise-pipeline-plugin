public class PuppetNodeItemV1 {
  private DateTime timestamp = new DateTime();
  private String state = "";
  private String transaction_uuid = "";
  private String name = "";
  private PuppetNodeItemDetails details  = null;
  private String message = "";

  public String getState() {
    return this.state;
  }

  public DateTime getTimeStamp() {
    return this.timestamp;
  }

  public String getTransactionUUID() {
    return this.transaction_uuid;
  }

  public String getName() {
    return this.name;
  }

  public String getMessage() {
    return this.message;
  }
  
  public String getReportURL() {
    return details.getReportURL();
  }

  public String getMetrics() {
    return details.getMetrics();
  }

  class PuppetNodeItemDetails {
    private URL report_url = new URL();
    private PuppetNodeMetricsV1 metrics = null;

    public URL getReportURL() {
      return this.report_url;
    }

    public PuppetNodeMetricsV1 getMetrics() {
      return metrics;
    }
  }
}
