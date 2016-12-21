public class PuppetNodeMetricsV1 {
  private Integer total = 0;
  private Integer failed = 0;
  private Integer changed = 0;
  private Integer corrective_changed = 0;
  private Integer skipped = 0;
  private Integer restarted = 0;
  private Integer scheduled = 0;
  private Integer out_of_sync = 0;
  private Integer failed_to_restart = 0;

  public Integer getTotal() {
    return this.total;
  }

  public Integer getFailed() {
    return this.failed;
  }

  public Integer getChanged() {
    return this.changed;
  }

  public Integer getCorrectiveChanged() {
    return this.corrective_changed;
  }

  public Integer getSkipped() {
    return this.skipped;
  }

  public Integer getRestarted() {
    return this.restarted;
  }

  public Integer getScheduled() {
    return this.scheduled;
  }

  public Integer getOutOfSync() {
    return this.out_of_sync;
  }

  public Integer getFailedToRestart() {
    return this.failed_to_restart;
  }
}
