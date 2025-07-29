/**
 * @author: QingQiu
 * @date: 2025/7/29 23:55
 * @description:
 */ //请求https://www.helloworld.net/getUrlHtml?url=返回的json格式
class Json {

  int code;
  String title;

  public String getHtml() {
    return html;
  }

  public void setHtml(String html) {
    this.html = html;
  }

  public int getCode() {
    return code;
  }

  public void setCode(int code) {
    this.code = code;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  String html;
}
