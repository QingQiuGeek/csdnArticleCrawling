
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.http.HttpUtil;
import com.vladsch.flexmark.html2md.converter.FlexmarkHtmlConverter;
import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

/**
 * @author: QingQiu
 * @date: 2025/7/29 17:39
 * @description:
 */

public class Main {

  // 全局复用转换器，线程安全
  private static final FlexmarkHtmlConverter CONVERTER = FlexmarkHtmlConverter.builder().build();


  public static void main(String[] args) {
    //爬取的链接
    String url = "https://blog.csdn.net/qq_73181349/article/list/";
    //最大页数
    Integer pageMaxNum = 5;
    String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36";
    //文章存储目录
    String dir = "A:\\frontend\\hugo-theme-stack-master\\content\\post";
    //抓取
    List<String> list = fetchHtml(url, pageMaxNum , userAgent);
    System.out.println("成功抓取页数："+list.size());
    //解析
    System.out.println("开始抓取每页文章");
    List<ArticleItem> articleItemList = parse(list);
    System.out.println("抓取文章篇数："+articleItemList.size());
    System.out.println("开始解析文章并存储为md");
    saveAsMd(articleItemList,dir);
  }


  /**
   * 传入url及最大页数，返回该页面的完整 HTML 字符串列表
   */
  public static List<String> fetchHtml(String url,int pageNum,String userAgent) {
    String html = "";
    ArrayList<String> list = new ArrayList<>();
    try {
      for (int i = 1; i <= pageNum; i++) {
        System.out.println("开始抓取第"+i+"页");
//        String fetchUrl = url + i;
        html = Jsoup.connect(url+i)

            .userAgent(userAgent)
            // 15 秒超时
            .timeout(15_000)
            // 先拿到响应
            .execute()
            // 直接取 body（即 HTML）
            .body();
        System.out.println("第"+i+"页抓取成功");
        list.add(html);
//        System.out.println(html);
//        break;
      }
    }catch (Exception e){
      System.err.println("抓取失败：" + e.getMessage());
    }
    return list;
  }

  /**
   * 传入完整的 HTML 字符串，返回文章列表
   */
  public static List<ArticleItem> parse(List<String> htmls) {
    List<ArticleItem> list = new ArrayList<>();
    try{
      //解析每个html
      for (String html : htmls) {
        Document doc = Jsoup.parse(html);
        // 精准定位文章块
        Element articleList = doc.selectFirst("div.article-list");
        if (articleList == null) {
          return list;
        }
        // 每条文章都被包在 <div class="article-item-box ...">
        for (Element box : articleList.select("div.article-item-box")) {
          Element a   = box.selectFirst("h4 a");
          Element p   = box.selectFirst("p.content");
          Element t   = box.selectFirst("span.date");
          if (a == null) {
            continue;
          }
          String href    = a.attr("abs:href");
          // 去掉前缀“原创”
          String title   = a.text().replaceFirst("^原创\\s*", "");
          String summary = p != null ? p.text().trim() : "";
          String time = t.text().trim();
          ArticleItem articleItem = new ArticleItem(href, title, summary, time,title);
          list.add(articleItem);
          System.out.println(articleItem);
        }
      }
    }catch (Exception e){
      System.err.println("解析html失败：" + e.getMessage());
    }
    return list;
  }

  /**
   * 将 html 转换成 markdown 并写入磁盘
   * @param baseDir  绝对路径，如 /Users/xxx/articles
   */
  public static void saveAsMd(List<ArticleItem> items, String baseDir) {
    int i = 0;
   for (ArticleItem articleItem : items) {
     String html = null;
     try {
       ++i;
       System.out.println("解析第["+i+"]篇文章--->"+articleItem.toString());
       //抓取文章详情html
//        html = JSONUtil.parse(
//           HttpUtil.createGet("https://www.helloworld.net/getUrlHtml?url=" + articleItem.getHref())
//               .execute().body()).toBean(Json.class).getHtml();
       html = HttpUtil.createGet(articleItem.getHref()).execute().body();
       if (html == null || html.contains("请进行安全验证")) {
         System.out.println("解析失败："+articleItem);
         return;
       }
       Document doc = Jsoup.parse(html);

       Element articleDiv = doc.selectFirst("div#article_content.article_content.clearfix");
       //标签提取
       List<String> tags = doc.select("div.blog-tags-box a.tag-link-new, div.blog-tags-box a.tag-link")
           .stream()
           .map(e -> e.text().replaceFirst("^#", "")).collect(Collectors.toList());
       System.out.println("文章标签提取："+tags);
       articleItem.setTags(tags);
     // 1. html -> md
     System.out.println("转为md--->"+articleItem.toString());
     String tmp = "本文转自 <"+articleItem.getHref()+">，如有侵权，请联系删除。";
     String markdown = CONVERTER.convert(articleDiv).replace("{#content_views}","").replace(tmp,"");
     String yaml = getFormat(articleItem);
     String fullContent = "---\n" + yaml + "---\n\n" + markdown + "---\n\n";
       // 2. 文件名安全化
       String dirName = articleItem.getTitle()
           .replaceAll("[\\\\/:*?\"<>|]", "-")
           .trim();
     // 3. 目标文件
     System.out.println("存储md--->"+articleItem.toString());
       // 1. 解析年月
       Date date = DateUtil.parse(articleItem.getTime());
       String year  = DateUtil.format(date, "yyyy");
       String month = DateUtil.format(date, "MM");
     // 4. 自动创建父目录
       File dir = FileUtil.file(baseDir, year, month, dirName);
       // 自动级联创建文件及目录
       FileUtil.mkdir(dir);
       File mdFile = new File(dir, "index.md");
       // 5. 覆盖写入 UTF-8
     FileUtil.writeUtf8String(fullContent, mdFile);
     //抓取太快会触发验证导致后续抓取失败，所以这里没抓一篇休眠一会
     Thread.sleep(10000);
     }catch (Exception e){
       System.err.println("文章详情抓取失败：" + articleItem.getHref()+"，"+ e.getMessage());
     }
   }
  }

  /*
  * 获取文件头
  * 如：
  date: '2025-06-29 17:48:00'
  draft: false
  description: 本文对比了两种基于Redis的Token认证方案。方案一采用传统双拦截器设计，前端需自行检查Token有效期并加密存储于localStorage，后端通过LoginInterceptor和RefreshTokenInterceptor分别处理登录校验和Token刷新，存在前后端有效期同步问题。方案二使用Sa-Token框架，实现多端登录隔离和自动续期，通过单一SaTokenInterceptor即可完成认证，内置Account-Session和Token-Session机制支持多终端独立管理，简化了开发流程
  categories:
      - java
  title: 前后端分离场景下的用户登录玩法&Sa-token框架使用
  slug: 前后端分离场景下的用户登录玩法&Sa-token框架使用
  tags:
      - java
  ---*/
  public static String getFormat(ArticleItem item) {
    // 1. 时间格式统一成 2023-08-04T16:08:31+08:00
    String dump = null;
//    SnakeYAML 发现 categories 和 tags 指向 同一个 List 对象，于是生成别名 *id001，而你又通过 setAnchorGenerator(anchor -> null) 把锚点禁掉，它就报 “anchor is not specified for alias”。
    try {
//      String isoDate = DateUtil.parse(item.getTime())
//          .toString("yyyy-MM-dd HH:mm:ss+0000");
      // 2. 生成 YAML（独立列表，无锚点）
      DumperOptions options = new DumperOptions();
      options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
      options.setPrettyFlow(true);
      // 关闭锚点/别名生成
      options.setAllowReadOnlyProperties(false);
      options.setAnchorGenerator(anchor -> null);   // 关键：不生成锚点
      // 2. 构造 YAML Map
      Map<String, Object> map = new HashMap<>();
      map.put("title",       item.getTitle());
      map.put("description", item.getDesc());
      map.put("draft",       false);
      map.put("slug",        item.getSlug());
      map.put("date",        item.getTime());
//    map.put("image",       "cover.jpg");
      map.put("categories",  new ArrayList<>(item.getTags()));
      map.put("tags",        new ArrayList<>(item.getTags()));
       dump = new Yaml(options).dump(map);
    }catch (Exception e){
      System.out.println("yaml转换失败："+e.getMessage());
    }
    return dump;
  }


}
