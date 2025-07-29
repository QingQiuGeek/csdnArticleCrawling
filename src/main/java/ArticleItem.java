import java.util.List;

/**
 * @author: QingQiu
 * @date: 2025/7/29 23:55
 * @description:
 */
class ArticleItem {

  private String href;
  private String title;
  private String desc;
  private String time;
  private String slug;    // 自定义 URL
  private List<String> tags; // 解析好的标签

  public ArticleItem(String desc, String href, String slug, List<String> tags, String time,
      String title) {
    this.desc = desc;
    this.href = href;
    this.slug = slug;
    this.tags = tags;
    this.time = time;
    this.title = title;
  }

  public String getSlug() {
    return slug;
  }

  public void setSlug(String slug) {
    this.slug = slug;
  }

  public List<String> getTags() {
    return tags;
  }

  public void setTags(List<String> tags) {
    this.tags = tags;
  }

  public ArticleItem(String href, String title, String desc, String time, String slug) {
    this.href = href;
    this.title = title;
    this.desc = desc;
    this.time = time;
    this.slug = slug;
  }

  public String getHref() {
    return href;
  }

  public String getTime() {
    return time;
  }

  public void setTime(String time) {
    this.time = time;
  }

  public void setHref(String href) {
    this.href = href;
  }

  public String getDesc() {
    return desc;
  }

  public void setDesc(String desc) {
    this.desc = desc;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  @Override
  public String toString() {
    return "ArticleItem{" +
        "href='" + href + '\'' +
        ", title='" + title + '\'' +
        ", summary='" + desc + '\'' +
        '}';
  }
}
